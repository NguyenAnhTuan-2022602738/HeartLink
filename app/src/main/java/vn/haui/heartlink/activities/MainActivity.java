package vn.haui.heartlink.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;
import vn.haui.heartlink.Constants;
import vn.haui.heartlink.R;
import vn.haui.heartlink.fragments.DiscoveryFragment;
import vn.haui.heartlink.fragments.MatchesFragment;
import vn.haui.heartlink.fragments.MessagesFragment;
import vn.haui.heartlink.fragments.ProfileFragment;
import vn.haui.heartlink.models.Interaction;
import vn.haui.heartlink.models.User;
import vn.haui.heartlink.utils.MatchRepository;
import vn.haui.heartlink.utils.UserRepository;

public class MainActivity extends AppCompatActivity implements DiscoveryFragment.NavigationListener, ProfileFragment.ProfileInteractionListener {

    private View discoverTab, matchesTab, messagesTab, profileTab;
    private ImageView discoverIndicator, matchesIndicator, messagesIndicator, profileIndicator;

    private final DiscoveryFragment discoveryFragment = new DiscoveryFragment();
    private final MessagesFragment messagesFragment = new MessagesFragment();
    private final Fragment matchesFragment = new MatchesFragment();
    private final ProfileFragment profileFragment = new ProfileFragment();
    private final FragmentManager fm = getSupportFragmentManager();
    private Fragment activeFragment = discoveryFragment;

    private ValueEventListener inAppNotificationListener;
    private long listenerStartTime;
    private final Set<String> selfInitiatedMatches = new HashSet<>();

    private final ActivityResultLauncher<Intent> locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    selectTab(profileTab);
                    profileFragment.refreshProfile();
                    discoveryFragment.forceRefresh();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        WindowInsetsControllerCompat controller = ViewCompat.getWindowInsetsController(root);
        if (controller != null) {
            controller.setAppearanceLightStatusBars(true);
            controller.setAppearanceLightNavigationBars(true);
        }

        bindViews();
        setupNavigation();
        setupFragments();
        setupPresenceHandling();
        setupInAppNotificationListener();

        handleIntent(getIntent());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (inAppNotificationListener != null && currentUser != null) {
            MatchRepository.getInstance().removeInteractionsListener(currentUser.getUid(), inAppNotificationListener);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;

        String navigateTo = intent.getStringExtra("NAVIGATE_TO");
        if ("MESSAGES".equals(navigateTo)) {
            selectTab(messagesTab);
            String chatWithUserId = intent.getStringExtra("CHAT_WITH_USER_ID");
            if (chatWithUserId != null) {
                messagesFragment.openChatWithUser(chatWithUserId);
            }
        }
    }

    private void setupInAppNotificationListener() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        listenerStartTime = System.currentTimeMillis(); // Reset listener start time on setup

        inAppNotificationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Interaction> newLikes = new ArrayList<>();
                Interaction latestMatch = null;
                long latestMatchTime = 0;

                for (DataSnapshot child : snapshot.getChildren()) {
                    Interaction interaction = child.getValue(Interaction.class);
                    String partnerId = child.getKey();

                    if (interaction == null || TextUtils.isEmpty(partnerId)) {
                        continue;
                    }

                    // Filter out self-initiated matches for notification display
                    if (MatchRepository.STATUS_MATCHED.equals(interaction.getStatus())) {
                        if (selfInitiatedMatches.contains(partnerId)) {
                            selfInitiatedMatches.remove(partnerId); // Clean up the set
                            continue; // This user initiated the match, so don't show a notification
                        }

                        // For a match not initiated by current user, consider it for latest match notification
                        // Only show matches that happened after listener started (new matches)
                        if (interaction.getMatchedAt() != null
                            && interaction.getMatchedAt() > listenerStartTime
                            && interaction.getMatchedAt() > latestMatchTime) {
                            latestMatchTime = interaction.getMatchedAt();
                            latestMatch = interaction;
                            latestMatch.setPartnerId(partnerId); // Set partnerId for later use
                        }
                    } else if (MatchRepository.STATUS_RECEIVED_LIKE.equals(interaction.getStatus())) {
                        // Only consider new likes (after listener started) that haven't been matched yet
                        // Removed the `interaction.getLikedAt() != null` check as getLikedAt() returns primitive long
                        if (interaction.getLikedAt() > listenerStartTime && !selfInitiatedMatches.contains(partnerId)) {
                            newLikes.add(interaction);
                            newLikes.get(newLikes.size() - 1).setPartnerId(partnerId); // Set partnerId
                        }
                    }
                }

                // Prioritize match notification over like notification
                if (latestMatch != null) {
                    fetchUserAndShowNotification(latestMatch.getPartnerId(), "đã ghép đôi với bạn!", "Nhắn tin");
                } else if (!newLikes.isEmpty()) {
                    // Sort new likes to get the latest one
                    Collections.sort(newLikes, (o1, o2) -> Long.compare(o2.getLikedAt(), o1.getLikedAt()));
                    Interaction latestLike = newLikes.get(0);

                    String message;
                    if (newLikes.size() > 1) {
                        message = String.format("và %d người khác đã thích bạn", newLikes.size() - 1);
                    } else {
                        message = "đã thích bạn";
                    }
                    fetchUserAndShowNotification(latestLike.getPartnerId(), message, "Xem hồ sơ");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Log error if needed
            }
        };
        MatchRepository.getInstance().addInteractionsListener(currentUser.getUid(), inAppNotificationListener);
    }

    private void fetchUserAndShowNotification(String userId, String message, String buttonText) {
        if (TextUtils.isEmpty(userId)) return;
        UserRepository.getInstance().getUserData(userId).addOnSuccessListener(snapshot -> {
            User user = snapshot.getValue(User.class);
            if (user != null) {
                showInAppNotification(user, message, buttonText);
            }
        });
    }

    private void showInAppNotification(User user, String message, String buttonText) {
        if (isFinishing() || isDestroyed()) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_in_app_notification, null);
        builder.setView(dialogView);

        CircleImageView userAvatar = dialogView.findViewById(R.id.user_avatar);
        TextView notificationTitle = dialogView.findViewById(R.id.notification_title);
        TextView notificationMessage = dialogView.findViewById(R.id.notification_message);
        Button actionButton = dialogView.findViewById(R.id.action_button);
        TextView dismissButton = dialogView.findViewById(R.id.dismiss_button);

        if (user.getPhotoUrls() != null && !user.getPhotoUrls().isEmpty()) {
            Glide.with(this).load(user.getPhotoUrls().get(0)).into(userAvatar);
        }

        notificationTitle.setText(buttonText.equals("Nhắn tin") ? "Kết đôi mới!" : "Lượt thích mới!");
        notificationMessage.setText(user.getName() + " " + message);
        actionButton.setText(buttonText);

        AlertDialog dialog = builder.create();

        actionButton.setOnClickListener(v -> {
            if ("Xem hồ sơ".equals(buttonText)) {
                Intent intent = new Intent(this, ProfileDetailActivity.class);
                intent.putExtra(Constants.EXTRA_USER_ID, user.getUid());
                startActivity(intent);
            } else if ("Nhắn tin".equals(buttonText)) {
                selectTab(messagesTab);
                messagesFragment.openChatWithUser(user.getUid());
            }
            dialog.dismiss();
        });
        dismissButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    public void onMatchCreatedByUser(String partnerId) {
        selfInitiatedMatches.add(partnerId);
    }

    private void bindViews() {
        discoverTab = findViewById(R.id.home_nav_discover);
        matchesTab = findViewById(R.id.home_nav_matches);
        messagesTab = findViewById(R.id.home_nav_messages);
        profileTab = findViewById(R.id.home_nav_profile);

        discoverIndicator = findViewById(R.id.home_nav_discover_indicator);
        matchesIndicator = findViewById(R.id.home_nav_matches_indicator);
        messagesIndicator = findViewById(R.id.home_nav_messages_indicator);
        profileIndicator = findViewById(R.id.home_nav_profile_indicator);
    }

    private void setupNavigation() {
        discoverTab.setOnClickListener(v -> selectTab(discoverTab));
        matchesTab.setOnClickListener(v -> selectTab(matchesTab));
        messagesTab.setOnClickListener(v -> selectTab(messagesTab));
        profileTab.setOnClickListener(v -> selectTab(profileTab));
    }

    private void setupFragments() {
        fm.beginTransaction().add(R.id.fragment_container, profileFragment, "4").hide(profileFragment).commit();
        fm.beginTransaction().add(R.id.fragment_container, messagesFragment, "3").hide(messagesFragment).commit();
        fm.beginTransaction().add(R.id.fragment_container, matchesFragment, "2").hide(matchesFragment).commit();
        fm.beginTransaction().add(R.id.fragment_container, discoveryFragment, "1").commit();

        selectTab(discoverTab);
    }

    public void selectTab(View tab) {
        discoverTab.setSelected(false);
        matchesTab.setSelected(false);
        messagesTab.setSelected(false);
        profileTab.setSelected(false);

        discoverIndicator.setVisibility(View.INVISIBLE);
        matchesIndicator.setVisibility(View.INVISIBLE);
        messagesIndicator.setVisibility(View.INVISIBLE);
        profileIndicator.setVisibility(View.INVISIBLE);

        tab.setSelected(true);

        FragmentTransaction transaction = fm.beginTransaction();
        if (tab.getId() == R.id.home_nav_discover) {
            discoverIndicator.setVisibility(View.VISIBLE);
            transaction.hide(activeFragment).show(discoveryFragment).commit();
            activeFragment = discoveryFragment;
        } else if (tab.getId() == R.id.home_nav_matches) {
            matchesIndicator.setVisibility(View.VISIBLE);
            transaction.hide(activeFragment).show(matchesFragment).commit();
            activeFragment = matchesFragment;
        } else if (tab.getId() == R.id.home_nav_messages) {
            messagesIndicator.setVisibility(View.VISIBLE);
            transaction.hide(activeFragment).show(messagesFragment).commit();
            activeFragment = messagesFragment;
        } else if (tab.getId() == R.id.home_nav_profile) {
            profileIndicator.setVisibility(View.VISIBLE);
            transaction.hide(activeFragment).show(profileFragment).commit();
            activeFragment = profileFragment;
        }
    }

    @Override
    public void onLaunchLocationPermission() {
        Intent intent = new Intent(this, LocationPermissionActivity.class);
        intent.putExtra("IS_EDIT_MODE", true);
        locationPermissionLauncher.launch(intent);
    }

    @Override
    public void onNavigateToMatches() {
        selectTab(matchesTab);
    }

    @Override
    public void onNavigateToMessages() {
        selectTab(messagesTab);
    }

    private void setupPresenceHandling() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            return; // No user logged in
        }
        String uid = firebaseUser.getUid();
        DatabaseReference userStatusRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");

        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class) != null && snapshot.getValue(Boolean.class);
                if (connected) {
                    userStatusRef.child("online").setValue(true);
                    userStatusRef.child("online").onDisconnect().setValue(false);
                    userStatusRef.child("lastSeen").onDisconnect().setValue(ServerValue.TIMESTAMP);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Log error if needed
            }
        });
    }
}

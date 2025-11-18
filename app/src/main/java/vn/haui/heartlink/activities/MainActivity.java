package vn.haui.heartlink.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import vn.haui.heartlink.R;
import vn.haui.heartlink.fragments.DiscoveryFragment;
import vn.haui.heartlink.fragments.MatchesFragment;
import vn.haui.heartlink.fragments.MessagesFragment;
import vn.haui.heartlink.fragments.NavigationListener;
import vn.haui.heartlink.fragments.ProfileFragment;

public class MainActivity extends AppCompatActivity implements NavigationListener, ProfileFragment.ProfileInteractionListener {

    private View discoverTab, matchesTab, messagesTab, profileTab;
    private ImageView discoverIndicator, matchesIndicator, messagesIndicator, profileIndicator;

    private final DiscoveryFragment discoveryFragment = new DiscoveryFragment(); // Changed to specific type
    private final Fragment matchesFragment = new MatchesFragment();
    private final Fragment messagesFragment = new MessagesFragment();
    private final ProfileFragment profileFragment = new ProfileFragment(); // Changed to specific type
    private final FragmentManager fm = getSupportFragmentManager();
    private Fragment activeFragment = discoveryFragment;

    private final ActivityResultLauncher<Intent> locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // First, switch to the profile tab
                    selectTab(profileTab);
                    // Then, refresh the profile
                    profileFragment.refreshProfile();
                    // Also refresh the discovery fragment
                    discoveryFragment.forceRefresh();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
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

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.hasExtra("NAVIGATE_TO")) {
            String destination = intent.getStringExtra("NAVIGATE_TO");
            if ("MATCHES".equals(destination)) {
                selectTab(matchesTab);
            }
        }
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

        selectTab(discoverTab); // Set initial tab
    }

    public void selectTab(View tab) {
        // Deselect all tabs visually
        discoverTab.setSelected(false);
        matchesTab.setSelected(false);
        messagesTab.setSelected(false);
        profileTab.setSelected(false);

        discoverIndicator.setVisibility(View.INVISIBLE);
        matchesIndicator.setVisibility(View.INVISIBLE);
        messagesIndicator.setVisibility(View.INVISIBLE);
        profileIndicator.setVisibility(View.INVISIBLE);

        // Select the clicked tab visually
        tab.setSelected(true);

        // Switch fragments
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
    public void navigateToTab(String tabName) {
        if ("PROFILE".equals(tabName)) {
            selectTab(profileTab);
        }
    }

    @Override
    public void onLaunchLocationPermission() {
        Intent intent = new Intent(this, LocationPermissionActivity.class);
        intent.putExtra("IS_EDIT_MODE", true);
        locationPermissionLauncher.launch(intent);
    }

    private void setupPresenceHandling() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            // Handle user not logged in, maybe redirect to LoginActivity
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        String uid = firebaseUser.getUid();
        DatabaseReference userStatusRef = FirebaseDatabase.getInstance().getReference("/users/" + uid);
        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");

        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class) != null && snapshot.getValue(Boolean.class);
                if (connected) {
                    userStatusRef.child("online").setValue(true);
                    userStatusRef.child("lastSeen").setValue(ServerValue.TIMESTAMP);
                    userStatusRef.child("online").onDisconnect().setValue(false);
                    userStatusRef.child("lastSeen").onDisconnect().setValue(ServerValue.TIMESTAMP);
                } else {
                    // This part is handled by onDisconnect()
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Log error
            }
        });
    }
}
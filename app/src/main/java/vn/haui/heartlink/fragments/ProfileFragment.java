package vn.haui.heartlink.fragments;

import androidx.appcompat.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch; // Import MaterialSwitch
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vn.haui.heartlink.R;
import vn.haui.heartlink.activities.GenderSelectionActivity;
import vn.haui.heartlink.activities.InterestsActivity;
import vn.haui.heartlink.activities.NotificationPermissionActivity;
import vn.haui.heartlink.activities.PhotoUploadActivity;
import vn.haui.heartlink.activities.ProfileInfoActivity;
import vn.haui.heartlink.activities.SeekingActivity;
import vn.haui.heartlink.activities.WelcomeActivity;
import vn.haui.heartlink.adapters.ProfilePhotoAdapter;
import vn.haui.heartlink.models.User;
import vn.haui.heartlink.utils.MatchRepository;
import vn.haui.heartlink.utils.UserRepository;

public class ProfileFragment extends Fragment {

    public interface ProfileInteractionListener {
        void onLaunchLocationPermission();
    }

    private ProfileInteractionListener mListener;

    private static final SimpleDateFormat DOB_FORMATTER = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private static final String PREFS_NAME = "HeartLinkPrefs";
    private static final String KEY_DARK_MODE = "darkModeEnabled";
    private static final String KEY_LANGUAGE = "language";

    private final UserRepository userRepository = UserRepository.getInstance();
    private final MatchRepository matchRepository = MatchRepository.getInstance();
    private ExecutorService geocoderExecutor;

    private View rootView;
    private NestedScrollView scrollView;
    private ProgressBar loadingView;
    private ImageView avatarView;
    private ImageView cameraBadgeView;
    private TextView nameView;
    private TextView taglineView;
    private TextView locationValueView;
    private ImageButton editLocationButton;
    private TextView interestsEmptyView;
    private TextView photosEmptyView;
    private TextView likesCountView;
    private TextView matchesCountView;
    private TextView superLikesCountView;
    private MaterialButton editButton;
    private TextView editInterestsAction;
    private TextView addPhotoAction;
    private ChipGroup interestsGroup;
    private RecyclerView photosRecyclerView;
    private final ProfilePhotoAdapter photoAdapter = new ProfilePhotoAdapter();

    private TextView genderValueView;
    private ImageButton editGenderButton;
    private TextView seekingValueView;
    private ImageButton editSeekingButton;
    private TextView notificationStatusView;
    private ImageButton editNotificationButton;
    private MaterialSwitch darkModeSwitch;
    private MaterialSwitch languageSwitch;

    @Nullable
    private FirebaseUser firebaseUser;
    private ValueEventListener interactionsListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ProfileInteractionListener) {
            mListener = (ProfileInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement ProfileInteractionListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        geocoderExecutor = Executors.newSingleThreadExecutor();
        rootView = view;
        bindViews(view);
        setupActions();
        setupDarkModeSwitch();
        setupLanguageSwitch();
    }

    private void setupDarkModeSwitch() {
        if (!isAdded() || darkModeSwitch == null) return;

        SharedPreferences preferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean darkModeEnabled = preferences.getBoolean(KEY_DARK_MODE, false);

        darkModeSwitch.setChecked(darkModeEnabled);

        darkModeSwitch.setOnClickListener(v -> {
            if (!isAdded()) return;

            boolean newValue = darkModeSwitch.isChecked();

            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(KEY_DARK_MODE, newValue);
            editor.apply();

            int newMode = newValue ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            AppCompatDelegate.setDefaultNightMode(newMode);

            if (getActivity() != null && !getActivity().isFinishing()) {
                getActivity().recreate();
            }
        });
    }

    private void setupLanguageSwitch() {
        if (!isAdded() || languageSwitch == null) return;

        SharedPreferences preferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String currentLanguage = preferences.getString(KEY_LANGUAGE, "vi");

        languageSwitch.setChecked("en".equals(currentLanguage));

        languageSwitch.setOnClickListener(v -> {
            if (!isAdded()) return;

            boolean isChecked = languageSwitch.isChecked();
            String newLanguage = isChecked ? "en" : "vi";

            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(KEY_LANGUAGE, newLanguage);
            editor.apply();

            if (getActivity() != null && !getActivity().isFinishing()) {
                getActivity().recreate();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserProfile();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (firebaseUser != null && interactionsListener != null) {
            matchRepository.removeInteractionsListener(firebaseUser.getUid(), interactionsListener);
        }
        if (geocoderExecutor != null && !geocoderExecutor.isShutdown()) {
            geocoderExecutor.shutdownNow();
        }
        geocoderExecutor = null;
    }

    public void refreshProfile() {
        loadUserProfile();
    }

    private void bindViews(View view) {
        scrollView = view.findViewById(R.id.profile_settings_scroll);
        loadingView = view.findViewById(R.id.profile_settings_loading);
        avatarView = view.findViewById(R.id.profile_settings_avatar);
        cameraBadgeView = view.findViewById(R.id.profile_settings_camera_badge);
        nameView = view.findViewById(R.id.profile_settings_name);
        taglineView = view.findViewById(R.id.profile_settings_tagline);
        editButton = view.findViewById(R.id.profile_settings_edit_button);
        locationValueView = view.findViewById(R.id.profile_settings_location_value);
        editLocationButton = view.findViewById(R.id.profile_settings_edit_location);
        editInterestsAction = view.findViewById(R.id.profile_settings_edit_interests);
        interestsGroup = view.findViewById(R.id.profile_settings_interests_group);
        interestsEmptyView = view.findViewById(R.id.profile_settings_interests_empty);
        addPhotoAction = view.findViewById(R.id.profile_settings_add_photo);
        photosRecyclerView = view.findViewById(R.id.profile_settings_photos_recycler);
        photosEmptyView = view.findViewById(R.id.profile_settings_photos_empty);
        likesCountView = view.findViewById(R.id.profile_settings_likes_count);
        matchesCountView = view.findViewById(R.id.profile_settings_matches_count);
        superLikesCountView = view.findViewById(R.id.profile_settings_superlikes_count);
        genderValueView = view.findViewById(R.id.profile_settings_gender_value);
        editGenderButton = view.findViewById(R.id.profile_settings_edit_gender);
        seekingValueView = view.findViewById(R.id.profile_settings_seeking_value);
        editSeekingButton = view.findViewById(R.id.profile_settings_edit_seeking);
        notificationStatusView = view.findViewById(R.id.profile_settings_notification_status);
        editNotificationButton = view.findViewById(R.id.profile_settings_edit_notification);
        darkModeSwitch = view.findViewById(R.id.profile_settings_dark_mode_switch);
        languageSwitch = view.findViewById(R.id.profile_settings_language_switch);

        if (photosRecyclerView != null) {
            photosRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            photosRecyclerView.setAdapter(photoAdapter);
        }
    }

    private void setupActions() {
        ImageButton optionsButton = rootView.findViewById(R.id.profile_settings_options_button);
        optionsButton.setOnClickListener(v -> showLogoutDialog());

        editButton.setOnClickListener(v -> {
            if (!isAdded()) return;
            Intent intent = new Intent(getActivity(), ProfileInfoActivity.class);
            intent.putExtra("IS_EDIT_MODE", true);
            startActivity(intent);
        });
        editInterestsAction.setOnClickListener(v -> {
            if (!isAdded()) return;
            Intent intent = new Intent(getActivity(), InterestsActivity.class);
            intent.putExtra("IS_EDIT_MODE", true);
            startActivity(intent);
        });
        addPhotoAction.setOnClickListener(v -> openPhotoManager());
        cameraBadgeView.setOnClickListener(v -> openPhotoManager());
        editGenderButton.setOnClickListener(v -> {
            if (!isAdded()) return;
            Intent intent = new Intent(getActivity(), GenderSelectionActivity.class);
            intent.putExtra("IS_EDIT_MODE", true);
            startActivity(intent);
        });
        editSeekingButton.setOnClickListener(v -> {
            if (!isAdded()) return;
            Intent intent = new Intent(getActivity(), SeekingActivity.class);
            intent.putExtra("IS_EDIT_MODE", true);
            startActivity(intent);
        });
        editLocationButton.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onLaunchLocationPermission();
            }
        });
        editNotificationButton.setOnClickListener(v -> {
            if (!isAdded()) return;
            Intent intent = new Intent(getActivity(), NotificationPermissionActivity.class);
            startActivity(intent);
        });
    }

    private void loadUserProfile() {
        if (!isAdded()) return;
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            showError(getString(R.string.profile_settings_load_error));
            if (getActivity() != null) getActivity().finish();
            return;
        }

        showLoading(true);
        userRepository.getUserData(firebaseUser.getUid())
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded()) return;
                    handleProfileSnapshot(snapshot);
                })
                .addOnFailureListener(throwable -> {
                    if (!isAdded()) return;
                    showLoading(false);
                    showError(getString(R.string.profile_settings_load_error));
                });
    }

    private void handleProfileSnapshot(@NonNull DataSnapshot snapshot) {
        if (!isAdded()) return;
        showLoading(false);
        User user = snapshot.getValue(User.class);
        if (user == null) {
            showError(getString(R.string.profile_settings_load_error));
            return;
        }
        if (TextUtils.isEmpty(user.getUid()) && firebaseUser != null) {
            user.setUid(firebaseUser.getUid());
        }
        bindUser(user);
    }

    private String getGenderDisplayString(String gender) {
        if (gender == null) {
            return "";
        }
        switch (gender) {
            case "male":
                return getString(R.string.gender_male);
            case "female":
                return getString(R.string.gender_female);
            default:
                return gender;
        }
    }

    private String getSeekingDisplayString(String seekingType) {
        if (seekingType == null) {
            return "";
        }
        switch (seekingType) {
            case "friend":
                return getString(R.string.seeking_friend);
            case "chat":
                return getString(R.string.seeking_chat);
            case "no_strings":
                return getString(R.string.seeking_no_strings);
            case "later":
                return getString(R.string.seeking_later);
            default:
                return seekingType;
        }
    }

    private void bindUser(@NonNull User user) {
        if (!isAdded()) return;
        String primaryPhotoUrl = null;
        List<String> photos = user.getPhotoUrls();
        if (photos != null && !photos.isEmpty()) {
            primaryPhotoUrl = photos.get(0);
        }

        if (!TextUtils.isEmpty(primaryPhotoUrl) && getContext() != null) {
            Glide.with(getContext())
                    .load(primaryPhotoUrl)
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .centerCrop()
                    .into(avatarView);
        } else {
            avatarView.setImageResource(R.drawable.ic_avatar_placeholder);
        }

        String displayName = !TextUtils.isEmpty(user.getName())
                ? user.getName()
                : getString(R.string.profile_name_fallback);
        int age = calculateAge(user.getDateOfBirth());
        if (age > 0) {
            displayName = getString(R.string.profile_name_with_age, displayName, age);
        }
        nameView.setText(displayName);

        String tagline = user.getBio();
        if (TextUtils.isEmpty(tagline)) {
            taglineView.setVisibility(View.GONE);
        } else {
            taglineView.setVisibility(View.VISIBLE);
            taglineView.setText(tagline);
        }
        genderValueView.setText(getGenderDisplayString(user.getGender()));
        seekingValueView.setText(getSeekingDisplayString(user.getSeekingType()));
        updateLocation(user);
        updateInterests(user.getInterests());
        updatePhotos(user.getPhotoUrls());
        loadInteractionStats(user.getUid());
        updateNotificationStatus();
    }

    private void updateNotificationStatus() {
        if (!isAdded() || getContext() == null) return;
        boolean areNotificationsEnabled = NotificationManagerCompat.from(getContext()).areNotificationsEnabled();
        if (areNotificationsEnabled) {
            notificationStatusView.setText(getString(R.string.notification_status_on));
        } else {
            notificationStatusView.setText(getString(R.string.notification_status_off));
        }
    }

    private void openPhotoManager() {
        if (!isAdded()) return;
        Intent intent = new Intent(getActivity(), PhotoUploadActivity.class);
        intent.putExtra("IS_EDIT_MODE", true);
        startActivity(intent);
    }

    private void showLogoutDialog() {
        if (!isAdded() || getContext() == null) return;

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_logout_confirmation, null);
        AlertDialog dialog = new MaterialAlertDialogBuilder(getContext())
                .setView(dialogView)
                .create();

        Button logoutButton = dialogView.findViewById(R.id.button_logout);
        logoutButton.setOnClickListener(v -> {
            performLogout();
            dialog.dismiss();
        });

        Button cancelButton = dialogView.findViewById(R.id.button_cancel);
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        dialog.show();
    }

    private void performLogout() {
        if (!isAdded()) return;
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference userStatusRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
            Map<String, Object> statusUpdate = new HashMap<>();
            statusUpdate.put("online", false);
            statusUpdate.put("lastSeen", ServerValue.TIMESTAMP);
            userStatusRef.updateChildren(statusUpdate).addOnCompleteListener(task -> {
                FirebaseAuth.getInstance().signOut();
                if (getActivity() != null && isAdded()) {
                    Intent intent = new Intent(getActivity(), WelcomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    getActivity().finishAffinity();
                }
            });
        } else {
            // Fallback for cases where user is null but logout is triggered
            if (getActivity() != null && isAdded()) {
                Intent intent = new Intent(getActivity(), WelcomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                getActivity().finishAffinity();
            }
        }
    }


    private void showLoading(boolean show) {
        if (!isAdded()) return;
        loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
        scrollView.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
    }

    private void showError(@NonNull String message) {
        if (!isAdded() || getView() == null) return;
        Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
    }

    private int calculateAge(@Nullable String dobString) {
        if (TextUtils.isEmpty(dobString)) {
            return -1;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                LocalDate birthDate = LocalDate.parse(dobString, formatter);
                LocalDate today = LocalDate.now();
                if (birthDate.isAfter(today)) {
                    return -1;
                }
                return Period.between(birthDate, today).getYears();
            } catch (DateTimeParseException exception) {
                return -1;
            }
        } else {
            try {
                Date birthDate = DOB_FORMATTER.parse(dobString);
                if (birthDate == null) {
                    return -1;
                }
                Calendar birthCalendar = Calendar.getInstance();
                birthCalendar.setTime(birthDate);
                Calendar today = Calendar.getInstance();
                int years = today.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR);
                if (today.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
                    years--;
                }
                return years;
            } catch (ParseException exception) {
                return -1;
            }
        }
    }

    private void updateLocation(@NonNull User user) {
        if (!isAdded()) return;
        boolean hasCoordinates = user.getLatitude() != null && user.getLongitude() != null;
        boolean isVisible = user.getLocationVisible() == null || Boolean.TRUE.equals(user.getLocationVisible());

        if (hasCoordinates && isVisible) {
            getAddressFromCoordinates(user.getLatitude(), user.getLongitude());
        } else if (hasCoordinates) {
            locationValueView.setText(R.string.profile_settings_location_hidden);
        } else {
            locationValueView.setText(R.string.profile_location_placeholder);
        }
    }

    private void getAddressFromCoordinates(double latitude, double longitude) {
        if (getContext() == null || !Geocoder.isPresent() || geocoderExecutor == null || geocoderExecutor.isShutdown()) {
            if (isAdded()) {
                locationValueView.setText(R.string.profile_settings_location_enabled);
            }
            return;
        }

        geocoderExecutor.execute(() -> {
            if (getContext() == null || !Geocoder.isPresent()) {
                if (getActivity() != null && isAdded()) getActivity().runOnUiThread(() -> {
                    locationValueView.setText(R.string.profile_settings_location_enabled);
                });
                return;
            }
            Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
            String locationText = getString(R.string.profile_settings_location_enabled);
            try {
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String district = address.getSubAdminArea(); // Huyện/Quận
                    String province = address.getAdminArea();    // Tỉnh
                    String country = address.getCountryName();   // Quốc gia

                    // Use LinkedHashSet to maintain order and remove duplicates
                    LinkedHashSet<String> locationParts = new LinkedHashSet<>();
                    if (district != null && !district.isEmpty()) {
                        locationParts.add(district);
                    }
                    if (province != null && !province.isEmpty()) {
                        locationParts.add(province);
                    }
                    if (country != null && !country.isEmpty()) {
                        locationParts.add(country);
                    }

                    if (!locationParts.isEmpty()) {
                        locationText = TextUtils.join(", ", locationParts);
                    }
                }
            } catch (IOException e) {
                // Fallback to the default text
            } finally {
                final String finalLocationText = locationText;
                if (getActivity() != null && isAdded()) {
                    getActivity().runOnUiThread(() -> {
                        locationValueView.setText(finalLocationText);
                    });
                }
            }
        });
    }



    private void updateInterests(@Nullable List<String> interests) {
        if (!isAdded() || interestsGroup == null || interestsEmptyView == null || getContext() == null) {
            return;
        }
        interestsGroup.removeAllViews();
        if (interests == null || interests.isEmpty()) {
            interestsGroup.setVisibility(View.GONE);
            interestsEmptyView.setVisibility(View.VISIBLE);
            return;
        }
        
        // Convert keys to display names for current language
        List<String> displayNames = vn.haui.heartlink.utils.InterestMapper.keysToDisplayNames(
                getContext(), interests);
        
        List<String> sanitized = new ArrayList<>();
        for (String entry : displayNames) {
            if (!TextUtils.isEmpty(entry)) {
                sanitized.add(entry);
            }
        }
        if (sanitized.isEmpty()) {
            interestsGroup.setVisibility(View.GONE);
            interestsEmptyView.setVisibility(View.VISIBLE);
            return;
        }
        interestsGroup.setVisibility(View.VISIBLE);
        interestsEmptyView.setVisibility(View.GONE);
        for (String interest : sanitized) {
            Chip chip = new Chip(getContext());
            chip.setText(interest);
            chip.setTextColor(ContextCompat.getColor(getContext(), R.color.home_title));
            chip.setChipBackgroundColorResource(R.color.profile_chip_background);
            chip.setChipStrokeColorResource(R.color.profile_chip_stroke);
            chip.setChipStrokeWidth(1f);
            chip.setClickable(false);
            chip.setCheckable(false);
            interestsGroup.addView(chip);
        }
    }

    private void updatePhotos(@Nullable List<String> photos) {
        if (!isAdded() || photosRecyclerView == null || photosEmptyView == null) {
            return;
        }
        List<String> sanitized = new ArrayList<>();
        if (photos != null) {
            for (String entry : photos) {
                if (!TextUtils.isEmpty(entry)) {
                    sanitized.add(entry);
                }
            }
        }
        if (sanitized.isEmpty()) {
            photosRecyclerView.setVisibility(View.GONE);
            photosEmptyView.setVisibility(View.VISIBLE);
            photoAdapter.submitPhotos(Collections.emptyList());
        } else {
            photosRecyclerView.setVisibility(View.VISIBLE);
            photosEmptyView.setVisibility(View.GONE);
            photoAdapter.submitPhotos(sanitized);
        }
    }

    private void loadInteractionStats(@Nullable String uid) {
        if (!isAdded() || TextUtils.isEmpty(uid)) {
            updateStatsViews(0, 0, 0);
            return;
        }
        interactionsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                int likes = 0;
                int matches = 0;
                int superLikes = 0;
                if (snapshot != null) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String status = child.child("status").getValue(String.class);
                        String type = child.child("type").getValue(String.class);
                        if (MatchRepository.STATUS_MATCHED.equals(status)) {
                            matches++;
                        } else if (MatchRepository.STATUS_LIKED.equals(status) || MatchRepository.STATUS_RECEIVED_LIKE.equals(status)) {
                            if (MatchRepository.isSuperLike(type)) {
                                superLikes++;
                            } else {
                                likes++;
                            }
                        }
                    }
                }
                updateStatsViews(likes, matches, superLikes);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded()) return;
                updateStatsViews(0, 0, 0);
            }
        };
        matchRepository.addInteractionsListener(uid, interactionsListener);
    }

    private void updateStatsViews(int likes, int matches, int superLikes) {
        if (!isAdded()) return;
        likesCountView.setText(String.valueOf(likes));
        matchesCountView.setText(String.valueOf(matches));
        superLikesCountView.setText(String.valueOf(superLikes));
    }
}

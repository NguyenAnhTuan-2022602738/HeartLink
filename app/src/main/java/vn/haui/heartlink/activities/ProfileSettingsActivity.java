package vn.haui.heartlink.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;

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
import java.util.List;
import java.util.Locale;

import vn.haui.heartlink.R;
import vn.haui.heartlink.models.User;
import vn.haui.heartlink.utils.MatchRepository;
import vn.haui.heartlink.utils.UserRepository;
import vn.haui.heartlink.adapters.ProfilePhotoAdapter;

/**
 * Screen for managing the signed-in user's profile and account preferences.
 */
public class ProfileSettingsActivity extends AppCompatActivity {

    private static final DateTimeFormatter DOB_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final UserRepository userRepository = UserRepository.getInstance();

    private View rootView;
    private NestedScrollView scrollView;
    private ProgressBar loadingView;
    private ImageView avatarView;
    private ImageView cameraBadgeView;
    private TextView nameView;
    private TextView emailView;
    private TextView locationValueView;
    private TextView interestsEmptyView;
    private TextView photosEmptyView;
    private TextView likesCountView;
    private TextView matchesCountView;
    private TextView superLikesCountView;
    private MaterialButton previewButton;
    private MaterialButton editBasicButton;
    private TextView editInterestsAction;
    private TextView addPhotoAction;
    private ChipGroup interestsGroup;
    private RecyclerView photosRecyclerView;
    private final ProfilePhotoAdapter photoAdapter = new ProfilePhotoAdapter();

    @Nullable
    private FirebaseUser firebaseUser;
    @Nullable
    private User currentUser;
    @Nullable
    private String primaryPhotoUrl;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile_settings);

        rootView = findViewById(R.id.profile_settings_root);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });

        bindViews();
        setupActions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
    }

    /**
     * Phương thức khởi tạo và bind các view components từ layout.
     * Thiết lập RecyclerView cho photos và các view khác.
     */
    private void bindViews() {
        scrollView = findViewById(R.id.profile_settings_scroll);
        loadingView = findViewById(R.id.profile_settings_loading);
        avatarView = findViewById(R.id.profile_settings_avatar);
        cameraBadgeView = findViewById(R.id.profile_settings_camera_badge);
        nameView = findViewById(R.id.profile_settings_name);
        emailView = findViewById(R.id.profile_settings_email);
        previewButton = findViewById(R.id.profile_settings_preview_button);
        editBasicButton = findViewById(R.id.profile_settings_edit_basic);
        locationValueView = findViewById(R.id.profile_settings_location_value);
        editInterestsAction = findViewById(R.id.profile_settings_edit_interests);
        interestsGroup = findViewById(R.id.profile_settings_interests_group);
        interestsEmptyView = findViewById(R.id.profile_settings_interests_empty);
        addPhotoAction = findViewById(R.id.profile_settings_add_photo);
        photosRecyclerView = findViewById(R.id.profile_settings_photos_recycler);
        photosEmptyView = findViewById(R.id.profile_settings_photos_empty);
        likesCountView = findViewById(R.id.profile_settings_likes_count);
        matchesCountView = findViewById(R.id.profile_settings_matches_count);
        superLikesCountView = findViewById(R.id.profile_settings_superlikes_count);

        if (photosRecyclerView != null) {
            photosRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            photosRecyclerView.setAdapter(photoAdapter);
        }
    }

    /**
     * Phương thức thiết lập các event listeners cho các button và action.
     * Bao gồm back button, options button, preview, edit buttons, v.v.
     */
    private void setupActions() {
        ImageButton backButton = findViewById(R.id.profile_settings_back);
        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        ImageButton optionsButton = findViewById(R.id.profile_settings_options_button);
        optionsButton.setOnClickListener(v -> showLogoutDialog());

        previewButton.setOnClickListener(v -> openProfilePreview());
        editBasicButton.setOnClickListener(v -> startActivity(new Intent(this, ProfileInfoActivity.class)));
        editInterestsAction.setOnClickListener(v -> startActivity(new Intent(this, InterestsActivity.class)));
        addPhotoAction.setOnClickListener(v -> openPhotoManager());
        cameraBadgeView.setOnClickListener(v -> openPhotoManager());
    }

    /**
     * Phương thức tải thông tin profile của user hiện tại từ Firebase.
     * Hiển thị loading và xử lý lỗi nếu không load được.
     */
    private void loadUserProfile() {
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            showError(getString(R.string.profile_settings_load_error));
            finish();
            return;
        }

        showLoading(true);
        userRepository.getUserData(firebaseUser.getUid())
                .addOnSuccessListener(this::handleProfileSnapshot)
                .addOnFailureListener(throwable -> {
                    showLoading(false);
                    showError(getString(R.string.profile_settings_load_error));
                });
    }

    /**
     * Phương thức xử lý dữ liệu profile từ Firebase snapshot.
     * Parse User object và bind dữ liệu lên UI.
     *
     * @param snapshot DataSnapshot chứa dữ liệu user từ Firebase
     */
    private void handleProfileSnapshot(@NonNull DataSnapshot snapshot) {
        showLoading(false);
        User user = snapshot.getValue(User.class);
        if (user == null) {
            showError(getString(R.string.profile_settings_load_error));
            return;
        }
        if (TextUtils.isEmpty(user.getUid()) && firebaseUser != null) {
            user.setUid(firebaseUser.getUid());
        }
        currentUser = user;
        bindUser(user);
    }

    /**
     * Phương thức bind thông tin user lên các view components.
     * Hiển thị avatar, tên, email, location, interests, photos, v.v.
     *
     * @param user Đối tượng User chứa thông tin cần hiển thị
     */
    private void bindUser(@NonNull User user) {
        List<String> photos = user.getPhotoUrls();
        primaryPhotoUrl = null;
        if (photos != null) {
            for (String entry : photos) {
                if (!TextUtils.isEmpty(entry)) {
                    primaryPhotoUrl = entry;
                    break;
                }
            }
        }
        if (!TextUtils.isEmpty(primaryPhotoUrl)) {
            Glide.with(this)
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

        String email = user.getEmail();
        if (TextUtils.isEmpty(email) && firebaseUser != null) {
            email = firebaseUser.getEmail();
        }
        if (TextUtils.isEmpty(email)) {
            emailView.setVisibility(View.GONE);
        } else {
            emailView.setVisibility(View.VISIBLE);
            emailView.setText(email);
        }

        updateLocation(user);
        updateInterests(user.getInterests());
        updatePhotos(user.getPhotoUrls());
        loadInteractionStats(user.getUid());
    }

    /**
     * Phương thức mở ProfileDetailActivity để xem preview profile của chính mình.
     * Sử dụng thông tin currentUser để tạo intent.
     */
    private void openProfilePreview() {
        if (currentUser == null || currentUser.getUid() == null) {
            return;
        }
        String fallbackName = currentUser.getName();
        Intent intent = ProfileDetailActivity.createIntent(
                this,
                currentUser.getUid(),
                fallbackName,
                primaryPhotoUrl
        );
        startActivity(intent);
    }

    /**
     * Phương thức mở PhotoUploadActivity để quản lý photos của user.
     */
    private void openPhotoManager() {
        startActivity(new Intent(this, PhotoUploadActivity.class));
    }

    /**
     * Phương thức hiển thị dialog xác nhận đăng xuất.
     * Hỏi người dùng có chắc chắn muốn đăng xuất không.
     */
    private void showLogoutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.profile_settings_logout_confirm_title)
                .setMessage(R.string.profile_settings_logout_confirm_message)
                .setNegativeButton(R.string.profile_settings_logout_confirm_negative, null)
                .setPositiveButton(R.string.profile_settings_logout_confirm_positive, (dialogInterface, i) -> performLogout())
                .show();
    }

    /**
     * Phương thức thực hiện đăng xuất user.
     * Sign out từ Firebase Auth và chuyển về WelcomeActivity.
     */
    private void performLogout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finishAffinity();
    }

    /**
     * Phương thức hiển thị hoặc ẩn loading indicator.
     *
     * @param show true để hiển thị loading, false để ẩn
     */
    private void showLoading(boolean show) {
        loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
        scrollView.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
    }

    /**
     * Phương thức hiển thị thông báo lỗi dưới dạng Snackbar.
     *
     * @param message Thông báo lỗi cần hiển thị
     */
    private void showError(@NonNull String message) {
        Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();
    }

    /**
     * Phương thức tính tuổi từ ngày sinh.
     * Hỗ trợ cả API level mới và cũ.
     *
     * @param dobString Chuỗi ngày sinh định dạng dd/MM/yyyy
     * @return Tuổi tính được, hoặc -1 nếu có lỗi
     */
    private int calculateAge(@Nullable String dobString) {
        if (TextUtils.isEmpty(dobString)) {
            return -1;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                LocalDate birthDate = LocalDate.parse(dobString, DOB_FORMATTER);
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
                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date birthDate = formatter.parse(dobString);
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

    /**
     * Phương thức cập nhật hiển thị location status.
     * Hiển thị enabled/hidden/placeholder dựa trên coordinates và visibility setting.
     *
     * @param user Đối tượng User chứa thông tin location
     */
    private void updateLocation(@NonNull User user) {
        boolean hasCoordinates = user.getLatitude() != null && user.getLongitude() != null;
        boolean isVisible = user.getLocationVisible() == null || Boolean.TRUE.equals(user.getLocationVisible());
        if (hasCoordinates && isVisible) {
            locationValueView.setText(R.string.profile_settings_location_enabled);
        } else if (hasCoordinates) {
            locationValueView.setText(R.string.profile_settings_location_hidden);
        } else {
            locationValueView.setText(R.string.profile_location_placeholder);
        }
    }

    /**
     * Phương thức cập nhật hiển thị danh sách interests dưới dạng chips.
     * Hiển thị empty state nếu không có interests.
     *
     * @param interests Danh sách interests của user
     */
    private void updateInterests(@Nullable List<String> interests) {
        if (interestsGroup == null || interestsEmptyView == null) {
            return;
        }
        interestsGroup.removeAllViews();
        if (interests == null || interests.isEmpty()) {
            interestsGroup.setVisibility(View.GONE);
            interestsEmptyView.setVisibility(View.VISIBLE);
            return;
        }
        List<String> sanitized = new ArrayList<>();
        for (String entry : interests) {
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
            Chip chip = new Chip(this);
            chip.setText(interest);
            chip.setTextColor(ContextCompat.getColor(this, R.color.home_title));
            chip.setChipBackgroundColorResource(R.color.profile_chip_background);
            chip.setChipStrokeColorResource(R.color.profile_chip_stroke);
            chip.setChipStrokeWidth(1f);
            chip.setClickable(false);
            chip.setCheckable(false);
            interestsGroup.addView(chip);
        }
    }

    /**
     * Phương thức cập nhật hiển thị danh sách photos trong RecyclerView.
     * Hiển thị empty state nếu không có photos.
     *
     * @param photos Danh sách URL photos của user
     */
    private void updatePhotos(@Nullable List<String> photos) {
        if (photosRecyclerView == null || photosEmptyView == null) {
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

    /**
     * Phương thức tải thống kê tương tác (likes, matches, super likes) từ Firebase.
     * Parse dữ liệu từ interactions snapshot và cập nhật UI.
     *
     * @param uid UID của user cần load stats
     */
    private void loadInteractionStats(@Nullable String uid) {
        if (TextUtils.isEmpty(uid)) {
            updateStatsViews(0, 0, 0);
            return;
        }
    MatchRepository.getInstance().getInteractionsSnapshot(uid)
                .addOnSuccessListener(snapshot -> {
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
                })
                .addOnFailureListener(throwable -> updateStatsViews(0, 0, 0));
    }

    /**
     * Phương thức cập nhật hiển thị các số liệu thống kê lên UI.
     *
     * @param likes Số lượt like
     * @param matches Số lượt match
     * @param superLikes Số lượt super like
     */
    private void updateStatsViews(int likes, int matches, int superLikes) {
        likesCountView.setText(String.valueOf(likes));
        matchesCountView.setText(String.valueOf(matches));
        superLikesCountView.setText(String.valueOf(superLikes));
    }
}

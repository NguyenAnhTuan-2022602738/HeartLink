package vn.haui.heartlink.activities;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.Lifecycle;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.yuyakaido.android.cardstackview.CardStackLayoutManager;
import com.yuyakaido.android.cardstackview.CardStackListener;
import com.yuyakaido.android.cardstackview.CardStackView;
import com.yuyakaido.android.cardstackview.Direction;
import com.yuyakaido.android.cardstackview.Duration;
import com.yuyakaido.android.cardstackview.StackFrom;
import com.yuyakaido.android.cardstackview.SwipeAnimationSetting;
import com.yuyakaido.android.cardstackview.SwipeableMethod;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vn.haui.heartlink.R;
import vn.haui.heartlink.adapters.DiscoveryCardAdapter;
import vn.haui.heartlink.models.DiscoveryProfile;
import vn.haui.heartlink.models.FilterPreferences;
import vn.haui.heartlink.models.User;
import vn.haui.heartlink.ui.FilterBottomSheetDialog;
import vn.haui.heartlink.utils.DiscoveryFilterStorage;
import vn.haui.heartlink.utils.MatchRepository;
import vn.haui.heartlink.utils.LikesNotificationManager;
import vn.haui.heartlink.utils.UserRepository;

/**
 * Main discovery screen showing nearby profiles with swipe interactions.
 */
public class MainActivity extends AppCompatActivity {

    private static final double NEARBY_THRESHOLD_KM = 50.0;

    private TextView locationTextView;
    private CardStackView cardStackView;
    private ProgressBar loadingIndicator;
    private TextView emptyStateView;
    private View dislikeButton;
    private View likeButton;
    private View superLikeButton;
    private View filterButton;
    private View matchesTab;
    private View messagesTab;
    private View profileTab;

    private DiscoveryCardAdapter cardAdapter;
    private CardStackLayoutManager cardStackLayoutManager;

    private final UserRepository userRepository = UserRepository.getInstance();
    private final MatchRepository matchRepository = MatchRepository.getInstance();
    private final ExecutorService geocoderExecutor = Executors.newSingleThreadExecutor();

    @Nullable
    private User currentUser;
    private final List<DiscoveryProfile> discoveryProfiles = new ArrayList<>();
    private final Set<String> excludedUserIds = new HashSet<>();
    @Nullable
    private ChildEventListener incomingLikeListener;
    private final Set<String> notifiedIncomingLikes = new HashSet<>();
    private DiscoveryFilterStorage filterStorage;
    private FilterPreferences filterPreferences;
    @Nullable
    private String currentLocationLabel;

    /**
     * Khởi tạo MainActivity. Thiết lập edge-to-edge display, bind views, setup card stack,
     * hookup actions và load thông tin user hiện tại.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        filterStorage = new DiscoveryFilterStorage(this);

        final View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        WindowInsetsControllerCompat controller = ViewCompat.getWindowInsetsController(root);
        if (controller != null) {
            controller.setAppearanceLightStatusBars(true);
            controller.setAppearanceLightNavigationBars(true);
        }

        bindViews();
        setupCardStack();
        hookupActions();
        loadCurrentUser();
    }

    /**
     * Bind các view từ layout vào các biến instance để sử dụng trong activity.
     */
    private void bindViews() {
        locationTextView = findViewById(R.id.home_location);
        cardStackView = findViewById(R.id.home_card_stack);
        loadingIndicator = findViewById(R.id.home_loading_indicator);
        emptyStateView = findViewById(R.id.home_empty_state);
        dislikeButton = findViewById(R.id.home_dislike_button);
        likeButton = findViewById(R.id.home_like_button);
        superLikeButton = findViewById(R.id.home_superlike_button);
        filterButton = findViewById(R.id.home_filter_button);
        matchesTab = findViewById(R.id.home_nav_matches);
        messagesTab = findViewById(R.id.home_nav_messages);
        profileTab = findViewById(R.id.home_nav_profile);

        locationTextView.setText(R.string.home_location_placeholder);
    }

    /**
     * Thiết lập CardStackView với adapter và layout manager, bao gồm các listener cho swipe actions.
     */
    private void setupCardStack() {
        cardAdapter = new DiscoveryCardAdapter();
        cardStackLayoutManager = new CardStackLayoutManager(this, new CardStackListener() {
            @Override
            public void onCardDragging(Direction direction, float ratio) {
                updateOverlay(direction, ratio);
            }

            @Override
            public void onCardSwiped(Direction direction) {
                int index = cardStackLayoutManager.getTopPosition() - 1;
                DiscoveryProfile profile = cardAdapter.getItem(index);
                if (profile == null) {
                    return;
                }
                switch (direction) {
                    case Right:
                        handleLike(profile);
                        break;
                    case Left:
                        handlePass(profile);
                        break;
                    case Top:
                        handleSuperLike(profile);
                        break;
                    default:
                        break;
                }
                resetOverlay();
            }

            @Override
            public void onCardRewound() {
                // no-op
            }

            @Override
            public void onCardCanceled() {
                resetOverlay();
            }

            @Override
            public void onCardAppeared(View view, int position) {
                resetOverlay(view);
            }

            @Override
            public void onCardDisappeared(View view, int position) {
                if (cardStackLayoutManager.getTopPosition() >= discoveryProfiles.size()) {
                    updateEmptyState();
                }
            }
        });

        cardStackLayoutManager.setStackFrom(StackFrom.None);
        cardStackLayoutManager.setVisibleCount(3);
        cardStackLayoutManager.setTranslationInterval(12f);
        cardStackLayoutManager.setScaleInterval(0.95f);
        cardStackLayoutManager.setSwipeThreshold(0.3f);
        cardStackLayoutManager.setMaxDegree(25f);
        cardStackLayoutManager.setDirections(Direction.FREEDOM);
        cardStackLayoutManager.setCanScrollHorizontal(true);
        cardStackLayoutManager.setCanScrollVertical(true);
        cardStackLayoutManager.setSwipeableMethod(SwipeableMethod.AutomaticAndManual);

        cardStackView.setLayoutManager(cardStackLayoutManager);
        cardStackView.setAdapter(cardAdapter);
    }

    /**
     * Thiết lập các onClickListener cho các nút và tab navigation.
     */
    private void hookupActions() {
        dislikeButton.setOnClickListener(v -> performSwipe(Direction.Left));
        likeButton.setOnClickListener(v -> performSwipe(Direction.Right));
        superLikeButton.setOnClickListener(v -> performSwipe(Direction.Top));
        filterButton.setOnClickListener(v -> openFilterBottomSheet());
        matchesTab.setOnClickListener(v -> openMatchesScreen());
        if (messagesTab != null) {
            messagesTab.setOnClickListener(v -> openMessagesScreen());
        }
        if (profileTab != null) {
            profileTab.setOnClickListener(v -> openProfileSettings());
        }
        if (filterPreferences == null) {
            filterPreferences = buildDefaultFilters();
        }
    }

    /**
     * Tải thông tin user hiện tại từ Firebase và khởi tạo các thành phần phụ thuộc.
     */
    private void loadCurrentUser() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(this, R.string.error_generic, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showLoading(true);
        userRepository.getUserData(firebaseUser.getUid())
                .addOnSuccessListener(snapshot -> {
                    currentUser = snapshot.getValue(User.class);
                    if (currentUser == null) {
                        showLoading(false);
                        showError(getString(R.string.error_generic));
                        return;
                    }
                    if (TextUtils.isEmpty(currentUser.getUid())) {
                        currentUser.setUid(firebaseUser.getUid());
                    }
                    filterPreferences = filterStorage.load(currentUser);
                    if (filterPreferences == null) {
                        filterPreferences = buildDefaultFilters();
                    }
                    startListeningForIncomingLikes();
                    updateLocationText();
                    updateLocationDisplay();
                    loadSuggestions();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    showError(e.getLocalizedMessage());
                });
    }

    /**
     * Tải danh sách gợi ý người dùng để hiển thị trên card stack, loại trừ những người đã tương tác.
     */
    private void loadSuggestions() {
        if (currentUser == null || TextUtils.isEmpty(currentUser.getUid())) {
            return;
        }

        matchRepository.getInteractionsSnapshot(currentUser.getUid())
                .addOnSuccessListener(snapshot -> {
                    excludedUserIds.clear();
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String otherUid = child.getKey();
                        String status = child.child("status").getValue(String.class);
                        if (!TextUtils.isEmpty(otherUid)
                                && (MatchRepository.STATUS_LIKED.equals(status)
                                || MatchRepository.STATUS_MATCHED.equals(status)
                                || MatchRepository.STATUS_RECEIVED_LIKE.equals(status))) {
                            excludedUserIds.add(otherUid);
                        }
                    }
                    fetchAllUsers();
                })
                .addOnFailureListener(e -> {
                    excludedUserIds.clear();
                    fetchAllUsers();
                });
    }

    /**
     * Lấy tất cả người dùng từ repository để xử lý gợi ý.
     */
    private void fetchAllUsers() {
        userRepository.getAllUsers()
                .addOnSuccessListener(this::handleSuggestionsSnapshot)
                .addOnFailureListener(e -> {
                    showLoading(false);
                    showError(e.getLocalizedMessage());
                });
    }

    /**
     * Xử lý snapshot dữ liệu người dùng để tạo danh sách gợi ý, lọc theo vị trí và sở thích.
     * @param snapshot DataSnapshot chứa danh sách người dùng từ Firebase.
     */
    private void handleSuggestionsSnapshot(DataSnapshot snapshot) {
        discoveryProfiles.clear();

        if (currentUser == null) {
            showLoading(false);
            return;
        }

        Double baseLat;
        Double baseLng;
        if (filterPreferences != null && filterPreferences.hasCustomLocation()) {
            baseLat = filterPreferences.getLocationLatitude();
            baseLng = filterPreferences.getLocationLongitude();
        } else {
            baseLat = currentUser.getLatitude();
            baseLng = currentUser.getLongitude();
        }

        boolean canComputeDistance = baseLat != null && baseLng != null;
        float maxDistance = filterPreferences != null ? filterPreferences.getMaxDistanceKm() : (float) NEARBY_THRESHOLD_KM;
        String interested = filterPreferences != null ? filterPreferences.getInterestedIn() : FilterPreferences.INTEREST_BOTH;
        int minAge = filterPreferences != null ? filterPreferences.getMinAge() : 18;
        int maxAge = filterPreferences != null ? filterPreferences.getMaxAge() : 45;

        for (DataSnapshot child : snapshot.getChildren()) {
            User candidate = child.getValue(User.class);
            if (candidate == null) {
                continue;
            }
            if (candidate.getUid() == null) {
                candidate.setUid(child.getKey());
            }
            if (currentUser.getUid() != null && currentUser.getUid().equals(candidate.getUid())) {
                continue;
            }
            if (candidate.getUid() != null && excludedUserIds.contains(candidate.getUid())) {
                continue;
            }
            if (!candidate.isProfileComplete()) {
                continue;
            }
            if (candidate.getLatitude() == null || candidate.getLongitude() == null) {
                continue;
            }
            if (candidate.getLocationVisible() != null && !candidate.getLocationVisible()) {
                continue;
            }

            if (!matchesGenderPreference(interested, candidate.getGender())) {
                continue;
            }

            int age = calculateAge(candidate.getDateOfBirth());
            if (age > 0 && (age < minAge || age > maxAge)) {
                continue;
            }

            double distance = canComputeDistance
                    ? haversineDistance(baseLat, baseLng, candidate.getLatitude(), candidate.getLongitude())
                    : Double.NaN;

            if (canComputeDistance && (Double.isNaN(distance) || distance > maxDistance)) {
                continue;
            }

            String displayName = buildDisplayName(candidate);
            String subtitle = !TextUtils.isEmpty(candidate.getSeekingType())
                    ? candidate.getSeekingType()
                    : getString(R.string.home_user_interest);
            String photoUrl = extractPrimaryPhoto(candidate);

            double distanceForDisplay = Double.isNaN(distance) ? Double.POSITIVE_INFINITY : distance;

            discoveryProfiles.add(new DiscoveryProfile(candidate, distanceForDisplay, displayName, subtitle, photoUrl));
        }

        Collections.sort(discoveryProfiles, Comparator.comparingDouble(DiscoveryProfile::getDistanceKm));

        cardAdapter.submitList(discoveryProfiles);
        if (!discoveryProfiles.isEmpty()) {
            cardStackView.post(() -> cardStackView.scrollToPosition(0));
        }
        showLoading(false);
        updateEmptyState();
    }

    /**
     * Hiển thị hoặc ẩn indicator loading.
     * @param show true để hiển thị loading, false để ẩn.
     */
    private void showLoading(boolean show) {
        loadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        cardStackView.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
    }

    /**
     * Cập nhật trạng thái empty state dựa trên danh sách gợi ý.
     */
    private void updateEmptyState() {
        boolean isEmpty = discoveryProfiles.isEmpty();
        emptyStateView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        cardStackView.setVisibility(isEmpty ? View.INVISIBLE : View.VISIBLE);
    }

    /**
     * Hiển thị thông báo lỗi cho người dùng.
     * @param message Thông điệp lỗi, nếu null sẽ dùng thông điệp mặc định.
     */
    private void showError(@Nullable String message) {
        if (TextUtils.isEmpty(message)) {
            message = getString(R.string.error_generic);
        }
        Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_LONG).show();
    }

    /**
     * Mở màn hình cài đặt hồ sơ cá nhân.
     */
    private void openProfileSettings() {
        startActivity(new Intent(this, ProfileSettingsActivity.class));
    }

    /**
     * Cập nhật text hiển thị vị trí dựa trên tọa độ của user hiện tại.
     */
    private void updateLocationText() {
        if (currentUser == null
                || currentUser.getLatitude() == null
                || currentUser.getLongitude() == null
                || (currentUser.getLocationVisible() != null && !currentUser.getLocationVisible())) {
            currentLocationLabel = null;
            updateLocationDisplay();
            return;
        }

        geocoderExecutor.execute(() -> {
            if (!Geocoder.isPresent()) {
                runOnUiThread(() -> {
                    currentLocationLabel = null;
                    updateLocationDisplay();
                });
                return;
            }
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> results = geocoder.getFromLocation(currentUser.getLatitude(), currentUser.getLongitude(), 1);
                if (results != null && !results.isEmpty()) {
                    Address address = results.get(0);
                    String city = !TextUtils.isEmpty(address.getSubAdminArea()) ? address.getSubAdminArea() : address.getLocality();
                    String country = address.getCountryName();
                    final String label;
                    if (!TextUtils.isEmpty(city) && !TextUtils.isEmpty(country)) {
                        label = city + ", " + country;
                    } else if (!TextUtils.isEmpty(city)) {
                        label = city;
                    } else if (!TextUtils.isEmpty(country)) {
                        label = country;
                    } else {
                        label = getString(R.string.home_location_placeholder);
                    }
                    runOnUiThread(() -> {
                        currentLocationLabel = label;
                        updateLocationDisplay();
                    });
                } else {
                    runOnUiThread(() -> {
                        currentLocationLabel = null;
                        updateLocationDisplay();
                    });
                }
            } catch (IOException e) {
                runOnUiThread(() -> {
                    currentLocationLabel = null;
                    updateLocationDisplay();
                });
            }
        });
    }

    /**
     * Thực hiện swipe card theo hướng chỉ định với animation.
     * @param direction Hướng swipe (Left, Right, Top).
     */
    private void performSwipe(Direction direction) {
        if (cardAdapter.getItemCount() == 0) {
            return;
        }
        SwipeAnimationSetting setting = new SwipeAnimationSetting.Builder()
                .setDirection(direction)
                .setDuration(Duration.Normal.duration)
                .build();
        cardStackLayoutManager.setSwipeAnimationSetting(setting);
        cardStackView.swipe();
    }

    /**
     * Cập nhật overlay hiển thị trên card khi người dùng kéo card.
     * @param direction Hướng kéo.
     * @param ratio Tỷ lệ kéo (0-1).
     */
    private void updateOverlay(Direction direction, float ratio) {
        if (Float.isNaN(ratio) || Float.isInfinite(ratio)) {
            resetOverlay();
            return;
        }

        View topView = cardStackLayoutManager.getTopView();
        if (topView == null) {
            return;
        }

        View likeOverlay = topView.findViewById(R.id.card_like_overlay);
        View passOverlay = topView.findViewById(R.id.card_pass_overlay);
        View superlikeOverlay = topView.findViewById(R.id.card_superlike_overlay);

        float normalized = Math.min(1f, Math.abs(ratio));
        likeOverlay.setAlpha(0f);
        passOverlay.setAlpha(0f);
        superlikeOverlay.setAlpha(0f);

        if (direction == Direction.Right) {
            likeOverlay.setAlpha(normalized);
        } else if (direction == Direction.Left) {
            passOverlay.setAlpha(normalized);
        } else if (direction == Direction.Top) {
            superlikeOverlay.setAlpha(normalized);
        }
    }

    private void resetOverlay() {
        View topView = cardStackLayoutManager.getTopView();
        resetOverlay(topView);
    }

    private void resetOverlay(@Nullable View view) {
        if (view == null) {
            return;
        }
        View likeOverlay = view.findViewById(R.id.card_like_overlay);
        View passOverlay = view.findViewById(R.id.card_pass_overlay);
        View superlikeOverlay = view.findViewById(R.id.card_superlike_overlay);
        if (likeOverlay != null) {
            likeOverlay.setAlpha(0f);
        }
        if (passOverlay != null) {
            passOverlay.setAlpha(0f);
        }
        if (superlikeOverlay != null) {
            superlikeOverlay.setAlpha(0f);
        }
    }

    private void handleLike(DiscoveryProfile profile) {
        processSwipeAction(profile, false);
    }

    private void handlePass(DiscoveryProfile profile) {
        Toast.makeText(this, getString(R.string.home_button_dislike), Toast.LENGTH_SHORT).show();
        // TODO: record pass state if needed.
    }

    private void handleSuperLike(DiscoveryProfile profile) {
        processSwipeAction(profile, true);
    }

    private void processSwipeAction(DiscoveryProfile profile, boolean isSuperLike) {
        if (currentUser == null || profile == null || profile.getUser() == null) {
            showError(getString(R.string.match_creation_error, getString(R.string.error_generic)));
            return;
        }

        matchRepository.likeUser(currentUser, profile.getUser(), isSuperLike, new MatchRepository.MatchResultCallback() {
            @Override
            public void onLikeRecorded() {
                int messageRes = isSuperLike
                        ? R.string.match_superlike_sent
                        : R.string.match_like_sent;
                runOnUiThread(() -> Toast.makeText(MainActivity.this, messageRes, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onMatchCreated() {
                runOnUiThread(() -> launchMatchSuccess(profile));
            }

            @Override
            public void onAlreadyMatched() {
                runOnUiThread(() -> launchMatchSuccess(profile));
            }

            @Override
            public void onError(@NonNull Exception throwable) {
                runOnUiThread(() -> showError(getString(R.string.match_creation_error, throwable.getLocalizedMessage())));
            }
        });

        if (profile.getUser() != null && !TextUtils.isEmpty(profile.getUser().getUid())) {
            excludedUserIds.add(profile.getUser().getUid());
        }
    }

    private void launchMatchSuccess(DiscoveryProfile profile) {
        String partnerName = profile.getUser() != null && !TextUtils.isEmpty(profile.getUser().getName())
                ? profile.getUser().getName()
                : profile.getDisplayName();
        startActivity(MatchSuccessActivity.createIntent(
                this,
                partnerName,
                profile.getPhotoUrl(),
                extractPrimaryPhoto(currentUser)));
    }

    private boolean matchesGenderPreference(@Nullable String interestedIn, @Nullable String candidateGender) {
        if (TextUtils.isEmpty(interestedIn) || FilterPreferences.INTEREST_BOTH.equalsIgnoreCase(interestedIn)) {
            return true;
        }
        if (TextUtils.isEmpty(candidateGender)) {
            return false;
        }
        String normalizedCandidate = candidateGender.trim().toLowerCase(Locale.ROOT);
        if (FilterPreferences.INTEREST_FEMALE.equalsIgnoreCase(interestedIn)) {
            return normalizedCandidate.startsWith("f") || normalizedCandidate.contains("nữ");
        } else if (FilterPreferences.INTEREST_MALE.equalsIgnoreCase(interestedIn)) {
            return normalizedCandidate.startsWith("m") || normalizedCandidate.contains("nam");
        }
        return true;
    }

    private void openFilterBottomSheet() {
        FilterPreferences toEdit = filterPreferences != null ? filterPreferences.copy() : buildDefaultFilters();
        FilterBottomSheetDialog dialog = FilterBottomSheetDialog.newInstance(toEdit, getActiveLocationLabel());
        dialog.setFilterApplyListener(new FilterBottomSheetDialog.FilterApplyListener() {
            @Override
            public void onApply(@NonNull FilterPreferences preferences) {
                filterPreferences = preferences;
                filterStorage.save(preferences);
                updateLocationDisplay();
                loadSuggestions();
            }

            @Override
            public void onClear() {
                filterStorage.clear();
                filterPreferences = buildDefaultFilters();
                updateLocationDisplay();
                loadSuggestions();
            }
        });
        dialog.show(getSupportFragmentManager(), "discovery_filters");
    }

    private void openMatchesScreen() {
        Intent intent = new Intent(this, MatchesActivity.class);
        startActivity(intent);
    }

    private void openMessagesScreen() {
        Intent intent = new Intent(this, MessagesActivity.class);
        startActivity(intent);
    }

    private void startListeningForIncomingLikes() {
        stopListeningForIncomingLikes();
        if (currentUser == null || TextUtils.isEmpty(currentUser.getUid())) {
            return;
        }
        Boolean notificationsEnabled = currentUser.getNotificationsEnabled();
        if (notificationsEnabled != null && !notificationsEnabled) {
            return;
        }
        incomingLikeListener = matchRepository.listenForIncomingLikes(currentUser.getUid(), like -> {
            String key = like.getLikerUid() + ":" + like.getLikedAt();
            if (!notifiedIncomingLikes.add(key)) {
                return;
            }
            runOnUiThread(() -> handleIncomingLikeNotification(like));
        });
    }

    private void stopListeningForIncomingLikes() {
        if (incomingLikeListener != null && currentUser != null && !TextUtils.isEmpty(currentUser.getUid())) {
            matchRepository.removeIncomingLikeListener(currentUser.getUid(), incomingLikeListener);
            incomingLikeListener = null;
        }
        notifiedIncomingLikes.clear();
    }

    private void handleIncomingLikeNotification(@NonNull MatchRepository.IncomingLike like) {
        if (isFinishing()) {
            return;
        }
        LikesNotificationManager.showLikeNotification(this, like);
        Lifecycle.State state = getLifecycle().getCurrentState();
        if (state.isAtLeast(Lifecycle.State.RESUMED)) {
            String displayName = !TextUtils.isEmpty(like.getDisplayName())
                    ? like.getDisplayName()
                    : getString(R.string.notification_incoming_like_unknown);
            Snackbar.make(findViewById(R.id.main),
                            getString(R.string.incoming_like_snackbar, displayName),
                            Snackbar.LENGTH_LONG)
                    .setAction(R.string.matches_open_button, v -> openMatchesScreen())
                    .show();
        }
    }

    @NonNull
    private FilterPreferences buildDefaultFilters() {
        FilterPreferences defaults = new FilterPreferences();
        if (currentUser != null) {
            if (!TextUtils.isEmpty(currentUser.getSeekingGender())) {
                defaults.setInterestedIn(normalizeInterestValue(currentUser.getSeekingGender()));
            }
            if (currentUser.getSeekingAgeMin() > 0) {
                defaults.setMinAge(currentUser.getSeekingAgeMin());
            }
            if (currentUser.getSeekingAgeMax() > 0) {
                int max = Math.max(defaults.getMinAge(), currentUser.getSeekingAgeMax());
                defaults.setMaxAge(max);
            }
        }
        return defaults;
    }

    private String getActiveLocationLabel() {
        if (filterPreferences != null && !TextUtils.isEmpty(filterPreferences.getLocationLabel())) {
            return filterPreferences.getLocationLabel();
        }
        return currentLocationLabel;
    }

    private void updateLocationDisplay() {
        if (filterPreferences != null && !TextUtils.isEmpty(filterPreferences.getLocationLabel())) {
            locationTextView.setText(filterPreferences.getLocationLabel());
        } else if (!TextUtils.isEmpty(currentLocationLabel)) {
            locationTextView.setText(currentLocationLabel);
        } else {
            locationTextView.setText(R.string.home_location_placeholder);
        }
    }

    private String normalizeInterestValue(@NonNull String raw) {
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("f") || normalized.contains("nữ")) {
            return FilterPreferences.INTEREST_FEMALE;
        }
        if (normalized.startsWith("m") || normalized.contains("nam")) {
            return FilterPreferences.INTEREST_MALE;
        }
        return FilterPreferences.INTEREST_BOTH;
    }

    private String buildDisplayName(User user) {
        String name = !TextUtils.isEmpty(user.getName()) ? user.getName() : getString(R.string.home_user_name_age);
        int age = calculateAge(user.getDateOfBirth());
        if (age > 0) {
            return name + ", " + age;
        }
        return name;
    }

    private int calculateAge(@Nullable String dob) {
        if (TextUtils.isEmpty(dob)) {
            return -1;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault());
                LocalDate birthDate = LocalDate.parse(dob, formatter);
                return Period.between(birthDate, LocalDate.now()).getYears();
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date date = sdf.parse(dob);
                if (date == null) {
                    return -1;
                }
                long ageInMillis = System.currentTimeMillis() - date.getTime();
                return (int) (ageInMillis / (1000L * 60 * 60 * 24 * 365));
            }
        } catch (ParseException | java.time.format.DateTimeParseException e) {
            return -1;
        }
    }

    private String extractPrimaryPhoto(User candidate) {
        if (candidate.getPhotoUrls() != null && !candidate.getPhotoUrls().isEmpty()) {
            return candidate.getPhotoUrls().get(0);
        }
        return null;
    }

    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS_KM = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(dLon / 2), 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    @Override
    protected void onDestroy() {
        stopListeningForIncomingLikes();
        super.onDestroy();
        geocoderExecutor.shutdownNow();
    }
}

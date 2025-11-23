package vn.haui.heartlink.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
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
import vn.haui.heartlink.activities.MainActivity;
import vn.haui.heartlink.activities.MatchSuccessActivity;
import vn.haui.heartlink.adapters.DiscoveryCardAdapter;
import vn.haui.heartlink.models.DiscoveryProfile;
import vn.haui.heartlink.models.FilterPreferences;
import vn.haui.heartlink.models.User;
import vn.haui.heartlink.ui.FilterBottomSheetDialog;
import vn.haui.heartlink.utils.MatchRepository;
import vn.haui.heartlink.utils.UserRepository;

public class DiscoveryFragment extends Fragment {

    private static final double NEARBY_THRESHOLD_KM = 50.0;
    private static final String PREFS_NAME = "FilterPrefs";
    private static final String KEY_INTERESTED_IN = "interestedIn";
    private static final String KEY_MIN_AGE = "minAge";
    private static final String KEY_MAX_AGE = "maxAge";
    private static final String KEY_MAX_DISTANCE = "maxDistance";

    private TextView locationTextView;
    private CardStackView cardStackView;
    private ProgressBar loadingIndicator;
    private TextView emptyStateView;
    private View dislikeButton;
    private View likeButton;
    private View superLikeButton;
    private ImageButton filterButton;

    private DiscoveryCardAdapter cardAdapter;
    private CardStackLayoutManager cardStackLayoutManager;

    private final UserRepository userRepository = UserRepository.getInstance();
    private final MatchRepository matchRepository = MatchRepository.getInstance();
    private final ExecutorService geocoderExecutor = Executors.newSingleThreadExecutor();

    @Nullable
    private User currentUser;
    private final List<DiscoveryProfile> discoveryProfiles = new ArrayList<>();
    private final Set<String> excludedUserIds = new HashSet<>();
    private FilterPreferences filterPreferences;
    @Nullable
    private String currentLocationLabel;
    private NavigationListener navigationListener;
    private ValueEventListener interactionsListener;

    public void forceRefresh() {
        loadCurrentUser();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof NavigationListener) {
            navigationListener = (NavigationListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement NavigationListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_discovery, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupCardStack();
        hookupActions();
        loadCurrentUser();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (currentUser != null && interactionsListener != null) {
            matchRepository.removeInteractionsListener(currentUser.getUid(), interactionsListener);
        }
    }

    private void bindViews(View view) {
        locationTextView = view.findViewById(R.id.home_location);
        cardStackView = view.findViewById(R.id.home_card_stack);
        loadingIndicator = view.findViewById(R.id.home_loading_indicator);
        emptyStateView = view.findViewById(R.id.home_empty_state);
        dislikeButton = view.findViewById(R.id.home_dislike_button);
        likeButton = view.findViewById(R.id.home_like_button);
        superLikeButton = view.findViewById(R.id.home_superlike_button);
        filterButton = view.findViewById(R.id.home_filter_button);

        locationTextView.setText(R.string.home_location_placeholder);
    }

    private void setupCardStack() {
        cardAdapter = new DiscoveryCardAdapter();
        cardStackLayoutManager = new CardStackLayoutManager(getContext(), new CardStackListener() {
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
            public void onCardRewound() { }

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

    private void hookupActions() {
        dislikeButton.setOnClickListener(v -> performSwipe(Direction.Left));
        likeButton.setOnClickListener(v -> performSwipe(Direction.Right));
        superLikeButton.setOnClickListener(v -> performSwipe(Direction.Top));
        filterButton.setOnClickListener(v -> openFilterBottomSheet());
    }

    private void loadCurrentUser() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(getContext(), R.string.error_generic, Toast.LENGTH_SHORT).show();
            if (getActivity() != null) getActivity().finish();
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
                    loadFilterPreferences();
                    updateLocationText();
                    updateLocationDisplay();
                    loadSuggestions();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    showError(e.getLocalizedMessage());
                });
    }

    private void loadSuggestions() {
        if (currentUser == null || TextUtils.isEmpty(currentUser.getUid())) {
            return;
        }

        interactionsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                excludedUserIds.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String otherUid = child.getKey();
                    if (!TextUtils.isEmpty(otherUid)) {
                        excludedUserIds.add(otherUid);
                    }
                }
                fetchAllUsers();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                excludedUserIds.clear();
                fetchAllUsers();
            }
        };
        matchRepository.addInteractionsListener(currentUser.getUid(), interactionsListener);
    }

    private void fetchAllUsers() {
        userRepository.getAllUsers()
                .addOnSuccessListener(this::handleSuggestionsSnapshot)
                .addOnFailureListener(e -> {
                    showLoading(false);
                    showError(e.getLocalizedMessage());
                });
    }

    private void handleSuggestionsSnapshot(DataSnapshot snapshot) {
        discoveryProfiles.clear();

        if (currentUser == null) {
            showLoading(false);
            return;
        }

        Double baseLat;
        Double baseLng;
        if (filterPreferences.hasCustomLocation()) {
            baseLat = filterPreferences.getLocationLatitude();
            baseLng = filterPreferences.getLocationLongitude();
        } else {
            baseLat = currentUser.getLatitude();
            baseLng = currentUser.getLongitude();
        }

        boolean canComputeDistance = baseLat != null && baseLng != null;
        float maxDistance = filterPreferences.getMaxDistanceKm();
        String interested = filterPreferences.getInterestedIn();
        int minAge = filterPreferences.getMinAge();
        int maxAge = filterPreferences.getMaxAge();

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

    private void showLoading(boolean show) {
        loadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        cardStackView.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
    }

    private void updateEmptyState() {
        boolean isEmpty = discoveryProfiles.isEmpty();
        emptyStateView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        cardStackView.setVisibility(isEmpty ? View.INVISIBLE : View.VISIBLE);
    }

    private void showError(@Nullable String message) {
        if (getView() == null || !isAdded()) return;
        if (TextUtils.isEmpty(message)) {
            message = getString(R.string.error_generic);
        }
        Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
    }

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
            if (getContext() == null || !Geocoder.isPresent()) {
                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                    currentLocationLabel = null;
                    updateLocationDisplay();
                });
                return;
            }
            Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
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
                    if (getActivity() != null) getActivity().runOnUiThread(() -> {
                        currentLocationLabel = label;
                        updateLocationDisplay();
                    });
                } else {
                    if (getActivity() != null) getActivity().runOnUiThread(() -> {
                        currentLocationLabel = null;
                        updateLocationDisplay();
                    });
                }
            } catch (IOException e) {
                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                    currentLocationLabel = null;
                    updateLocationDisplay();
                });
            }
        });
    }

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
        Toast.makeText(getContext(), getString(R.string.home_button_dislike), Toast.LENGTH_SHORT).show();
    }

    private void handleSuperLike(DiscoveryProfile profile) {
        processSwipeAction(profile, true);
    }

    private void processSwipeAction(DiscoveryProfile profile, boolean isSuperLike) {
        if (currentUser == null || profile == null || profile.getUser() == null) {
            showError(getString(R.string.match_creation_error, getString(R.string.error_generic)));
            return;
        }

        // Notify MainActivity TRƯỚC khi tạo match để add vào selfInitiatedMatches
        // Điều này ngăn MainActivity listener hiển thị dialog cho người swipe
        if (navigationListener != null && profile.getUser().getUid() != null) {
            navigationListener.onMatchCreatedByUser(profile.getUser().getUid());
        }

        matchRepository.likeUser(currentUser, profile.getUser(), isSuperLike, new MatchRepository.MatchResultCallback() {
            @Override
            public void onLikeRecorded() {
                int messageRes = isSuperLike
                        ? R.string.match_superlike_sent
                        : R.string.match_like_sent;
                if (getActivity() != null) getActivity().runOnUiThread(() -> Toast.makeText(getContext(), messageRes, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onMatchCreated() {
                if (getActivity() != null) getActivity().runOnUiThread(() -> launchMatchSuccess(profile));
            }

            @Override
            public void onAlreadyMatched() {
                if (getActivity() != null) getActivity().runOnUiThread(() -> launchMatchSuccess(profile));
            }

            @Override
            public void onError(@NonNull Exception throwable) {
                if (getActivity() != null) getActivity().runOnUiThread(() -> showError(getString(R.string.match_creation_error, throwable.getLocalizedMessage())));
            }
        });

        if (profile.getUser() != null && !TextUtils.isEmpty(profile.getUser().getUid())) {
            excludedUserIds.add(profile.getUser().getUid());
        }
    }

    private void launchMatchSuccess(DiscoveryProfile profile) {
        if (getContext() == null || profile.getUser() == null) return;
         if (navigationListener != null) {
             navigationListener.onMatchCreatedByUser(profile.getUser().getUid());
         }
        String partnerName = !TextUtils.isEmpty(profile.getUser().getName())
                ? profile.getUser().getName()
                : profile.getDisplayName();
        startActivity(MatchSuccessActivity.createIntent(
                getContext(),
                profile.getUser().getUid(), // Correctly pass partnerId here
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
        if (getFragmentManager() == null) return;
        FilterBottomSheetDialog dialog = FilterBottomSheetDialog.newInstance(filterPreferences, getActiveLocationLabel());
        dialog.setFilterApplyListener(new FilterBottomSheetDialog.FilterApplyListener() {
            @Override
            public void onApply(@NonNull FilterPreferences preferences) {
                filterPreferences = preferences;
                saveFilterPreferences();
                updateLocationDisplay();
                loadSuggestions();
            }

            @Override
            public void onClear() {
                filterPreferences = buildDefaultFilters();
                saveFilterPreferences();
                updateLocationDisplay();
                loadSuggestions();
            }
        });
        dialog.show(getParentFragmentManager(), "discovery_filters");
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

    private void loadFilterPreferences() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.contains(KEY_INTERESTED_IN)) {
            filterPreferences = buildDefaultFilters();
            saveFilterPreferences();
        } else {
            String interestedIn = prefs.getString(KEY_INTERESTED_IN, FilterPreferences.INTEREST_BOTH);
            int minAge = prefs.getInt(KEY_MIN_AGE, 18);
            int maxAge = prefs.getInt(KEY_MAX_AGE, 35);
            float maxDistance = prefs.getFloat(KEY_MAX_DISTANCE, (float) NEARBY_THRESHOLD_KM);

            filterPreferences = new FilterPreferences();
            filterPreferences.setInterestedIn(interestedIn);
            filterPreferences.setMinAge(minAge);
            filterPreferences.setMaxAge(maxAge);
            filterPreferences.setMaxDistanceKm(maxDistance);
        }
    }

    private void saveFilterPreferences() {
        if (getContext() == null) return;
        SharedPreferences.Editor editor = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(KEY_INTERESTED_IN, filterPreferences.getInterestedIn());
        editor.putInt(KEY_MIN_AGE, filterPreferences.getMinAge());
        editor.putInt(KEY_MAX_AGE, filterPreferences.getMaxAge());
        editor.putFloat(KEY_MAX_DISTANCE, filterPreferences.getMaxDistanceKm());
        editor.apply();
    }

    private String getActiveLocationLabel() {
        if (filterPreferences != null && !TextUtils.isEmpty(filterPreferences.getLocationLabel())) {
            return filterPreferences.getLocationLabel();
        }
        return currentLocationLabel;
    }

    private void updateLocationDisplay() {
        String locationLabel = getActiveLocationLabel();
        if (!TextUtils.isEmpty(locationLabel)) {
            locationTextView.setText(locationLabel);
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
        if (candidate != null && candidate.getPhotoUrls() != null && !candidate.getPhotoUrls().isEmpty()) {
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
    public void onDetach() {
        super.onDetach();
        navigationListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        geocoderExecutor.shutdownNow();
    }

    public interface NavigationListener {
        void onNavigateToMatches();
        void onNavigateToMessages();
        void onMatchCreatedByUser(String partnerId);
    }
}
package vn.haui.heartlink.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vn.haui.heartlink.R;
import vn.haui.heartlink.adapters.ProfilePhotoAdapter;
import vn.haui.heartlink.models.User;
import vn.haui.heartlink.utils.MatchRepository;
import vn.haui.heartlink.utils.UserRepository;

public class ProfileDetailActivity extends AppCompatActivity {

    private static final String EXTRA_USER_ID = "extra_user_id";
    private static final String EXTRA_DISPLAY_NAME = "extra_display_name";
    private static final String EXTRA_PHOTO_URL = "extra_photo_url";

    private static final int BIO_COLLAPSED_LINES = 4;

    private final UserRepository userRepository = UserRepository.getInstance();
    private final MatchRepository matchRepository = MatchRepository.getInstance();
    private final ExecutorService geocoderExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private View rootView;
    private ImageView headerImageView;
    private TextView nameView;
    private TextView jobView;
    private TextView locationView;
    private TextView distanceView;
    private TextView bioView;
    private TextView bioToggleView;
    private ChipGroup interestsGroup;
    private TextView interestsEmptyView;
    private RecyclerView photosRecycler;
    private TextView photosViewAll;
    private NestedScrollView scrollView;
    private View contentContainer;
    private View primaryActions;
    private ProgressBar loadingView;
    private ProfilePhotoAdapter photoAdapter;

    private String partnerUid;
    private String currentUid;
    private ValueEventListener interactionListener;
    private boolean isBioExpanded = false;

    public static Intent createIntent(@NonNull Context context,
                                      @NonNull String partnerUid,
                                      @Nullable String fallbackName,
                                      @Nullable String fallbackPhotoUrl) {
        Intent intent = new Intent(context, ProfileDetailActivity.class);
        intent.putExtra(EXTRA_USER_ID, partnerUid);
        if (!TextUtils.isEmpty(fallbackName)) {
            intent.putExtra(EXTRA_DISPLAY_NAME, fallbackName);
        }
        if (!TextUtils.isEmpty(fallbackPhotoUrl)) {
            intent.putExtra(EXTRA_PHOTO_URL, fallbackPhotoUrl);
        }
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile_detail);

        rootView = findViewById(R.id.profileRoot);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(view.getPaddingLeft(), systemBars.top, view.getPaddingRight(), Math.max(view.getPaddingBottom(), systemBars.bottom));
            return insets;
        });

        bindViews();
        setupRecycler();
        hydrateFromIntent();
        if (isFinishing()) {
            return;
        }
        setupActions();
        loadPartnerProfile();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        geocoderExecutor.shutdownNow();
        if (currentUid != null && partnerUid != null && interactionListener != null) {
            matchRepository.removeInteractionsListener(currentUid, interactionListener);
        }
    }

    private void bindViews() {
        headerImageView = findViewById(R.id.profileHeaderImage);
        nameView = findViewById(R.id.profileName);
        jobView = findViewById(R.id.profileJob);
        locationView = findViewById(R.id.profileLocation);
        distanceView = findViewById(R.id.profileDistance);
        bioView = findViewById(R.id.profileBio);
        bioToggleView = findViewById(R.id.profileBioToggle);
        interestsGroup = findViewById(R.id.profileInterestsGroup);
        interestsEmptyView = findViewById(R.id.profileInterestsEmpty);
        photosRecycler = findViewById(R.id.profilePhotosRecycler);
        photosViewAll = findViewById(R.id.profilePhotosViewAll);
        scrollView = findViewById(R.id.profileScroll);
        contentContainer = findViewById(R.id.profileContentContainer);
        primaryActions = findViewById(R.id.profilePrimaryActions);
        loadingView = findViewById(R.id.profileLoading);
    }

    private void setupRecycler() {
        photoAdapter = new ProfilePhotoAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false);
        photosRecycler.setLayoutManager(layoutManager);
        photosRecycler.setAdapter(photoAdapter);
    }

    private void setupActions() {
        ImageButton backButton = findViewById(R.id.profileBackButton);
        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        photosViewAll.setOnClickListener(v -> Toast.makeText(this, R.string.matches_action_coming_soon, Toast.LENGTH_SHORT).show());
        bioToggleView.setOnClickListener(v -> toggleBioExpand());

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            primaryActions.setVisibility(View.GONE);
            return;
        }
        currentUid = currentUser.getUid();

        interactionListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // We listen to the whole interaction node for the user now, so find the specific partner
                DataSnapshot partnerInteraction = snapshot.child(partnerUid);
                String status = partnerInteraction.child("status").getValue(String.class);
                updateActionButtons(status);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        };
        matchRepository.addInteractionsListener(currentUid, interactionListener);
    }

    private void updateActionButtons(@Nullable String status) {
        FrameLayout dislikeButton = findViewById(R.id.profileDislikeButton);
        FrameLayout likeButton = findViewById(R.id.profileLikeButton);
        FrameLayout superLikeButton = findViewById(R.id.profileSuperLikeButton);

        // Reset to default state first
        likeButton.setAlpha(1.0f);
        likeButton.setOnClickListener(v -> performLike(false));

        if (MatchRepository.STATUS_LIKED.equals(status)) {
            likeButton.setAlpha(0.5f); // Visually indicate it's liked
            likeButton.setOnClickListener(v -> performUnlike());
        } else if (MatchRepository.STATUS_MATCHED.equals(status)) {
            // Handle matched state if needed (e.g., show a message button)
            likeButton.setAlpha(0.5f);
            likeButton.setOnClickListener(null); // Or open chat
        }

        // You can add more logic for dislike and superlike here if you want
        dislikeButton.setOnClickListener(v -> Toast.makeText(this, "Dislike coming soon", Toast.LENGTH_SHORT).show());
        superLikeButton.setOnClickListener(v -> performLike(true));
    }

    private void performLike(boolean isSuperLike) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null || partnerUid == null) return;

        userRepository.getUserData(firebaseUser.getUid()).addOnSuccessListener(mySnapshot -> {
            User me = mySnapshot.getValue(User.class);
            userRepository.getUserData(partnerUid).addOnSuccessListener(partnerSnapshot -> {
                User partner = partnerSnapshot.getValue(User.class);
                if (me != null && partner != null) {
                    matchRepository.likeUser(me, partner, isSuperLike, new MatchRepository.MatchResultCallback() {
                        @Override
                        public void onLikeRecorded() {
                            Snackbar.make(rootView, isSuperLike ? R.string.match_superlike_sent : R.string.match_like_sent, Snackbar.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onMatchCreated() {
                            // Handle match success UI
                        }

                        @Override
                        public void onError(@NonNull Exception throwable) {
                            showError(throwable.getMessage());
                        }
                    });
                }
            });
        });
    }

    private void performUnlike() {
        if (currentUid == null || partnerUid == null) return;
        matchRepository.removeInteraction(currentUid, partnerUid)
                .addOnSuccessListener(aVoid -> Snackbar.make(rootView, R.string.matches_action_unlike, Snackbar.LENGTH_SHORT).show())
                .addOnFailureListener(e -> showError(e.getMessage()));
    }

    private void hydrateFromIntent() {
        Intent intent = getIntent();
        partnerUid = intent.getStringExtra(EXTRA_USER_ID);

        if (TextUtils.isEmpty(partnerUid)) {
            Toast.makeText(this, R.string.error_generic, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String fallbackName = intent.getStringExtra(EXTRA_DISPLAY_NAME);
        if (!TextUtils.isEmpty(fallbackName)) {
            nameView.setText(fallbackName);
        }
        String fallbackPhotoUrl = intent.getStringExtra(EXTRA_PHOTO_URL);
        if (!TextUtils.isEmpty(fallbackPhotoUrl)) {
            loadProfileImage(fallbackPhotoUrl);
        } else {
            headerImageView.setImageResource(R.drawable.welcome_person_1);
        }

        jobView.setText(R.string.profile_job_placeholder);
        distanceView.setVisibility(View.GONE);
        locationView.setText(R.string.profile_location_unknown);
        bioView.setText(R.string.profile_bio_placeholder);
        bioView.setMaxLines(BIO_COLLAPSED_LINES);
    }

    private void loadPartnerProfile() {
        showContent(false, false);
        Task<DataSnapshot> partnerTask = userRepository.getUserData(partnerUid);
        partnerTask
                .addOnSuccessListener(snapshot -> {
                    User partner = snapshot.getValue(User.class);
                    if (partner == null) {
                        showError(getString(R.string.error_user_not_found));
                        finish();
                        return;
                    }
                    bindPartner(partner);
                    fetchCurrentUserForDistance(partner);
                })
                .addOnFailureListener(throwable -> {
                    showError(throwable.getLocalizedMessage());
                    finish();
                });
    }

    private void bindPartner(@NonNull User partner) {
        nameView.setText(buildDisplayName(partner));
        bindPrimaryPhoto(partner);
        bindBio(partner);
        bindInterests(partner);
        resolveLocation(partner);
        showContent(true, true);
    }

    private void bindPrimaryPhoto(@NonNull User partner) {
        List<String> photos = partner.getPhotoUrls();
        if (photos != null && !photos.isEmpty()) {
            loadProfileImage(photos.get(0));
            photoAdapter.submitPhotos(photos);
            photosRecycler.setVisibility(View.VISIBLE);
            photosViewAll.setVisibility(photos.size() > 3 ? View.VISIBLE : View.GONE);
        } else {
            headerImageView.setImageResource(R.drawable.welcome_person_1);
            photosRecycler.setVisibility(View.GONE);
            photosViewAll.setVisibility(View.GONE);
        }
    }

    private void bindBio(@NonNull User partner) {
        String bio = partner.getBio();
        if (TextUtils.isEmpty(bio)) {
            bioView.setText(R.string.profile_bio_placeholder);
            bioToggleView.setVisibility(View.GONE);
            return;
        }

        bioView.setText(bio);
        bioView.setMaxLines(BIO_COLLAPSED_LINES);
        bioView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        bioView.post(() -> {
            if (bioView.getLineCount() > BIO_COLLAPSED_LINES) {
                bioToggleView.setVisibility(View.VISIBLE);
                bioToggleView.setText(R.string.profile_read_more);
            } else {
                bioToggleView.setVisibility(View.GONE);
            }
        });
    }

    private void bindInterests(@NonNull User partner) {
        List<String> interests = partner.getInterests();
        interestsGroup.removeAllViews();
        if (interests == null || interests.isEmpty()) {
            interestsGroup.setVisibility(View.GONE);
            interestsEmptyView.setVisibility(View.VISIBLE);
            return;
        }

        interestsGroup.setVisibility(View.VISIBLE);
        interestsEmptyView.setVisibility(View.GONE);

    float density = getResources().getDisplayMetrics().density;
    float strokeWidth = density;
    float horizontalPadding = 12f * density;
    float cornerRadius = 18f * density;
        int backgroundColor = ContextCompat.getColor(this, R.color.profile_chip_background);
        int strokeColor = ContextCompat.getColor(this, R.color.profile_chip_stroke);
        int textColor = ContextCompat.getColor(this, R.color.colorPrimary);
        ColorStateList bgStateList = ColorStateList.valueOf(backgroundColor);
        ColorStateList strokeStateList = ColorStateList.valueOf(strokeColor);

        for (String interest : interests) {
            if (TextUtils.isEmpty(interest)) {
                continue;
            }
            Chip chip = new Chip(this);
            chip.setText(interest);
            chip.setTextColor(textColor);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            chip.setChipBackgroundColor(bgStateList);
            chip.setChipStrokeColor(strokeStateList);
            chip.setChipStrokeWidth(strokeWidth);
            chip.setChipStartPadding(horizontalPadding);
            chip.setChipEndPadding(horizontalPadding);
            chip.setChipCornerRadius(cornerRadius);
            chip.setCheckable(false);
            chip.setClickable(false);
            chip.setEnsureMinTouchTargetSize(false);
            chip.setEllipsize(android.text.TextUtils.TruncateAt.END);
            interestsGroup.addView(chip);
        }
    }

    private void resolveLocation(@NonNull User partner) {
        Double lat = partner.getLatitude();
        Double lng = partner.getLongitude();
        if (lat == null || lng == null) {
            locationView.setText(R.string.profile_location_unknown);
            return;
        }

        geocoderExecutor.execute(() -> {
            if (!Geocoder.isPresent()) {
                postLocation(getString(R.string.profile_location_unknown));
                return;
            }
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> results = geocoder.getFromLocation(lat, lng, 1);
                if (results != null && !results.isEmpty()) {
                    Address address = results.get(0);
                    List<String> components = new ArrayList<>();
                    if (!TextUtils.isEmpty(address.getSubAdminArea())) {
                        components.add(address.getSubAdminArea());
                    }
                    if (!TextUtils.isEmpty(address.getAdminArea())) {
                        components.add(address.getAdminArea());
                    }
                    if (components.isEmpty() && !TextUtils.isEmpty(address.getLocality())) {
                        components.add(address.getLocality());
                    }
                    if (!components.isEmpty()) {
                        postLocation(TextUtils.join(", ", components));
                    } else {
                        postLocation(getString(R.string.profile_location_unknown));
                    }
                } else {
                    postLocation(getString(R.string.profile_location_unknown));
                }
            } catch (IOException | IllegalArgumentException e) {
                postLocation(getString(R.string.profile_location_unknown));
            }
        });
    }

    private void postLocation(@NonNull String locationText) {
        mainHandler.post(() -> locationView.setText(locationText));
    }

    private void fetchCurrentUserForDistance(@NonNull User partner) {
        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
        if (current == null) {
            distanceView.setVisibility(View.GONE);
            return;
        }

        userRepository.getUserData(current.getUid())
                .addOnSuccessListener(snapshot -> {
                    User me = snapshot.getValue(User.class);
                    if (me == null) {
                        distanceView.setVisibility(View.GONE);
                        return;
                    }
                    updateDistanceForDisplay(partner, me);
                })
                .addOnFailureListener(throwable -> distanceView.setVisibility(View.GONE));
    }

    private void updateDistanceForDisplay(@NonNull User partner, @NonNull User me) {
        Double baseLat = me.getLatitude();
        Double baseLng = me.getLongitude();
        Double partnerLat = partner.getLatitude();
        Double partnerLng = partner.getLongitude();
        if (baseLat == null || baseLng == null || partnerLat == null || partnerLng == null) {
            distanceView.setVisibility(View.GONE);
            return;
        }

        double distanceKm = haversineDistance(baseLat, baseLng, partnerLat, partnerLng);
        if (Double.isNaN(distanceKm) || Double.isInfinite(distanceKm)) {
            distanceView.setVisibility(View.GONE);
            return;
        }
        distanceView.setVisibility(View.VISIBLE);
        distanceView.setText(formatDistance(distanceKm));
    }

    private void toggleBioExpand() {
        if (bioToggleView.getVisibility() != View.VISIBLE) {
            return;
        }
        isBioExpanded = !isBioExpanded;
        bioView.setMaxLines(isBioExpanded ? Integer.MAX_VALUE : BIO_COLLAPSED_LINES);
        bioView.setEllipsize(isBioExpanded ? null : android.text.TextUtils.TruncateAt.END);
        bioToggleView.setText(isBioExpanded ? R.string.profile_read_less : R.string.profile_read_more);
    }

    private void loadProfileImage(@NonNull String url) {
        Glide.with(this)
                .load(url)
                .placeholder(R.drawable.welcome_person_1)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(headerImageView);
    }

    private void showContent(boolean show, boolean showActions) {
        if (show) {
            loadingView.setVisibility(View.GONE);
            scrollView.setVisibility(View.VISIBLE);
            primaryActions.setVisibility(showActions ? View.VISIBLE : View.GONE);
            contentContainer.setVisibility(View.VISIBLE);
        } else {
            loadingView.setVisibility(View.VISIBLE);
            scrollView.setVisibility(View.GONE);
            primaryActions.setVisibility(View.GONE);
            contentContainer.setVisibility(View.GONE);
        }
    }

    private void showError(@Nullable String message) {
        if (TextUtils.isEmpty(message)) {
            message = getString(R.string.error_generic);
        }
        Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();
    }

    private String buildDisplayName(@NonNull User partner) {
        String name = partner.getName();
        int age = calculateAge(partner.getDateOfBirth());
        if (!TextUtils.isEmpty(name)) {
            if (age > 0) {
                return getString(R.string.profile_name_with_age, name, age);
            }
            return name;
        }
        if (age > 0) {
            return getString(R.string.profile_age_fallback, age);
        }
        return getString(R.string.profile_name_fallback);
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
                java.util.Date date = sdf.parse(dob);
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

    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double radius = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return radius * c;
    }

    private String formatDistance(double distanceKm) {
        if (distanceKm < 1) {
            return String.format(Locale.getDefault(), "%dm", Math.round(distanceKm * 1000));
        }
        return String.format(Locale.getDefault(), "%.0f km", distanceKm);
    }
}

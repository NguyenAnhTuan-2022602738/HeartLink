package vn.haui.heartlink.activities;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import vn.haui.heartlink.R;
import vn.haui.heartlink.adapters.MatchesAdapter;
import vn.haui.heartlink.models.User;
import vn.haui.heartlink.utils.MatchRepository;
import vn.haui.heartlink.utils.UserRepository;
import vn.haui.heartlink.utils.ChatRepository;

/**
 * Displays the matches tab showing mutual connections grouped by recency.
 */
public class MatchesActivity extends AppCompatActivity implements MatchesAdapter.MatchActionListener {

    private View rootView;
    private RecyclerView matchesRecyclerView;
    private View loadingView;
    private TextView emptyStateView;
    private MatchesAdapter matchesAdapter;

    private final MatchRepository matchRepository = MatchRepository.getInstance();
    private final UserRepository userRepository = UserRepository.getInstance();
    private final ChatRepository chatRepository = ChatRepository.getInstance();

    @Nullable
    private User currentUser;
    @Nullable
    private String currentUid;
    private final Map<String, MatchInteraction> interactionCache = new HashMap<>();

    /**
     * Phương thức khởi tạo activity hiển thị danh sách matches.
     * Thiết lập edge-to-edge display, bind views và load dữ liệu matches.
     *
     * @param savedInstanceState Trạng thái đã lưu của activity (có thể null)
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_matches);

        rootView = findViewById(R.id.matches_root);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(view.getPaddingLeft(), systemBars.top, view.getPaddingRight(), Math.max(view.getPaddingBottom(), systemBars.bottom));
            return insets;
        });

        bindViews();
        setupRecyclerView();
        setupClicks();
        loadMatches();
    }

    /**
     * Phương thức bind các view từ layout vào các biến thành viên.
     */
    private void bindViews() {
        matchesRecyclerView = findViewById(R.id.matches_list);
        loadingView = findViewById(R.id.matches_loading);
        emptyStateView = findViewById(R.id.matches_empty_state);
    }

    /**
     * Phương thức thiết lập RecyclerView với GridLayoutManager và MatchesAdapter.
     */
    private void setupRecyclerView() {
        matchesAdapter = new MatchesAdapter(this);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (matchesAdapter.getItemCount() == 0) {
                    return 2;
                }
                int viewType = matchesAdapter.getItemViewType(position);
                return viewType == 0 ? 2 : 1;
            }
        });
        matchesRecyclerView.setLayoutManager(layoutManager);
        matchesRecyclerView.setAdapter(matchesAdapter);
    }

    /**
     * Phương thức thiết lập các click listeners cho các button điều hướng.
     */
    private void setupClicks() {
        ImageButton backButton = findViewById(R.id.matches_back_button);
        ImageButton filterButton = findViewById(R.id.matches_filter_button);

    backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        filterButton.setOnClickListener(v -> Toast.makeText(this, R.string.matches_action_coming_soon, Toast.LENGTH_SHORT).show());
    }

    /**
     * Phương thức tải danh sách matches và interactions của người dùng hiện tại.
     * Gọi MatchRepository để lấy dữ liệu từ Firebase.
     */
    private void loadMatches() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            showError(getString(R.string.error_generic));
            finish();
            return;
        }

        showLoading(true);
        currentUid = firebaseUser.getUid();
        userRepository.getUserData(currentUid)
                .addOnSuccessListener(snapshot -> {
                    currentUser = snapshot.getValue(User.class);
                    if (currentUser == null) {
                        currentUser = new User();
                        currentUser.setUid(currentUid);
                    } else if (TextUtils.isEmpty(currentUser.getUid())) {
                        currentUser.setUid(currentUid);
                    }
                    loadInteractionSnapshot();
                })
                .addOnFailureListener(throwable -> {
                    showLoading(false);
                    showError(throwable.getLocalizedMessage());
                });
    }

    /**
     * Phương thức tải snapshot của các interactions (likes) từ Firebase.
     */
    private void loadInteractionSnapshot() {
        if (TextUtils.isEmpty(currentUid)) {
            showLoading(false);
            return;
        }
        matchRepository.getInteractionsSnapshot(currentUid)
                .addOnSuccessListener(snapshot -> handleInteractionSnapshot(currentUid, snapshot))
                .addOnFailureListener(throwable -> {
                    showLoading(false);
                    showError(throwable.getLocalizedMessage());
                });
    }

    /**
     * Phương thức xử lý snapshot dữ liệu interactions từ Firebase.
     * Chuyển đổi dữ liệu thành danh sách MatchInteraction.
     *
     * @param currentUid UID của người dùng hiện tại
     * @param snapshot Snapshot dữ liệu từ Firebase
     */
    private void handleInteractionSnapshot(@NonNull String currentUid, @Nullable DataSnapshot snapshot) {
        List<MatchInteraction> interactions = new ArrayList<>();
        if (snapshot != null) {
            for (DataSnapshot child : snapshot.getChildren()) {
                String partnerUid = child.getKey();
                String status = child.child("status").getValue(String.class);
                if (TextUtils.isEmpty(partnerUid) || TextUtils.isEmpty(status)) {
                    continue;
                }

                boolean isMutual = MatchRepository.STATUS_MATCHED.equals(status);
                boolean isIncomingLike = MatchRepository.STATUS_RECEIVED_LIKE.equals(status);
                if (!isMutual && !isIncomingLike) {
                    continue;
                }

                MatchInteraction interaction = new MatchInteraction(partnerUid);
                interaction.status = status;
                interaction.displayName = child.child("displayName").getValue(String.class);
                interaction.photoUrl = child.child("photoUrl").getValue(String.class);
                interaction.type = child.child("type").getValue(String.class);
                Long matchedAt = child.child("matchedAt").getValue(Long.class);
                Long likedAt = child.child("likedAt").getValue(Long.class);
                interaction.likedAt = likedAt != null ? likedAt : 0L;
                interaction.timestamp = isMutual
                        ? (matchedAt != null ? matchedAt : interaction.likedAt)
                        : interaction.likedAt;
                interactions.add(interaction);
            }
        }

        if (interactions.isEmpty()) {
            showLoading(false);
            matchesRecyclerView.setVisibility(View.GONE);
            emptyStateView.setVisibility(View.VISIBLE);
            return;
        }

        fetchPartnerDetails(interactions);
    }

    /**
     * Phương thức tải thông tin chi tiết của các partner trong danh sách interactions.
     *
     * @param interactions Danh sách các interactions cần tải thông tin partner
     */
    private void fetchPartnerDetails(@NonNull List<MatchInteraction> interactions) {
        Map<String, MatchInteraction> interactionMap = new LinkedHashMap<>();
        List<Task<DataSnapshot>> detailTasks = new ArrayList<>();
        for (MatchInteraction interaction : interactions) {
            interactionMap.put(interaction.partnerUid, interaction);
            detailTasks.add(userRepository.getUserData(interaction.partnerUid));
        }

        if (detailTasks.isEmpty()) {
            displayInteractions(interactionMap.values());
            return;
        }

        Tasks.whenAllComplete(detailTasks)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (Task<?> detail : task.getResult()) {
                            if (detail.isSuccessful()) {
                                Object result = detail.getResult();
                                if (result instanceof DataSnapshot) {
                                    DataSnapshot snapshot = (DataSnapshot) result;
                                    User user = snapshot.getValue(User.class);
                                    if (user != null && !TextUtils.isEmpty(user.getUid())) {
                                        MatchInteraction interaction = interactionMap.get(user.getUid());
                                        if (interaction != null) {
                                            interaction.applyUser(user);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    displayInteractions(interactionMap.values());
                })
                .addOnFailureListener(throwable -> {
                    showError(throwable.getLocalizedMessage());
                    displayInteractions(interactionMap.values());
                });
    }

    /**
     * Phương thức hiển thị danh sách interactions lên RecyclerView.
     * Nhóm các interactions theo thời gian và tạo CardItem để hiển thị.
     *
     * @param interactions Danh sách interactions cần hiển thị
     */
    private void displayInteractions(@NonNull Iterable<MatchInteraction> interactions) {
        List<MatchInteraction> sorted = new ArrayList<>();
        for (MatchInteraction interaction : interactions) {
            sorted.add(interaction);
        }
        Collections.sort(sorted, (a, b) -> Long.compare(b.timestamp, a.timestamp));

        interactionCache.clear();
        for (MatchInteraction interaction : sorted) {
            interactionCache.put(interaction.partnerUid, interaction);
        }

        List<MatchesAdapter.ListItem> items = buildAdapterItems(sorted);
        matchesAdapter.submitItems(items);

        showLoading(false);
        boolean hasItems = !items.isEmpty();
        matchesRecyclerView.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        emptyStateView.setVisibility(hasItems ? View.GONE : View.VISIBLE);
    }

    private List<MatchesAdapter.ListItem> buildAdapterItems(@NonNull List<MatchInteraction> interactions) {
        LinkedHashMap<String, List<MatchInteraction>> grouped = new LinkedHashMap<>();
        for (MatchInteraction interaction : interactions) {
            String section = resolveSectionLabel(interaction.timestamp);
            grouped.computeIfAbsent(section, key -> new ArrayList<>()).add(interaction);
        }

        List<MatchesAdapter.ListItem> items = new ArrayList<>();
        for (Map.Entry<String, List<MatchInteraction>> entry : grouped.entrySet()) {
            items.add(new MatchesAdapter.HeaderItem(entry.getKey()));
            for (MatchInteraction interaction : entry.getValue()) {
                String name = !TextUtils.isEmpty(interaction.displayName)
                        ? interaction.displayName
                        : getString(R.string.home_user_name_age);
                MatchesAdapter.InteractionState state = interaction.isMutualMatch()
                        ? MatchesAdapter.InteractionState.MUTUAL_MATCH
                        : MatchesAdapter.InteractionState.INCOMING_LIKE;
                items.add(new MatchesAdapter.CardItem(
                        interaction.partnerUid,
                        name,
                        interaction.photoUrl,
                        interaction.timestamp,
                        buildStatusLabel(interaction),
                        state,
                        interaction.type));
            }
        }
        return items;
    }

    private String buildStatusLabel(@NonNull MatchInteraction interaction) {
        if (interaction.isMutualMatch()) {
            return getString(R.string.matches_status_matched);
        }
        if (interaction.isIncomingLike()) {
            if (MatchRepository.isSuperLike(interaction.type)) {
                return getString(R.string.matches_status_superliked_you);
            }
            return getString(R.string.matches_status_liked_you);
        }
        return "";
    }

    private String resolveSectionLabel(long timestamp) {
        if (timestamp <= 0L) {
            return getString(R.string.matches_section_earlier);
        }
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(timestamp);

        Calendar today = Calendar.getInstance();
        if (isSameDay(today, target)) {
            return getString(R.string.matches_section_today);
        }

        Calendar yesterday = (Calendar) today.clone();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        if (isSameDay(yesterday, target)) {
            return getString(R.string.matches_section_yesterday);
        }

        return android.text.format.DateFormat.format("dd MMM yyyy", target).toString();
    }

    private boolean isSameDay(@NonNull Calendar first, @NonNull Calendar second) {
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Phương thức hiển thị hoặc ẩn loading indicator.
     *
     * @param show true để hiển thị loading, false để ẩn
     */
    private void showLoading(boolean show) {
        loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private boolean isActivityActive() {
        return !(isFinishing() || isDestroyed());
    }

    /**
     * Phương thức hiển thị thông báo lỗi cho người dùng.
     *
     * @param message Thông báo lỗi (có thể null)
     */
    private void showError(@Nullable String message) {
        if (TextUtils.isEmpty(message)) {
            message = getString(R.string.error_generic);
        }
        Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();
    }

    @Override
    /**
     * Phương thức callback khi người dùng tap vào card.
     * Mở màn hình chi tiết profile của match.
     *
     * @param item CardItem được tap
     */
    public void onCardTapped(@NonNull MatchesAdapter.CardItem item) {
        openProfileDetail(item);
    }

    @Override
    /**
     * Phương thức callback cho primary action (thường là mở chat).
     *
     * @param item CardItem được thực hiện action
     */
    public void onPrimaryAction(@NonNull MatchesAdapter.CardItem item) {
        MatchInteraction interaction = interactionCache.get(item.getPartnerUid());
        if (interaction == null) {
            return;
        }
        if (interaction.isMutualMatch()) {
            openChatWithMatch(item);
        } else if (interaction.isIncomingLike()) {
            respondToIncomingLike(interaction);
        }
    }

    @Override
    /**
     * Phương thức callback cho secondary action (thường là like back).
     *
     * @param item CardItem được thực hiện action
     */
    public void onSecondaryAction(@NonNull MatchesAdapter.CardItem item) {
        MatchInteraction interaction = interactionCache.get(item.getPartnerUid());
        if (interaction == null) {
            return;
        }
        if (interaction.isIncomingLike()) {
            dismissIncomingLike(interaction);
        }
    }

    /**
     * Phương thức mở màn hình chi tiết profile của match.
     *
     * @param item CardItem chứa thông tin match
     */
    private void openProfileDetail(@NonNull MatchesAdapter.CardItem item) {
        startActivity(ProfileDetailActivity.createIntent(
                this,
                item.getPartnerUid(),
                item.getDisplayName(),
                item.getPhotoUrl()));
    }

    /**
     * Phương thức mở màn hình chat với match.
     *
     * @param item CardItem chứa thông tin match
     */
    private void openChatWithMatch(@NonNull MatchesAdapter.CardItem item) {
        if (TextUtils.isEmpty(currentUid)) {
            showError(getString(R.string.error_generic));
            return;
        }
    chatRepository.ensureDirectChat(currentUid, item.getPartnerUid())
        .addOnSuccessListener(chatId -> startActivity(ChatActivity.createIntent(
            this,
            chatId,
            item.getPartnerUid(),
            item.getDisplayName(),
            item.getPhotoUrl())))
        .addOnFailureListener(e -> showError(e.getLocalizedMessage()));
    }

    /**
     * Phương thức xử lý phản hồi cho incoming like.
     * Hiển thị dialog để người dùng quyết định like back hay không.
     *
     * @param interaction MatchInteraction chứa thông tin like
     */
    private void respondToIncomingLike(@NonNull MatchInteraction interaction) {
        if (currentUser == null || TextUtils.isEmpty(currentUser.getUid())) {
            showError(getString(R.string.matches_like_error_missing_profile));
            return;
        }
        if (interaction.user != null) {
            performLikeBack(interaction, interaction.user);
            return;
        }

        userRepository.getUserData(interaction.partnerUid)
                .addOnSuccessListener(snapshot -> {
                    User user = snapshot.getValue(User.class);
                    runOnUiThread(() -> {
                        if (!isActivityActive()) {
                            return;
                        }
                        if (user != null) {
                            if (TextUtils.isEmpty(user.getUid())) {
                                user.setUid(interaction.partnerUid);
                            }
                            interaction.applyUser(user);
                            performLikeBack(interaction, user);
                        } else {
                            showError(getString(R.string.matches_like_error_missing_profile));
                        }
                    });
                })
                .addOnFailureListener(e -> runOnUiThread(() -> {
                    if (!isActivityActive()) {
                        return;
                    }
                    showError(e.getLocalizedMessage());
                }));
    }

    /**
     * Phương thức thực hiện like back cho incoming like.
     * Gọi MatchRepository để tạo match nếu có thể.
     *
     * @param interaction MatchInteraction chứa thông tin like
     * @param partner Thông tin user partner
     */
    private void performLikeBack(@NonNull MatchInteraction interaction, @NonNull User partner) {
        User actor = currentUser;
        if (actor == null) {
            showError(getString(R.string.matches_like_error_missing_profile));
            return;
        }
        if (TextUtils.isEmpty(actor.getUid())) {
            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if (firebaseUser != null) {
                actor.setUid(firebaseUser.getUid());
            }
        }

        matchRepository.likeUser(actor, partner, false, new MatchRepository.MatchResultCallback() {
            @Override
            public void onLikeRecorded() {
                runOnUiThread(() -> {
                    if (!isActivityActive()) {
                        return;
                    }
                    Snackbar.make(rootView, R.string.match_like_sent, Snackbar.LENGTH_SHORT).show();
                    loadInteractionSnapshot();
                });
            }

            @Override
            public void onMatchCreated() {
                runOnUiThread(() -> {
                    if (!isActivityActive()) {
                        return;
                    }
                    launchMatchSuccess(interaction, partner);
                    loadInteractionSnapshot();
                });
            }

            @Override
            public void onAlreadyMatched() {
                runOnUiThread(() -> {
                    if (!isActivityActive()) {
                        return;
                    }
                    launchMatchSuccess(interaction, partner);
                    loadInteractionSnapshot();
                });
            }

            @Override
            public void onError(@NonNull Exception throwable) {
                runOnUiThread(() -> {
                    if (!isActivityActive()) {
                        return;
                    }
                    showError(throwable.getLocalizedMessage());
                });
            }
        });
    }

    private void dismissIncomingLike(@NonNull MatchInteraction interaction) {
        if (TextUtils.isEmpty(currentUid)) {
            return;
        }
        matchRepository.removeInteraction(currentUid, interaction.partnerUid)
                .addOnCompleteListener(task -> runOnUiThread(() -> {
                    if (!isActivityActive()) {
                        return;
                    }
                    if (!task.isSuccessful()) {
                        Exception exception = task.getException();
                        showError(exception != null ? exception.getLocalizedMessage() : getString(R.string.error_generic));
                    } else {
                        Snackbar.make(rootView, R.string.matches_skip_success, Snackbar.LENGTH_SHORT).show();
                        interactionCache.remove(interaction.partnerUid);
                        matchesAdapter.removeCard(interaction.partnerUid);
                        loadInteractionSnapshot();
                    }
                }));
    }

    private void launchMatchSuccess(@NonNull MatchInteraction interaction, @NonNull User partner) {
        String partnerName = !TextUtils.isEmpty(interaction.displayName)
                ? interaction.displayName
                : partner.getName();
        String partnerPhoto = !TextUtils.isEmpty(interaction.photoUrl)
                ? interaction.photoUrl
                : extractPrimaryPhoto(partner);
        String selfPhoto = (currentUser != null
                && currentUser.getPhotoUrls() != null
                && !currentUser.getPhotoUrls().isEmpty())
                ? currentUser.getPhotoUrls().get(0)
                : null;

        startActivity(MatchSuccessActivity.createIntent(this, partnerName, partnerPhoto, selfPhoto));
    }

    @Nullable
    private String extractPrimaryPhoto(@NonNull User user) {
        if (user.getPhotoUrls() != null && !user.getPhotoUrls().isEmpty()) {
            return user.getPhotoUrls().get(0);
        }
        return null;
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

    private final class MatchInteraction {
        final String partnerUid;
        String displayName;
        String photoUrl;
        String status;
        String type;
        long timestamp;
        long likedAt;
        @Nullable
        User user;

        MatchInteraction(@NonNull String partnerUid) {
            this.partnerUid = partnerUid;
        }

        void applyUser(@NonNull User user) {
            this.user = user;
            if (TextUtils.isEmpty(user.getUid())) {
                user.setUid(partnerUid);
            }
            if (!TextUtils.isEmpty(user.getName())) {
                int age = calculateAge(user.getDateOfBirth());
                if (age > 0) {
                    displayName = user.getName() + ", " + age;
                } else {
                    displayName = user.getName();
                }
            }

            if (user.getPhotoUrls() != null && !user.getPhotoUrls().isEmpty()) {
                photoUrl = user.getPhotoUrls().get(0);
            }
        }

        boolean isMutualMatch() {
            return MatchRepository.STATUS_MATCHED.equals(status);
        }

        boolean isIncomingLike() {
            return MatchRepository.STATUS_RECEIVED_LIKE.equals(status);
        }
    }
}

package vn.haui.heartlink.fragments;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import vn.haui.heartlink.R;
import vn.haui.heartlink.activities.MatchSuccessActivity;
import vn.haui.heartlink.activities.ProfileDetailActivity;
import vn.haui.heartlink.adapters.MatchesAdapter;
import vn.haui.heartlink.models.User;
import vn.haui.heartlink.ui.ChatBottomSheetFragment;
import vn.haui.heartlink.utils.ChatRepository;
import vn.haui.heartlink.utils.MatchRepository;
import vn.haui.heartlink.utils.UserRepository;

public class MatchesFragment extends Fragment implements MatchesAdapter.MatchActionListener {

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_matches, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;
        bindViews(view);
        setupRecyclerView();
        setupClicks(view);
        loadMatches();
    }

    private void bindViews(View view) {
        matchesRecyclerView = view.findViewById(R.id.matches_list);
        loadingView = view.findViewById(R.id.matches_loading);
        emptyStateView = view.findViewById(R.id.matches_empty_state);
    }

    private void setupRecyclerView() {
        matchesAdapter = new MatchesAdapter(this);
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
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

    private void setupClicks(View view) {
        ImageButton filterButton = view.findViewById(R.id.matches_filter_button);
        filterButton.setOnClickListener(v -> Toast.makeText(getContext(), R.string.matches_action_coming_soon, Toast.LENGTH_SHORT).show());
    }

    private void loadMatches() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            showError(getString(R.string.error_generic));
            if (getActivity() != null) getActivity().finish();
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
            if(matchesRecyclerView != null) matchesRecyclerView.setVisibility(View.GONE);
            if(emptyStateView != null) emptyStateView.setVisibility(View.VISIBLE);
            return;
        }

        fetchPartnerDetails(interactions);
    }

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
        if (matchesRecyclerView != null) matchesRecyclerView.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        if (emptyStateView != null) emptyStateView.setVisibility(hasItems ? View.GONE : View.VISIBLE);
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
        if (getContext() == null) return "";
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
        if (getContext() == null) return "";
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

    private void showLoading(boolean show) {
        if (loadingView != null) loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private boolean isFragmentActive() {
        return isAdded() && getActivity() != null && !getActivity().isFinishing() && !getActivity().isDestroyed();
    }

    private void showError(@Nullable String message) {
        if (!isFragmentActive() || getView() == null) return;
        if (TextUtils.isEmpty(message)) {
            message = getString(R.string.error_generic);
        }
        Snackbar.make(getView(), message, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onCardTapped(@NonNull MatchesAdapter.CardItem item) {
        openProfileDetail(item);
    }

    @Override
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
    public void onSecondaryAction(@NonNull MatchesAdapter.CardItem item) {
        MatchInteraction interaction = interactionCache.get(item.getPartnerUid());
        if (interaction == null) {
            return;
        }
        if (interaction.isIncomingLike()) {
            dismissIncomingLike(interaction);
        }
    }

    private void openProfileDetail(@NonNull MatchesAdapter.CardItem item) {
        if (getContext() == null) return;
        startActivity(ProfileDetailActivity.createIntent(
                getContext(),
                item.getPartnerUid(),
                item.getDisplayName(),
                item.getPhotoUrl()));
    }

    private void openChatWithMatch(@NonNull MatchesAdapter.CardItem item) {
        if (TextUtils.isEmpty(currentUid)) {
            showError(getString(R.string.error_generic));
            return;
        }
        if (getChildFragmentManager() == null) return;
        chatRepository.ensureDirectChat(currentUid, item.getPartnerUid())
                .addOnSuccessListener(chatId -> {
                    if (!isFragmentActive()) return;
                    ChatBottomSheetFragment fragment = ChatBottomSheetFragment.newInstance(chatId, item.getPartnerUid(), item.getDisplayName(), item.getPhotoUrl());
                    fragment.show(getChildFragmentManager(), fragment.getTag());
                })
                .addOnFailureListener(e -> showError(e.getLocalizedMessage()));
    }

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
                    if (getActivity() != null) getActivity().runOnUiThread(() -> {
                        if (!isFragmentActive()) {
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
                .addOnFailureListener(e -> {
                    if (getActivity() != null) getActivity().runOnUiThread(() -> {
                        if (!isFragmentActive()) {
                            return;
                        }
                        showError(e.getLocalizedMessage());
                    });
                });
    }

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
                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                    if (!isFragmentActive()) return;
                    if (rootView != null) Snackbar.make(rootView, R.string.match_like_sent, Snackbar.LENGTH_SHORT).show();
                    loadInteractionSnapshot();
                });
            }

            @Override
            public void onMatchCreated() {
                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                    if (!isFragmentActive()) return;
                    launchMatchSuccess(interaction, partner);
                    loadInteractionSnapshot();
                });
            }

            @Override
            public void onAlreadyMatched() {
                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                    if (!isFragmentActive()) return;
                    launchMatchSuccess(interaction, partner);
                    loadInteractionSnapshot();
                });
            }

            @Override
            public void onError(@NonNull Exception throwable) {
                if (getActivity() != null) getActivity().runOnUiThread(() -> {
                    if (!isFragmentActive()) return;
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
                .addOnCompleteListener(task -> {
                    if (getActivity() != null) getActivity().runOnUiThread(() -> {
                        if (!isFragmentActive()) return;
                        if (!task.isSuccessful()) {
                            Exception exception = task.getException();
                            showError(exception != null ? exception.getLocalizedMessage() : getString(R.string.error_generic));
                        } else {
                            if (rootView != null) Snackbar.make(rootView, R.string.matches_skip_success, Snackbar.LENGTH_SHORT).show();
                            interactionCache.remove(interaction.partnerUid);
                            if (matchesAdapter != null) matchesAdapter.removeCard(interaction.partnerUid);
                            loadInteractionSnapshot();
                        }
                    });
                });
    }

    private void launchMatchSuccess(@NonNull MatchInteraction interaction, @NonNull User partner) {
        if (getContext() == null) return;
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

        startActivity(MatchSuccessActivity.createIntent(getContext(), partnerName, partnerPhoto, selfPhoto));
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
        } catch (ParseException | DateTimeParseException e) {
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
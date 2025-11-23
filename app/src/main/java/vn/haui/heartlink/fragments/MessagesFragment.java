package vn.haui.heartlink.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import vn.haui.heartlink.R;
import vn.haui.heartlink.adapters.ActiveUsersAdapter;
import vn.haui.heartlink.adapters.MessageThreadsAdapter;
import vn.haui.heartlink.models.ChatThread;
import vn.haui.heartlink.models.User;
import vn.haui.heartlink.ui.ChatBottomSheetFragment;
import vn.haui.heartlink.utils.ChatRepository;
import vn.haui.heartlink.utils.MatchRepository;
import vn.haui.heartlink.utils.UserRepository;

public class MessagesFragment extends Fragment implements
        ActiveUsersAdapter.OnActiveUserClickListener,
        MessageThreadsAdapter.OnThreadClickListener {

    private View rootView;
    private RecyclerView activeUsersList;
    private RecyclerView threadsList;
    private ProgressBar loadingIndicator;
    private TextView emptyStateView;
    private EditText searchField;
    private TextView activeLabel;
    private TextView threadsLabel;

    private ActiveUsersAdapter activeUsersAdapter;
    private MessageThreadsAdapter threadsAdapter;

    private final MatchRepository matchRepository = MatchRepository.getInstance();
    private final ChatRepository chatRepository = ChatRepository.getInstance();
    private final UserRepository userRepository = UserRepository.getInstance();

    private final Map<String, PartnerInfo> partners = new HashMap<>();
    private final Set<String> pendingPartnerFetches = new HashSet<>();
    private final List<ChatThread> currentThreads = new ArrayList<>();
    private final Map<String, ValueEventListener> partnerStatusListeners = new HashMap<>();

    @Nullable
    private Query threadsQuery;
    @Nullable
    private ValueEventListener threadsListener;
    @Nullable
    private ValueEventListener interactionsListener;

    @Nullable
    private String currentUid;

    private boolean interactionsLoaded;
    private boolean threadsLoaded;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_messages, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;
        bindViews(view);
        setupRecyclerViews();
        setupClicks(view);
        loadContent();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        detachThreadsListener();
        detachAllPartnerStatusListeners();
        if (currentUid != null && interactionsListener != null) {
            matchRepository.removeInteractionsListener(currentUid, interactionsListener);
        }
    }

    private void bindViews(View view) {
        activeUsersList = view.findViewById(R.id.messages_active_list);
        threadsList = view.findViewById(R.id.messages_threads_list);
        loadingIndicator = view.findViewById(R.id.messages_loading);
        emptyStateView = view.findViewById(R.id.messages_empty_state);
        searchField = view.findViewById(R.id.messages_search);
        activeLabel = view.findViewById(R.id.messages_active_label);
        threadsLabel = view.findViewById(R.id.messages_threads_label);
    }

    private void setupRecyclerViews() {
        activeUsersAdapter = new ActiveUsersAdapter(this);
        activeUsersList.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        activeUsersList.setAdapter(activeUsersAdapter);

        threadsAdapter = new MessageThreadsAdapter(this);
        threadsList.setLayoutManager(new LinearLayoutManager(getContext()));
        threadsList.setAdapter(threadsAdapter);
    }

    private void setupClicks(View view) {
        ImageButton filterButton = view.findViewById(R.id.messages_filter_button);
        filterButton.setOnClickListener(v -> Toast.makeText(getContext(), R.string.matches_action_coming_soon, Toast.LENGTH_SHORT).show());
        searchField.setOnEditorActionListener((textView, actionId, keyEvent) -> false);
    }

    private void loadContent() {
        interactionsLoaded = false;
        threadsLoaded = false;
        setLoading(true);

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            if (getContext() != null) Toast.makeText(getContext(), R.string.error_generic, Toast.LENGTH_SHORT).show();
            if (getActivity() != null) getActivity().finish();
            return;
        }
        currentUid = firebaseUser.getUid();

        interactionsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                handleInteractionsSnapshot(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                interactionsLoaded = true;
                updateLoadingState();
                if (getContext() != null) Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        matchRepository.addInteractionsListener(currentUid, interactionsListener);

        listenForThreads();
    }

    private void handleInteractionsSnapshot(@Nullable DataSnapshot snapshot) {
        partners.clear();

        if (snapshot != null) {
            for (DataSnapshot child : snapshot.getChildren()) {
                String status = child.child("status").getValue(String.class);
                if (!MatchRepository.STATUS_MATCHED.equals(status)) {
                    continue;
                }
                String partnerUid = child.getKey();
                if (TextUtils.isEmpty(partnerUid)) {
                    continue;
                }
                PartnerInfo info = new PartnerInfo(partnerUid);
                info.displayName = safeString(child.child("displayName").getValue(String.class));
                info.photoUrl = child.child("photoUrl").getValue(String.class);
                Long matchedAt = child.child("matchedAt").getValue(Long.class);
                Long likedAt = child.child("likedAt").getValue(Long.class);
                info.lastInteraction = matchedAt != null ? matchedAt : (likedAt != null ? likedAt : 0L);
                partners.put(partnerUid, info);

                if (TextUtils.isEmpty(info.displayName) || TextUtils.isEmpty(info.photoUrl)) {
                    fetchPartnerProfile(partnerUid);
                }

                attachPartnerStatusListener(partnerUid);
            }
        }

        interactionsLoaded = true;
        refreshActiveUsers();
        refreshThreadItems();
        updateLoadingState();
    }

    private void refreshActiveUsers() {
        if (getContext() == null) return;
        List<ActiveUsersAdapter.PartnerItem> items = new ArrayList<>();
        for (PartnerInfo partner : partners.values()) {
            if (partner.isOnline) {
                String name = !TextUtils.isEmpty(partner.displayName)
                        ? partner.displayName
                        : getString(R.string.profile_name_fallback);
                items.add(new ActiveUsersAdapter.PartnerItem(partner.uid, name, partner.photoUrl));
            }
        }
        Collections.sort(items, (first, second) -> {
            long a = getPartnerInteraction(first.getUserId());
            long b = getPartnerInteraction(second.getUserId());
            return Long.compare(b, a);
        });

        activeUsersAdapter.submitList(items);

        boolean hasActive = !items.isEmpty();
        activeUsersList.setVisibility(hasActive ? View.VISIBLE : View.GONE);
        activeLabel.setVisibility(hasActive ? View.GONE : View.VISIBLE);
    }

    private long getPartnerInteraction(@NonNull String userId) {
        PartnerInfo info = partners.get(userId);
        return info != null ? info.lastInteraction : 0L;
    }

    private void refreshThreadItems() {
        if (getContext() == null) return;
        Map<String, ChatThread> threadByPartner = new HashMap<>();
        for (ChatThread thread : currentThreads) {
            if (currentUid == null) {
                continue;
            }
            String partnerUid = thread.resolvePartnerId(currentUid);
            if (TextUtils.isEmpty(partnerUid)) {
                continue;
            }
            threadByPartner.put(partnerUid, thread);
        }

        List<MessageThreadsAdapter.ThreadItem> items = new ArrayList<>();
        for (Map.Entry<String, PartnerInfo> entry : partners.entrySet()) {
            String partnerUid = entry.getKey();
            PartnerInfo partner = entry.getValue();
            ChatThread thread = threadByPartner.get(partnerUid);
            items.add(buildThreadItem(partner, thread));
        }

        for (ChatThread thread : currentThreads) {
            if (currentUid == null) {
                continue;
            }
            String partnerUid = thread.resolvePartnerId(currentUid);
            if (TextUtils.isEmpty(partnerUid) || partners.containsKey(partnerUid)) {
                continue;
            }
            PartnerInfo partner = new PartnerInfo(partnerUid);
            partner.displayName = getString(R.string.profile_name_fallback);
            items.add(buildThreadItem(partner, thread));
        }

        Collections.sort(items, Comparator.comparingLong(MessageThreadsAdapter.ThreadItem::getLastTimestamp).reversed());
        threadsAdapter.submitList(items);

        boolean hasThreads = !items.isEmpty();
        threadsList.setVisibility(hasThreads ? View.VISIBLE : View.GONE);
        threadsLabel.setVisibility(hasThreads ? View.VISIBLE : View.GONE);
        emptyStateView.setVisibility(hasThreads ? View.GONE : View.VISIBLE);
    }

    @NonNull
    private MessageThreadsAdapter.ThreadItem buildThreadItem(@NonNull PartnerInfo partner,
                                                             @Nullable ChatThread thread) {
        String name = !TextUtils.isEmpty(partner.displayName)
                ? partner.displayName
                : getString(R.string.profile_name_fallback);
        String chatId = thread != null && !TextUtils.isEmpty(thread.getChatId())
                ? thread.getChatId()
                : (currentUid != null ? ChatRepository.buildChatId(currentUid, partner.uid) : "");

        String preview;
        long timestamp = 0L;
        boolean hasUnread = false;
        if (thread != null) {
            preview = !TextUtils.isEmpty(thread.getLastMessage())
                    ? thread.getLastMessage()
                    : getString(R.string.messages_thread_prompt, name);
            timestamp = thread.getLastMessageAt() > 0
                    ? thread.getLastMessageAt()
                    : (thread.getUpdatedAt() > 0 ? thread.getUpdatedAt() : partner.lastInteraction);

            if (currentUid != null
                    && !TextUtils.isEmpty(thread.getLastSenderId())
                    && !currentUid.equals(thread.getLastSenderId())) {
                Map<String, Long> readTimestamps = thread.getReadTimestamps();
                Long readAt = readTimestamps != null ? readTimestamps.get(currentUid) : null;
                hasUnread = readAt == null || readAt < thread.getLastMessageAt();
            }
        } else {
            preview = getString(R.string.messages_thread_prompt, name);
            timestamp = partner.lastInteraction;
        }

        return new MessageThreadsAdapter.ThreadItem(
                chatId,
                partner.uid,
                name,
                partner.photoUrl,
                preview,
                timestamp,
                hasUnread);
    }

    private void listenForThreads() {
        if (currentUid == null) {
            return;
        }
        detachThreadsListener();
        threadsQuery = chatRepository.queryUserChats(currentUid);
        threadsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentThreads.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    ChatThread thread = child.getValue(ChatThread.class);
                    if (thread == null) {
                        continue;
                    }
                    if (TextUtils.isEmpty(thread.getChatId())) {
                        thread.setChatId(child.getKey());
                    }
                    currentThreads.add(thread);

                    if (currentUid != null) {
                        String partnerUid = thread.resolvePartnerId(currentUid);
                        if (!TextUtils.isEmpty(partnerUid) && !partners.containsKey(partnerUid)) {
                            PartnerInfo partnerInfo = new PartnerInfo(partnerUid);
                            partners.put(partnerUid, partnerInfo);
                            fetchPartnerProfile(partnerUid);
                        }
                    }
                }

                threadsLoaded = true;
                refreshThreadItems();
                refreshActiveUsers();
                updateLoadingState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                threadsLoaded = true;
                updateLoadingState();
                if (getContext() != null) {
                    Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };
        threadsQuery.addValueEventListener(threadsListener);
    }

    private void detachThreadsListener() {
        if (threadsQuery != null && threadsListener != null) {
            threadsQuery.removeEventListener(threadsListener);
        }
        threadsListener = null;
        threadsQuery = null;
    }

    private void attachPartnerStatusListener(String partnerUid) {
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                PartnerInfo info = partners.get(partnerUid);
                if (info == null) {
                    return;
                }
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    info.isOnline = user.isOnline();
                    info.lastSeen = user.getLastSeen();
                }
                refreshActiveUsers();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        userRepository.getUserData(partnerUid).addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                snapshot.getRef().addValueEventListener(listener);
                partnerStatusListeners.put(partnerUid, listener);
            }
        });
    }

    private void detachAllPartnerStatusListeners() {
        for (Map.Entry<String, ValueEventListener> entry : partnerStatusListeners.entrySet()) {
            userRepository.getUserData(entry.getKey()).addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    snapshot.getRef().removeEventListener(entry.getValue());
                }
            });
        }
        partnerStatusListeners.clear();
    }

    private void fetchPartnerProfile(@NonNull String partnerUid) {
        if (pendingPartnerFetches.contains(partnerUid)) {
            return;
        }
        pendingPartnerFetches.add(partnerUid);
        userRepository.getUserData(partnerUid)
                .addOnSuccessListener(snapshot -> {
                    pendingPartnerFetches.remove(partnerUid);
                    PartnerInfo info = partners.get(partnerUid);
                    if (info == null) {
                        return;
                    }
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        info.displayName = safeString(user.getName());
                        if (user.getPhotoUrls() != null && !user.getPhotoUrls().isEmpty()) {
                            info.photoUrl = user.getPhotoUrls().get(0);
                        }
                    }
                    refreshActiveUsers();
                    refreshThreadItems();
                })
                .addOnFailureListener(error -> pendingPartnerFetches.remove(partnerUid));
    }

    private void setLoading(boolean loading) {
        if (loadingIndicator != null) loadingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void updateLoadingState() {
        setLoading(!(interactionsLoaded && threadsLoaded));
    }

    private String safeString(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public void onActiveUserClicked(@NonNull ActiveUsersAdapter.PartnerItem item) {
        openChat(item.getUserId(), item.getDisplayName(), item.getPhotoUrl());
    }

    @Override
    public void onThreadClicked(@NonNull MessageThreadsAdapter.ThreadItem item) {
        openChat(item.getPartnerUid(), item.getDisplayName(), item.getPhotoUrl());
    }

    public void openChatWithUser(@NonNull String userId) {
        PartnerInfo partnerInfo = partners.get(userId);
        String displayName = "";
        String photoUrl = null;

        if (partnerInfo != null) {
            displayName = partnerInfo.displayName;
            photoUrl = partnerInfo.photoUrl;
        } else {
            // Fallback: Try to fetch user data if not in partners map
            // This might happen if the fragment isn't fully loaded or for a new match
            // For simplicity, we'll use a placeholder for now and rely on ChatBottomSheetFragment to fetch if needed
            displayName = getString(R.string.profile_name_fallback); // Default name
        }
        openChat(userId, displayName, photoUrl);
    }

    private void openChat(@NonNull String partnerUid,
                          @NonNull String displayName,
                          @Nullable String photoUrl) {
        if (currentUid == null) {
            if (getContext() != null) Toast.makeText(getContext(), R.string.error_generic, Toast.LENGTH_SHORT).show();
            return;
        }
        if (getParentFragmentManager() == null) return;
        chatRepository.ensureDirectChat(currentUid, partnerUid)
                .addOnSuccessListener(chatId -> {
                    ChatBottomSheetFragment fragment = ChatBottomSheetFragment.newInstance(chatId, partnerUid, displayName, photoUrl);
                    fragment.show(getParentFragmentManager(), fragment.getTag());
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) Toast.makeText(getContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private static final class PartnerInfo {
        final String uid;
        String displayName = "";
        @Nullable
        String photoUrl;
        long lastInteraction;
        boolean isOnline;
        long lastSeen;

        PartnerInfo(@NonNull String uid) {
            this.uid = uid;
        }
    }
}
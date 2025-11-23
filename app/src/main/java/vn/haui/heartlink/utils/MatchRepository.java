package vn.haui.heartlink.utils;

import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vn.haui.heartlink.Constants;
import vn.haui.heartlink.models.Match;
import vn.haui.heartlink.models.User;

public final class MatchRepository {

    private static final String TAG = "MatchRepository";

    public static final String STATUS_LIKED = "liked";
    public static final String STATUS_MATCHED = "matched";
    public static final String STATUS_RECEIVED_LIKE = "incoming_like";
    private static final String TYPE_LIKE = "like";
    private static final String TYPE_SUPERLIKE = "superlike";
    private static final String TYPE_MATCH = "match";

    private static MatchRepository instance;

    private final DatabaseReference matchInteractionsRef;
    private final DatabaseReference matchesRef;
    private final ChatRepository chatRepository;

    private MatchRepository() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        matchInteractionsRef = database.getReference(Constants.MATCH_INTERACTIONS_NODE);
        matchesRef = database.getReference(Constants.MATCHES_NODE);
        chatRepository = ChatRepository.getInstance();
    }

    public static synchronized MatchRepository getInstance() {
        if (instance == null) {
            instance = new MatchRepository();
        }
        return instance;
    }

    public void likeUser(@Nullable User currentUser,
                         @Nullable User targetUser,
                         boolean isSuperLike,
                         @Nullable MatchResultCallback callback) {
        if (currentUser == null || targetUser == null
                || TextUtils.isEmpty(currentUser.getUid())
                || TextUtils.isEmpty(targetUser.getUid())) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Missing user identifiers for match operation"));
            }
            return;
        }

        final String currentUid = currentUser.getUid();
        final String targetUid = targetUser.getUid();

        DatabaseReference targetMatchesRef = matchInteractionsRef.child(targetUid).child(currentUid);
        targetMatchesRef.get()
                .addOnSuccessListener(snapshot -> handleLikeSnapshot(
                        snapshot,
                        currentUser,
                        targetUser,
                        isSuperLike,
                        callback))
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
    }

    private void handleLikeSnapshot(@NonNull DataSnapshot targetSnapshot,
                                     @NonNull User currentUser,
                                     @NonNull User targetUser,
                                     boolean isSuperLike,
                                     @Nullable MatchResultCallback callback) {
        final String currentUid = currentUser.getUid();
        final String targetUid = targetUser.getUid();

        String existingStatus = targetSnapshot.child("status").getValue(String.class);
        Long existingMatchedAt = targetSnapshot.child("matchedAt").getValue(Long.class);
        Long existingLikedAt = targetSnapshot.child("likedAt").getValue(Long.class);

        boolean alreadyMatched = STATUS_MATCHED.equals(existingStatus);
        boolean theyLikedYou = STATUS_LIKED.equals(existingStatus) || alreadyMatched;
        long now = System.currentTimeMillis();

        Map<String, Object> updates = new HashMap<>();

        if (theyLikedYou) {
            long matchTimestamp = existingMatchedAt != null ? existingMatchedAt : now;

            populateMetadata(updates, currentUid, targetUid, targetUser);
            populateMetadata(updates, targetUid, currentUid, currentUser);

            updates.put(path(currentUid, targetUid, "status"), STATUS_MATCHED);
            updates.put(path(currentUid, targetUid, "matchedAt"), matchTimestamp);
            updates.put(path(currentUid, targetUid, "likedAt"), now);
            updates.put(path(currentUid, targetUid, "type"), TYPE_MATCH);

            updates.put(path(targetUid, currentUid, "status"), STATUS_MATCHED);
            updates.put(path(targetUid, currentUid, "matchedAt"), matchTimestamp);
            updates.put(path(targetUid, currentUid, "type"), TYPE_MATCH);
            if (existingLikedAt == null) {
                updates.put(path(targetUid, currentUid, "likedAt"), now);
            }

            matchInteractionsRef.updateChildren(updates)
                    .addOnSuccessListener(ignored -> ensureMatchRecord(currentUid, targetUid, matchTimestamp, callback, alreadyMatched))
                    .addOnFailureListener(e -> {
                        if (callback != null) {
                            callback.onError(e);
                        }
                    });
        } else {
            populateMetadata(updates, currentUid, targetUid, targetUser);
            populateMetadata(updates, targetUid, currentUid, currentUser);
            updates.put(path(currentUid, targetUid, "status"), STATUS_LIKED);
            updates.put(path(currentUid, targetUid, "likedAt"), now);
            updates.put(path(currentUid, targetUid, "type"), isSuperLike ? TYPE_SUPERLIKE : TYPE_LIKE);

            updates.put(path(targetUid, currentUid, "status"), STATUS_RECEIVED_LIKE);
            updates.put(path(targetUid, currentUid, "likedAt"), now);
            updates.put(path(targetUid, currentUid, "type"), isSuperLike ? TYPE_SUPERLIKE : TYPE_LIKE);

            matchInteractionsRef.updateChildren(updates)
                    .addOnSuccessListener(ignored -> {
                        if (callback != null) {
                            callback.onLikeRecorded();
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (callback != null) {
                            callback.onError(e);
                        }
                    });
        }
    }

    private void populateMetadata(@NonNull Map<String, Object> updates,
                                  @NonNull String ownerUid,
                                  @NonNull String otherUid,
                                  @NonNull User otherUser) {
        String displayName = safeName(otherUser.getName());
        if (!TextUtils.isEmpty(displayName)) {
            updates.put(path(ownerUid, otherUid, "displayName"), displayName);
        }

        String photoUrl = getPrimaryPhoto(otherUser);
        if (!TextUtils.isEmpty(photoUrl)) {
            updates.put(path(ownerUid, otherUid, "photoUrl"), photoUrl);
        }
    }

    private String path(String ownerUid, String otherUid, String field) {
        return ownerUid + "/" + otherUid + "/" + field;
    }

    private void ensureMatchRecord(@NonNull String userA,
                                   @NonNull String userB,
                                   long matchTimestamp,
                                   @Nullable MatchResultCallback callback,
                                   boolean alreadyMatched) {
        String[] ordered = orderUids(userA, userB);
        String matchId = buildMatchId(ordered[0], ordered[1]);

        Map<String, Object> record = new HashMap<>();
        record.put("match_id", matchId);
        record.put("user_id_1", ordered[0]);
        record.put("user_id_2", ordered[1]);
        record.put("matched_at", matchTimestamp);

        matchesRef.child(matchId)
                .updateChildren(record)
                .addOnSuccessListener(ignored -> {
                    chatRepository.ensureDirectChat(userA, userB)
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to ensure chat for match " + matchId, e));
                    if (callback != null) {
                        if (alreadyMatched) {
                            callback.onAlreadyMatched();
                        } else {
                            callback.onMatchCreated();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
    }

    private String[] orderUids(@NonNull String uid1, @NonNull String uid2) {
        if (uid1.compareTo(uid2) <= 0) {
            return new String[]{uid1, uid2};
        }
        return new String[]{uid2, uid1};
    }

    private String buildMatchId(@NonNull String sortedUid1, @NonNull String sortedUid2) {
        return sortedUid1 + "_" + sortedUid2;
    }

    public void addInteractionsListener(@NonNull String userId, @NonNull ValueEventListener listener) {
        matchInteractionsRef.child(userId).addValueEventListener(listener);
    }

    public void removeInteractionsListener(@NonNull String userId, @NonNull ValueEventListener listener) {
        matchInteractionsRef.child(userId).removeEventListener(listener);
    }

    public void addInteractionListener(@NonNull String userId, @NonNull ChildEventListener listener) {
        matchInteractionsRef.child(userId).addChildEventListener(listener);
    }

    public void removeInteractionListener(@NonNull String userId, @NonNull ChildEventListener listener) {
        matchInteractionsRef.child(userId).removeEventListener(listener);
    }

    public Task<Void> removeInteraction(@NonNull String ownerUid, @NonNull String otherUid) {
        return matchInteractionsRef.child(ownerUid).child(otherUid).removeValue();
    }

    public Task<List<Match>> getMatchesForUser(@NonNull String userId) {
        return matchesRef.get().continueWith(task -> {
            if (!task.isSuccessful()) {
                Exception exception = task.getException();
                if (exception != null) {
                    throw exception;
                }
                throw new IllegalStateException("Failed to load matches for user " + userId);
            }

            DataSnapshot snapshot = task.getResult();
            List<Match> matches = new ArrayList<>();
            if (snapshot != null) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    Match match = child.getValue(Match.class);
                    if (match == null) {
                        continue;
                    }
                    String user1 = match.getUserId1();
                    String user2 = match.getUserId2();
                    if (userId.equals(user1) || userId.equals(user2)) {
                        matches.add(match);
                    }
                }
            }
            return matches;
        });
    }

    public ChildEventListener listenForIncomingLikes(@NonNull String userId,
                                                     @NonNull IncomingLikeListener listener) {
        ChildEventListener childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                emitIfIncomingLike(snapshot, listener);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                emitIfIncomingLike(snapshot, listener);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };

        matchInteractionsRef.child(userId).addChildEventListener(childEventListener);
        return childEventListener;
    }

    public void removeIncomingLikeListener(@NonNull String userId,
                                            @NonNull ChildEventListener listener) {
        matchInteractionsRef.child(userId).removeEventListener(listener);
    }

    private void emitIfIncomingLike(@NonNull DataSnapshot snapshot,
                                    @NonNull IncomingLikeListener listener) {
        String status = snapshot.child("status").getValue(String.class);
        if (!STATUS_RECEIVED_LIKE.equals(status)) {
            return;
        }

        String likerUid = snapshot.getKey();
        if (TextUtils.isEmpty(likerUid)) {
            return;
        }

        String displayName = snapshot.child("displayName").getValue(String.class);
        String photoUrl = snapshot.child("photoUrl").getValue(String.class);
        String type = snapshot.child("type").getValue(String.class);
        Long likedAt = snapshot.child("likedAt").getValue(Long.class);
        long timestamp = likedAt != null ? likedAt : System.currentTimeMillis();

        listener.onIncomingLike(new IncomingLike(likerUid, displayName, photoUrl, type, timestamp));
    }

    private String safeName(@Nullable String name) {
        return name == null ? "" : name.trim();
    }

    @Nullable
    private String getPrimaryPhoto(@NonNull User user) {
        if (user.getPhotoUrls() != null && !user.getPhotoUrls().isEmpty()) {
            return user.getPhotoUrls().get(0);
        }
        return null;
    }

    public static boolean isSuperLike(@Nullable String type) {
        return TYPE_SUPERLIKE.equals(type);
    }

    public interface IncomingLikeListener {
        void onIncomingLike(@NonNull IncomingLike like);
    }

    public static final class IncomingLike {
        private final String likerUid;
        @Nullable
        private final String displayName;
        @Nullable
        private final String photoUrl;
        @Nullable
        private final String type;
        private final long likedAt;

        IncomingLike(@NonNull String likerUid,
                     @Nullable String displayName,
                     @Nullable String photoUrl,
                     @Nullable String type,
                     long likedAt) {
            this.likerUid = likerUid;
            this.displayName = displayName;
            this.photoUrl = photoUrl;
            this.type = type;
            this.likedAt = likedAt;
        }

        @NonNull
        public String getLikerUid() {
            return likerUid;
        }

        @Nullable
        public String getDisplayName() {
            return displayName;
        }

        @Nullable
        public String getPhotoUrl() {
            return photoUrl;
        }

        @Nullable
        public String getType() {
            return type;
        }

        public long getLikedAt() {
            return likedAt;
        }
    }

    public interface MatchResultCallback {
        default void onLikeRecorded() {
        }

        default void onMatchCreated() {
        }

        default void onAlreadyMatched() {
        }

        default void onError(@NonNull Exception throwable) {
        }
    }
}

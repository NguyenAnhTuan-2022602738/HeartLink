package vn.haui.heartlink.utils;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import vn.haui.heartlink.Constants;
import vn.haui.heartlink.models.ChatMessage;

public final class ChatRepository {

    private static ChatRepository instance;

    private final DatabaseReference rootRef;
    private final DatabaseReference chatsRef;
    private final DatabaseReference usersRef;
    private final DatabaseReference blocksRef;
    private final DatabaseReference userNewMessagesRef;

    private ChatRepository() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        rootRef = database.getReference();
        chatsRef = rootRef.child(Constants.CHATS_NODE);
        usersRef = rootRef.child(Constants.USERS_NODE);
        blocksRef = rootRef.child("blocks");
        userNewMessagesRef = rootRef.child("user_new_messages");
    }

    public static synchronized ChatRepository getInstance() {
        if (instance == null) {
            instance = new ChatRepository();
        }
        return instance;
    }

    @NonNull
    public static String buildChatId(@NonNull String userA, @NonNull String userB) {
        if (userA.compareTo(userB) <= 0) {
            return userA + "_" + userB;
        }
        return userB + "_" + userA;
    }

    public Task<Void> blockUser(String currentUid, String partnerId) {
        return blocksRef.child(currentUid).child(partnerId).setValue(true);
    }

    public Task<Void> unblockUser(String currentUid, String partnerId) {
        return blocksRef.child(currentUid).child(partnerId).removeValue();
    }

    public void isBlocked(String currentUid, String partnerId, BlockedStatusListener listener) {
        blocksRef.child(currentUid).child(partnerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean isBlocked = snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                blocksRef.child(partnerId).child(currentUid).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean isBlockedBy = snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                        listener.onStatusChecked(isBlocked, isBlockedBy);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        listener.onStatusChecked(isBlocked, false);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onStatusChecked(false, false);
            }
        });
    }

    public interface BlockedStatusListener {
        void onStatusChecked(boolean isBlocked, boolean isBlockedBy);
    }

    public Task<String> ensureDirectChat(@NonNull String userA, @NonNull String userB) {
        final String chatId = buildChatId(userA, userB);
        return chatsRef.child(chatId).get().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                Map<String, Object> participants = new HashMap<>();
                participants.put(userA, true);
                participants.put(userB, true);

                Map<String, Object> readTimestamps = new HashMap<>();
                readTimestamps.put(userA, 0L);
                readTimestamps.put(userB, 0L);

                Map<String, Object> data = new HashMap<>();
                data.put("chatId", chatId);
                data.put("participants", participants);
                data.put("lastMessage", "");
                data.put("lastSenderId", "");
                data.put("lastMessageAt", 0L);
                data.put("updatedAt", System.currentTimeMillis());
                data.put("readTimestamps", readTimestamps);

                return chatsRef.child(chatId).setValue(data).continueWith(t -> chatId);
            }
            return Tasks.forResult(chatId);
        });
    }

    public DatabaseReference getChatReference(@NonNull String chatId) {
        return chatsRef.child(chatId);
    }

    public DatabaseReference getMessagesReference(@NonNull String chatId) {
        return chatsRef.child(chatId).child("messages");
    }

    public DatabaseReference getReadTimestampsReference(@NonNull String chatId) {
        return chatsRef.child(chatId).child("readTimestamps");
    }

    public Query queryUserChats(@NonNull String userId) {
        return chatsRef.orderByChild("participants/" + userId).equalTo(true);
    }

    public Task<Void> sendMessage(@NonNull ChatMessage message, @NonNull String recipientId) {
        if (TextUtils.isEmpty(message.getChatId())) {
            throw new IllegalArgumentException("Chat id must be provided when sending a message");
        }

        long timestamp = message.getTimestamp() > 0 ? message.getTimestamp() : System.currentTimeMillis();
        message.setTimestamp(timestamp);

        DatabaseReference chatRef = chatsRef.child(message.getChatId());
        DatabaseReference newMessageRef = chatRef.child("messages").push();
        String messageId = newMessageRef.getKey();
        message.setMessageId(messageId);

        Map<String, Object> messageMap = message.toMap();

        Map<String, Object> chatUpdates = new HashMap<>();
        chatUpdates.put("messages/" + messageId, messageMap);
        chatUpdates.put("lastMessage", message.getText());
        chatUpdates.put("lastSenderId", message.getSenderId());
        chatUpdates.put("lastMessageAt", timestamp);
        chatUpdates.put("updatedAt", timestamp);

        // Add a trigger for the notification as a separate operation
        userNewMessagesRef.child(recipientId).child(messageId).setValue(messageMap);

        return chatRef.updateChildren(chatUpdates);
    }

    public Task<Void> markThreadRead(@NonNull String chatId, @NonNull String userId) {
        long now = System.currentTimeMillis();
        Map<String, Object> update = new HashMap<>();
        update.put("readTimestamps/" + userId, now);
        return chatsRef.child(chatId).updateChildren(update);
    }

    public ChildEventListener listenForNewMessages(@NonNull String userId, @NonNull NewMessageListener listener) {
        Query newMessagesQuery = userNewMessagesRef.child(userId);
        ChildEventListener childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                ChatMessage message = snapshot.getValue(ChatMessage.class);
                if (message == null) {
                    return;
                }

                // Clean up the trigger
                snapshot.getRef().removeValue();

                if (TextUtils.isEmpty(message.getSenderId()) || TextUtils.isEmpty(message.getText()) || TextUtils.isEmpty(message.getChatId())) {
                    return;
                }

                usersRef.child(message.getSenderId()).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot nameSnapshot) {
                        String senderName = nameSnapshot.getValue(String.class);
                        if (TextUtils.isEmpty(senderName)) {
                            senderName = "Một người bạn";
                        }
                        listener.onNewMessage(new NewMessage(message.getChatId(), senderName, message.getText()));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        listener.onNewMessage(new NewMessage(message.getChatId(), "Một người bạn", message.getText()));
                    }
                });
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        newMessagesQuery.addChildEventListener(childEventListener);
        return childEventListener;
    }

    public void removeMessagesListener(@NonNull String userId, @NonNull ChildEventListener listener) {
        userNewMessagesRef.child(userId).removeEventListener(listener);
    }

    public interface NewMessageListener {
        void onNewMessage(@NonNull NewMessage message);
    }

    public static final class NewMessage {
        public final String chatId;
        public final String senderName;
        public final String text;

        NewMessage(String chatId, String senderName, String text) {
            this.chatId = chatId;
            this.senderName = senderName;
            this.text = text;
        }

        public String getChatId() {
            return chatId;
        }

        public String getSenderName() {
            return senderName;
        }

        public String getText() {
            return text;
        }
    }
}

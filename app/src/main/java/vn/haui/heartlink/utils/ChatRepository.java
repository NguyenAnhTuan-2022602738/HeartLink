package vn.haui.heartlink.utils;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.util.HashMap;
import java.util.Map;

import vn.haui.heartlink.Constants;
import vn.haui.heartlink.models.ChatMessage;

/**
 * Repository encapsulating Firebase Realtime Database calls related to chats.
 */
public final class ChatRepository {

    private static ChatRepository instance;

    private final DatabaseReference chatsRef;

    private ChatRepository() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        chatsRef = database.getReference(Constants.CHATS_NODE);
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

    /**
     * Tạo hoặc lấy chat ID cho cuộc trò chuyện trực tiếp giữa hai người dùng.
     * Đảm bảo chat ID luôn consistent bất kể thứ tự userA và userB.
     *
     * @param userA ID của người dùng thứ nhất
     * @param userB ID của người dùng thứ hai
     * @return Task chứa chat ID
     */
    public Task<String> ensureDirectChat(@NonNull String userA, @NonNull String userB) {
        final String chatId = buildChatId(userA, userB);
        return chatsRef.child(chatId)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Exception exception = task.getException();
                        if (exception != null) {
                            throw exception;
                        }
                        throw new IllegalStateException("Failed to check chat existence for " + chatId);
                    }

                    DataSnapshot snapshot = task.getResult();
                    if (snapshot != null && snapshot.exists()) {
                        return Tasks.forResult(chatId);
                    }

                    Map<String, Object> participants = new HashMap<>();
                    participants.put(userA, true);
                    participants.put(userB, true);

                    Map<String, Object> readTimestamps = new HashMap<>();
                    readTimestamps.put(userA, 0L);
                    readTimestamps.put(userB, 0L);

                    Map<String, Object> data = new HashMap<>();
                    data.put("chatId", chatId);
                    data.put("userId1", userA.compareTo(userB) <= 0 ? userA : userB);
                    data.put("userId2", userA.compareTo(userB) <= 0 ? userB : userA);
                    data.put("participants", participants);
                    data.put("lastMessage", "");
                    data.put("lastSenderId", "");
                    data.put("lastMessageAt", 0L);
                    data.put("updatedAt", System.currentTimeMillis());
                    data.put("readTimestamps", readTimestamps);

                    return chatsRef.child(chatId)
                            .setValue(data)
                            .continueWith(t -> chatId);
                });
    }

    /**
     * Lấy DatabaseReference đến một chat thread cụ thể.
     *
     * @param chatId ID của chat thread
     * @return DatabaseReference đến chat
     */
    public DatabaseReference getChatReference(@NonNull String chatId) {
        return chatsRef.child(chatId);
    }

    /**
     * Lấy DatabaseReference đến collection messages của một chat.
     *
     * @param chatId ID của chat thread
     * @return DatabaseReference đến messages collection
     */
    public DatabaseReference getMessagesReference(@NonNull String chatId) {
        return chatsRef.child(chatId).child("messages");
    }

    /**
     * Tạo query để lấy tất cả chat threads mà user tham gia.
     *
     * @param userId ID của user cần query
     * @return Query để lấy chat threads của user
     */
    public Query queryUserChats(@NonNull String userId) {
        return chatsRef.orderByChild("participants/" + userId).equalTo(true);
    }

    /**
     * Gửi tin nhắn mới vào chat thread và cập nhật metadata của chat.
     *
     * @param message Đối tượng ChatMessage chứa thông tin tin nhắn
     * @return Task<Void> hoàn thành khi gửi thành công
     * @throws IllegalArgumentException nếu chatId trống
     */
    public Task<Void> sendMessage(@NonNull ChatMessage message) {
        if (TextUtils.isEmpty(message.getChatId())) {
            throw new IllegalArgumentException("Chat id must be provided when sending a message");
        }

        long timestamp = message.getTimestamp() > 0 ? message.getTimestamp() : System.currentTimeMillis();
        message.setTimestamp(timestamp);

        DatabaseReference chatRef = chatsRef.child(message.getChatId());
        DatabaseReference newMessageRef = chatRef.child("messages").push();
        String messageId = newMessageRef.getKey();
        message.setMessageId(messageId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("messages/" + messageId, message.toMap());
        updates.put("lastMessage", message.getText());
        updates.put("lastSenderId", message.getSenderId());
        updates.put("lastMessageAt", timestamp);
        updates.put("updatedAt", timestamp);
        updates.put("readTimestamps/" + message.getSenderId(), timestamp);

        return chatRef.updateChildren(updates);
    }

    /**
     * Đánh dấu chat thread đã được đọc bởi một user cụ thể.
     *
     * @param chatId ID của chat thread
     * @param userId ID của user đã đọc
     * @return Task<Void> hoàn thành khi cập nhật thành công
     */
    public Task<Void> markThreadRead(@NonNull String chatId, @NonNull String userId) {
        long now = System.currentTimeMillis();
        Map<String, Object> update = new HashMap<>();
        update.put("readTimestamps/" + userId, now);
        return chatsRef.child(chatId).updateChildren(update);
    }
}

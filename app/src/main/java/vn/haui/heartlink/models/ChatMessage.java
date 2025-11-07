package vn.haui.heartlink.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single message inside a chat thread.
 */
public class ChatMessage {

    private String messageId;
    private String chatId;
    private String senderId;
    private String text;
    private String imageUrl;
    private long timestamp;

    public ChatMessage() {
        // Default constructor required for Firebase.
    }

    public ChatMessage(@NonNull String chatId,
                       @NonNull String senderId,
                       @Nullable String text,
                       @Nullable String imageUrl,
                       long timestamp) {
        this.chatId = chatId;
        this.senderId = senderId;
        this.text = text;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
    }

    @Nullable
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(@Nullable String messageId) {
        this.messageId = messageId;
    }

    @NonNull
    public String getChatId() {
        return chatId;
    }

    public void setChatId(@NonNull String chatId) {
        this.chatId = chatId;
    }

    @NonNull
    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(@NonNull String senderId) {
        this.senderId = senderId;
    }

    @Nullable
    public String getText() {
        return text;
    }

    public void setText(@Nullable String text) {
        this.text = text;
    }

    @Nullable
    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(@Nullable String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("messageId", messageId);
        map.put("chatId", chatId);
        map.put("senderId", senderId);
        map.put("text", text);
        map.put("imageUrl", imageUrl);
        map.put("timestamp", timestamp);
        return map;
    }
}

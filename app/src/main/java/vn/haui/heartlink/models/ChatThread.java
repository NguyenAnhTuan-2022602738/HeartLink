package vn.haui.heartlink.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

/**
 * Represents metadata about a two-person chat thread.
 */
public class ChatThread {

    private String chatId;
    private String userId1;
    private String userId2;
    private Map<String, Boolean> participants;
    private String lastMessage;
    private String lastSenderId;
    private long lastMessageAt;
    private long updatedAt;
    private Map<String, Long> readTimestamps;

    public ChatThread() {
        // Default constructor for Firebase.
    }

    @Nullable
    public String getChatId() {
        return chatId;
    }

    public void setChatId(@Nullable String chatId) {
        this.chatId = chatId;
    }

    @Nullable
    public String getUserId1() {
        return userId1;
    }

    public void setUserId1(@Nullable String userId1) {
        this.userId1 = userId1;
    }

    @Nullable
    public String getUserId2() {
        return userId2;
    }

    public void setUserId2(@Nullable String userId2) {
        this.userId2 = userId2;
    }

    @Nullable
    public Map<String, Boolean> getParticipants() {
        return participants;
    }

    public void setParticipants(@Nullable Map<String, Boolean> participants) {
        this.participants = participants;
    }

    @Nullable
    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(@Nullable String lastMessage) {
        this.lastMessage = lastMessage;
    }

    @Nullable
    public String getLastSenderId() {
        return lastSenderId;
    }

    public void setLastSenderId(@Nullable String lastSenderId) {
        this.lastSenderId = lastSenderId;
    }

    public long getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(long lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Nullable
    public Map<String, Long> getReadTimestamps() {
        return readTimestamps;
    }

    public void setReadTimestamps(@Nullable Map<String, Long> readTimestamps) {
        this.readTimestamps = readTimestamps;
    }

    @Nullable
    public String resolvePartnerId(@NonNull String currentUserId) {
        if (participants == null || participants.isEmpty()) {
            return null;
        }
        for (String participant : participants.keySet()) {
            if (!currentUserId.equals(participant)) {
                return participant;
            }
        }
        return null;
    }
}

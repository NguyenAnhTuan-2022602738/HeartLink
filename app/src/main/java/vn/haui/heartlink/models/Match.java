package vn.haui.heartlink.models;

import androidx.annotation.Nullable;

import com.google.firebase.database.PropertyName;

/**
 * Represents a mutual connection saved under the Matches table in Firebase.
 */
public class Match {

    @PropertyName("match_id")
    private String matchId;

    @PropertyName("user_id_1")
    private String userId1;

    @PropertyName("user_id_2")
    private String userId2;

    @PropertyName("matched_at")
    private Long matchedAt;

    // Required default constructor for Firebase deserialization
    public Match() {
    }

    public Match(@Nullable String matchId,
                 @Nullable String userId1,
                 @Nullable String userId2,
                 @Nullable Long matchedAt) {
        this.matchId = matchId;
        this.userId1 = userId1;
        this.userId2 = userId2;
        this.matchedAt = matchedAt;
    }

    @PropertyName("match_id")
    public String getMatchId() {
        return matchId;
    }

    @PropertyName("match_id")
    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    @PropertyName("user_id_1")
    public String getUserId1() {
        return userId1;
    }

    @PropertyName("user_id_1")
    public void setUserId1(String userId1) {
        this.userId1 = userId1;
    }

    @PropertyName("user_id_2")
    public String getUserId2() {
        return userId2;
    }

    @PropertyName("user_id_2")
    public void setUserId2(String userId2) {
        this.userId2 = userId2;
    }

    @PropertyName("matched_at")
    public Long getMatchedAt() {
        return matchedAt;
    }

    @PropertyName("matched_at")
    public void setMatchedAt(Long matchedAt) {
        this.matchedAt = matchedAt;
    }
}

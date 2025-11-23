package vn.haui.heartlink.models;

public class Interaction {
    private String status;
    private String type;
    private long likedAt;
    private Long matchedAt; // Added matchedAt
    private String partnerId; // Added partnerId
    private String displayName; // Added displayName

    public Interaction() { }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getLikedAt() {
        return likedAt;
    }

    public void setLikedAt(long likedAt) {
        this.likedAt = likedAt;
    }

    // Added getter and setter for matchedAt
    public Long getMatchedAt() {
        return matchedAt;
    }

    public void setMatchedAt(Long matchedAt) {
        this.matchedAt = matchedAt;
    }

    // Added getter and setter for partnerId
    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    // Added getter and setter for displayName
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}

package vn.haui.heartlink.models;

/**
 * View model used to render a discovery card.
 */
public class DiscoveryProfile {
    private final User user;
    private final double distanceKm;
    private final String displayName;
    private final String subtitle;
    private final String photoUrl;

    public DiscoveryProfile(User user, double distanceKm, String displayName, String subtitle, String photoUrl) {
        this.user = user;
        this.distanceKm = distanceKm;
        this.displayName = displayName;
        this.subtitle = subtitle;
        this.photoUrl = photoUrl;
    }

    public User getUser() {
        return user;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }
}

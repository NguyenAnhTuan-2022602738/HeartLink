package vn.haui.heartlink.models;

import com.google.firebase.database.ServerValue;

import java.util.List;

/**
 * User model for Firebase Realtime Database
 */
public class User {
    private String uid;
    private String email;
    private String name;
    private String gender;
    private String dateOfBirth;
    private String bio;
    private List<String> photoUrls;
    private String seekingGender;
    private int seekingAgeMin;
    private int seekingAgeMax;
    private String seekingType;
    private List<String> interests;
    private boolean profileComplete;
    private Double latitude;
    private Double longitude;
    private Boolean locationVisible;
    private Boolean notificationsEnabled;
    private boolean online;
    private long lastSeen;

    public User() {
        // Default constructor required for Firebase
    }

    public User(String uid, String email) {
        this.uid = uid;
        this.email = email;
        this.profileComplete = false;
        this.online = false;
        this.lastSeen = 0; // Initialize with a default value
    }

    // Getters
    public String getUid() {
        return uid;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getGender() {
        return gender;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getBio() {
        return bio;
    }

    public List<String> getPhotoUrls() {
        return photoUrls;
    }

    public String getSeekingGender() {
        return seekingGender;
    }

    public int getSeekingAgeMin() {
        return seekingAgeMin;
    }

    public int getSeekingAgeMax() {
        return seekingAgeMax;
    }

    public String getSeekingType() {
        return seekingType;
    }

    public List<String> getInterests() {
        return interests;
    }

    public boolean isProfileComplete() {
        return profileComplete;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Boolean getLocationVisible() {
        return locationVisible;
    }

    public Boolean getNotificationsEnabled() {
        return notificationsEnabled;
    }

    public boolean isOnline() {
        return online;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    // Setters
    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setPhotoUrls(List<String> photoUrls) {
        this.photoUrls = photoUrls;
    }

    public void setSeekingGender(String seekingGender) {
        this.seekingGender = seekingGender;
    }

    public void setSeekingAgeMin(int seekingAgeMin) {
        this.seekingAgeMin = seekingAgeMin;
    }

    public void setSeekingAgeMax(int seekingAgeMax) {
        this.seekingAgeMax = seekingAgeMax;
    }

    public void setSeekingType(String seekingType) {
        this.seekingType = seekingType;
    }

    public void setInterests(List<String> interests) {
        this.interests = interests;
    }

    public void setProfileComplete(boolean profileComplete) {
        this.profileComplete = profileComplete;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setLocationVisible(Boolean locationVisible) {
        this.locationVisible = locationVisible;
    }

    public void setNotificationsEnabled(Boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }
}

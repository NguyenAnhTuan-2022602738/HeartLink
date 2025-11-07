package vn.haui.heartlink.models;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Stores discovery filter selections persisted across sessions.
 */
public class FilterPreferences implements Parcelable {

    public static final String INTEREST_FEMALE = "female";
    public static final String INTEREST_MALE = "male";
    public static final String INTEREST_BOTH = "both";

    private String interestedIn = INTEREST_BOTH;
    private float maxDistanceKm = 50f;
    private int minAge = 18;
    private int maxAge = 45;
    @Nullable
    private Double locationLatitude;
    @Nullable
    private Double locationLongitude;
    @Nullable
    private String locationLabel;

    public FilterPreferences() {
        // Defaults already defined in fields.
    }

    protected FilterPreferences(Parcel in) {
        interestedIn = in.readString();
        maxDistanceKm = in.readFloat();
        minAge = in.readInt();
        maxAge = in.readInt();
        if (in.readByte() == 0) {
            locationLatitude = null;
        } else {
            locationLatitude = in.readDouble();
        }
        if (in.readByte() == 0) {
            locationLongitude = null;
        } else {
            locationLongitude = in.readDouble();
        }
        locationLabel = in.readString();
    }

    public static final Creator<FilterPreferences> CREATOR = new Creator<FilterPreferences>() {
        @Override
        public FilterPreferences createFromParcel(Parcel in) {
            return new FilterPreferences(in);
        }

        @Override
        public FilterPreferences[] newArray(int size) {
            return new FilterPreferences[size];
        }
    };

    public String getInterestedIn() {
        return interestedIn;
    }

    public void setInterestedIn(String interestedIn) {
        this.interestedIn = interestedIn;
    }

    public float getMaxDistanceKm() {
        return maxDistanceKm;
    }

    public void setMaxDistanceKm(float maxDistanceKm) {
        this.maxDistanceKm = maxDistanceKm;
    }

    public int getMinAge() {
        return minAge;
    }

    public void setMinAge(int minAge) {
        this.minAge = minAge;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }

    @Nullable
    public Double getLocationLatitude() {
        return locationLatitude;
    }

    public void setLocationLatitude(@Nullable Double locationLatitude) {
        this.locationLatitude = locationLatitude;
    }

    @Nullable
    public Double getLocationLongitude() {
        return locationLongitude;
    }

    public void setLocationLongitude(@Nullable Double locationLongitude) {
        this.locationLongitude = locationLongitude;
    }

    @Nullable
    public String getLocationLabel() {
        return locationLabel;
    }

    public void setLocationLabel(@Nullable String locationLabel) {
        this.locationLabel = locationLabel;
    }

    public boolean hasCustomLocation() {
        return locationLatitude != null && locationLongitude != null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(interestedIn);
        dest.writeFloat(maxDistanceKm);
        dest.writeInt(minAge);
        dest.writeInt(maxAge);
        if (locationLatitude == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeDouble(locationLatitude);
        }
        if (locationLongitude == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeDouble(locationLongitude);
        }
        dest.writeString(locationLabel);
    }

    @NonNull
    public FilterPreferences copy() {
        FilterPreferences copy = new FilterPreferences();
        copy.setInterestedIn(interestedIn);
        copy.setMaxDistanceKm(maxDistanceKm);
        copy.setMinAge(minAge);
        copy.setMaxAge(maxAge);
        copy.setLocationLatitude(locationLatitude);
        copy.setLocationLongitude(locationLongitude);
        copy.setLocationLabel(locationLabel);
        return copy;
    }
}

package vn.haui.heartlink.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

import vn.haui.heartlink.models.FilterPreferences;
import vn.haui.heartlink.models.User;

/**
 * Handles persistence of discovery filter selections using SharedPreferences.
 */
public class DiscoveryFilterStorage {

    private static final String PREFS_FILE = "discovery_filters";
    private static final String KEY_INTERESTED_IN = "filters_interested_in";
    private static final String KEY_DISTANCE = "filters_distance_km";
    private static final String KEY_MIN_AGE = "filters_min_age";
    private static final String KEY_MAX_AGE = "filters_max_age";
    private static final String KEY_LOCATION_LAT = "filters_location_lat";
    private static final String KEY_LOCATION_LNG = "filters_location_lng";
    private static final String KEY_LOCATION_LABEL = "filters_location_label";

    private final SharedPreferences preferences;

    public DiscoveryFilterStorage(@NonNull Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
    }

    /**
     * Tải filter preferences từ SharedPreferences, fallback về user preferences nếu chưa có.
     *
     * @param currentUser User hiện tại để lấy default values
     * @return FilterPreferences đã được load
     */
    @NonNull
    public FilterPreferences load(@Nullable User currentUser) {
        FilterPreferences preferencesModel = new FilterPreferences();

        String interestedIn = preferences.getString(KEY_INTERESTED_IN, null);
        if (TextUtils.isEmpty(interestedIn) && currentUser != null && !TextUtils.isEmpty(currentUser.getSeekingGender())) {
            interestedIn = currentUser.getSeekingGender();
        }
        if (TextUtils.isEmpty(interestedIn)) {
            interestedIn = FilterPreferences.INTEREST_BOTH;
        }
        preferencesModel.setInterestedIn(normalizeInterestedValue(interestedIn));

        float defaultDistance = 50f;
        if (preferences.contains(KEY_DISTANCE)) {
            defaultDistance = preferences.getFloat(KEY_DISTANCE, defaultDistance);
        }
        preferencesModel.setMaxDistanceKm(defaultDistance);

        int minAge = preferences.getInt(KEY_MIN_AGE, 18);
        int maxAge = preferences.getInt(KEY_MAX_AGE, 45);
        if (currentUser != null) {
            if (minAge == 18 && currentUser.getSeekingAgeMin() > 0) {
                minAge = currentUser.getSeekingAgeMin();
            }
            if (maxAge == 45 && currentUser.getSeekingAgeMax() > 0) {
                maxAge = currentUser.getSeekingAgeMax();
            }
        }
        preferencesModel.setMinAge(minAge);
        preferencesModel.setMaxAge(Math.max(minAge, maxAge));

        if (preferences.contains(KEY_LOCATION_LAT) && preferences.contains(KEY_LOCATION_LNG)) {
            double lat = Double.longBitsToDouble(preferences.getLong(KEY_LOCATION_LAT, Double.doubleToLongBits(0)));
            double lng = Double.longBitsToDouble(preferences.getLong(KEY_LOCATION_LNG, Double.doubleToLongBits(0)));
            preferencesModel.setLocationLatitude(lat);
            preferencesModel.setLocationLongitude(lng);
        }

        String label = preferences.getString(KEY_LOCATION_LABEL, null);
        preferencesModel.setLocationLabel(label);

        return preferencesModel;
    }

    /**
     * Lưu filter preferences vào SharedPreferences.
     *
     * @param filters FilterPreferences cần lưu
     */
    public void save(@NonNull FilterPreferences filters) {
        preferences.edit()
                .putString(KEY_INTERESTED_IN, filters.getInterestedIn())
                .putFloat(KEY_DISTANCE, filters.getMaxDistanceKm())
                .putInt(KEY_MIN_AGE, filters.getMinAge())
                .putInt(KEY_MAX_AGE, filters.getMaxAge())
                .apply();

        SharedPreferences.Editor editor = preferences.edit();
        if (filters.hasCustomLocation()) {
            editor.putLong(KEY_LOCATION_LAT, Double.doubleToLongBits(filters.getLocationLatitude()));
            editor.putLong(KEY_LOCATION_LNG, Double.doubleToLongBits(filters.getLocationLongitude()));
        } else {
            editor.remove(KEY_LOCATION_LAT);
            editor.remove(KEY_LOCATION_LNG);
        }

        if (!TextUtils.isEmpty(filters.getLocationLabel())) {
            editor.putString(KEY_LOCATION_LABEL, filters.getLocationLabel());
        } else {
            editor.remove(KEY_LOCATION_LABEL);
        }
        editor.apply();
    }

    /**
     * Xóa tất cả filter preferences đã lưu trong SharedPreferences.
     */
    public void clear() {
        preferences.edit()
                .remove(KEY_INTERESTED_IN)
                .remove(KEY_DISTANCE)
                .remove(KEY_MIN_AGE)
                .remove(KEY_MAX_AGE)
                .remove(KEY_LOCATION_LAT)
                .remove(KEY_LOCATION_LNG)
                .remove(KEY_LOCATION_LABEL)
                .apply();
    }

    /**
     * Chuẩn hóa giá trị interested in về format chuẩn (male/female/both).
     *
     * @param raw Giá trị thô từ input
     * @return Giá trị đã chuẩn hóa
     */
    @NonNull
    private String normalizeInterestedValue(@NonNull String raw) {
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("f") || normalized.contains("nữ")) {
            return FilterPreferences.INTEREST_FEMALE;
        }
        if (normalized.startsWith("m") || normalized.contains("nam")) {
            return FilterPreferences.INTEREST_MALE;
        }
        return FilterPreferences.INTEREST_BOTH;
    }
}

package vn.haui.heartlink.utils;

import android.content.Context;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;
import vn.haui.heartlink.R;

/**
 * Helper class to handle interest localization
 * Interests are stored in DB using language-independent keys
 * and displayed using localized names based on current language
 */
public class InterestMapper {
    
    /**
     * Convert display names to keys for database storage
     */
    public static List<String> displayNamesToKeys(@NonNull Context context, @NonNull List<String> displayNames) {
        String[] keys = context.getResources().getStringArray(R.array.interest_keys);
        String[] names = context.getResources().getStringArray(R.array.interest_names);
        
        List<String> result = new ArrayList<>();
        for (String displayName : displayNames) {
            for (int i = 0; i < names.length && i < keys.length; i++) {
                if (names[i].equals(displayName)) {
                    result.add(keys[i]);
                    break;
                }
            }
        }
        return result;
    }
    
    /**
     * Convert keys from database to display names for current language
     */
    public static List<String> keysToDisplayNames(@NonNull Context context, @NonNull List<String> keys) {
        String[] keyArray = context.getResources().getStringArray(R.array.interest_keys);
        String[] names = context.getResources().getStringArray(R.array.interest_names);
        
        List<String> result = new ArrayList<>();
        for (String key : keys) {
            for (int i = 0; i < keyArray.length && i < names.length; i++) {
                if (keyArray[i].equals(key)) {
                    result.add(names[i]);
                    break;
                }
            }
        }
        return result;
    }
    
    /**
     * Get all interest display names for current language
     */
    public static String[] getAllDisplayNames(@NonNull Context context) {
        return context.getResources().getStringArray(R.array.interest_names);
    }
    
    /**
     * Get all interest keys
     */
    public static String[] getAllKeys(@NonNull Context context) {
        return context.getResources().getStringArray(R.array.interest_keys);
    }
}

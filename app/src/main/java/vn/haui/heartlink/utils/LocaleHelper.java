package vn.haui.heartlink.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import androidx.annotation.NonNull;
import java.util.Locale;

/**
 * Helper to apply user's language preference to any Context
 */
public class LocaleHelper {
    
    private static final String PREFS_NAME = "HeartLinkPrefs";
    private static final String KEY_LANGUAGE = "language";
    
    /**
     * Wrap context with user's preferred language
     * Use this when creating notifications or other system UI
     */
    @NonNull
    public static Context wrapContext(@NonNull Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String language = preferences.getString(KEY_LANGUAGE, "vi");
        
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        
        Resources resources = context.getResources();
        Configuration config = new Configuration(resources.getConfiguration());
        config.setLocale(locale);
        
        return context.createConfigurationContext(config);
    }
    
    /**
     * Get user's preferred language code
     */
    @NonNull
    public static String getPreferredLanguage(@NonNull Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return preferences.getString(KEY_LANGUAGE, "vi");
    }
}

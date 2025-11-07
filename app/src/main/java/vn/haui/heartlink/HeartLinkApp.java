package vn.haui.heartlink;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Application class responsible for initializing global services once per app start.
 */
public class HeartLinkApp extends Application {

    private static final String TAG = "HeartLinkApp";

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        
        // Enable Firebase Database offline persistence
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
            Log.d(TAG, "Firebase Database initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Firebase Database initialization failed", e);
        }
    }
}

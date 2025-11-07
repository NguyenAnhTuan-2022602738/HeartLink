package vn.haui.heartlink.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;

import vn.haui.heartlink.activities.GenderSelectionActivity;
import vn.haui.heartlink.activities.MainActivity;
import vn.haui.heartlink.models.User;

/**
 * Helper class to check user profile status and navigate accordingly
 */
public class NavigationHelper {
    
    private static final String TAG = "NavigationHelper";
    
    /**
     * Check user profile and navigate to appropriate activity
     * If profile incomplete -> GenderSelectionActivity
     * If profile complete -> MainActivity
     */
    public static void checkProfileAndNavigate(@NonNull Context context, @NonNull FirebaseUser firebaseUser) {
        String uid = firebaseUser.getUid();
        String email = firebaseUser.getEmail();
        
        Log.d(TAG, "checkProfileAndNavigate called for uid: " + uid);
        
        UserRepository userRepository = UserRepository.getInstance();
        
        userRepository.getUserData(uid)
            .addOnSuccessListener(dataSnapshot -> {
                Log.d(TAG, "getUserData success, dataSnapshot.exists(): " + dataSnapshot.exists());
                if (dataSnapshot.exists()) {
                    // User exists, check if profile is complete
                    User user = dataSnapshot.getValue(User.class);
                    Log.d(TAG, "User data retrieved: " + (user != null ? "not null" : "null"));
                    if (user != null && user.isProfileComplete()) {
                        // Profile complete, go to MainActivity
                        Log.d(TAG, "Profile complete, navigating to MainActivity");
                        navigateToMainActivity(context);
                    } else {
                        // Profile incomplete, start onboarding
                        Log.d(TAG, "Profile incomplete, navigating to GenderSelectionActivity");
                        navigateToGenderSelection(context);
                    }
                } else {
                    // User doesn't exist, create new user and start onboarding
                    Log.d(TAG, "User doesn't exist, creating new user");
                    createNewUserAndNavigate(context, uid, email);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking user profile: " + e.getClass().getSimpleName() + " - " + e.getMessage(), e);
                Toast.makeText(context, "Lỗi khi kiểm tra thông tin người dùng: " + e.getMessage(), 
                    Toast.LENGTH_LONG).show();
                // On error, assume new user and start onboarding
                createNewUserAndNavigate(context, uid, email);
            });
    }
    
    /**
     * Tạo user mới trong database và chuyển đến onboarding.
     */
    private static void createNewUserAndNavigate(Context context, String uid, String email) {
        User newUser = new User(uid, email);
        UserRepository userRepository = UserRepository.getInstance();
        
        Log.d(TAG, "Creating new user in database");
        userRepository.createUser(newUser)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "New user created successfully");
                navigateToGenderSelection(context);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error creating new user: " + e.getClass().getSimpleName() + " - " + e.getMessage(), e);
                
                // Check if Firebase Database is not configured
                String errorMsg = e.getMessage();
                if (errorMsg != null && (errorMsg.contains("database") || errorMsg.contains("URL"))) {
                    Toast.makeText(context, "⚠️ Firebase Database chưa được cấu hình. Vui lòng enable Realtime Database trong Firebase Console.", 
                        Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, "Lỗi khi tạo thông tin người dùng: " + errorMsg, 
                        Toast.LENGTH_LONG).show();
                }
                
                // Still navigate to onboarding even if database fails
                Log.d(TAG, "Navigating to onboarding despite database error");
                navigateToGenderSelection(context);
            });
    }
    
    /**
     * Chuyển đến GenderSelectionActivity để bắt đầu onboarding.
     */
    private static void navigateToGenderSelection(Context context) {
        Intent intent = new Intent(context, GenderSelectionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
    
    /**
     * Chuyển đến MainActivity - màn hình chính của app.
     */
    private static void navigateToMainActivity(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}

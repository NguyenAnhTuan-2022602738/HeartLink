package vn.haui.heartlink.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import vn.haui.heartlink.R;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_TIME_OUT = 3000; // 3 seconds

    /**
     * Initializes the splash screen activity, sets up the logo animation,
     * and navigates to WelcomeActivity or MainActivity after a delay based on
     * the user's authentication state.
     *
     * @param savedInstanceState Bundle containing the activity's previously saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Animation
        ImageView logo = findViewById(R.id.splash_logo);
        Animation flyIn = AnimationUtils.loadAnimation(this, R.anim.fly_in);
        logo.startAnimation(flyIn);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                // User is signed in, navigate to MainActivity
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
            } else {
                // No user is signed in, navigate to WelcomeActivity
                Intent intent = new Intent(SplashActivity.this, WelcomeActivity.class);
                startActivity(intent);
            }
            finish();
        }, SPLASH_TIME_OUT);
    }
}
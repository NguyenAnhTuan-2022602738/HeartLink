package vn.haui.heartlink.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import vn.haui.heartlink.R;
import vn.haui.heartlink.admin.AdminActivity;
import vn.haui.heartlink.models.User;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_TIME_OUT = 3000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.splash_logo);
        Animation flyIn = AnimationUtils.loadAnimation(this, R.anim.fly_in);
        logo.startAnimation(flyIn);

        new Handler(Looper.getMainLooper()).postDelayed(this::checkUserStatus, SPLASH_TIME_OUT);
    }

    private void checkUserStatus() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // User is signed in, check their role
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        User user = snapshot.getValue(User.class);
                        if (user != null && "Admin".equals(user.getRole())) {
                            // Navigate to Admin Dashboard
                            startActivity(new Intent(SplashActivity.this, AdminActivity.class));
                        } else {
                            // Navigate to Main Activity for regular users
                            startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        }
                    } else {
                        // User data not found, go to Welcome screen
                        startActivity(new Intent(SplashActivity.this, WelcomeActivity.class));
                    }
                    finish();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // On error, default to Welcome screen
                    startActivity(new Intent(SplashActivity.this, WelcomeActivity.class));
                    finish();
                }
            });
        } else {
            // No user is signed in, navigate to WelcomeActivity
            startActivity(new Intent(SplashActivity.this, WelcomeActivity.class));
            finish();
        }
    }
}

package vn.haui.heartlink.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import vn.haui.heartlink.R;
import vn.haui.heartlink.activities.WelcomeActivity;

public class AdminActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private final Fragment dashboardFragment = new AdminDashboardFragment();
    private final Fragment userManagementFragment = new UserManagementFragment();
    private Fragment activeFragment = dashboardFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        toolbar = findViewById(R.id.admin_toolbar);
        setSupportActionBar(toolbar);

        BottomNavigationView bottomNav = findViewById(R.id.admin_bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListener);

        getSupportFragmentManager().beginTransaction().add(R.id.admin_fragment_container, userManagementFragment, "2").hide(userManagementFragment).commit();
        getSupportFragmentManager().beginTransaction().add(R.id.admin_fragment_container, dashboardFragment, "1").commit();

        toolbar.setTitle("Thống kê");
        setupPresenceHandling();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            showLogoutConfirmationDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> performLogout())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void performLogout() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference userStatusRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
            Map<String, Object> statusUpdate = new HashMap<>();
            statusUpdate.put("online", false);
            statusUpdate.put("lastSeen", ServerValue.TIMESTAMP);
            userStatusRef.updateChildren(statusUpdate).addOnCompleteListener(task -> {
                FirebaseAuth.getInstance().signOut();
                navigateToWelcomeScreen();
            });
        } else {
            navigateToWelcomeScreen();
        }
    }

    private void navigateToWelcomeScreen() {
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupPresenceHandling() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            return; // No user logged in
        }
        String uid = firebaseUser.getUid();
        DatabaseReference userStatusRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");

        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class) != null && snapshot.getValue(Boolean.class);
                if (connected) {
                    userStatusRef.child("online").onDisconnect().setValue(false);
                    userStatusRef.child("lastSeen").onDisconnect().setValue(ServerValue.TIMESTAMP);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Log error if needed
            }
        });
    }

    private final BottomNavigationView.OnNavigationItemSelectedListener navListener = item -> {
        Fragment selectedFragment = null;
        String title = "";

        if (item.getItemId() == R.id.admin_nav_dashboard) {
            selectedFragment = dashboardFragment;
            title = "Thống kê";
        } else if (item.getItemId() == R.id.admin_nav_users) {
            selectedFragment = userManagementFragment;
            title = "Quản lý người dùng";
        }

        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction().hide(activeFragment).show(selectedFragment).commit();
            activeFragment = selectedFragment;
            toolbar.setTitle(title);
            return true;
        }
        return false;
    };
}

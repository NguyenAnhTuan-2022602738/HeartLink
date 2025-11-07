package vn.haui.heartlink.activities;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

import vn.haui.heartlink.R;
import vn.haui.heartlink.utils.UserRepository;

/**
 * Final onboarding screen prompting the user to enable push notifications.
 */
public class NotificationPermissionActivity extends AppCompatActivity {

    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private Button allowButton;

    /**
     * Phương thức khởi tạo activity yêu cầu quyền thông báo.
     * Thiết lập permission launcher và giao diện người dùng.
     *
     * @param savedInstanceState Trạng thái đã lưu của activity (có thể null)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_permission);

        setupPermissionLauncher();
        setupUi();
    }

    /**
     * Phương thức thiết lập ActivityResultLauncher để request quyền thông báo.
     */
    private void setupPermissionLauncher() {
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted || isNotificationsEnabled()) {
                        onPermissionGranted();
                    } else {
                        onPermissionDenied();
                    }
                });
    }

    /**
     * Phương thức thiết lập giao diện người dùng và click listeners.
     */
    private void setupUi() {
        TextView skipButton = findViewById(R.id.notification_skip_button);
        allowButton = findViewById(R.id.notification_allow_button);

        skipButton.setOnClickListener(v -> {
            updateNotificationPreference(false);
        });

        allowButton.setOnClickListener(v -> requestNotificationPermission());
    }

    /**
     * Phương thức request quyền hiển thị thông báo từ người dùng.
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (hasNotificationPermission()) {
                onPermissionGranted();
            } else {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            onPermissionGranted();
        }
    }

    /**
     * Phương thức được gọi khi quyền thông báo được cấp.
     * Cập nhật preference và điều hướng đến MainActivity.
     */
    private void onPermissionGranted() {
        updateNotificationPreference(true);
        Toast.makeText(this, R.string.notification_permission_granted, Toast.LENGTH_SHORT).show();
    }

    /**
     * Phương thức được gọi khi quyền thông báo bị từ chối.
     * Vẫn cập nhật preference và điều hướng đến MainActivity.
     */
    private void onPermissionDenied() {
        updateNotificationPreference(false);
        Toast.makeText(this, R.string.notification_permission_denied, Toast.LENGTH_SHORT).show();
    }

    private boolean isNotificationsEnabled() {
        return NotificationManagerCompat.from(this).areNotificationsEnabled();
    }

    private boolean hasNotificationPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Phương thức cập nhật preference thông báo trong Firebase.
     *
     * @param enabled true nếu bật thông báo, false nếu tắt
     */
    private void updateNotificationPreference(boolean enabled) {
        allowButton.setEnabled(false);
        allowButton.setAlpha(0.5f);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            navigateToMain();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("notificationsEnabled", enabled);

        UserRepository.getInstance().updateUser(currentUser.getUid(), updates, new UserRepository.OnCompleteListener() {
            @Override
            public void onSuccess() {
                navigateToMain();
            }

            @Override
            public void onFailure(Exception e) {
                allowButton.setEnabled(true);
                allowButton.setAlpha(1f);
                Toast.makeText(NotificationPermissionActivity.this,
                        getString(R.string.notification_save_error, e.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Phương thức điều hướng đến MainActivity.
     * Tạo Intent với flags để clear task stack.
     */
    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

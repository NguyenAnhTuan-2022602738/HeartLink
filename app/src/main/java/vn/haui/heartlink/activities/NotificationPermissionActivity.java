package vn.haui.heartlink.activities;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
    private boolean isEditMode = false;

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

        isEditMode = getIntent().getBooleanExtra("IS_EDIT_MODE", false);

        setupPermissionLauncher();
        setupUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateButtonState();
    }

    /**
     * Phương thức thiết lập ActivityResultLauncher để request quyền thông báo.
     */
    private void setupPermissionLauncher() {
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    updateButtonState(); // Cập nhật trạng thái nút sau khi có kết quả
                    if (isGranted) {
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
        View header = findViewById(R.id.header);
        ImageView backButton = header.findViewById(R.id.back_button);
        TextView skipButton = header.findViewById(R.id.skip_button);
        ProgressBar progressBar = header.findViewById(R.id.progress_bar);

        allowButton = findViewById(R.id.notification_allow_button);

        if (isEditMode) {
            skipButton.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
        } else {
            progressBar.setProgress(100);
        }

        skipButton.setOnClickListener(v -> {
            updateNotificationPreference(false, true);
        });
        backButton.setOnClickListener(v -> finish());

        allowButton.setOnClickListener(v -> {
            if (isNotificationsEnabled()) {
                // Mở cài đặt thông báo của ứng dụng
                Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                startActivity(intent);
            } else {
                requestNotificationPermission();
            }
        });
    }

    private void updateButtonState() {
        if (isNotificationsEnabled()) {
            allowButton.setText(getString(R.string.notification_permission_denied));
        } else {
            allowButton.setText(getString(R.string.notification_permission_allow));
        }
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
            // Đối với các phiên bản Android cũ hơn, quyền được coi là đã cấp
            onPermissionGranted();
        }
    }

    /**
     * Phương thức được gọi khi quyền thông báo được cấp.
     * Cập nhật preference và điều hướng đến MainActivity.
     */
    private void onPermissionGranted() {
        updateNotificationPreference(true, !isEditMode);
        Toast.makeText(this, R.string.notification_permission_granted, Toast.LENGTH_SHORT).show();
        if (isEditMode) {
            finish();
        }
    }

    /**
     * Phương thức được gọi khi quyền thông báo bị từ chối.
     * Vẫn cập nhật preference và điều hướng đến MainActivity.
     */
    private void onPermissionDenied() {
        updateNotificationPreference(false, !isEditMode);
        Toast.makeText(this, R.string.notification_permission_denied, Toast.LENGTH_SHORT).show();
        if (isEditMode) {
            finish();
        }
    }

    private boolean isNotificationsEnabled() {
        return NotificationManagerCompat.from(this).areNotificationsEnabled();
    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED;
        }
        return true; // Đối với API < 33, quyền được coi là đã cấp
    }

    /**
     * Phương thức cập nhật preference thông báo trong Firebase.
     *
     * @param enabled      true nếu bật thông báo, false nếu tắt
     * @param navigateNext true nếu cần điều hướng đến màn hình tiếp theo
     */
    private void updateNotificationPreference(boolean enabled, boolean navigateNext) {
        allowButton.setEnabled(false);
        allowButton.setAlpha(0.5f);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            if (navigateNext) navigateToMain();
            else finish();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("notificationsEnabled", enabled);

        UserRepository.getInstance().updateUser(currentUser.getUid(), updates, new UserRepository.OnCompleteListener() {
            @Override
            public void onSuccess() {
                if (navigateNext) navigateToMain();
                else {
                    allowButton.setEnabled(true);
                    allowButton.setAlpha(1f);
                    if (isEditMode) finish();
                }
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
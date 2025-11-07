package vn.haui.heartlink.activities;

import android.Manifest;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

import vn.haui.heartlink.R;
import vn.haui.heartlink.utils.UserRepository;

/**
 * Shows the user a friendly explanation before requesting location permission.
 */
public class LocationPermissionActivity extends AppCompatActivity {

    private ActivityResultLauncher<String> permissionLauncher;
    private FusedLocationProviderClient fusedLocationClient;
    private Button allowButton;
    private Button manualButton;

    /**
     * Phương thức khởi tạo activity yêu cầu quyền truy cập vị trí.
     * Thiết lập permission launcher, giao diện người dùng và FusedLocationProviderClient.
     *
     * @param savedInstanceState Trạng thái đã lưu của activity (có thể null)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_permission);

        setupPermissionLauncher();
        setupUi();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    /**
     * Phương thức thiết lập ActivityResultLauncher để request quyền truy cập vị trí.
     * Xử lý kết quả khi permission được cấp hoặc từ chối.
     */
    private void setupPermissionLauncher() {
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                onPermissionGranted();
            } else {
                Toast.makeText(this, R.string.location_permission_denied_message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Phương thức thiết lập giao diện người dùng và các click listeners.
     * Gán sự kiện cho các button skip, back, allow và manual input.
     */
    private void setupUi() {
        TextView skipButton = findViewById(R.id.location_skip_button);
        View backContainer = findViewById(R.id.location_back_button_container);
        ImageView backIcon = findViewById(R.id.location_back_button);
        allowButton = findViewById(R.id.location_allow_button);
        manualButton = findViewById(R.id.location_manual_button);

    View.OnClickListener finishFlow = v -> navigateToNotification();

        skipButton.setOnClickListener(finishFlow);
        backContainer.setOnClickListener(v -> finish());
        backIcon.setOnClickListener(v -> finish());

        allowButton.setOnClickListener(v -> requestLocationPermission());
        manualButton.setOnClickListener(v -> onManualInputSelected());
    }

    /**
     * Phương thức request quyền truy cập vị trí từ người dùng.
     * Sử dụng permission launcher để yêu cầu ACCESS_FINE_LOCATION.
     */
    private void requestLocationPermission() {
        if (permissionLauncher != null) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Phương thức được gọi khi quyền truy cập vị trí được cấp.
     * Bắt đầu lấy vị trí hiện tại với độ chính xác cao.
     */
    private void onPermissionGranted() {
        setProcessing(true);
        CancellationTokenSource tokenSource = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.getToken())
                .addOnSuccessListener(this::handleLocationResult)
                .addOnFailureListener(e -> fetchLastKnownLocation());
    }

    /**
     * Phương thức xử lý khi người dùng chọn nhập vị trí thủ công.
     * Hiển thị thông báo placeholder và lưu vị trí null (tắt chia sẻ vị trí).
     */
    private void onManualInputSelected() {
        Toast.makeText(this, R.string.location_manual_entry_placeholder, Toast.LENGTH_SHORT).show();
        persistLocation(null, null, false);
    }

    /**
     * Phương thức lấy vị trí cuối cùng đã biết từ FusedLocationProviderClient.
     * Được gọi khi không thể lấy vị trí hiện tại.
     */
    private void fetchLastKnownLocation() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this::handleLocationResult)
                .addOnFailureListener(e -> {
                    setProcessing(false);
                    Toast.makeText(this, R.string.location_fetch_error, Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Phương thức xử lý kết quả vị trí được trả về từ FusedLocationProviderClient.
     * Nếu có vị trí thì lưu vào Firebase, nếu không thì hiển thị lỗi.
     *
     * @param location Đối tượng Location chứa thông tin vị trí (có thể null)
     */
    private void handleLocationResult(Location location) {
        if (location != null) {
            persistLocation(location.getLatitude(), location.getLongitude(), true);
        } else {
            setProcessing(false);
            Toast.makeText(this, R.string.location_fetch_error, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Phương thức lưu thông tin vị trí vào Firebase database.
     * Cập nhật latitude, longitude và locationVisible cho người dùng hiện tại.
     *
     * @param latitude Vĩ độ của vị trí (có thể null)
     * @param longitude Kinh độ của vị trí (có thể null)
     * @param visible Cờ cho biết có chia sẻ vị trí hay không
     */
    private void persistLocation(Double latitude, Double longitude, boolean visible) {
        setProcessing(true);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            navigateToNotification();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        if (latitude != null && longitude != null) {
            updates.put("latitude", latitude);
            updates.put("longitude", longitude);
        } else {
            updates.put("latitude", null);
            updates.put("longitude", null);
        }
        updates.put("locationVisible", visible);

        UserRepository.getInstance().updateUser(currentUser.getUid(), updates, new UserRepository.OnCompleteListener() {
            @Override
            public void onSuccess() {
        if (visible) {
            Toast.makeText(LocationPermissionActivity.this,
                R.string.location_permission_granted_message, Toast.LENGTH_SHORT).show();
        }
        navigateToNotification();
            }

            @Override
            public void onFailure(Exception e) {
                setProcessing(false);
                Toast.makeText(LocationPermissionActivity.this,
                        getString(R.string.location_save_error, e.getMessage()),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Phương thức thiết lập trạng thái processing của giao diện.
     * Vô hiệu hóa và làm mờ các button khi đang xử lý,
     * kích hoạt lại khi hoàn thành.
     *
     * @param processing true nếu đang xử lý, false nếu đã hoàn thành
     */
    private void setProcessing(boolean processing) {
        if (allowButton != null) {
            allowButton.setEnabled(!processing);
            allowButton.setAlpha(processing ? 0.5f : 1f);
        }
        if (manualButton != null) {
            manualButton.setEnabled(!processing);
            manualButton.setAlpha(processing ? 0.5f : 1f);
        }
    }

    /**
     * Phương thức điều hướng đến NotificationPermissionActivity.
     * Tạo Intent với flags để clear task stack và bắt đầu activity mới.
     */
    private void navigateToNotification() {
        Intent intent = new Intent(this, NotificationPermissionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

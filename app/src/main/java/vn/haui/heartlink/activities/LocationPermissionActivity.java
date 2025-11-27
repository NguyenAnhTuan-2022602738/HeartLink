package vn.haui.heartlink.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

import vn.haui.heartlink.R;
import vn.haui.heartlink.models.User;
import vn.haui.heartlink.utils.UserRepository;

public class LocationPermissionActivity extends AppCompatActivity {

    private ActivityResultLauncher<String> permissionLauncher;
    private FusedLocationProviderClient fusedLocationClient;
    private Button allowButton;
    private Button manualButton;
    private boolean isEditMode = false;
    private boolean locationSharingEnabled = false; // State for location sharing

    private final ActivityResultLauncher<Intent> mapPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    double latitude = result.getData().getDoubleExtra(MapPickerActivity.EXTRA_LATITUDE, 0);
                    double longitude = result.getData().getDoubleExtra(MapPickerActivity.EXTRA_LONGITUDE, 0);
                    persistLocation(latitude, longitude, true);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_permission);

        isEditMode = getIntent().getBooleanExtra("IS_EDIT_MODE", false);

        setupPermissionLauncher();
        setupUi();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        loadUserLocationState();
    }

    private void loadUserLocationState() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) return;

        setProcessing(true);
        UserRepository.getInstance().getUserData(firebaseUser.getUid()).addOnSuccessListener(snapshot -> {
            User user = snapshot.getValue(User.class);
            locationSharingEnabled = user != null && user.getLocationVisible() != null && user.getLocationVisible();
            updateUi();
            setProcessing(false);
        }).addOnFailureListener(e -> {
            locationSharingEnabled = false;
            updateUi();
            setProcessing(false);
        });
    }

    private void setupPermissionLauncher() {
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            updateUi();
            if (isGranted) {
                onPermissionGranted();
            } else {
                Toast.makeText(this, R.string.location_permission_denied_message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupUi() {
        View header = findViewById(R.id.header);
        ImageView backButton = header.findViewById(R.id.back_button);
        TextView skipButton = header.findViewById(R.id.skip_button);
        ProgressBar progressBar = header.findViewById(R.id.progress_bar);

        allowButton = findViewById(R.id.location_allow_button);
        manualButton = findViewById(R.id.location_manual_button);

        if (isEditMode) {
            skipButton.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
        } else {
            progressBar.setProgress(100);
            skipButton.setOnClickListener(v -> navigateToNext());
        }

        backButton.setOnClickListener(v -> finish());
        allowButton.setOnClickListener(v -> handleAllowButtonClick());
        manualButton.setOnClickListener(v -> onManualInputSelected());
    }

    private void updateUi() {
        if (!hasLocationPermission()) {
            allowButton.setText(getString(R.string.location_permission_allow));
        } else {
            if (locationSharingEnabled) {
                allowButton.setText(R.string.profile_settings_disable_location);
            } else {
                allowButton.setText(R.string.location_permission_allow);
            }
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void handleAllowButtonClick() {
        if (!hasLocationPermission()) {
            requestLocationPermission();
        } else {
            if (locationSharingEnabled) {
                persistLocation(null, null, false);
            } else {
                onPermissionGranted();
            }
        }
    }

    private void requestLocationPermission() {
        if (permissionLauncher != null) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void onPermissionGranted() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // This check is required by Lint, but in this flow, permission is already granted.
            return;
        }
        setProcessing(true);
        CancellationTokenSource tokenSource = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.getToken())
                .addOnSuccessListener(this::handleLocationResult)
                .addOnFailureListener(e -> fetchLastKnownLocation());
    }

    private void onManualInputSelected() {
        Intent intent = new Intent(this, MapPickerActivity.class);
        mapPickerLauncher.launch(intent);
    }

    private void fetchLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // This check is required by Lint.
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this::handleLocationResult)
                .addOnFailureListener(e -> {
                    setProcessing(false);
                    Toast.makeText(this, R.string.location_fetch_error, Toast.LENGTH_SHORT).show();
                });
    }

    private void handleLocationResult(Location location) {
        if (location != null) {
            persistLocation(location.getLatitude(), location.getLongitude(), true);
        } else {
            setProcessing(false);
            Toast.makeText(this, R.string.location_fetch_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void persistLocation(Double latitude, Double longitude, boolean visible) {
        setProcessing(true);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            setProcessing(false);
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("latitude", latitude);
        updates.put("longitude", longitude);
        updates.put("locationVisible", visible);

        UserRepository.getInstance().updateUser(currentUser.getUid(), updates, new UserRepository.OnCompleteListener() {
            @Override
            public void onSuccess() {
                locationSharingEnabled = visible;
                setResult(Activity.RESULT_OK);

                if (visible) {
                    Toast.makeText(LocationPermissionActivity.this,
                            R.string.location_permission_granted_message, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LocationPermissionActivity.this,
                            R.string.profile_settings_location_hidden, Toast.LENGTH_SHORT).show();
                }

                if (isEditMode) {
                    updateUi();
                    setProcessing(false);
                } else {
                    navigateToNext();
                }
            }

            @Override
            public void onFailure(Exception e) {
                setProcessing(false);
                Toast.makeText(LocationPermissionActivity.this,
                        getString(R.string.location_save_error, e.getMessage()),
                        Toast.LENGTH_SHORT).show();
                updateUi();
            }
        });
    }

    private void setProcessing(boolean processing) {
        allowButton.setEnabled(!processing);
        allowButton.setAlpha(processing ? 0.5f : 1f);
        manualButton.setEnabled(!processing);
        manualButton.setAlpha(processing ? 0.5f : 1f);
    }

    private void navigateToNext() {
        Intent intent = new Intent(this, NotificationPermissionActivity.class);
        startActivity(intent);
        finish();
    }
}

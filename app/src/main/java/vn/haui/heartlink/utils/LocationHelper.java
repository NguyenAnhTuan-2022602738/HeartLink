package vn.haui.heartlink.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationHelper {

    public interface LocationCallback {
        void onLocationFound(double latitude, double longitude, String address);
        void onLocationError(String error);
    }

    private final Context context;
    private final FusedLocationProviderClient fusedLocationClient;
    private final ActivityResultLauncher<String> requestPermissionLauncher;

    public LocationHelper(Context context, ActivityResultLauncher<String> requestPermissionLauncher) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        this.requestPermissionLauncher = requestPermissionLauncher;
    }

    public void getCurrentLocation(LocationCallback callback) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    getAddressFromLocation(location, callback);
                } else {
                    callback.onLocationError("Không thể lấy vị trí. GPS đã được bật chưa?");
                }
            }).addOnFailureListener(e -> callback.onLocationError(e.getMessage()));
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void getAddressFromLocation(Location location, LocationCallback callback) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String addressString = address.getLocality() + ", " + address.getAdminArea();
                callback.onLocationFound(location.getLatitude(), location.getLongitude(), addressString);
            } else {
                callback.onLocationError("Không tìm thấy địa chỉ cho vị trí này.");
            }
        } catch (IOException e) {
            callback.onLocationError("Lỗi dịch vụ Geocoder.");
        }
    }
}

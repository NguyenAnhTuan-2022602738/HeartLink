package vn.haui.heartlink.activities;

import android.app.Activity;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;

import com.google.android.material.button.MaterialButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import vn.haui.heartlink.R;

public class MapPickerActivity extends AppCompatActivity implements Marker.OnMarkerDragListener {

    private MapView map;
    private GeoPoint selectedGeoPoint;
    private Marker selectedLocationMarker;
    private MaterialButton confirmButton;
    private SearchView searchView;

    public static final String EXTRA_LATITUDE = "extra_latitude";
    public static final String EXTRA_LONGITUDE = "extra_longitude";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize osmdroid configuration
        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        setContentView(R.layout.activity_map_picker);

        map = findViewById(R.id.map);
        confirmButton = findViewById(R.id.confirm_location_button);
        searchView = findViewById(R.id.search_view);

        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        double initialLat = getIntent().getDoubleExtra(EXTRA_LATITUDE, 0);
        double initialLng = getIntent().getDoubleExtra(EXTRA_LONGITUDE, 0);

        if (initialLat != 0 && initialLng != 0) {
            selectedGeoPoint = new GeoPoint(initialLat, initialLng);
            confirmButton.setEnabled(true);
        } else {
            // Default to a central point but don't select it
            selectedGeoPoint = new GeoPoint(21.028511, 105.804817); // Ha Noi
            confirmButton.setEnabled(false);
        }

        map.getController().setZoom(15.0);
        map.getController().setCenter(selectedGeoPoint);

        selectedLocationMarker = new Marker(map);
        selectedLocationMarker.setPosition(selectedGeoPoint);
        selectedLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        selectedLocationMarker.setDraggable(true);
        map.getOverlays().add(selectedLocationMarker);

        selectedLocationMarker.setOnMarkerDragListener(this);

        confirmButton.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            if (selectedGeoPoint != null) {
                resultIntent.putExtra(EXTRA_LATITUDE, selectedGeoPoint.getLatitude());
                resultIntent.putExtra(EXTRA_LONGITUDE, selectedGeoPoint.getLongitude());
            }
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        });

        setupSearchView();
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchLocation(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    private void searchLocation(String query) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(query, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                selectedGeoPoint = new GeoPoint(address.getLatitude(), address.getLongitude());
                map.getController().animateTo(selectedGeoPoint);
                selectedLocationMarker.setPosition(selectedGeoPoint);
                confirmButton.setEnabled(true);
            } else {
                Toast.makeText(this, "Không tìm thấy địa chỉ", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Lỗi khi tìm kiếm địa chỉ", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        confirmButton.setEnabled(false);
    }

    @Override
    public void onMarkerDrag(Marker marker) {
        // Method is required but can be empty
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        selectedGeoPoint = marker.getPosition();
        confirmButton.setEnabled(true);
    }
}

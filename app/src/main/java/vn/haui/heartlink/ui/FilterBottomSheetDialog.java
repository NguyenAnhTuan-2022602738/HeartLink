package vn.haui.heartlink.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vn.haui.heartlink.R;
import vn.haui.heartlink.models.FilterPreferences;

/**
 * Bottom sheet allowing the user to adjust discovery filters.
 */
public class FilterBottomSheetDialog extends BottomSheetDialogFragment {

    public interface FilterApplyListener {
        void onApply(@NonNull FilterPreferences preferences);

        void onClear();
    }

    private static final String ARG_FILTERS = "arg_filters";
    private static final String ARG_DEFAULT_LOCATION = "arg_default_location";

    @Nullable
    private FilterApplyListener listener;
    private FilterPreferences workingCopy;
    private ExecutorService geocodeExecutor;

    private MaterialButtonToggleGroup interestedToggleGroup;
    private TextView locationValueText;
    private ProgressBar locationProgress;
    private Slider distanceSlider;
    private RangeSlider ageRangeSlider;
    private TextView distanceValueText;
    private TextView ageValueText;

    private String fallbackLocationLabel;

    public static FilterBottomSheetDialog newInstance(@NonNull FilterPreferences filters,
                                                      @Nullable String fallbackLocationLabel) {
        FilterBottomSheetDialog dialog = new FilterBottomSheetDialog();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILTERS, filters);
        args.putString(ARG_DEFAULT_LOCATION, fallbackLocationLabel);
        dialog.setArguments(args);
        return dialog;
    }

    public void setFilterApplyListener(@Nullable FilterApplyListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        geocodeExecutor = Executors.newSingleThreadExecutor();
    }

    @NonNull
    @Override
    public android.app.Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        android.app.Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            if (dialog instanceof com.google.android.material.bottomsheet.BottomSheetDialog) {
                com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet = (com.google.android.material.bottomsheet.BottomSheetDialog) dialog;
                View sheet = bottomSheet.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (sheet != null) {
                    sheet.setBackgroundResource(R.drawable.bg_filter_bottom_sheet);
                }
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_discovery_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        workingCopy = requireArguments().getParcelable(ARG_FILTERS);
        if (workingCopy == null) {
            workingCopy = new FilterPreferences();
        } else {
            workingCopy = workingCopy.copy();
        }
        fallbackLocationLabel = requireArguments().getString(ARG_DEFAULT_LOCATION);

        interestedToggleGroup = view.findViewById(R.id.filter_interested_toggle);
        locationValueText = view.findViewById(R.id.filter_location_value);
        locationProgress = view.findViewById(R.id.filter_location_progress);
        distanceSlider = view.findViewById(R.id.filter_distance_slider);
        ageRangeSlider = view.findViewById(R.id.filter_age_slider);
    distanceValueText = view.findViewById(R.id.filter_distance_value);
    ageValueText = view.findViewById(R.id.filter_age_value);
        MaterialButton clearButton = view.findViewById(R.id.filter_clear_button);
        MaterialButton applyButton = view.findViewById(R.id.filter_apply_button);
        View locationRow = view.findViewById(R.id.filter_location_row);

        setupToggleGroup();
        setupSliders();
        updateLocationLabel();

        clearButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClear();
            }
            dismiss();
        });

        applyButton.setOnClickListener(v -> {
            persistSelections();
            if (listener != null) {
                listener.onApply(workingCopy);
            }
            dismiss();
        });

        locationRow.setOnClickListener(v -> showLocationInputDialog());
    }

    private void setupToggleGroup() {
        String interested = workingCopy.getInterestedIn();
        int buttonId = R.id.filter_interested_all;
        if (FilterPreferences.INTEREST_FEMALE.equalsIgnoreCase(interested)) {
            buttonId = R.id.filter_interested_female;
        } else if (FilterPreferences.INTEREST_MALE.equalsIgnoreCase(interested)) {
            buttonId = R.id.filter_interested_male;
        }
        interestedToggleGroup.check(buttonId);
        interestedToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            if (checkedId == R.id.filter_interested_female) {
                workingCopy.setInterestedIn(FilterPreferences.INTEREST_FEMALE);
            } else if (checkedId == R.id.filter_interested_male) {
                workingCopy.setInterestedIn(FilterPreferences.INTEREST_MALE);
            } else {
                workingCopy.setInterestedIn(FilterPreferences.INTEREST_BOTH);
            }
        });
    }

    private void setupSliders() {
        float distanceMin = distanceSlider.getValueFrom();
        float distanceMax = distanceSlider.getValueTo();
        float distanceValue = workingCopy.getMaxDistanceKm();
        distanceValue = Math.max(distanceMin, Math.min(distanceMax, distanceValue));
        distanceSlider.setValue(distanceValue);

        float ageMinBound = ageRangeSlider.getValueFrom();
        float ageMaxBound = ageRangeSlider.getValueTo();
        float minAgeValue = workingCopy.getMinAge();
        float maxAgeValue = workingCopy.getMaxAge();
        minAgeValue = Math.max(ageMinBound, Math.min(ageMaxBound, minAgeValue));
        maxAgeValue = Math.max(minAgeValue, Math.min(ageMaxBound, maxAgeValue));
        ageRangeSlider.setValues(minAgeValue, maxAgeValue);

        updateDistanceLabel(distanceSlider.getValue());
        updateAgeLabel(ageRangeSlider.getValues());

        distanceSlider.addOnChangeListener((slider, value, fromUser) -> updateDistanceLabel(value));
        ageRangeSlider.addOnChangeListener((slider, value, fromUser) -> updateAgeLabel(slider.getValues()));
    }

    private void persistSelections() {
        workingCopy.setMaxDistanceKm(distanceSlider.getValue());
        List<Float> ageValues = ageRangeSlider.getValues();
        if (ageValues.size() >= 2) {
            int min = Math.round(ageValues.get(0));
            int max = Math.round(ageValues.get(1));
            if (min > max) {
                int tmp = min;
                min = max;
                max = tmp;
            }
            workingCopy.setMinAge(min);
            workingCopy.setMaxAge(max);
        }
    }

    private void updateLocationLabel() {
        String label = workingCopy.getLocationLabel();
        if (TextUtils.isEmpty(label)) {
            label = fallbackLocationLabel;
        }
        if (TextUtils.isEmpty(label)) {
            label = getString(R.string.filter_location_placeholder);
        }
        locationValueText.setText(label);
    }

    private void updateDistanceLabel(float value) {
        if (distanceValueText != null) {
            distanceValueText.setText(String.format(Locale.getDefault(), "%dkm", Math.round(value)));
        }
    }

    private void updateAgeLabel(@NonNull List<Float> values) {
        if (values.size() >= 2 && ageValueText != null) {
            int min = Math.round(values.get(0));
            int max = Math.round(values.get(1));
            ageValueText.setText(String.format(Locale.getDefault(), "%d-%d", min, max));
        }
    }

    private void showLocationInputDialog() {
    View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.view_location_input_field, null, false);
    final TextInputEditText input = dialogView.findViewById(R.id.filter_location_input);
    if (input != null) {
        input.setText(workingCopy.getLocationLabel());
    }

    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.filter_location_dialog_title)
        .setView(dialogView)
                .setPositiveButton(R.string.action_apply, (dialog, which) -> geocodeAndSetLocation(input.getText() != null ? input.getText().toString() : null))
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.filter_location_use_current, (dialog, which) -> {
                    workingCopy.setLocationLatitude(null);
                    workingCopy.setLocationLongitude(null);
                    workingCopy.setLocationLabel(null);
                    updateLocationLabel();
                })
                .show();
    }

    private void geocodeAndSetLocation(@Nullable String query) {
        if (TextUtils.isEmpty(query)) {
            workingCopy.setLocationLatitude(null);
            workingCopy.setLocationLongitude(null);
            workingCopy.setLocationLabel(null);
            updateLocationLabel();
            return;
        }

        locationProgress.setVisibility(View.VISIBLE);
        geocodeExecutor.execute(() -> {
            try {
                android.location.Geocoder geocoder = new android.location.Geocoder(requireContext(), Locale.getDefault());
                List<android.location.Address> results = geocoder.getFromLocationName(query, 1);
                if (results != null && !results.isEmpty()) {
                    android.location.Address address = results.get(0);
                    Double lat = address.getLatitude();
                    Double lng = address.getLongitude();
                    String label = address.getFeatureName();
                    if (TextUtils.isEmpty(label)) {
                        label = address.getSubAdminArea();
                    }
                    if (TextUtils.isEmpty(label)) {
                        label = address.getAdminArea();
                    }
                    if (TextUtils.isEmpty(label)) {
                        label = address.getCountryName();
                    }
                    final Double finalLat = lat;
                    final Double finalLng = lng;
                    final String finalLabel = !TextUtils.isEmpty(label) ? label : query;
                    requireActivity().runOnUiThread(() -> {
                        workingCopy.setLocationLatitude(finalLat);
                        workingCopy.setLocationLongitude(finalLng);
                        workingCopy.setLocationLabel(finalLabel);
                        updateLocationLabel();
                        locationProgress.setVisibility(View.GONE);
                    });
                } else {
                    showGeocodeError();
                }
            } catch (Exception e) {
                showGeocodeError();
            }
        });
    }

    private void showGeocodeError() {
        requireActivity().runOnUiThread(() -> {
            locationProgress.setVisibility(View.GONE);
            android.widget.Toast.makeText(requireContext(), R.string.filter_location_error, android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        if (!geocodeExecutor.isShutdown()) {
            geocodeExecutor.shutdownNow();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        interestedToggleGroup = null;
        locationValueText = null;
        locationProgress = null;
        distanceSlider = null;
        ageRangeSlider = null;
        distanceValueText = null;
        ageValueText = null;
    }
}

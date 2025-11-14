package vn.haui.heartlink.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.slider.Slider;

import java.util.List;
import java.util.Locale;

import vn.haui.heartlink.R;
import vn.haui.heartlink.models.FilterPreferences;

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

    private RadioGroup interestedToggleGroup;
    private TextView locationValueText;
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
        distanceSlider = view.findViewById(R.id.filter_distance_slider);
        ageRangeSlider = view.findViewById(R.id.filter_age_slider);
        distanceValueText = view.findViewById(R.id.filter_distance_value);
        ageValueText = view.findViewById(R.id.filter_age_value);
        TextView clearButton = view.findViewById(R.id.filter_clear_button);
        MaterialButton applyButton = view.findViewById(R.id.filter_apply_button);

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

    }

    private void setupToggleGroup() {
        String interested = workingCopy.getInterestedIn();
        int buttonId = R.id.filter_interested_all;
        if (FilterPreferences.INTEREST_FEMALE.equalsIgnoreCase(interested)) {
            buttonId = R.id.filter_interested_female;
        } else if (FilterPreferences.INTEREST_MALE.equalsIgnoreCase(interested)) {
            buttonId = R.id.filter_interested_male;
        }
        RadioButton button = interestedToggleGroup.findViewById(buttonId);
        if (button != null) {
            button.setChecked(true);
        }
        interestedToggleGroup.setOnCheckedChangeListener((group, checkedId) -> {
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        interestedToggleGroup = null;
        locationValueText = null;
        distanceSlider = null;
        ageRangeSlider = null;
        distanceValueText = null;
        ageValueText = null;
    }
}

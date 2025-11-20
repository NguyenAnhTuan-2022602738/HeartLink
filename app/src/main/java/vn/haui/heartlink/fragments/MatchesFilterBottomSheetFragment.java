package vn.haui.heartlink.fragments;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import vn.haui.heartlink.R;

public class MatchesFilterBottomSheetFragment extends BottomSheetDialogFragment {

    public static final String TAG = "MatchesFilterBottomSheet";
    private static final String ARG_CURRENT_FILTER = "current_filter";

    private FilterSelectionListener listener;
    private String currentFilter = "all";

    public interface FilterSelectionListener {
        void onFilterSelected(String filter);
    }

    public static MatchesFilterBottomSheetFragment newInstance(String currentFilter) {
        MatchesFilterBottomSheetFragment fragment = new MatchesFilterBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CURRENT_FILTER, currentFilter);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (getParentFragment() instanceof FilterSelectionListener) {
            listener = (FilterSelectionListener) getParentFragment();
        } else {
            throw new RuntimeException("Calling fragment must implement FilterSelectionListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentFilter = getArguments().getString(ARG_CURRENT_FILTER, "all");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_bottom_sheet_matches_filter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.filter_all).setOnClickListener(v -> selectFilter("all"));
        view.findViewById(R.id.filter_matched).setOnClickListener(v -> selectFilter("matched"));
        view.findViewById(R.id.filter_liked).setOnClickListener(v -> selectFilter("liked"));
        view.findViewById(R.id.filter_superliked).setOnClickListener(v -> selectFilter("superlike"));

        updateSelectionView(view);
    }

    private void updateSelectionView(View view) {
        if (getContext() == null) return;

        TextView all = view.findViewById(R.id.filter_all);
        TextView matched = view.findViewById(R.id.filter_matched);
        TextView liked = view.findViewById(R.id.filter_liked);
        TextView superliked = view.findViewById(R.id.filter_superliked);

        resetTextViewStyles(all, matched, liked, superliked);

        TextView selectedView = null;
        switch (currentFilter) {
            case "all":
                selectedView = all;
                break;
            case "matched":
                selectedView = matched;
                break;
            case "liked":
                selectedView = liked;
                break;
            case "superlike":
                selectedView = superliked;
                break;
        }

        if (selectedView != null) {
            selectedView.setTextColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
            Typeface boldTypeface = ResourcesCompat.getFont(getContext(), R.font.montserrat_bold);
            selectedView.setTypeface(boldTypeface);
        }
    }

    private void resetTextViewStyles(TextView... textViews) {
        if (getContext() == null) return;

        Typeface regularTypeface = ResourcesCompat.getFont(getContext(), R.font.montserrat_semibold);
        int defaultColor = ContextCompat.getColor(getContext(), R.color.textColorPrimary);

        for (TextView textView : textViews) {
            textView.setTypeface(regularTypeface);
            textView.setTextColor(defaultColor);
        }
    }

     private void selectFilter(String filter) {
        if (listener != null) {
            listener.onFilterSelected(filter);
        }
        dismiss();
    }
}
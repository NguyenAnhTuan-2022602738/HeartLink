package vn.haui.heartlink.activities;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vn.haui.heartlink.R;
import vn.haui.heartlink.ui.GradientTextView;
import vn.haui.heartlink.utils.UserRepository;

public class InterestsActivity extends AppCompatActivity {

    private static final int MIN_INTEREST_SELECTION = 3;

    private Button continueButton;
    private InterestsAdapter adapter;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interests);

        isEditMode = getIntent().getBooleanExtra("IS_EDIT_MODE", false);

        View header = findViewById(R.id.header);
        ImageView backButton = header.findViewById(R.id.back_button);
        TextView skipButton = header.findViewById(R.id.skip_button);
        ProgressBar progressBar = header.findViewById(R.id.progress_bar);

        continueButton = findViewById(R.id.continue_button_interests);
        RecyclerView recyclerView = findViewById(R.id.interests_recycler_view);

        if (isEditMode) {
            continueButton.setText(getString(R.string.save));
            skipButton.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
        } else {
            progressBar.setProgress(60);
        }

        backButton.setOnClickListener(v -> onBackPressed());

        String[] names = getResources().getStringArray(R.array.interest_names);
        int[] icons = new int[]{
                R.drawable.ic_interest_camera,
                R.drawable.ic_interest_shopping,
                R.drawable.ic_interest_mic,
                R.drawable.ic_interest_yoga,
                R.drawable.ic_interest_cooking,
                R.drawable.ic_interest_tennis,
                R.drawable.ic_interest_running,
                R.drawable.ic_interest_swimming,
                R.drawable.ic_interest_paint,
                R.drawable.ic_interest_mountain,
                R.drawable.ic_interest_parachute,
                R.drawable.ic_interest_music,
                R.drawable.ic_interest_wine,
                R.drawable.ic_interest_game
        };

        List<InterestItem> items = new ArrayList<>();
        for (int i = 0; i < names.length && i < icons.length; i++) {
            items.add(new InterestItem(names[i], icons[i]));
        }

        adapter = new InterestsAdapter(items, this::onSelectionChanged);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        int spacing = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(2, spacing, true));
        recyclerView.setAdapter(adapter);

        updateContinueButton(false);

        if (isEditMode) {
            loadUserInterests();
        }

        continueButton.setOnClickListener(v -> handleContinue());
        skipButton.setOnClickListener(v -> saveInterestsAndFinish(Collections.emptyList()));
    }

    private void onSelectionChanged(int selectedCount) {
        updateContinueButton(selectedCount >= MIN_INTEREST_SELECTION);
    }

    private void updateContinueButton(boolean enabled) {
        continueButton.setEnabled(enabled);
        continueButton.setAlpha(enabled ? 1f : 0.5f);
    }

    private void handleContinue() {
        List<String> selected = adapter.getSelectedInterests();
        if (selected.size() < MIN_INTEREST_SELECTION) {
            Toast.makeText(this, R.string.interests_min_selection_message, Toast.LENGTH_SHORT).show();
            return;
        }
        saveInterestsAndFinish(selected);
    }

    private void loadUserInterests() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            UserRepository.getInstance().getUserData(currentUser.getUid()).addOnSuccessListener(dataSnapshot -> {
                if (dataSnapshot.exists()) {
                    Map<String, Object> data = (Map<String, Object>) dataSnapshot.getValue();
                    List<String> interestKeys = (List<String>) data.get("interests");
                    if (interestKeys != null) {
                        // Convert keys to display names for current language
                        List<String> displayNames = vn.haui.heartlink.utils.InterestMapper.keysToDisplayNames(
                                InterestsActivity.this, interestKeys);
                        adapter.setSelectedInterests(displayNames);
                    }
                }
            });
        }
    }

    private void saveInterestsAndFinish(List<String> interests) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            if (!isEditMode) {
                navigateToPhotoUpload();
            }
            return;
        }

        // Convert display names to keys for database storage
        List<String> interestKeys = vn.haui.heartlink.utils.InterestMapper.displayNamesToKeys(
                InterestsActivity.this, interests);

        Map<String, Object> updates = new HashMap<>();
        updates.put("interests", interestKeys);

        continueButton.setEnabled(false);

        UserRepository.getInstance().updateUser(currentUser.getUid(), updates, new UserRepository.OnCompleteListener() {
            @Override
            public void onSuccess() {
                if (isEditMode) {
                    finish();
                } else {
                    navigateToPhotoUpload();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(InterestsActivity.this,
                        getString(R.string.interests_save_error, e.getMessage()),
                        Toast.LENGTH_SHORT).show();
                if (!isEditMode) {
                    navigateToPhotoUpload();
                }
            }
        });
    }

    private void navigateToPhotoUpload() {
        Intent intent = new Intent(this, PhotoUploadActivity.class);
        startActivity(intent);
        finish();
    }

    private static class InterestItem {
        final String name;
        final int iconRes;

        InterestItem(String name, int iconRes) {
            this.name = name;
            this.iconRes = iconRes;
        }
    }

    private interface SelectionChangedListener {
        void onSelectionChanged(int count);
    }

    private static class InterestsAdapter extends RecyclerView.Adapter<InterestsAdapter.InterestViewHolder> {

        private final List<InterestItem> items;
        private final List<Integer> selectedPositions = new ArrayList<>();
        private final SelectionChangedListener listener;

        InterestsAdapter(List<InterestItem> items, SelectionChangedListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public InterestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_interest_option, parent, false);
            return new InterestViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull InterestViewHolder holder, int position) {
            InterestItem item = items.get(position);
            boolean selected = selectedPositions.contains(position);
            holder.bind(item, selected);
            holder.itemView.setOnClickListener(v -> toggleSelection(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        private void toggleSelection(int position) {
            if (selectedPositions.contains(position)) {
                selectedPositions.remove(Integer.valueOf(position));
            } else {
                selectedPositions.add(position);
            }
            notifyItemChanged(position);
            if (listener != null) {
                listener.onSelectionChanged(selectedPositions.size());
            }
        }

        List<String> getSelectedInterests() {
            List<String> result = new ArrayList<>();
            for (int index : selectedPositions) {
                if (index >= 0 && index < items.size()) {
                    result.add(items.get(index).name);
                }
            }
            return result;
        }

        void setSelectedInterests(List<String> interests) {
            selectedPositions.clear();
            for (String interest : interests) {
                for (int i = 0; i < items.size(); i++) {
                    if (items.get(i).name.equals(interest)) {
                        selectedPositions.add(i);
                        break;
                    }
                }
            }
            notifyDataSetChanged();
            if (listener != null) {
                listener.onSelectionChanged(selectedPositions.size());
            }
        }

        static class InterestViewHolder extends RecyclerView.ViewHolder {

            private final MaterialCardView cardView;
            private final ImageView iconView;
            private final TextView titleView;

            InterestViewHolder(@NonNull View itemView) {
                super(itemView);
                cardView = (MaterialCardView) itemView;
                iconView = itemView.findViewById(R.id.interest_icon);
                titleView = itemView.findViewById(R.id.interest_label);
            }

            void bind(InterestItem item, boolean selected) {
                iconView.setImageResource(item.iconRes);
                titleView.setText(item.name);
                cardView.setChecked(selected);

                if (selected) {
                    cardView.setCardElevation(8 * itemView.getResources().getDisplayMetrics().density);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        cardView.setOutlineSpotShadowColor(ContextCompat.getColor(itemView.getContext(), R.color.colorPrimary));
                        cardView.setOutlineAmbientShadowColor(ContextCompat.getColor(itemView.getContext(), R.color.colorPrimary));
                        titleView.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.interest_selected ));
                        iconView.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.interest_selected));
                    }
                } else {
                    cardView.setCardElevation(0);
                }
            }
        }
    }

    public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private int spanCount;
        private int spacing;
        private boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int column = position % spanCount;

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount;
                outRect.right = (column + 1) * spacing / spanCount;

                if (position < spanCount) { // top edge
                    outRect.top = spacing;
                }
                outRect.bottom = spacing; // item bottom
            } else {
                outRect.left = column * spacing / spanCount;
                outRect.right = spacing - (column + 1) * spacing / spanCount;
                if (position >= spanCount) {
                    outRect.top = spacing;
                }
            }
        }
    }
}

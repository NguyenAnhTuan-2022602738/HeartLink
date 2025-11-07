package vn.haui.heartlink.activities;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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
import vn.haui.heartlink.utils.UserRepository;

public class InterestsActivity extends AppCompatActivity {

    private static final int MIN_INTEREST_SELECTION = 3;

    private Button continueButton;
    private InterestsAdapter adapter;

    /**
     * Phương thức khởi tạo activity chọn sở thích.
     * Thiết lập RecyclerView với GridLayoutManager, tạo danh sách sở thích,
     * thiết lập adapter và các click listeners cho điều hướng.
     *
     * @param savedInstanceState Trạng thái đã lưu của activity (có thể null)
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interests);

        continueButton = findViewById(R.id.continue_button_interests);
        TextView skipButton = findViewById(R.id.skip_button_interests);
        RecyclerView recyclerView = findViewById(R.id.interests_recycler_view);
        MaterialCardView backButtonContainer = findViewById(R.id.back_button_container);
        View backIcon = findViewById(R.id.back_button_interests);

        backButtonContainer.setOnClickListener(v -> onBackPressed());
        backIcon.setOnClickListener(v -> onBackPressed());

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
        int spacing = getResources().getDimensionPixelSize(R.dimen.interests_vertical_spacing);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(2, spacing));
        recyclerView.setAdapter(adapter);

        updateContinueButton(false);

        continueButton.setOnClickListener(v -> handleContinue());
        skipButton.setOnClickListener(v -> saveInterestsAndFinish(Collections.emptyList()));
    }

    /**
     * Phương thức callback được gọi khi số lượng sở thích được chọn thay đổi.
     * Cập nhật trạng thái của button continue dựa trên số lượng tối thiểu yêu cầu.
     *
     * @param selectedCount Số lượng sở thích đã được chọn
     */
    private void onSelectionChanged(int selectedCount) {
        updateContinueButton(selectedCount >= MIN_INTEREST_SELECTION);
    }

    /**
     * Phương thức cập nhật trạng thái của button continue.
     * Kích hoạt button khi có đủ số lượng sở thích tối thiểu,
     * và làm mờ button khi chưa đủ.
     *
     * @param enabled true nếu button được kích hoạt, false nếu bị vô hiệu hóa
     */
    private void updateContinueButton(boolean enabled) {
        continueButton.setEnabled(enabled);
        continueButton.setAlpha(enabled ? 1f : 0.5f);
    }

    /**
     * Phương thức xử lý khi người dùng nhấn button continue.
     * Kiểm tra số lượng sở thích đã chọn có đạt tối thiểu không,
     * nếu đủ thì lưu và chuyển activity tiếp theo.
     */
    private void handleContinue() {
        List<String> selected = adapter.getSelectedInterests();
        if (selected.size() < MIN_INTEREST_SELECTION) {
            Toast.makeText(this, R.string.interests_min_selection_message, Toast.LENGTH_SHORT).show();
            return;
        }
        saveInterestsAndFinish(selected);
    }

    /**
     * Phương thức lưu danh sách sở thích đã chọn vào Firebase
     * và chuyển sang activity tiếp theo (PhotoUploadActivity).
     * Xử lý cả trường hợp thành công và thất bại khi lưu dữ liệu.
     *
     * @param interests Danh sách tên sở thích đã được chọn
     */
    private void saveInterestsAndFinish(List<String> interests) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            navigateToPhotoUpload();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
    updates.put("interests", interests);

        continueButton.setEnabled(false);

        UserRepository.getInstance().updateUser(currentUser.getUid(), updates, new UserRepository.OnCompleteListener() {
            @Override
            public void onSuccess() {
                navigateToPhotoUpload();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(InterestsActivity.this,
                        getString(R.string.interests_save_error, e.getMessage()),
                        Toast.LENGTH_SHORT).show();
                navigateToPhotoUpload();
            }
        });
    }

    /**
     * Phương thức điều hướng đến PhotoUploadActivity.
     * Tạo Intent với flags để clear task stack và bắt đầu activity mới.
     */
    private void navigateToPhotoUpload() {
        Intent intent = new Intent(this, PhotoUploadActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
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

        static class InterestViewHolder extends RecyclerView.ViewHolder {

            private final MaterialCardView cardView;
            private final View container;
            private final ImageView iconView;
            private final TextView titleView;
            private final int defaultIconColor;
            private final int defaultTextColor;
            private final int selectedColor;
            private final int defaultStrokeWidth;
            private final float density;

            InterestViewHolder(@NonNull View itemView) {
                super(itemView);
                cardView = (MaterialCardView) itemView;
                container = itemView.findViewById(R.id.interest_container);
                iconView = itemView.findViewById(R.id.interest_icon);
                titleView = itemView.findViewById(R.id.interest_label);
                defaultIconColor = ContextCompat.getColor(itemView.getContext(), R.color.interest_chip_icon_default);
                defaultTextColor = ContextCompat.getColor(itemView.getContext(), R.color.interest_chip_text_default);
                selectedColor = ContextCompat.getColor(itemView.getContext(), android.R.color.white);
                this.density = itemView.getResources().getDisplayMetrics().density;
                defaultStrokeWidth = Math.max(1, Math.round(this.density));
            }

            void bind(InterestItem item, boolean selected) {
                iconView.setImageResource(item.iconRes);
                iconView.setColorFilter(selected ? selectedColor : defaultIconColor);
                titleView.setText(item.name);
                titleView.setTextColor(selected ? selectedColor : defaultTextColor);

                container.setBackgroundResource(selected ?
                        R.drawable.bg_interest_option_selected :
                        R.drawable.bg_interest_option_unselected);

                cardView.setCardElevation(selected ? 6f * density : 0f);
                cardView.setStrokeWidth(selected ? 0 : defaultStrokeWidth);
            }
        }
    }

    private static class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private final int spanCount;
        private final int spacing;

        GridSpacingItemDecoration(int spanCount, int spacing) {
            this.spanCount = spanCount;
            this.spacing = spacing;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                   @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int column = position % spanCount;

            outRect.left = spacing - column * spacing / spanCount;
            outRect.right = (column + 1) * spacing / spanCount;
            outRect.bottom = spacing;
            if (position >= spanCount) {
                outRect.top = spacing;
            }
        }
    }
}

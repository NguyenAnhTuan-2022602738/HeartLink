package vn.haui.heartlink.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import vn.haui.heartlink.Constants;
import vn.haui.heartlink.R;

/**
 * Screen that celebrates a new match between two users.
 */
public class MatchSuccessActivity extends AppCompatActivity {

    public static Intent createIntent(Context context,
                                      @Nullable String partnerName,
                                      @Nullable String partnerPhotoUrl,
                                      @Nullable String selfPhotoUrl) {
        Intent intent = new Intent(context, MatchSuccessActivity.class);
        intent.putExtra(Constants.EXTRA_MATCH_PARTNER_NAME, partnerName);
        intent.putExtra(Constants.EXTRA_MATCH_PARTNER_PHOTO_URL, partnerPhotoUrl);
        intent.putExtra(Constants.EXTRA_MATCH_SELF_PHOTO_URL, selfPhotoUrl);
        return intent;
    }

    /**
     * Phương thức khởi tạo activity hiển thị thành công match.
     * Thiết lập giao diện và tải thông tin của match partner.
     *
     * @param savedInstanceState Trạng thái đã lưu của activity (có thể null)
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_match_success);

        View root = findViewById(R.id.match_success_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return windowInsets;
        });

        WindowInsetsControllerCompat controller = ViewCompat.getWindowInsetsController(root);
        if (controller != null) {
            controller.setAppearanceLightStatusBars(true);
            controller.setAppearanceLightNavigationBars(true);
        }

        ImageView partnerImage = findViewById(R.id.match_partner_image);
        ImageView currentImage = findViewById(R.id.match_current_image);
        TextView titleView = findViewById(R.id.match_title);
        TextView subtitleView = findViewById(R.id.match_subtitle);
        MaterialButton waveButton = findViewById(R.id.match_wave_button);
        MaterialButton continueButton = findViewById(R.id.match_continue_button);

        Intent intent = getIntent();
        String partnerName = intent.getStringExtra(Constants.EXTRA_MATCH_PARTNER_NAME);
        String partnerPhoto = intent.getStringExtra(Constants.EXTRA_MATCH_PARTNER_PHOTO_URL);
        String currentPhoto = intent.getStringExtra(Constants.EXTRA_MATCH_SELF_PHOTO_URL);

        if (TextUtils.isEmpty(partnerName)) {
            partnerName = getString(R.string.match_success_unknown_person);
        }

        titleView.setText(getString(R.string.match_success_title, partnerName));
        subtitleView.setText(R.string.match_success_subtitle);

        loadImage(partnerImage, partnerPhoto);
        loadImage(currentImage, currentPhoto);

        waveButton.setOnClickListener(v -> Toast.makeText(
                MatchSuccessActivity.this,
                R.string.match_wave_placeholder,
                Toast.LENGTH_SHORT
        ).show());

        continueButton.setOnClickListener(v -> finish());
    }

    /**
     * Phương thức tải và hiển thị ảnh từ URL vào ImageView.
     * Sử dụng Glide để tải ảnh với placeholder mặc định.
     *
     * @param target ImageView đích để hiển thị ảnh
     * @param photoUrl URL của ảnh (có thể null)
     */
    private void loadImage(ImageView target, @Nullable String photoUrl) {
        if (!TextUtils.isEmpty(photoUrl)) {
            Glide.with(target.getContext())
                    .load(photoUrl)
                    .placeholder(R.drawable.welcome_person_2)
                    .error(R.drawable.welcome_person_1)
                    .centerCrop()
                    .into(target);
        } else {
            target.setImageResource(R.drawable.welcome_person_1);
        }
    }
}

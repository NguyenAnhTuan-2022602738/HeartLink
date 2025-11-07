package vn.haui.heartlink.activities;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import vn.haui.heartlink.R;
import vn.haui.heartlink.utils.UserRepository;

public class GenderSelectionActivity extends AppCompatActivity {

    private LinearLayout maleOption;
    private LinearLayout femaleOption;
    private TextView maleText;
    private TextView femaleText;
    private ImageView maleIcon;
    private ImageView femaleIcon;

    private boolean isMaleSelected = true;

    /**
     * Phương thức khởi tạo activity chọn giới tính.
     * Thiết lập giao diện người dùng, bind các view và thiết lập
     * các click listener cho việc chọn giới tính và điều hướng.
     *
     * @param savedInstanceState Trạng thái đã lưu của activity (có thể null)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gender_selection);

        maleOption = findViewById(R.id.male_option);
        femaleOption = findViewById(R.id.female_option);
        maleText = findViewById(R.id.male_text);
        femaleText = findViewById(R.id.female_text);
        maleIcon = findViewById(R.id.male_icon);
        femaleIcon = findViewById(R.id.female_icon);
        Button continueButton = findViewById(R.id.continue_button_gender);

        updateSelectionUI();

        maleOption.setOnClickListener(v -> {
            if (!isMaleSelected) {
                isMaleSelected = true;
                updateSelectionUI();
            }
        });

        femaleOption.setOnClickListener(v -> {
            if (isMaleSelected) {
                isMaleSelected = false;
                updateSelectionUI();
            }
        });

        continueButton.setOnClickListener(v -> {
            saveGenderAndProceed();
        });

        findViewById(R.id.back_button_gender).setOnClickListener(v -> onBackPressed());
    }

    /**
     * Phương thức cập nhật giao diện người dùng dựa trên lựa chọn giới tính.
     * Thay đổi background, màu chữ và màu icon cho option được chọn
     * và option không được chọn.
     */
    private void updateSelectionUI() {
        if (isMaleSelected) {
            maleOption.setBackgroundResource(R.drawable.gender_option_selected);
            maleText.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            maleIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.white), PorterDuff.Mode.SRC_IN);

            femaleOption.setBackgroundResource(R.drawable.gender_option_unselected);
            femaleText.setTextColor(ContextCompat.getColor(this, R.color.textColorPrimary));
            femaleIcon.clearColorFilter();
        } else {
            maleOption.setBackgroundResource(R.drawable.gender_option_unselected);
            maleText.setTextColor(ContextCompat.getColor(this, R.color.textColorPrimary));
            maleIcon.clearColorFilter();

            femaleOption.setBackgroundResource(R.drawable.gender_option_selected);
            femaleText.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            femaleIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.white), PorterDuff.Mode.SRC_IN);
        }
    }

    /**
     * Phương thức lưu giới tính đã chọn vào Firebase và chuyển sang activity tiếp theo.
     * Kiểm tra người dùng hiện tại, cập nhật trường gender trong database,
     * và điều hướng đến ProfileInfoActivity nếu thành công.
     */
    private void saveGenderAndProceed() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedGender = isMaleSelected ? "male" : "female";
        UserRepository.getInstance().updateField(currentUser.getUid(), "gender", selectedGender, new UserRepository.OnCompleteListener() {
            @Override
            public void onSuccess() {
                // Chuyển sang ProfileInfoActivity để tiếp tục hoàn thiện hồ sơ
                Intent intent = new Intent(GenderSelectionActivity.this, ProfileInfoActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(GenderSelectionActivity.this, "Lỗi khi lưu giới tính: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
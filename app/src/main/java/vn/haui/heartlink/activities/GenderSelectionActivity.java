package vn.haui.heartlink.activities;

import android.content.Intent;
import android.graphics.LinearGradient;
import android.graphics.PorterDuff;
import android.graphics.Shader;
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

import java.util.Map;

import vn.haui.heartlink.R;
import vn.haui.heartlink.utils.UserRepository;

public class GenderSelectionActivity extends AppCompatActivity {

    private LinearLayout maleOption;
    private LinearLayout femaleOption;
    private TextView maleText;
    private TextView femaleText;
    private ImageView maleIcon;
    private ImageView femaleIcon;
    private Button continueButton;

    private boolean isMaleSelected = true;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gender_selection);

        isEditMode = getIntent().getBooleanExtra("IS_EDIT_MODE", false);

        TextView genderTitle = findViewById(R.id.gender_title);
        int startColor = ContextCompat.getColor(this, R.color.interest_gradient_start);
        int endColor = ContextCompat.getColor(this, R.color.interest_gradient_end);

        Shader shader = new LinearGradient(
                0, 0, genderTitle.getPaint().measureText(genderTitle.getText().toString()), genderTitle.getTextSize(),
                new int[]{startColor, endColor},
                null, Shader.TileMode.CLAMP);
        genderTitle.getPaint().setShader(shader);
        maleOption = findViewById(R.id.male_option);
        femaleOption = findViewById(R.id.female_option);
        maleText = findViewById(R.id.male_text);
        femaleText = findViewById(R.id.female_text);
        maleIcon = findViewById(R.id.male_icon);
        femaleIcon = findViewById(R.id.female_icon);
        continueButton = findViewById(R.id.continue_button_gender);

        if (isEditMode) {
            continueButton.setText("Lưu");
            loadUserGender();
        } else {
            updateSelectionUI();
        }

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

    private void updateSelectionUI() {
        if (isMaleSelected) {
            maleOption.setBackgroundResource(R.drawable.gender_option_selected);
            maleText.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            maleIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.white), PorterDuff.Mode.SRC_IN);

            femaleOption.setBackgroundResource(R.drawable.gender_option_unselected);
            femaleText.setTextColor(ContextCompat.getColor(this, R.color.white));
            femaleIcon.clearColorFilter();
        } else {
            maleOption.setBackgroundResource(R.drawable.gender_option_unselected);
            maleText.setTextColor(ContextCompat.getColor(this, R.color.white));
            maleIcon.clearColorFilter();

            femaleOption.setBackgroundResource(R.drawable.gender_option_selected);
            femaleText.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            femaleIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.white), PorterDuff.Mode.SRC_IN);
        }
    }

    private void loadUserGender() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            UserRepository.getInstance().getUserData(currentUser.getUid()).addOnSuccessListener(dataSnapshot -> {
                if (dataSnapshot.exists()) {
                    Map<String, Object> data = (Map<String, Object>) dataSnapshot.getValue();
                    String gender = (String) data.get("gender");
                    if (gender != null) {
                        isMaleSelected = gender.equals("male");
                        updateSelectionUI();
                    }
                }
            });
        }
    }

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
                if (isEditMode) {
                    finish();
                } else {
                    Intent intent = new Intent(GenderSelectionActivity.this, ProfileInfoActivity.class);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(GenderSelectionActivity.this, "Lỗi khi lưu giới tính: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

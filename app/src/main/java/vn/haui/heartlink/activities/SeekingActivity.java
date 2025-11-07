package vn.haui.heartlink.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

import vn.haui.heartlink.R;
import vn.haui.heartlink.utils.UserRepository;

public class SeekingActivity extends AppCompatActivity {

    private RadioGroup seekingRadioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seeking);

        seekingRadioGroup = findViewById(R.id.seeking_radio_group);
        Button continueButton = findViewById(R.id.continue_button_seeking);
        TextView skipButton = findViewById(R.id.skip_button_seeking);

        continueButton.setOnClickListener(v -> saveSeekingPreferenceAndContinue());

        skipButton.setOnClickListener(v -> {
            navigateToInterests();
        });

        findViewById(R.id.back_button_seeking).setOnClickListener(v -> onBackPressed());
    }

    /**
     * Phương thức lưu sở thích tìm kiếm và chuyển sang activity tiếp theo.
     * Validate lựa chọn, lưu vào Firebase và chuyển đến InterestsActivity.
     */
    private void saveSeekingPreferenceAndContinue() {
        int selectedId = seekingRadioGroup.getCheckedRadioButtonId();
        String seekingType = "";

        if (selectedId == R.id.radio_friend) {
            seekingType = "friend";
        } else if (selectedId == R.id.radio_chat) {
            seekingType = "chat";
        } else if (selectedId == R.id.radio_no_strings) {
            seekingType = "no_strings";
        } else if (selectedId == R.id.radio_later) {
            seekingType = "later";
        }

        if (seekingType.isEmpty()) {
            Toast.makeText(this, getString(R.string.seeking_select_option_warning), Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.error_user_not_found), Toast.LENGTH_SHORT).show();
            return;
        }

        // Lưu sở thích tìm kiếm để sử dụng cho gợi ý
        Map<String, Object> updates = new HashMap<>();
        updates.put("seekingType", seekingType);
        updates.put("seekingGender", "both"); // Mặc định tìm cả nam và nữ
        updates.put("seekingAgeMin", 18); // Mặc định 18+
        updates.put("seekingAgeMax", 50); // Mặc định đến 50

        UserRepository.getInstance().updateUser(currentUser.getUid(), updates, new UserRepository.OnCompleteListener() {
            @Override
            public void onSuccess() {
                navigateToInterests();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(SeekingActivity.this, getString(R.string.seeking_save_error, e.getMessage()), Toast.LENGTH_SHORT).show();
                navigateToInterests();
            }
        });
    }

    /**
     * Phương thức chuyển hướng đến InterestsActivity.
     * Kết thúc activity hiện tại sau khi start activity mới.
     */
    private void navigateToInterests() {
        Intent intent = new Intent(SeekingActivity.this, InterestsActivity.class);
        startActivity(intent);
        finish();
    }
}
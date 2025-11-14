package vn.haui.heartlink.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
    private Button continueButton;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seeking);

        isEditMode = getIntent().getBooleanExtra("IS_EDIT_MODE", false);

        View header = findViewById(R.id.header);
        ImageView backButton = header.findViewById(R.id.back_button);
        TextView skipButton = header.findViewById(R.id.skip_button);
        ProgressBar progressBar = header.findViewById(R.id.progress_bar);

        seekingRadioGroup = findViewById(R.id.seeking_radio_group);
        continueButton = findViewById(R.id.continue_button_seeking);

        if (isEditMode) {
            continueButton.setText("Lưu");
            skipButton.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            loadUserSeekingPreference();
        } else {
            progressBar.setProgress(42);
        }

        continueButton.setOnClickListener(v -> saveSeekingPreferenceAndContinue());

        skipButton.setOnClickListener(v -> {
            if (!isEditMode) {
                navigateToInterests();
            }
        });

        backButton.setOnClickListener(v -> onBackPressed());
    }

    private void loadUserSeekingPreference() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            UserRepository.getInstance().getUserData(currentUser.getUid()).addOnSuccessListener(dataSnapshot -> {
                if (dataSnapshot.exists()) {
                    Map<String, Object> data = (Map<String, Object>) dataSnapshot.getValue();
                    String seekingType = (String) data.get("seekingType");
                    if (seekingType != null) {
                        if (seekingType.equals("friend")) {
                            seekingRadioGroup.check(R.id.radio_friend);
                        } else if (seekingType.equals("chat")) {
                            seekingRadioGroup.check(R.id.radio_chat);
                        } else if (seekingType.equals("no_strings")) {
                            seekingRadioGroup.check(R.id.radio_no_strings);
                        } else if (seekingType.equals("later")) {
                            seekingRadioGroup.check(R.id.radio_later);
                        }
                    }
                }
            });
        }
    }

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

        Map<String, Object> updates = new HashMap<>();
        updates.put("seekingType", seekingType);

        UserRepository.getInstance().updateUser(currentUser.getUid(), updates, new UserRepository.OnCompleteListener() {
            @Override
            public void onSuccess() {
                if (isEditMode) {
                    finish();
                } else {
                    navigateToInterests();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(SeekingActivity.this, getString(R.string.seeking_save_error, e.getMessage()), Toast.LENGTH_SHORT).show();
                if (!isEditMode) {
                    navigateToInterests();
                }
            }
        });
    }

    private void navigateToInterests() {
        Intent intent = new Intent(SeekingActivity.this, InterestsActivity.class);
        startActivity(intent);
        finish();
    }
}

package vn.haui.heartlink.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vn.haui.heartlink.BuildConfig;
import vn.haui.heartlink.R;
import vn.haui.heartlink.utils.UserRepository;

public class ProfileInfoActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView avatarImage;
    private Uri imageUri;
    private EditText editTextLastName, editTextFirstName;
    private TextView birthdayButton;
    private Button confirmButton;
    private ProgressBar progressBar;

    private int selectedYear, selectedMonth, selectedDay;
    private boolean cloudinaryConfigured;
    private static final String TAG = "ProfileInfoActivity";
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_info);

        isEditMode = getIntent().getBooleanExtra("IS_EDIT_MODE", false);

        cloudinaryConfigured = ensureCloudinaryInitialized();

        avatarImage = findViewById(R.id.avatar_image);
        ImageView addAvatarButton = findViewById(R.id.add_avatar_button);
        editTextLastName = findViewById(R.id.edit_text_lastname);
        editTextFirstName = findViewById(R.id.edit_text_firstname);
        birthdayButton = findViewById(R.id.birthday_button);
        confirmButton = findViewById(R.id.confirm_button);
        progressBar = findViewById(R.id.profile_progress_bar);
        ImageView backButton = findViewById(R.id.back_button_profile);

        final Calendar c = Calendar.getInstance();
        selectedYear = c.get(Calendar.YEAR);
        selectedMonth = c.get(Calendar.MONTH);
        selectedDay = c.get(Calendar.DAY_OF_MONTH);

        if (isEditMode) {
            confirmButton.setText("Lưu");
        }

        backButton.setOnClickListener(v -> onBackPressed());
        addAvatarButton.setOnClickListener(v -> openFileChooser());
        birthdayButton.setOnClickListener(v -> showDatePickerDialog());
        confirmButton.setOnClickListener(v -> saveProfile());

        loadUserData();
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            Glide.with(this).load(imageUri).into(avatarImage);
        }
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
                (view, year, month, dayOfMonth) -> {
                    selectedYear = year;
                    selectedMonth = month;
                    selectedDay = dayOfMonth;
                    String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
                    birthdayButton.setText(selectedDate);
                },
                selectedYear, selectedMonth, selectedDay);

        datePickerDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        datePickerDialog.show();
    }


    private void loadUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            UserRepository.getInstance().getUserData(user.getUid()).addOnSuccessListener(dataSnapshot -> {
                if (dataSnapshot.exists()) {
                    Map<String, Object> data = (Map<String, Object>) dataSnapshot.getValue();
                    String name = (String) data.get("name");
                    if (name != null) {
                        String[] nameParts = name.split(" ");
                        if (nameParts.length > 1) {
                            editTextFirstName.setText(nameParts[nameParts.length - 1]);
                            StringBuilder lastName = new StringBuilder();
                            for (int i = 0; i < nameParts.length - 1; i++) {
                                lastName.append(nameParts[i]).append(" ");
                            }
                            editTextLastName.setText(lastName.toString().trim());
                        } else {
                            editTextFirstName.setText(name);
                        }
                    }
                    String dateOfBirth = (String) data.get("dateOfBirth");
                    if (dateOfBirth != null) {
                        birthdayButton.setText(dateOfBirth);
                    }
                    List<String> photoUrls = (List<String>) data.get("photoUrls");
                    if (photoUrls != null && !photoUrls.isEmpty()) {
                        Glide.with(this).load(photoUrls.get(0)).into(avatarImage);
                    }
                }
            });
        }
    }

    private void saveProfile() {
        String lastName = editTextLastName.getText().toString().trim();
        String firstName = editTextFirstName.getText().toString().trim();
        String birthday = birthdayButton.getText().toString();

        if (TextUtils.isEmpty(lastName) || TextUtils.isEmpty(firstName) || TextUtils.isEmpty(birthday)) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            UserRepository.getInstance().getUserData(userId).addOnSuccessListener(dataSnapshot -> {
                List<String> photoUrls = null;
                if(dataSnapshot.exists()) {
                    Map<String, Object> data = (Map<String, Object>) dataSnapshot.getValue();
                    photoUrls = (List<String>) data.get("photoUrls");
                }

                if ((photoUrls == null || photoUrls.isEmpty()) && imageUri == null) {
                    Toast.makeText(this, "Vui lòng chọn ảnh đại diện", Toast.LENGTH_SHORT).show();
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);
                if (imageUri != null) {
                    if (!cloudinaryConfigured) {
                        Toast.makeText(this, "Cloudinary not configured", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        return;
                    }
                    MediaManager.get().upload(imageUri)
                            .callback(new UploadCallback() {
                                @Override
                                public void onStart(String requestId) {}

                                @Override
                                public void onProgress(String requestId, long bytes, long totalBytes) {}

                                @Override
                                public void onSuccess(String requestId, Map resultData) {
                                    String imageUrl = (String) resultData.get("secure_url");
                                    updateProfile(userId, firstName, lastName, birthday, imageUrl);
                                }

                                @Override
                                public void onError(String requestId, ErrorInfo error) {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(ProfileInfoActivity.this, error.getDescription(), Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onReschedule(String requestId, ErrorInfo error) {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(ProfileInfoActivity.this, error.getDescription(), Toast.LENGTH_SHORT).show();
                                }
                            })
                            .dispatch();
                } else {
                    updateProfile(userId, firstName, lastName, birthday, null);
                }
            });
        }
    }

    private void updateProfile(String userId, String firstName, String lastName, String birthday, @Nullable String imageUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", firstName + " " + lastName);
        updates.put("dateOfBirth", birthday);

        UserRepository.getInstance().getUserData(userId).addOnSuccessListener(dataSnapshot -> {
            Map<String, Object> data = (Map<String, Object>) dataSnapshot.getValue();
            List<String> photoUrls = data != null ? (List<String>) data.get("photoUrls") : null;
            if (photoUrls == null) {
                photoUrls = new ArrayList<>();
            }

            if (imageUrl != null) {
                if (photoUrls.isEmpty()) {
                    photoUrls.add(imageUrl);
                } else {
                    photoUrls.set(0, imageUrl);
                }
            }
            updates.put("photoUrls", photoUrls);

            UserRepository.getInstance().updateUser(userId, updates, new UserRepository.OnCompleteListener() {
                @Override
                public void onSuccess() {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ProfileInfoActivity.this, "Hồ sơ đã được lưu thành công", Toast.LENGTH_SHORT).show();
                    if (isEditMode) {
                        finish();
                    } else {
                        startActivity(new Intent(ProfileInfoActivity.this, SeekingActivity.class));
                        finish();
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ProfileInfoActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private boolean ensureCloudinaryInitialized() {
        try {
            MediaManager.get();
            return true;
        } catch (IllegalStateException notInitialized) {
            Map<String, String> config = loadCloudinaryConfig();
            if (config.isEmpty()) {
                Log.e(TAG, "Cloudinary configuration missing. Provide credentials via BuildConfig fields.");
                return false;
            }
            MediaManager.init(getApplicationContext(), config);
            return true;
        }
    }

    private Map<String, String> loadCloudinaryConfig() {
        Map<String, String> config = new HashMap<>();
        if (!TextUtils.isEmpty(BuildConfig.CLOUDINARY_CLOUD_NAME) && !"CHANGE_ME".equals(BuildConfig.CLOUDINARY_CLOUD_NAME)) {
            config.put("cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME);
        }
        if (!TextUtils.isEmpty(BuildConfig.CLOUDINARY_API_KEY) && !"CHANGE_ME".equals(BuildConfig.CLOUDINARY_API_KEY)) {
            config.put("api_key", BuildConfig.CLOUDINARY_API_KEY);
        }
        if (!TextUtils.isEmpty(BuildConfig.CLOUDINARY_API_SECRET) && !"CHANGE_ME".equals(BuildConfig.CLOUDINARY_API_SECRET)) {
            config.put("api_secret", BuildConfig.CLOUDINARY_API_SECRET);
        }
        return config;
    }
}
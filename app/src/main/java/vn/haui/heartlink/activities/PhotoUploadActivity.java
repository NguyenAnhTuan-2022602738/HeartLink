package vn.haui.heartlink.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vn.haui.heartlink.BuildConfig;
import vn.haui.heartlink.R;
import vn.haui.heartlink.utils.UserRepository;

public class PhotoUploadActivity extends AppCompatActivity {

    private static final String TAG = "PhotoUploadActivity";

    private static final int MAX_PHOTO_SLOTS = 6;
    private final Uri[] selectedUris = new Uri[MAX_PHOTO_SLOTS];
    private final ImageView[] photoSlots = new ImageView[MAX_PHOTO_SLOTS];
    private final ImageView[] addButtons = new ImageView[MAX_PHOTO_SLOTS];

    private Button continueButton;

    private ActivityResultLauncher<String> pickImageLauncher;
    private int pendingSlotIndex = -1;
    private boolean cloudinaryConfigured;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_upload);

        isEditMode = getIntent().getBooleanExtra("IS_EDIT_MODE", false);

        cloudinaryConfigured = ensureCloudinaryInitialized();

        View header = findViewById(R.id.header);
        ImageView backButton = header.findViewById(R.id.back_button);
        TextView skipButton = header.findViewById(R.id.skip_button);
        ProgressBar progressBar = header.findViewById(R.id.progress_bar);

        continueButton = findViewById(R.id.continue_button_photo);

        if (isEditMode) {
            continueButton.setText("Lưu");
            skipButton.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
        } else {
            progressBar.setProgress(80);
        }

        backButton.setOnClickListener(v -> finish());
        skipButton.setOnClickListener(v -> {
            if (!isEditMode) {
                navigateToLocationPermission();
            }
        });
        continueButton.setOnClickListener(v -> handleContinue());

        initSlots();
        initImagePicker();
        loadInitialProfileImage();
        updateContinueButtonState();
    }

    private void initSlots() {
        int[] imageIds = {R.id.photo_image_0, R.id.photo_image_1, R.id.photo_image_2, R.id.photo_image_3, R.id.photo_image_4, R.id.photo_image_5};
        int[] buttonIds = {R.id.add_photo_button_0, R.id.add_photo_button_1, R.id.add_photo_button_2, R.id.add_photo_button_3, R.id.add_photo_button_4, R.id.add_photo_button_5};

        for (int i = 0; i < MAX_PHOTO_SLOTS; i++) {
            final int index = i;
            photoSlots[i] = findViewById(imageIds[i]);
            addButtons[i] = findViewById(buttonIds[i]);

            photoSlots[i].setOnClickListener(v -> openImagePicker(index));
            addButtons[i].setOnClickListener(v -> openImagePicker(index));
        }
    }

    private void initImagePicker() {
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null && pendingSlotIndex != -1) {
                selectedUris[pendingSlotIndex] = uri;
                Glide.with(this).load(uri).into(photoSlots[pendingSlotIndex]);
                addButtons[pendingSlotIndex].setVisibility(View.GONE);
                photoSlots[pendingSlotIndex].setBackground(null);
                updateContinueButtonState();
            }
            pendingSlotIndex = -1;
        });
    }

    private void openImagePicker(int slotIndex) {
        pendingSlotIndex = slotIndex;
        pickImageLauncher.launch("image/*");
    }

    private void loadInitialProfileImage() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            UserRepository.getInstance().getUserData(user.getUid()).addOnSuccessListener(dataSnapshot -> {
                if (dataSnapshot.exists()) {
                    Map<String, Object> data = (Map<String, Object>) dataSnapshot.getValue();
                    List<String> photoUrls = (List<String>) data.get("photoUrls");
                    if (photoUrls != null && !photoUrls.isEmpty()) {
                        for (int i = 0; i < photoUrls.size() && i < MAX_PHOTO_SLOTS; i++) {
                            String url = photoUrls.get(i);
                            if (url != null && !url.isEmpty()) {
                                Glide.with(this).load(url).into(photoSlots[i]);
                                addButtons[i].setVisibility(View.GONE);
                                photoSlots[i].setBackground(null);
                            }
                        }
                        updateContinueButtonState();
                    }
                }
            });
        }
    }

    private void handleContinue() {
        List<Uri> photoUris = new ArrayList<>();
        for (Uri uri : selectedUris) {
            if (uri != null) {
                photoUris.add(uri);
            }
        }

        if (photoUris.isEmpty() && !isEditMode) {
            navigateToLocationPermission();
            return;
        }
        
        if (photoUris.isEmpty() && isEditMode) {
            finish();
            return;
        }

        uploadPhotosAndComplete(photoUris);
    }

    private void updateContinueButtonState() {
        boolean hasPhoto = false;
        for (Uri uri : selectedUris) {
            if (uri != null) {
                hasPhoto = true;
                break;
            }
        }

        if (!hasPhoto && isEditMode) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                UserRepository.getInstance().getUserData(user.getUid()).addOnSuccessListener(dataSnapshot -> {
                    Map<String, Object> data = (Map<String, Object>) dataSnapshot.getValue();
                    if (data != null && data.containsKey("photoUrls")) {
                        List<String> photoUrls = (List<String>) data.get("photoUrls");
                        if (photoUrls != null && !photoUrls.isEmpty()) {
                            continueButton.setEnabled(true);
                            continueButton.setAlpha(1f);
                        }
                    } else {
                        continueButton.setEnabled(false);
                        continueButton.setAlpha(0.5f);
                    }
                });
            }
        } else {
            continueButton.setEnabled(hasPhoto);
            continueButton.setAlpha(hasPhoto ? 1f : 0.5f);
        }
    }

    private void completeOnboarding(List<String> photoStrings) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            if (!isEditMode) {
                navigateToLocationPermission();
            }
            return;
        }

        continueButton.setEnabled(false);

        UserRepository.getInstance().getUserData(currentUser.getUid()).addOnSuccessListener(dataSnapshot -> {
            Map<String, Object> data = (Map<String, Object>) dataSnapshot.getValue();
            List<String> existingPhotoUrls = data != null && data.containsKey("photoUrls") ? (List<String>) data.get("photoUrls") : new ArrayList<>();
            
            while (existingPhotoUrls.size() < MAX_PHOTO_SLOTS) {
                existingPhotoUrls.add(null);
            }

            int uploadedIndex = 0;
            for (int i = 0; i < selectedUris.length; i++) {
                if (selectedUris[i] != null) {
                    if (uploadedIndex < photoStrings.size()) {
                        existingPhotoUrls.set(i, photoStrings.get(uploadedIndex++));
                    }
                }
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("photoUrls", existingPhotoUrls);
            updates.put("profileComplete", true);

            UserRepository.getInstance().updateUser(currentUser.getUid(), updates, new UserRepository.OnCompleteListener() {
                @Override
                public void onSuccess() {
                    if (isEditMode) {
                        finish();
                    } else {
                        navigateToLocationPermission();
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(PhotoUploadActivity.this, "Lỗi khi lưu ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (!isEditMode) {
                        navigateToLocationPermission();
                    }
                }
            });
        });
    }

    private void navigateToLocationPermission() {
        Intent intent = new Intent(this, LocationPermissionActivity.class);
        startActivity(intent);
        finish();
    }

    private void uploadPhotosAndComplete(@NonNull List<Uri> photoUris) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Không tìm thấy người dùng", Toast.LENGTH_SHORT).show();
            if (!isEditMode) {
                navigateToLocationPermission();
            }
            return;
        }

        if (!cloudinaryConfigured) {
            Toast.makeText(this, "Cloudinary not configured", Toast.LENGTH_SHORT).show();
            return;
        }

        continueButton.setEnabled(false);
        continueButton.setAlpha(0.5f);

        final List<String> downloadUrls = new ArrayList<>();
        final int totalPhotos = photoUris.size();
        final int[] successCount = {0};

        for(Uri uri : photoUris) {
            String publicId = "users/" + currentUser.getUid() + "/photos/photo_" + System.currentTimeMillis();
            MediaManager.get().upload(uri)
                    .option("public_id", publicId)
                    .option("overwrite", true)
                    .option("resource_type", "image")
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {}

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {}

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String url = (String) resultData.get("secure_url");
                            downloadUrls.add(url);
                            successCount[0]++;
                            if(successCount[0] == totalPhotos) {
                                completeOnboarding(downloadUrls);
                            }
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            handleUploadFailure(new Exception(error.getDescription()));
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                            handleUploadFailure(new Exception(error.getDescription()));
                        }
                    }).dispatch();
        }
    }


    private void handleUploadFailure(@NonNull Exception exception) {
        Log.e(TAG, "Failed to upload profile photo", exception);
        Toast.makeText(this, "Lỗi khi tải ảnh lên", Toast.LENGTH_SHORT).show();
        continueButton.setEnabled(true);
        continueButton.setAlpha(1f);
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

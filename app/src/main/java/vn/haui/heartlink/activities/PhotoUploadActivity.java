package vn.haui.heartlink.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
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

/**
 * Screen that allows users to add up to six profile photos before entering the main experience.
 */
public class PhotoUploadActivity extends AppCompatActivity {

    private static final String TAG = "PhotoUploadActivity";

    private static final int MAX_PHOTO_SLOTS = 6;
    private static final String STATE_PHOTO_URIS = "state_photo_uris";

    private final Uri[] selectedUris = new Uri[MAX_PHOTO_SLOTS];

    private FrameLayout[] slotContainers;
    private ImageView[] slotImages;
    private ImageView[] slotAddIcons;
    private Button continueButton;
    private boolean cloudinaryConfigured;

    private ActivityResultLauncher<String> pickImageLauncher;
    private int pendingSlotIndex = -1;

    /**
     * Phương thức khởi tạo activity upload ảnh.
     * Thiết lập giao diện và khởi tạo các thành phần cần thiết.
     *
     * @param savedInstanceState Trạng thái đã lưu của activity (có thể null)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_upload);

        cloudinaryConfigured = ensureCloudinaryInitialized();

        continueButton = findViewById(R.id.photo_continue_button);
        TextView skipButton = findViewById(R.id.photo_skip_button);

        View backContainer = findViewById(R.id.photo_back_button_container);
        View backIcon = findViewById(R.id.photo_back_button);

        backContainer.setOnClickListener(v -> finish());
        backIcon.setOnClickListener(v -> finish());

        skipButton.setOnClickListener(v -> completeOnboarding(Collections.emptyList()));
        continueButton.setOnClickListener(v -> handleContinue());

        initSlots();
        initImagePicker();

        if (savedInstanceState != null) {
            ArrayList<String> storedUris = savedInstanceState.getStringArrayList(STATE_PHOTO_URIS);
            if (storedUris != null) {
                for (int i = 0; i < Math.min(storedUris.size(), MAX_PHOTO_SLOTS); i++) {
                    String value = storedUris.get(i);
                    if (value != null && !value.isEmpty()) {
                        selectedUris[i] = Uri.parse(value);
                        updateSlotUi(i);
                    }
                }
            }
        }

        updateContinueButtonState();
    }

    /**
     * Phương thức khởi tạo các slot upload ảnh.
     */
    private void initSlots() {
        int[] containerIds = {
                R.id.photo_slot_0,
                R.id.photo_slot_1,
                R.id.photo_slot_2,
                R.id.photo_slot_3,
                R.id.photo_slot_4,
                R.id.photo_slot_5
        };
        int[] imageIds = {
                R.id.photo_image_0,
                R.id.photo_image_1,
                R.id.photo_image_2,
                R.id.photo_image_3,
                R.id.photo_image_4,
                R.id.photo_image_5
        };
        int[] addIconIds = {
                R.id.photo_add_icon_0,
                R.id.photo_add_icon_1,
                R.id.photo_add_icon_2,
                R.id.photo_add_icon_3,
                R.id.photo_add_icon_4,
                R.id.photo_add_icon_5
        };

        slotContainers = new FrameLayout[MAX_PHOTO_SLOTS];
        slotImages = new ImageView[MAX_PHOTO_SLOTS];
        slotAddIcons = new ImageView[MAX_PHOTO_SLOTS];

        for (int i = 0; i < MAX_PHOTO_SLOTS; i++) {
            final int index = i;
            slotContainers[i] = findViewById(containerIds[i]);
            slotImages[i] = findViewById(imageIds[i]);
            slotAddIcons[i] = findViewById(addIconIds[i]);

            View.OnClickListener openPicker = v -> openImagePicker(index);
            slotContainers[i].setOnClickListener(openPicker);
            slotAddIcons[i].setOnClickListener(openPicker);
            slotImages[i].setOnClickListener(openPicker);

            updateSlotUi(i);
        }
    }

    /**
     * Phương thức khởi tạo image picker launcher.
     */
    private void initImagePicker() {
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null && pendingSlotIndex >= 0 && pendingSlotIndex < MAX_PHOTO_SLOTS) {
                selectedUris[pendingSlotIndex] = uri;
                updateSlotUi(pendingSlotIndex);
                pendingSlotIndex = -1;
                updateContinueButtonState();
            } else {
                pendingSlotIndex = -1;
                updateContinueButtonState();
            }
        });
    }

    /**
     * Phương thức mở image picker cho slot chỉ định.
     *
     * @param slotIndex Chỉ số của slot cần chọn ảnh
     */
    private void openImagePicker(int slotIndex) {
        pendingSlotIndex = slotIndex;
        pickImageLauncher.launch("image/*");
    }

    /**
     * Phương thức xử lý khi người dùng nhấn continue.
     * Thu thập các ảnh đã chọn và bắt đầu upload.
     */
    private void handleContinue() {
        List<Uri> photoUris = new ArrayList<>();
        for (Uri uri : selectedUris) {
            if (uri != null) {
                photoUris.add(uri);
            }
        }

        if (photoUris.isEmpty()) {
            Toast.makeText(this, R.string.photo_upload_min_message, Toast.LENGTH_SHORT).show();
            return;
        }

        uploadPhotosAndComplete(photoUris);
    }

    /**
     * Phương thức cập nhật giao diện của slot chỉ định.
     *
     * @param index Chỉ số của slot cần cập nhật
     */
    private void updateSlotUi(int index) {
        Uri uri = selectedUris[index];
        if (uri != null) {
            slotContainers[index].setBackgroundResource(R.drawable.bg_photo_slot_filled);
            slotImages[index].setImageURI(uri);
            slotImages[index].setVisibility(View.VISIBLE);
            slotAddIcons[index].setVisibility(View.GONE);
        } else {
            slotContainers[index].setBackgroundResource(R.drawable.bg_photo_slot_placeholder);
            slotImages[index].setImageDrawable(null);
            slotImages[index].setVisibility(View.INVISIBLE);
            slotAddIcons[index].setVisibility(View.VISIBLE);
        }
    }

    /**
     * Phương thức cập nhật trạng thái của button continue.
     */
    private void updateContinueButtonState() {
        boolean hasPhoto = false;
        for (Uri uri : selectedUris) {
            if (uri != null) {
                hasPhoto = true;
                break;
            }
        }
        continueButton.setEnabled(hasPhoto);
        continueButton.setAlpha(hasPhoto ? 1f : 0.5f);
    }

    /**
     * Phương thức hoàn thành quá trình onboarding.
     * Lưu danh sách URL ảnh vào Firebase.
     *
     * @param photoStrings Danh sách URL của các ảnh đã upload
     */
    private void completeOnboarding(List<String> photoStrings) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            navigateToLocationPermission();
            return;
        }

        continueButton.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("photoUrls", photoStrings);
        updates.put("profileComplete", true);

        UserRepository.getInstance().updateUser(currentUser.getUid(), updates, new UserRepository.OnCompleteListener() {
            @Override
            public void onSuccess() {
                navigateToLocationPermission();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(PhotoUploadActivity.this,
                        getString(R.string.photo_upload_save_error, e.getMessage()),
                        Toast.LENGTH_SHORT).show();
                navigateToLocationPermission();
            }
        });
    }

    /**
     * Phương thức điều hướng đến LocationPermissionActivity.
     */
    private void navigateToLocationPermission() {
        Intent intent = new Intent(this, LocationPermissionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Phương thức upload các ảnh và hoàn thành quá trình.
     *
     * @param photoUris Danh sách URI của các ảnh cần upload
     */
    private void uploadPhotosAndComplete(@NonNull List<Uri> photoUris) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, R.string.error_user_not_found, Toast.LENGTH_SHORT).show();
            navigateToLocationPermission();
            return;
        }

        if (!cloudinaryConfigured) {
            Toast.makeText(this, R.string.photo_upload_missing_cloudinary, Toast.LENGTH_SHORT).show();
            updateContinueButtonState();
            return;
        }

        continueButton.setEnabled(false);
        continueButton.setAlpha(0.5f);
        updateUploadProgress(0, photoUris.size());

        List<String> downloadUrls = new ArrayList<>(photoUris.size());
        uploadNextPhoto(currentUser.getUid(), photoUris, downloadUrls, 0, photoUris.size());
    }

    /**
     * Phương thức upload ảnh tiếp theo trong danh sách.
     *
     * @param userId ID của người dùng
     * @param sourceUris Danh sách URI nguồn của các ảnh
     * @param downloadUrls Danh sách URL đã download
     * @param index Chỉ số của ảnh hiện tại đang upload
     * @param totalCount Tổng số ảnh cần upload
     */
    private void uploadNextPhoto(@NonNull String userId,
                                 @NonNull List<Uri> sourceUris,
                                 @NonNull List<String> downloadUrls,
                                 int index,
                                 int totalCount) {
        if (index >= sourceUris.size()) {
            completeOnboarding(downloadUrls);
            return;
        }

        Uri currentUri = sourceUris.get(index);
        String extension = resolveFileExtension(currentUri);
        String publicId = "users/" + userId + "/photos/photo_" + System.currentTimeMillis() + "_" + index;

        updateUploadProgress(index + 1, totalCount);

        MediaManager.get().upload(currentUri)
                .option("public_id", publicId)
                .option("overwrite", true)
                .option("resource_type", "image")
                .option("format", extension)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        // no-op
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        // could surface per-file progress if needed
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        Object url = resultData != null ? resultData.get("secure_url") : null;
                        if (url instanceof String) {
                            downloadUrls.add((String) url);
                        }
                        uploadNextPhoto(userId, sourceUris, downloadUrls, index + 1, totalCount);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        String message = error != null ? error.getDescription() : "Cloudinary upload error";
                        handleUploadFailure(new IllegalStateException(message));
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        String message = error != null ? error.getDescription() : "Cloudinary upload reschedule";
                        handleUploadFailure(new IllegalStateException(message));
                    }
                })
                .dispatch();
    }

    /**
     * Phương thức xử lý khi upload thất bại.
     *
     * @param exception Exception xảy ra trong quá trình upload
     */
    private void handleUploadFailure(@NonNull Exception exception) {
        Log.e(TAG, "Failed to upload profile photo", exception);
        Toast.makeText(this, R.string.photo_upload_uploading_error, Toast.LENGTH_SHORT).show();
        continueButton.setText(R.string.continue_label);
        updateContinueButtonState();
    }

    /**
     * Phương thức cập nhật tiến trình upload.
     *
     * @param completed Số ảnh đã upload thành công
     * @param total Tổng số ảnh cần upload
     */
    private void updateUploadProgress(int completed, int total) {
        if (total <= 0) {
            continueButton.setText(R.string.photo_upload_uploading);
            return;
        }

        int safeCompleted = Math.max(0, Math.min(completed, total));
        continueButton.setText(getString(R.string.photo_upload_uploading_progress, safeCompleted, total));
    }

    private String resolveFileExtension(@NonNull Uri uri) {
        String extension = null;
        String mimeType = getContentResolver().getType(uri);
        if (mimeType != null) {
            extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        }

        if (extension == null) {
            String lastPath = uri.getLastPathSegment();
            if (lastPath != null) {
                int dotIndex = lastPath.lastIndexOf('.');
                if (dotIndex != -1 && dotIndex < lastPath.length() - 1) {
                    extension = lastPath.substring(dotIndex + 1);
                }
            }
        }

        if (extension == null) {
            extension = "jpg";
        }
        return extension;
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

    /**
     * Phương thức lưu trạng thái instance khi activity bị destroy.
     *
     * @param outState Bundle để lưu trạng thái
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        ArrayList<String> storedUris = new ArrayList<>(MAX_PHOTO_SLOTS);
        for (Uri uri : selectedUris) {
            storedUris.add(uri != null ? uri.toString() : "");
        }
        outState.putStringArrayList(STATE_PHOTO_URIS, storedUris);
    }
}

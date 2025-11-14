package vn.haui.heartlink.activities;

import android.content.Intent;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import vn.haui.heartlink.R;
import vn.haui.heartlink.utils.FirebaseHelper;
import vn.haui.heartlink.utils.NavigationHelper;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText editTextEmail, editTextPassword;
    private TextInputLayout textInputLayoutEmail, textInputLayoutPassword;
    private Button buttonLogin, buttonGoogleLogin;
    private ProgressBar progressBar;
    private FirebaseHelper firebaseHelper;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    /**
     * Khởi tạo activity đăng nhập. Kiểm tra nếu user đã đăng nhập thì chuyển đến MainActivity,
     * ngược lại thiết lập UI và listeners cho các nút đăng nhập.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firebaseHelper = FirebaseHelper.getInstance();

        FirebaseUser currentUser = firebaseHelper.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        configureGoogleSignIn();

        TextView loginTitle = findViewById(R.id.login_title);
        TextView signUpText = findViewById(R.id.sign_up_text);
        editTextEmail = findViewById(R.id.edit_text_email_login);
        editTextPassword = findViewById(R.id.edit_text_password_login);
        textInputLayoutEmail = findViewById(R.id.text_input_layout_email_login);
        textInputLayoutPassword = findViewById(R.id.text_input_layout_password_login);
        buttonLogin = findViewById(R.id.button_login);
        buttonGoogleLogin = findViewById(R.id.button_google_login);
        progressBar = findViewById(R.id.progress_bar_login);

        applyTitleGradient(loginTitle);

        signUpText.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
        buttonLogin.setOnClickListener(v -> loginUser());
        buttonGoogleLogin.setOnClickListener(v -> {
            setLoading(true);
            // Sign out from Google to force account picker
            googleSignInClient.signOut().addOnCompleteListener(this, task -> {
                googleSignInLauncher.launch(googleSignInClient.getSignInIntent());
            });
        });

        // Add TextWatchers to clear errors
        editTextEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textInputLayoutEmail.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        editTextPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textInputLayoutPassword.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Cấu hình đăng nhập Google bằng GoogleSignInClient và ActivityResultLauncher.
     */
    private void configureGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this, "Đăng nhập Google bị hủy.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null) {
                            firebaseAuthWithGoogle(account.getIdToken());
                        } else {
                            setLoading(false);
                            Toast.makeText(LoginActivity.this, "Không lấy được thông tin tài khoản Google.", Toast.LENGTH_LONG).show();
                        }
                    } catch (ApiException e) {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this, "Đăng nhập Google thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    /**
     * Áp dụng gradient màu cho tiêu đề đăng nhập để làm đẹp UI.
     * @param loginTitle TextView của tiêu đề.
     */
    private void applyTitleGradient(TextView loginTitle) {
        int color1 = ContextCompat.getColor(this, R.color.colorPrimary);
        int color2 = ContextCompat.getColor(this, R.color.colorAccent);

        Shader textShader = new LinearGradient(
                0,
                0,
                loginTitle.getPaint().measureText(loginTitle.getText().toString()),
                loginTitle.getTextSize(),
                new int[]{color1, color2},
                null,
                Shader.TileMode.CLAMP
        );

        loginTitle.getPaint().setShader(textShader);
    }

    /**
     * Xử lý đăng nhập bằng email và mật khẩu. Kiểm tra input, gọi FirebaseHelper để đăng nhập,
     * và điều hướng dựa trên kết quả.
     */
    private void loginUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        textInputLayoutEmail.setError(null);
        textInputLayoutPassword.setError(null);

        boolean isValid = true;

        if (TextUtils.isEmpty(email)) {
            textInputLayoutEmail.setError("Vui lòng nhập email");
            isValid = false;
        }

        if (TextUtils.isEmpty(password)) {
            textInputLayoutPassword.setError("Vui lòng nhập mật khẩu");
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        setLoading(true);
        firebaseHelper.loginUser(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        setLoading(false);
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Đăng nhập thành công.", Toast.LENGTH_SHORT).show();
                            // Check profile and navigate accordingly
                            // Don't call finish() here - NavigationHelper will handle clearing activities
                            NavigationHelper.checkProfileAndNavigate(LoginActivity.this, task.getResult().getUser());
                        } else {
                            textInputLayoutPassword.setError("Email hoặc mật khẩu không đúng.");
                        }
                    }
                });
    }

    /**
     * Xác thực với Firebase bằng Google ID Token. Đăng nhập user và điều hướng.
     * @param idToken ID Token từ Google Sign-In.
     */
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseHelper.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Đăng nhập Google thành công.", Toast.LENGTH_SHORT).show();
                        // Check profile and navigate accordingly
                        // Don't call finish() here - NavigationHelper will handle clearing activities
                        NavigationHelper.checkProfileAndNavigate(LoginActivity.this, task.getResult().getUser());
                    } else {
                        String message = task.getException() != null ? task.getException().getMessage() : "";
                        Toast.makeText(LoginActivity.this, "Đăng nhập Google thất bại: " + message, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Thiết lập trạng thái loading cho UI: hiển thị progress bar và disable các nút.
     * @param isLoading true để hiển thị loading, false để ẩn.
     */
    private void setLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (buttonLogin != null) {
            buttonLogin.setEnabled(!isLoading);
        }
        if (buttonGoogleLogin != null) {
            buttonGoogleLogin.setEnabled(!isLoading);
        }
    }
}
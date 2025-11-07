package vn.haui.heartlink.activities;

import android.content.Intent;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
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
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.GoogleAuthProvider;

import vn.haui.heartlink.R;
import vn.haui.heartlink.utils.FirebaseHelper;
import vn.haui.heartlink.utils.NavigationHelper;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText editTextEmail, editTextPassword, editTextConfirmPassword;
    private Button buttonRegister, buttonGoogle;
    private ProgressBar progressBar;
    private FirebaseHelper firebaseHelper;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    /**
     * Khởi tạo activity đăng ký. Thiết lập UI, listeners cho các nút và cấu hình đăng nhập Google.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        firebaseHelper = FirebaseHelper.getInstance();
        
        // Register Google Sign-In Activity Result Launcher
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        handleGoogleSignInResult(result);
                    }
                }
        );
        
        setContentView(R.layout.activity_register);
        
        // Configure Google Sign-In
        configureGoogleSignIn();

        // Find Views
        TextView registerTitle = findViewById(R.id.register_title);
        TextView loginLink = findViewById(R.id.login_text_on_register);
        editTextEmail = findViewById(R.id.edit_text_email);
        editTextPassword = findViewById(R.id.edit_text_password);
        editTextConfirmPassword = findViewById(R.id.edit_text_confirm_password);
        buttonRegister = findViewById(R.id.button_register);
        buttonGoogle = findViewById(R.id.button_google);
        progressBar = findViewById(R.id.progress_bar_register);

        // --- Create Gradient for Title ---
        int color1 = ContextCompat.getColor(this, R.color.colorPrimary);
        int color2 = ContextCompat.getColor(this, R.color.colorAccent);

        Shader textShader = new LinearGradient(0, 0, registerTitle.getPaint().measureText(registerTitle.getText().toString()), registerTitle.getTextSize(),
                new int[]{color1, color2},
                null, Shader.TileMode.CLAMP);

        registerTitle.getPaint().setShader(textShader);

        // --- Set OnClickListener for Login Link ---
        loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        // --- Set OnClickListener for Register Button ---
        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });
        
        // --- Set OnClickListener for Google Button ---
        buttonGoogle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUpWithGoogle();
            }
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
    }
    
    /**
     * Bắt đầu quá trình đăng ký bằng Google, hiển thị loading và khởi chạy Google Sign-In intent.
     */
    private void signUpWithGoogle() {
        progressBar.setVisibility(View.VISIBLE);
        buttonGoogle.setEnabled(false);
        buttonRegister.setEnabled(false);
        
        // Sign out from Google to force account picker
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }
    
    /**
     * Xử lý kết quả từ Google Sign-In intent, lấy account và xác thực với Firebase.
     * @param result Kết quả từ ActivityResultLauncher.
     */
    private void handleGoogleSignInResult(ActivityResult result) {
        if (result.getResultCode() == RESULT_OK) {
            Intent data = result.getData();
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d("RegisterActivity", "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w("RegisterActivity", "Google sign in failed", e);
                Toast.makeText(this, "Đăng ký với Google thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                buttonGoogle.setEnabled(true);
                buttonRegister.setEnabled(true);
            }
        } else {
            progressBar.setVisibility(View.GONE);
            buttonGoogle.setEnabled(true);
            buttonRegister.setEnabled(true);
        }
    }
    
    /**
     * Xác thực với Firebase bằng Google ID Token và điều hướng sau khi đăng ký thành công.
     * @param idToken ID Token từ Google Sign-In.
     */
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseHelper.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE);
                        buttonGoogle.setEnabled(true);
                        buttonRegister.setEnabled(true);
                        
                        if (task.isSuccessful()) {
                            AuthResult authResult = task.getResult();
                            boolean isNewUser = authResult.getAdditionalUserInfo() != null 
                                && authResult.getAdditionalUserInfo().isNewUser();
                            
                            if (isNewUser) {
                                // New user - proceed with registration
                                Log.d("RegisterActivity", "signInWithCredential:success - NEW USER");
                                Toast.makeText(RegisterActivity.this, "Đăng ký với Google thành công!", Toast.LENGTH_SHORT).show();
                                
                                // Check profile and navigate accordingly (will start onboarding for new user)
                                // Don't call finish() here - NavigationHelper will handle clearing activities
                                NavigationHelper.checkProfileAndNavigate(RegisterActivity.this, authResult.getUser());
                            } else {
                                // Existing user - should not register again
                                Log.d("RegisterActivity", "signInWithCredential:success - EXISTING USER");
                                Toast.makeText(RegisterActivity.this, "Tài khoản đã tồn tại. Vui lòng đăng nhập!", Toast.LENGTH_LONG).show();
                                
                                // Sign out and redirect to login
                                firebaseHelper.signOut();
                                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            }
                        } else {
                            Log.w("RegisterActivity", "signInWithCredential:failure", task.getException());
                            Toast.makeText(RegisterActivity.this, "Xác thực thất bại: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /**
     * Xử lý đăng ký tài khoản mới bằng email và mật khẩu. Kiểm tra input và gọi FirebaseHelper để tạo tài khoản.
     */
    private void registerUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Vui lòng nhập mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Vui lòng xác nhận mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseHelper.registerUser(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, create user profile and navigate to onboarding
                            Log.d("RegisterActivity", "createUserWithEmail:success");
                            Toast.makeText(RegisterActivity.this, "Đăng ký thành công.", Toast.LENGTH_SHORT).show();
                            // Check profile and navigate accordingly (will start onboarding for new user)
                            // Don't call finish() here - NavigationHelper will handle clearing activities
                            NavigationHelper.checkProfileAndNavigate(RegisterActivity.this, task.getResult().getUser());
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("RegisterActivity", "createUserWithEmail:failure", task.getException());
                            Toast.makeText(RegisterActivity.this, "Đăng ký thất bại: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}
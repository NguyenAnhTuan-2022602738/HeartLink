package vn.haui.heartlink.activities;

import android.content.Intent;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

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
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.GoogleAuthProvider;

import vn.haui.heartlink.R;
import vn.haui.heartlink.utils.DialogHelper;
import vn.haui.heartlink.utils.FirebaseHelper;
import vn.haui.heartlink.utils.NavigationHelper;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText editTextEmail, editTextPassword, editTextConfirmPassword;
    private TextInputLayout textInputLayoutEmail, textInputLayoutPassword, textInputLayoutConfirmPassword;
    private Button buttonRegister, buttonGoogle;
    private ProgressBar progressBar;
    private FirebaseHelper firebaseHelper;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firebaseHelper = FirebaseHelper.getInstance();

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                this::handleGoogleSignInResult
        );

        setContentView(R.layout.activity_register);

        configureGoogleSignIn();

        TextView registerTitle = findViewById(R.id.register_title);
        TextView loginLink = findViewById(R.id.login_text_on_register);
        editTextEmail = findViewById(R.id.edit_text_email);
        editTextPassword = findViewById(R.id.edit_text_password);
        editTextConfirmPassword = findViewById(R.id.edit_text_confirm_password);
        textInputLayoutEmail = findViewById(R.id.text_input_layout_email);
        textInputLayoutPassword = findViewById(R.id.text_input_layout_password);
        textInputLayoutConfirmPassword = findViewById(R.id.text_input_layout_confirm_password);
        buttonRegister = findViewById(R.id.button_register);
        buttonGoogle = findViewById(R.id.button_google);
        progressBar = findViewById(R.id.progress_bar_register);

        applyTitleGradient(registerTitle);

        loginLink.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        buttonRegister.setOnClickListener(v -> registerUser());

        buttonGoogle.setOnClickListener(v -> signUpWithGoogle());

        addTextWatchers();
    }

    private void configureGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void signUpWithGoogle() {
        setLoading(true);
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void handleGoogleSignInResult(ActivityResult result) {
        setLoading(false);
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                Log.w("RegisterActivity", "Google sign in failed", e);
                DialogHelper.showStatusDialog(this, "Đăng ký thất bại", "Không thể đăng ký với Google. Vui lòng thử lại.", false, null);
            }
        } else {
            DialogHelper.showStatusDialog(this, "Đăng ký bị hủy", "Bạn đã hủy quá trình đăng ký với Google.", false, null);
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        setLoading(true);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseHelper.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        AuthResult authResult = task.getResult();
                        boolean isNewUser = authResult.getAdditionalUserInfo() != null && authResult.getAdditionalUserInfo().isNewUser();

                        if (isNewUser) {
                            DialogHelper.showStatusDialog(this, "Đăng ký thành công!", "Chào mừng bạn đến với HeartLink!", true, () ->
                                    NavigationHelper.checkProfileAndNavigate(RegisterActivity.this, authResult.getUser())
                            );
                        } else {
                            firebaseHelper.signOut();
                            DialogHelper.showStatusDialog(this, "Tài khoản đã tồn tại", "Email này đã được sử dụng. Vui lòng đăng nhập.", false, () -> {
                                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            });
                        }
                    } else {
                        DialogHelper.showStatusDialog(this, "Xác thực thất bại", "Đã có lỗi xảy ra. Vui lòng thử lại.", false, null);
                    }
                });
    }

    private void registerUser() {
        if (!validateInput()) {
            return;
        }

        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        setLoading(true);
        firebaseHelper.registerUser(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        DialogHelper.showStatusDialog(this, "Đăng ký thành công!", "Chào mừng bạn đến với HeartLink!", true, () ->
                                NavigationHelper.checkProfileAndNavigate(RegisterActivity.this, task.getResult().getUser())
                        );
                    } else {
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            textInputLayoutEmail.setError("Email này đã được sử dụng");
                        } else {
                            DialogHelper.showStatusDialog(this, "Đăng ký thất bại", "Đã có lỗi xảy ra. Vui lòng thử lại.", false, null);
                        }
                    }
                });
    }

    private boolean validateInput() {
        textInputLayoutEmail.setError(null);
        textInputLayoutPassword.setError(null);
        textInputLayoutConfirmPassword.setError(null);

        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        boolean isValid = true;

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            textInputLayoutEmail.setError("Email không hợp lệ");
            isValid = false;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            textInputLayoutPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            isValid = false;
        }

        if (!password.equals(confirmPassword)) {
            textInputLayoutConfirmPassword.setError("Mật khẩu không khớp");
            isValid = false;
        }

        return isValid;
    }

    private void addTextWatchers() {
        editTextEmail.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { textInputLayoutEmail.setError(null); }
            @Override public void afterTextChanged(Editable s) {}
        });

        editTextPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { textInputLayoutPassword.setError(null); }
            @Override public void afterTextChanged(Editable s) {}
        });

        editTextConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { textInputLayoutConfirmPassword.setError(null); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void applyTitleGradient(TextView registerTitle) {
        int color1 = ContextCompat.getColor(this, R.color.colorPrimary);
        int color2 = ContextCompat.getColor(this, R.color.colorAccent);
        Shader textShader = new LinearGradient(0, 0, registerTitle.getPaint().measureText(registerTitle.getText().toString()), registerTitle.getTextSize(), new int[]{color1, color2}, null, Shader.TileMode.CLAMP);
        registerTitle.getPaint().setShader(textShader);
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        buttonRegister.setEnabled(!isLoading);
        buttonGoogle.setEnabled(!isLoading);
    }
}

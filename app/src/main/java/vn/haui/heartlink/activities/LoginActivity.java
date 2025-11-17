package vn.haui.heartlink.activities;

import android.content.Intent;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import vn.haui.heartlink.R;
import vn.haui.heartlink.utils.DialogHelper;
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
        TextView forgotPasswordText = findViewById(R.id.forgot_password_text);
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
            googleSignInClient.signOut().addOnCompleteListener(this, task -> {
                googleSignInLauncher.launch(googleSignInClient.getSignInIntent());
            });
        });

        forgotPasswordText.setOnClickListener(v -> showForgotPasswordDialog());

        addTextWatchers();
    }

    private void configureGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    setLoading(false);
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                        DialogHelper.showStatusDialog(this, "Đăng nhập bị hủy", "Bạn đã hủy quá trình đăng nhập với Google.", false, null);
                        return;
                    }

                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null) {
                            firebaseAuthWithGoogle(account.getIdToken());
                        } else {
                            DialogHelper.showStatusDialog(this, "Đăng nhập thất bại", "Không thể lấy thông tin tài khoản Google.", false, null);
                        }
                    } catch (ApiException e) {
                        DialogHelper.showStatusDialog(this, "Đăng nhập thất bại", "Đã có lỗi xảy ra khi đăng nhập với Google.", false, null);
                    }
                }
        );
    }

    private void loginUser() {
        if (!validateInput()) {
            return;
        }

        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        setLoading(true);
        firebaseHelper.loginUser(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        DialogHelper.showStatusDialog(this, "Đăng nhập thành công", "Chào mừng bạn trở lại!", true, () ->
                                NavigationHelper.checkProfileAndNavigate(LoginActivity.this, task.getResult().getUser())
                        );
                    } else {
                        if (task.getException() instanceof FirebaseAuthInvalidUserException || task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            textInputLayoutPassword.setError("Email hoặc mật khẩu không đúng.");
                        } else {
                            DialogHelper.showStatusDialog(this, "Đăng nhập thất bại", "Đã có lỗi xảy ra. Vui lòng thử lại.", false, null);
                        }
                    }
                });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        setLoading(true);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseHelper.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        DialogHelper.showStatusDialog(this, "Đăng nhập thành công", "Chào mừng bạn đến với HeartLink!", true, () ->
                                NavigationHelper.checkProfileAndNavigate(LoginActivity.this, task.getResult().getUser())
                        );
                    } else {
                        DialogHelper.showStatusDialog(this, "Đăng nhập thất bại", "Đã có lỗi xảy ra. Vui lòng thử lại.", false, null);
                    }
                });
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_forgot_password, null);
        builder.setView(dialogView);

        final TextInputEditText emailEditText = dialogView.findViewById(R.id.edit_text_email_forgot);
        final TextInputLayout emailInputLayout = dialogView.findViewById(R.id.text_input_layout_email_forgot);

        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { emailInputLayout.setError(null); }
            @Override public void afterTextChanged(Editable s) {}
        });

        builder.setTitle("Quên mật khẩu").setPositiveButton("Gửi", null).setNegativeButton("Hủy", (d, w) -> d.dismiss());
        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                String email = emailEditText.getText().toString().trim();
                if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    emailInputLayout.setError("Vui lòng nhập một email hợp lệ");
                    return;
                }
                sendPasswordResetEmail(email, dialog, emailInputLayout);
            });
        });

        dialog.show();
    }

    private void sendPasswordResetEmail(String email, AlertDialog dialog, TextInputLayout emailInputLayout) {
        setLoading(true);
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        DialogHelper.showStatusDialog(this, "Thành công", "Email đặt lại mật khẩu đã được gửi đến \n" + email, true, null);
                        dialog.dismiss();
                    } else {
                        if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                            emailInputLayout.setError("Email không tồn tại trong hệ thống.");
                        } else {
                            DialogHelper.showStatusDialog(this, "Gửi thất bại", "Không thể gửi email. Vui lòng thử lại.", false, null);
                        }
                    }
                });
    }

    private boolean validateInput() {
        textInputLayoutEmail.setError(null);
        textInputLayoutPassword.setError(null);

        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        boolean isValid = true;

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            textInputLayoutEmail.setError("Email không hợp lệ");
            isValid = false;
        }

        if (TextUtils.isEmpty(password)) {
            textInputLayoutPassword.setError("Vui lòng nhập mật khẩu");
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
    }

    private void applyTitleGradient(TextView loginTitle) {
        int color1 = ContextCompat.getColor(this, R.color.colorPrimary);
        int color2 = ContextCompat.getColor(this, R.color.colorAccent);
        Shader textShader = new LinearGradient(0, 0, loginTitle.getPaint().measureText(loginTitle.getText().toString()), loginTitle.getTextSize(), new int[]{color1, color2}, null, Shader.TileMode.CLAMP);
        loginTitle.getPaint().setShader(textShader);
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        buttonLogin.setEnabled(!isLoading);
        buttonGoogleLogin.setEnabled(!isLoading);
    }
}
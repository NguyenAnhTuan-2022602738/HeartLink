package vn.haui.heartlink.utils;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Lightweight wrapper around {@link FirebaseAuth} used across the app.
 */
public final class FirebaseHelper {

    private static FirebaseHelper instance;

    private final FirebaseAuth firebaseAuth;

    private FirebaseHelper() {
        firebaseAuth = FirebaseAuth.getInstance();
    }

    public static synchronized FirebaseHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }

    /**
     * Đăng ký tài khoản mới với email và mật khẩu.
     *
     * @param email Email của user
     * @param password Mật khẩu của user
     * @return Task<AuthResult> chứa kết quả đăng ký
     */
    public Task<AuthResult> registerUser(String email, String password) {
        return firebaseAuth.createUserWithEmailAndPassword(email, password);
    }

    /**
     * Đăng nhập với email và mật khẩu.
     *
     * @param email Email của user
     * @param password Mật khẩu của user
     * @return Task<AuthResult> chứa kết quả đăng nhập
     */
    public Task<AuthResult> loginUser(String email, String password) {
        return firebaseAuth.signInWithEmailAndPassword(email, password);
    }

    /**
     * Đăng nhập với AuthCredential (Google, Facebook, etc.).
     *
     * @param credential AuthCredential từ provider
     * @return Task<AuthResult> chứa kết quả đăng nhập
     */
    public Task<AuthResult> signInWithCredential(AuthCredential credential) {
        return firebaseAuth.signInWithCredential(credential);
    }

    /**
     * Lấy thông tin user hiện tại đang đăng nhập.
     *
     * @return FirebaseUser hiện tại hoặc null nếu chưa đăng nhập
     */
    @Nullable
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    /**
     * Đăng xuất user hiện tại khỏi Firebase Auth.
     */
    public void signOut() {
        firebaseAuth.signOut();
    }
}

package vn.haui.heartlink.utils;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;

import vn.haui.heartlink.Constants;
import vn.haui.heartlink.models.User;

/**
 * Repository cho các thao tác dữ liệu người dùng với Firebase Realtime Database
 */
public class UserRepository {
    
    private static UserRepository instance;
    private final DatabaseReference usersRef;
    
    private UserRepository() {
        usersRef = FirebaseDatabase.getInstance().getReference(Constants.USERS_NODE);
    }
    
    /**
     * Lấy instance duy nhất của UserRepository (Singleton pattern)
     * @return Instance của UserRepository
     */
    public static synchronized UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }
    
    /**
     * Lấy dữ liệu người dùng từ Firebase
     * @param uid ID của người dùng cần lấy
     * @return Task chứa DataSnapshot của dữ liệu người dùng
     */
    public Task<DataSnapshot> getUserData(String uid) {
        return usersRef.child(uid).get();
    }

    /**
     * Lấy tất cả người dùng từ hệ thống
     * @return Task chứa DataSnapshot của tất cả người dùng
     */
    public Task<DataSnapshot> getAllUsers() {
        return usersRef.get();
    }
    
    /**
     * Tạo người dùng mới trong Firebase
     * @param user Đối tượng User chứa thông tin người dùng cần tạo
     * @return Task<Void> để theo dõi trạng thái tạo người dùng
     */
    public Task<Void> createUser(User user) {
        return usersRef.child(user.getUid()).setValue(user);
    }
    
    /**
     * Cập nhật toàn bộ dữ liệu người dùng
     * @param uid ID của người dùng cần cập nhật
     * @param user Đối tượng User chứa thông tin mới
     * @return Task<Void> để theo dõi trạng thái cập nhật
     */
    public Task<Void> updateUser(String uid, User user) {
        return usersRef.child(uid).setValue(user);
    }
    
    /**
     * Cập nhật nhiều trường dữ liệu cùng lúc với callback
     * @param uid ID của người dùng cần cập nhật
     * @param updates Map chứa các trường cần cập nhật và giá trị mới
     * @param listener Callback để xử lý kết quả thành công/thất bại
     */
    public void updateUser(String uid, java.util.Map<String, Object> updates, OnCompleteListener listener) {
        usersRef.child(uid).updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                if (listener != null) {
                    listener.onSuccess();
                }
            })
            .addOnFailureListener(e -> {
                if (listener != null) {
                    listener.onFailure(e);
                }
            });
    }
    
    /**
     * Cập nhật một trường dữ liệu cụ thể
     * @param uid ID của người dùng cần cập nhật
     * @param field Tên trường cần cập nhật
     * @param value Giá trị mới cho trường
     * @return Task<Void> để theo dõi trạng thái cập nhật
     */
    public Task<Void> updateField(String uid, String field, Object value) {
        return usersRef.child(uid).child(field).setValue(value);
    }
    
    /**
     * Cập nhật một trường dữ liệu cụ thể với callback
     * @param uid ID của người dùng cần cập nhật
     * @param field Tên trường cần cập nhật
     * @param value Giá trị mới cho trường
     * @param listener Callback để xử lý kết quả thành công/thất bại
     */
    public void updateField(String uid, String field, Object value, OnCompleteListener listener) {
        usersRef.child(uid).child(field).setValue(value)
            .addOnSuccessListener(aVoid -> {
                if (listener != null) {
                    listener.onSuccess();
                }
            })
            .addOnFailureListener(e -> {
                if (listener != null) {
                    listener.onFailure(e);
                }
            });
    }
    
    /**
     * Interface callback cho các thao tác bất đồng bộ
     */
    public interface OnCompleteListener {
        void onSuccess();
        void onFailure(Exception e);
    }
    
    /**
     * Kiểm tra xem người dùng có tồn tại trong hệ thống không
     * @param uid ID của người dùng cần kiểm tra
     * @return Task chứa DataSnapshot để kiểm tra sự tồn tại
     */
    public Task<DataSnapshot> checkUserExists(String uid) {
        return usersRef.child(uid).get();
    }
}

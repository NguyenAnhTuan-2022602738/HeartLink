package vn.haui.heartlink.admin;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import vn.haui.heartlink.R;
import vn.haui.heartlink.models.User;
import vn.haui.heartlink.utils.DialogHelper;

/**
 * Fragment quản lý người dùng
 * Hiển thị danh sách người dùng và cung cấp các chức năng khóa/mở khóa, xóa người dùng
 */
public class UserManagementFragment extends Fragment implements UserManagementAdapter.UserActionListener {

    private RecyclerView recyclerView; // RecyclerView hiển thị danh sách người dùng
    private UserManagementAdapter adapter; // Adapter quản lý dữ liệu cho RecyclerView
    private List<User> userList; // Danh sách người dùng
    private DatabaseReference usersRef; // Tham chiếu đến node Users trong Firebase

    /**
     * Tạo view cho fragment
     * @param inflater LayoutInflater để inflate layout
     * @param container ViewGroup chứa fragment
     * @param savedInstanceState Bundle chứa trạng thái đã lưu
     * @return View đã được inflate
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_management, container, false);
    }

    /**
     * Được gọi sau khi view đã được tạo
     * Khởi tạo các components và tải dữ liệu người dùng
     * @param view View đã được tạo
     * @param savedInstanceState Bundle chứa trạng thái đã lưu
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Tìm RecyclerView trong layout
        recyclerView = view.findViewById(R.id.recycler_view_users);
        // Khởi tạo tham chiếu Firebase
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        // Thiết lập RecyclerView và tải dữ liệu
        setupRecyclerView();
        loadUsers();
    }

    /**
     * Thiết lập RecyclerView với adapter và layout manager
     */
    private void setupRecyclerView() {
        userList = new ArrayList<>();
        adapter = new UserManagementAdapter(getContext(), userList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    /**
     * Tải danh sách người dùng từ Firebase
     * Lọc ra những người dùng không phải là Admin
     */
    private void loadUsers() {
        // Sắp xếp người dùng theo tên và lắng nghe thay đổi
        usersRef.orderByChild("name").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear(); // Xóa danh sách cũ
                // Duyệt qua tất cả người dùng
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    User user = userSnapshot.getValue(User.class);
                    // Chỉ thêm người dùng không phải Admin
                    if (user != null && !"Admin".equals(user.getRole())) {
                        userList.add(user);
                    }
                }
                // Cập nhật adapter
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Hiển thị lỗi nếu không tải được dữ liệu
                if (getContext() != null) {
                    DialogHelper.showStatusDialog(getContext(), "Lỗi", "Không thể tải danh sách người dùng.", false, null);
                }
            }
        });
    }

    /**
     * Xử lý sự kiện khi nhấn vào nút tùy chọn của một người dùng
     * Hiển thị popup menu với các tùy chọn khóa/mở khóa và xóa
     * @param user Người dùng được chọn
     * @param view View anchor cho popup menu
     */
    @Override
    public void onUserOptionsClicked(User user, View view) {
        if (getContext() == null) return;
        // Tạo popup menu
        PopupMenu popup = new PopupMenu(getContext(), view);
        popup.getMenuInflater().inflate(R.menu.menu_user_options, popup.getMenu());

        // Thay đổi text của menu item dựa trên trạng thái khóa
        MenuItem blockItem = popup.getMenu().findItem(R.id.action_block_user);
        blockItem.setTitle(user.isBlocked() ? "Mở khóa" : "Khóa");

        // Xử lý sự kiện chọn menu item
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_delete_user) {
                showDeleteConfirmationDialog(user);
                return true;
            } else if (itemId == R.id.action_block_user) {
                showBlockConfirmationDialog(user);
                return true;
            }
            return false;
        });
        popup.show();
    }

    /**
     * Hiển thị hộp thoại xác nhận khóa/mở khóa người dùng
     * @param user Người dùng cần khóa/mở khóa
     */
    private void showBlockConfirmationDialog(User user) {
        if (getContext() == null) return;
        String action = user.isBlocked() ? "mở khóa" : "khóa";
        new AlertDialog.Builder(getContext())
                .setTitle("Xác nhận " + action)
                .setMessage("Bạn có chắc chắn muốn " + action + " người dùng '" + user.getName() + "'?")
                .setPositiveButton(action.substring(0, 1).toUpperCase() + action.substring(1), (dialog, which) -> toggleBlockStatus(user))
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Đảo ngược trạng thái khóa của người dùng
     * Cập nhật trạng thái blocked trong Firebase
     * @param user Người dùng cần thay đổi trạng thái
     */
    private void toggleBlockStatus(User user) {
        boolean newBlockedStatus = !user.isBlocked(); // Đảo trạng thái
        // Cập nhật trạng thái trong Firebase
        usersRef.child(user.getUid()).child("blocked").setValue(newBlockedStatus)
                .addOnSuccessListener(aVoid -> {
                    String action = newBlockedStatus ? "khóa" : "mở khóa";
                    if (getContext() != null) {
                        DialogHelper.showStatusDialog(getContext(), "Thành công", "Đã " + action + " người dùng.", true, null);
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        DialogHelper.showStatusDialog(getContext(), "Thất bại", "Không thể thực hiện thao tác.", false, null);
                    }
                });
    }

    /**
     * Hiển thị hộp thoại xác nhận xóa người dùng
     * Cảnh báo rằng hành động này không thể hoàn tác
     * @param user Người dùng cần xóa
     */
    private void showDeleteConfirmationDialog(User user) {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Xóa người dùng")
                .setMessage("Bạn có chắc chắn muốn xóa người dùng '" + user.getName() + "'? Hành động này không thể hoàn tác.")
                .setPositiveButton("Xóa", (dialog, which) -> deleteUser(user.getUid()))
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Xóa người dùng khỏi Firebase Database
     * @param uid UID của người dùng cần xóa
     */
    private void deleteUser(String uid) {
        // Xóa node của người dùng trong Firebase
        usersRef.child(uid).removeValue().addOnCompleteListener(task -> {
            if (getContext() == null) return;
            if (task.isSuccessful()) {
                DialogHelper.showStatusDialog(getContext(), "Thành công", "Đã xóa người dùng.", true, null);
            } else {
                DialogHelper.showStatusDialog(getContext(), "Thất bại", "Không thể xóa người dùng.", false, null);
            }
        });
    }
}

package vn.haui.heartlink.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

import vn.haui.heartlink.R;
import vn.haui.heartlink.activities.WelcomeActivity;

/**
 * Activity quản lý admin - Màn hình chính cho quản trị viên
 * Chứa thanh công cụ và điều hướng giữa các fragment quản lý
 */
public class AdminActivity extends AppCompatActivity {

    // Thanh công cụ hiển thị ở đầu màn hình
    private Toolbar toolbar;
    // Fragment hiển thị bảng thống kê dashboard
    private final Fragment dashboardFragment = new AdminDashboardFragment();
    // Fragment hiển thị danh sách và quản lý người dùng
    private final Fragment userManagementFragment = new UserManagementFragment();
    // Fragment đang được hiển thị hiện tại
    private Fragment activeFragment = dashboardFragment;

    /**
     * Hàm khởi tạo activity khi được tạo
     * Thiết lập giao diện, toolbar và điều hướng dưới cùng
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Thiết lập toolbar
        toolbar = findViewById(R.id.admin_toolbar);
        setSupportActionBar(toolbar);

        // Thiết lập thanh điều hướng dưới cùng và lắng nghe sự kiện chọn mục
        BottomNavigationView bottomNav = findViewById(R.id.admin_bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListener);

        // Thêm cả 2 fragment vào container, ẩn fragment quản lý người dùng
        getSupportFragmentManager().beginTransaction().add(R.id.admin_fragment_container, userManagementFragment, "2").hide(userManagementFragment).commit();
        // Hiển thị fragment dashboard mặc định
        getSupportFragmentManager().beginTransaction().add(R.id.admin_fragment_container, dashboardFragment, "1").commit();

        // Đặt tiêu đề cho toolbar
        toolbar.setTitle("Thống kê");
    }

    /**
     * Tạo menu trên toolbar (menu đăng xuất)
     * @param menu Menu được tạo
     * @return true nếu menu được tạo thành công
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_toolbar_menu, menu);
        return true;
    }

    /**
     * Xử lý khi người dùng chọn một mục trong menu toolbar
     * @param item Mục menu được chọn
     * @return true nếu sự kiện được xử lý
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Nếu chọn mục đăng xuất, hiển thị hộp thoại xác nhận
        if (item.getItemId() == R.id.action_logout) {
            showLogoutConfirmationDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Hiển thị hộp thoại xác nhận đăng xuất
     * Yêu cầu người dùng xác nhận trước khi thực hiện đăng xuất
     */
    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> performLogout())
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Thực hiện đăng xuất
     * Cập nhật trạng thái offline cho người dùng trong Firebase,
     * sau đó đăng xuất khỏi Firebase Auth và chuyển về màn hình chào mừng
     */
    private void performLogout() {
        // Lấy người dùng hiện tại
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Cập nhật trạng thái người dùng thành offline trong database
            DatabaseReference userStatusRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
            Map<String, Object> statusUpdate = new HashMap<>();
            statusUpdate.put("online", false); // Đặt trạng thái offline
            statusUpdate.put("lastSeen", ServerValue.TIMESTAMP); // Cập nhật thời gian xem lần cuối
            userStatusRef.updateChildren(statusUpdate).addOnCompleteListener(task -> {
                // Đăng xuất khỏi Firebase
                FirebaseAuth.getInstance().signOut();
                // Chuyển về màn hình chào mừng
                navigateToWelcomeScreen();
            });
        } else {
            // Nếu không có người dùng đăng nhập, chuyển thẳng về màn hình chào mừng
            navigateToWelcomeScreen();
        }
    }

    /**
     * Điều hướng về màn hình chào mừng
     * Xóa tất cả các activity trước đó trong stack để người dùng không thể quay lại
     */
    private void navigateToWelcomeScreen() {
        Intent intent = new Intent(this, WelcomeActivity.class);
        // Xóa tất cả activity và tạo task mới
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Kết thúc activity hiện tại
    }

    /**
     * Listener cho thanh điều hướng dưới cùng
     * Xử lý việc chuyển đổi giữa các fragment khi người dùng chọn mục điều hướng
     */
    private final BottomNavigationView.OnNavigationItemSelectedListener navListener = item -> {
        Fragment selectedFragment = null;
        String title = "";

        // Xác định fragment nào được chọn dựa trên id của mục điều hướng
        if (item.getItemId() == R.id.admin_nav_dashboard) {
            selectedFragment = dashboardFragment;
            title = "Thống kê";
        } else if (item.getItemId() == R.id.admin_nav_users) {
            selectedFragment = userManagementFragment;
            title = "Quản lý người dùng";
        }

        // Nếu fragment được chọn hợp lệ, thực hiện chuyển đổi
        if (selectedFragment != null) {
            // Ẩn fragment hiện tại và hiển thị fragment được chọn
            getSupportFragmentManager().beginTransaction().hide(activeFragment).show(selectedFragment).commit();
            activeFragment = selectedFragment; // Cập nhật fragment đang hoạt động
            toolbar.setTitle(title); // Cập nhật tiêu đề toolbar
            return true;
        }
        return false;
    };
}

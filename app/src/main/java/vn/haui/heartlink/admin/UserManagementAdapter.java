package vn.haui.heartlink.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;

import java.util.List;

import vn.haui.heartlink.R;
import vn.haui.heartlink.models.User;

/**
 * Adapter cho RecyclerView hiển thị danh sách người dùng
 * Quản lý việc hiển thị thông tin người dùng và xử lý các tương tác
 */
public class UserManagementAdapter extends RecyclerView.Adapter<UserManagementAdapter.UserViewHolder> {

    private final Context context;
    private final List<User> userList; // Danh sách người dùng cần hiển thị
    private final UserActionListener listener; // Listener để xử lý các hành động trên người dùng

    /**
     * Interface để lắng nghe sự kiện khi người dùng nhấn vào menu tùy chọn
     */
    public interface UserActionListener {
        void onUserOptionsClicked(User user, View view);
    }

    /**
     * Constructor khởi tạo adapter
     * @param context Context của ứng dụng
     * @param userList Danh sách người dùng
     * @param listener Listener xử lý sự kiện
     */
    public UserManagementAdapter(Context context, List<User> userList, UserActionListener listener) {
        this.context = context;
        this.userList = userList;
        this.listener = listener;
    }

    /**
     * Tạo ViewHolder mới cho mỗi item trong danh sách
     * @param parent ViewGroup chứa item
     * @param viewType Loại view
     * @return ViewHolder mới được tạo
     */
    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_management, parent, false);
        return new UserViewHolder(view);
    }

    /**
     * Gắn dữ liệu người dùng vào ViewHolder tại vị trí cụ thể
     * @param holder ViewHolder cần gắn dữ liệu
     * @param position Vị trí của item trong danh sách
     */
    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.bind(user);
    }

    /**
     * Trả về số lượng item trong danh sách
     * @return Số lượng người dùng
     */
    @Override
    public int getItemCount() {
        return userList.size();
    }

    /**
     * ViewHolder chứa các view components cho mỗi item người dùng
     */
    class UserViewHolder extends RecyclerView.ViewHolder {
        private final ImageView avatar; // Ảnh đại diện
        private final TextView name, email; // Tên và email
        private final Chip statusChip; // Chip hiển thị trạng thái
        private final ImageButton menuButton; // Nút menu tùy chọn

        /**
         * Constructor khởi tạo các view components
         * @param itemView View của item
         */
        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.user_avatar);
            name = itemView.findViewById(R.id.user_name);
            email = itemView.findViewById(R.id.user_email);
            statusChip = itemView.findViewById(R.id.user_status_chip);
            menuButton = itemView.findViewById(R.id.user_options_button);
        }

        /**
         * Gắn dữ liệu người dùng vào các view components
         * @param user Đối tượng User cần hiển thị
         */
        void bind(User user) {
            // Hiển thị tên người dùng, nếu chưa có tên thì hiển thị "(Chưa có tên)"
            name.setText(user.getName() != null ? user.getName() : "(Chưa có tên)");
            email.setText(user.getEmail());

            // Tải và hiển thị ảnh đại diện
            if (user.getPhotoUrls() != null && !user.getPhotoUrls().isEmpty()) {
                Glide.with(context).load(user.getPhotoUrls().get(0)).placeholder(R.drawable.ic_avatar_placeholder).circleCrop().into(avatar);
            } else {
                Glide.with(context).load(R.drawable.ic_avatar_placeholder).circleCrop().into(avatar);
            }

            // Thiết lập chip trạng thái dựa trên trạng thái của người dùng
            if (user.isBlocked()) {
                // Người dùng bị khóa
                statusChip.setText("Bị khóa");
                statusChip.setChipIconResource(R.drawable.ic_admin_shield);
                statusChip.setChipBackgroundColorResource(R.color.admin_stat_red);
            } else if (user.isOnline()) {
                // Người dùng đang online
                statusChip.setText("Online");
                statusChip.setChipIconResource(R.drawable.ic_analytics);
                statusChip.setChipBackgroundColorResource(R.color.admin_stat_green);
            } else {
                // Người dùng offline
                statusChip.setText("Offline");
                statusChip.setChipIconResource(R.drawable.ic_person);
                statusChip.setChipBackgroundColorResource(R.color.grey_100);
            }

            // Thiết lập sự kiện click cho nút menu
            menuButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserOptionsClicked(user, v);
                }
            });
        }
    }
}

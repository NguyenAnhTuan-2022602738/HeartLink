package vn.haui.heartlink.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import vn.haui.heartlink.R;
import vn.haui.heartlink.adapters.ChatMessagesAdapter;
import vn.haui.heartlink.models.ChatMessage;
import vn.haui.heartlink.utils.ChatRepository;

/**
 * Conversation screen for a single chat thread.
 */
public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_CHAT_ID = "extra_chat_id";
    public static final String EXTRA_PARTNER_ID = "extra_partner_id";
    public static final String EXTRA_PARTNER_NAME = "extra_partner_name";
    public static final String EXTRA_PARTNER_PHOTO = "extra_partner_photo";

    private ImageView partnerAvatarView;
    private TextView partnerNameView;
    private TextView partnerStatusView;
    private RecyclerView messagesList;
    private EditText messageInput;
    private ImageButton sendButton;
    private ProgressBar loadingIndicator;

    private ChatMessagesAdapter messagesAdapter;

    @Nullable
    private String chatId;
    @Nullable
    private String partnerId;
    @Nullable
    private String partnerName;
    @Nullable
    private String partnerPhotoUrl;
    @Nullable
    private String currentUid;

    @Nullable
    private DatabaseReference messagesRef;
    @Nullable
    private ValueEventListener messagesListener;

    private final ChatRepository chatRepository = ChatRepository.getInstance();

    /**
     * Tạo Intent để khởi chạy ChatActivity với thông tin chat thread.
     * @param context Context của ứng dụng
     * @param chatId ID của luồng chat
     * @param partnerId ID của người đối thoại
     * @param partnerName Tên của người đối thoại
     * @param partnerPhotoUrl URL ảnh đại diện của người đối thoại (có thể null)
     * @return Intent đã được cấu hình để khởi chạy ChatActivity
     */
    public static android.content.Intent createIntent(@NonNull android.content.Context context,
                                                      @NonNull String chatId,
                                                      @NonNull String partnerId,
                                                      @NonNull String partnerName,
                                                      @Nullable String partnerPhotoUrl) {
        android.content.Intent intent = new android.content.Intent(context, ChatActivity.class);
        intent.putExtra(EXTRA_CHAT_ID, chatId);
        intent.putExtra(EXTRA_PARTNER_ID, partnerId);
        intent.putExtra(EXTRA_PARTNER_NAME, partnerName);
        intent.putExtra(EXTRA_PARTNER_PHOTO, partnerPhotoUrl);
        return intent;
    }

    /**
     * Phương thức khởi tạo activity chat khi được tạo.
     * Thiết lập giao diện người dùng, xử lý insets cho edge-to-edge,
     * và khởi tạo các thành phần cần thiết cho chat.
     * Nếu không thể bind dữ liệu từ intent, activity sẽ kết thúc.
     *
     * @param savedInstanceState Trạng thái đã lưu của activity (có thể null)
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);

        View root = findViewById(R.id.chat_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(view.getPaddingLeft(), systemBars.top, view.getPaddingRight(), Math.max(view.getPaddingBottom(), systemBars.bottom));
            return insets;
        });

        if (!bindExtras()) {
            finish();
            return;
        }

        bindViews();
        setupRecyclerView();
        setupClicks();
        populateHeader();
        attachMessageListener();
    }

    /**
     * Phương thức bind dữ liệu từ Intent khi khởi tạo activity.
     * Lấy thông tin chat ID, partner ID, tên và ảnh của đối phương từ Intent.
     * Đồng thời lấy UID của người dùng hiện tại từ Firebase Auth.
     * Kiểm tra tính hợp lệ của các dữ liệu cần thiết.
     *
     * @return true nếu tất cả dữ liệu hợp lệ, false nếu thiếu dữ liệu
     */
    private boolean bindExtras() {
        chatId = getIntent().getStringExtra(EXTRA_CHAT_ID);
        partnerId = getIntent().getStringExtra(EXTRA_PARTNER_ID);
        partnerName = getIntent().getStringExtra(EXTRA_PARTNER_NAME);
        partnerPhotoUrl = getIntent().getStringExtra(EXTRA_PARTNER_PHOTO);

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUid = firebaseUser != null ? firebaseUser.getUid() : null;

        if (TextUtils.isEmpty(chatId) || TextUtils.isEmpty(partnerId) || TextUtils.isEmpty(partnerName) || currentUid == null) {
            Toast.makeText(this, R.string.error_generic, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    /**
     * Phương thức bind các view từ layout XML vào các biến thành viên.
     * Gán các ImageView, TextView, RecyclerView, EditText, Button và ProgressBar.
     * Đồng thời thiết lập các click listener cho các button điều hướng và chức năng.
     */
    private void bindViews() {
        partnerAvatarView = findViewById(R.id.chat_partner_avatar);
        partnerNameView = findViewById(R.id.chat_partner_name);
        partnerStatusView = findViewById(R.id.chat_partner_status);
        messagesList = findViewById(R.id.chat_messages_list);
        messageInput = findViewById(R.id.chat_message_input);
        sendButton = findViewById(R.id.chat_send_button);
        loadingIndicator = findViewById(R.id.chat_loading);

        ImageButton backButton = findViewById(R.id.chat_back_button);
        ImageButton moreButton = findViewById(R.id.chat_more_button);
        ImageButton extraButton = findViewById(R.id.chat_extra_button);
        ImageButton voiceButton = findViewById(R.id.chat_voice_button);

        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        moreButton.setOnClickListener(v -> Toast.makeText(this, R.string.matches_action_coming_soon, Toast.LENGTH_SHORT).show());
        extraButton.setOnClickListener(v -> Toast.makeText(this, R.string.matches_action_coming_soon, Toast.LENGTH_SHORT).show());
        voiceButton.setOnClickListener(v -> Toast.makeText(this, R.string.matches_action_coming_soon, Toast.LENGTH_SHORT).show());
    }

    /**
     * Phương thức thiết lập RecyclerView cho danh sách tin nhắn.
     * Sử dụng LinearLayoutManager với stackFromEnd=true để hiển thị tin nhắn từ cuối danh sách.
     * Tạo và cấu hình ChatMessagesAdapter với thông tin người dùng hiện tại và ảnh đối phương.
     */
    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        messagesList.setLayoutManager(layoutManager);
        messagesAdapter = new ChatMessagesAdapter();
        messagesAdapter.setCurrentUserId(currentUid);
        messagesAdapter.setPartnerPhotoUrl(partnerPhotoUrl);
        messagesList.setAdapter(messagesAdapter);
    }

    /**
     * Phương thức thiết lập các click listener cho giao diện chat.
     * Gán sự kiện click cho nút gửi tin nhắn và xử lý sự kiện nhấn Enter
     * hoặc IME_ACTION_SEND trên trường nhập tin nhắn.
     */
    private void setupClicks() {
        sendButton.setOnClickListener(v -> sendCurrentMessage());
        messageInput.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendCurrentMessage();
                return true;
            }
            if (event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_UP) {
                sendCurrentMessage();
                return true;
            }
            return false;
        });
    }

    /**
     * Phương thức điền thông tin vào header của chat.
     * Hiển thị tên đối phương, trạng thái hoạt động và tải ảnh đại diện
     * từ URL hoặc sử dụng ảnh mặc định nếu không có URL.
     */
    private void populateHeader() {
        if (!TextUtils.isEmpty(partnerName)) {
            partnerNameView.setText(partnerName);
        }
        partnerStatusView.setText(R.string.chat_partner_status_active);

        if (!TextUtils.isEmpty(partnerPhotoUrl)) {
            Glide.with(this)
                    .load(partnerPhotoUrl)
                    .placeholder(R.drawable.welcome_person_2)
                    .into(partnerAvatarView);
        } else {
            partnerAvatarView.setImageResource(R.drawable.welcome_person_2);
        }
    }

    /**
     * Phương thức gửi tin nhắn hiện tại đang được nhập.
     * Kiểm tra tính hợp lệ của dữ liệu, tạo đối tượng ChatMessage,
     * gửi tin nhắn qua repository và xử lý kết quả thành công/thất bại.
     * Xóa nội dung input và cuộn xuống cuối sau khi gửi thành công.
     */
    private void sendCurrentMessage() {
        if (TextUtils.isEmpty(chatId) || TextUtils.isEmpty(currentUid)) {
            return;
        }
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }
        sendButton.setEnabled(false);
        ChatMessage message = new ChatMessage(chatId, currentUid, text, null, System.currentTimeMillis());
        chatRepository.sendMessage(message)
                .addOnSuccessListener(unused -> {
                    messageInput.setText("");
                    sendButton.setEnabled(true);
                    scrollToBottom();
                })
                .addOnFailureListener(error -> {
                    sendButton.setEnabled(true);
                    Toast.makeText(ChatActivity.this, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Phương thức gắn ValueEventListener để lắng nghe thay đổi tin nhắn từ Firebase.
     * Khi có dữ liệu mới, parse các ChatMessage, sắp xếp theo thời gian,
     * cập nhật adapter và thực hiện các hành động như cuộn xuống cuối và đánh dấu đã đọc.
     * Xử lý lỗi nếu có khi kết nối database thất bại.
     */
    private void attachMessageListener() {
        if (TextUtils.isEmpty(chatId)) {
            return;
        }
        setLoading(true);
        messagesRef = chatRepository.getMessagesReference(chatId);
        messagesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ChatMessage> newMessages = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    ChatMessage message = child.getValue(ChatMessage.class);
                    if (message == null) {
                        continue;
                    }
                    if (TextUtils.isEmpty(message.getMessageId())) {
                        message.setMessageId(child.getKey());
                    }
                    newMessages.add(message);
                }
                Collections.sort(newMessages, (first, second) -> {
                    String firstId = first.getMessageId();
                    String secondId = second.getMessageId();
                    if (!TextUtils.isEmpty(firstId) && !TextUtils.isEmpty(secondId)) {
                        int idCompare = firstId.compareTo(secondId);
                        if (idCompare != 0) {
                            return idCompare;
                        }
                    }
                    return Long.compare(first.getTimestamp(), second.getTimestamp());
                });
                messagesAdapter.submitMessages(newMessages);
                scrollToBottom();
                setLoading(false);
                markThreadRead();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setLoading(false);
                Toast.makeText(ChatActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        messagesRef.addValueEventListener(messagesListener);
    }

    /**
     * Phương thức đánh dấu thread chat đã được đọc.
     * Gọi repository để cập nhật trạng thái đã đọc cho người dùng hiện tại.
     */
    private void markThreadRead() {
        if (!TextUtils.isEmpty(chatId) && !TextUtils.isEmpty(currentUid)) {
            chatRepository.markThreadRead(chatId, currentUid);
        }
    }

    /**
     * Phương thức cuộn RecyclerView xuống vị trí cuối cùng (tin nhắn mới nhất).
     * Chỉ thực hiện khi có ít nhất một item trong adapter.
     */
    private void scrollToBottom() {
        int itemCount = messagesAdapter.getItemCount();
        if (itemCount > 0) {
            messagesList.scrollToPosition(itemCount - 1);
        }
    }

    /**
     * Phương thức thiết lập trạng thái loading của giao diện.
     * Hiển thị hoặc ẩn ProgressBar loading indicator.
     *
     * @param loading true để hiển thị loading, false để ẩn
     */
    private void setLoading(boolean loading) {
        loadingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    /**
     * Phương thức được gọi khi Activity bị hủy.
     * Gỡ bỏ ValueEventListener để tránh memory leak và rò rỉ tài nguyên.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesRef != null && messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }
    }
}

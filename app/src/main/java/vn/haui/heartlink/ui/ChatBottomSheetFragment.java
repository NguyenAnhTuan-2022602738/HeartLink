package vn.haui.heartlink.ui;

import android.app.Dialog;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.vanniktech.emoji.EmojiEditText;
import com.vanniktech.emoji.EmojiPopup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vn.haui.heartlink.R;
import vn.haui.heartlink.activities.ProfileDetailActivity;
import vn.haui.heartlink.adapters.ChatMessagesAdapter;
import vn.haui.heartlink.models.ChatMessage;
import vn.haui.heartlink.models.User;
import vn.haui.heartlink.utils.ChatRepository;
import vn.haui.heartlink.utils.MatchRepository;
import vn.haui.heartlink.utils.UserRepository;

public class ChatBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String ARG_CHAT_ID = "chat_id";
    private static final String ARG_PARTNER_ID = "partner_id";
    private static final String ARG_PARTNER_NAME = "partner_name";
    private static final String ARG_PARTNER_PHOTO = "partner_photo";

    private ImageView partnerAvatarView;
    private TextView partnerNameView;
    private TextView partnerStatusView;
    private RecyclerView messagesList;
    private EmojiEditText messageInput;
    private ImageButton sendButton;
    private ImageButton emojiButton;
    private ConstraintLayout rootView;

    private ChatMessagesAdapter messagesAdapter;
    @Nullable
    private EmojiPopup emojiPopup;

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
    @Nullable
    private DatabaseReference partnerStatusRef;
    @Nullable
    private ValueEventListener partnerStatusListener;

    private final ChatRepository chatRepository = ChatRepository.getInstance();
    private final UserRepository userRepository = UserRepository.getInstance();

    public static ChatBottomSheetFragment newInstance(String chatId, String partnerId, String partnerName, String partnerPhotoUrl) {
        ChatBottomSheetFragment fragment = new ChatBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHAT_ID, chatId);
        args.putString(ARG_PARTNER_ID, partnerId);
        args.putString(ARG_PARTNER_NAME, partnerName);
        args.putString(ARG_PARTNER_PHOTO, partnerPhotoUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            chatId = getArguments().getString(ARG_CHAT_ID);
            partnerId = getArguments().getString(ARG_PARTNER_ID);
            partnerName = getArguments().getString(ARG_PARTNER_NAME);
            partnerPhotoUrl = getArguments().getString(ARG_PARTNER_PHOTO);
        }

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUid = firebaseUser != null ? firebaseUser.getUid() : null;

        if (TextUtils.isEmpty(chatId) || TextUtils.isEmpty(partnerId) || TextUtils.isEmpty(partnerName) || currentUid == null) {
            Toast.makeText(getContext(), R.string.error_generic, Toast.LENGTH_SHORT).show();
            dismiss();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        dialog.setOnShowListener(d -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) d;
            View bottomSheetInternal = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheetInternal != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheetInternal);
                ViewGroup.LayoutParams layoutParams = bottomSheetInternal.getLayoutParams();
                int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
                layoutParams.height = (int) (screenHeight * 0.85);
                bottomSheetInternal.setLayoutParams(layoutParams);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view.findViewById(R.id.chat_root_view);
        bindViews(view);
        setupRecyclerView();
        setupEmojiPopup();
        setupClicks();
        populateHeader();
        attachMessageListener();
        attachPartnerStatusListener();
    }

    private void bindViews(View view) {
        partnerAvatarView = view.findViewById(R.id.chat_partner_avatar);
        partnerNameView = view.findViewById(R.id.chat_partner_name);
        partnerStatusView = view.findViewById(R.id.chat_partner_status);
        messagesList = view.findViewById(R.id.chat_messages_list);
        messageInput = view.findViewById(R.id.chat_message_input);
        sendButton = view.findViewById(R.id.chat_voice_button);
        emojiButton = view.findViewById(R.id.chat_emoji_button);

        ImageButton moreButton = view.findViewById(R.id.chat_more_button);
        ImageButton closeButton = view.findViewById(R.id.chat_close_button);

        moreButton.setOnClickListener(v -> Toast.makeText(getContext(), R.string.matches_action_coming_soon, Toast.LENGTH_SHORT).show());
        closeButton.setOnClickListener(v -> dismiss());
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        messagesList.setLayoutManager(layoutManager);
        messagesAdapter = new ChatMessagesAdapter();
        messagesAdapter.setCurrentUserId(currentUid);
        messagesAdapter.setPartnerPhotoUrl(partnerPhotoUrl);
        messagesList.setAdapter(messagesAdapter);
    }

    private void setupEmojiPopup() {
        emojiPopup = EmojiPopup.Builder.fromRootView(rootView).build(messageInput);
    }

    private void setupClicks() {
        sendButton.setOnClickListener(v -> sendCurrentMessage());

        emojiButton.setOnClickListener(v -> {
            if (emojiPopup != null) {
                emojiPopup.toggle();
            }
        });

        messageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    sendButton.setImageResource(R.drawable.ic_chat_send);
                } else {
                    sendButton.setImageResource(R.drawable.ic_mic);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        partnerNameView.setOnClickListener(v -> openPartnerProfile());
    }

    private void openPartnerProfile() {
        if (getContext() == null || TextUtils.isEmpty(partnerId)) {
            return;
        }
        // When opening profile from chat, they are already matched.
        Intent intent = ProfileDetailActivity.createIntent(
                getContext(),
                partnerId,
                partnerName,
                partnerPhotoUrl,
                MatchRepository.STATUS_MATCHED
        );
        startActivity(intent);
    }

    private void populateHeader() {
        if (!TextUtils.isEmpty(partnerName)) {
            String nameOnly = partnerName;
            if (nameOnly.contains(",")) {
                nameOnly = nameOnly.substring(0, nameOnly.indexOf(',')).trim();
            }
            partnerNameView.setText(nameOnly);
        }

        if (!TextUtils.isEmpty(partnerPhotoUrl) && getContext() != null) {
            Glide.with(getContext())
                    .load(partnerPhotoUrl)
                    .placeholder(R.drawable.welcome_person_2)
                    .into(partnerAvatarView);
        } else {
            partnerAvatarView.setImageResource(R.drawable.welcome_person_2);
        }
    }

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
                    if (getContext() != null) {
                        Toast.makeText(getContext(), error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void attachMessageListener() {
        if (TextUtils.isEmpty(chatId)) {
            return;
        }
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
                Collections.sort(newMessages, (first, second) -> Long.compare(first.getTimestamp(), second.getTimestamp()));
                messagesAdapter.submitMessages(newMessages);
                scrollToBottom();
                markThreadRead();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };
        messagesRef.addValueEventListener(messagesListener);
    }

    private void attachPartnerStatusListener() {
        if (TextUtils.isEmpty(partnerId)) {
            return;
        }
        partnerStatusRef = userRepository.getUsersRef().child(partnerId);
        partnerStatusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User partner = snapshot.getValue(User.class);
                if (partner != null) {
                    updatePartnerStatus(partner.isOnline(), partner.getLastSeen());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // No-op
            }
        };
        partnerStatusRef.addValueEventListener(partnerStatusListener);
    }

    private void updatePartnerStatus(boolean isOnline, long lastSeen) {
        if (getContext() == null) {
            return;
        }
        if (isOnline) {
            partnerStatusView.setText(R.string.chat_partner_status_active);
            partnerStatusView.setTextColor(ContextCompat.getColor(getContext(), R.color.green));
        } else {
            partnerStatusView.setText(formatLastSeen(lastSeen));
            partnerStatusView.setTextColor(ContextCompat.getColor(getContext(), R.color.profile_section_subtitle));
        }
    }

    private String formatLastSeen(long lastSeen) {
        if (getContext() == null) {
            return "";
        }
        if (lastSeen <= 0) {
            return getString(R.string.chat_partner_status_offline);
        }
        long now = System.currentTimeMillis();
        long diff = now - lastSeen;
        long minutes = diff / (1000 * 60);
        if (minutes < 1) {
            return getString(R.string.chat_partner_status_just_now);
        }
        if (minutes < 60) {
            return getString(R.string.chat_partner_status_minutes_ago, minutes);
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return getString(R.string.chat_partner_status_hours_ago, hours);
        }
        long days = hours / 24;
        return getString(R.string.chat_partner_status_days_ago, days);
    }


    private void markThreadRead() {
        if (!TextUtils.isEmpty(chatId) && !TextUtils.isEmpty(currentUid)) {
            chatRepository.markThreadRead(chatId, currentUid);
        }
    }

    private void scrollToBottom() {
        int itemCount = messagesAdapter.getItemCount();
        if (itemCount > 0) {
            messagesList.scrollToPosition(itemCount - 1);
        }
    }

    @Override
    public void onStop() {
        if (emojiPopup != null) {
            emojiPopup.dismiss();
        }
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (messagesRef != null && messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }
        if (partnerStatusRef != null && partnerStatusListener != null) {
            partnerStatusRef.removeEventListener(partnerStatusListener);
        }
        if (emojiPopup != null) {
            emojiPopup.dismiss();
        }
    }
}

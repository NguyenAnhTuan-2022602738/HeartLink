package vn.haui.heartlink.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import vn.haui.heartlink.R;
import vn.haui.heartlink.models.ChatMessage;

public class ChatMessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_OUTGOING = 1;
    private static final int TYPE_INCOMING = 2;
    private static final long FIVE_MINUTES_IN_MILLIS = 5 * 60 * 1000;

    private final List<ChatMessage> messages = new ArrayList<>();
    @Nullable
    private String currentUserId;
    @Nullable
    private String partnerPhotoUrl;
    private long partnerReadTimestamp = 0L;

    public void setCurrentUserId(@Nullable String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setPartnerPhotoUrl(@Nullable String partnerPhotoUrl) {
        this.partnerPhotoUrl = partnerPhotoUrl;
    }

    public void setPartnerReadTimestamp(long partnerReadTimestamp) {
        if (this.partnerReadTimestamp != partnerReadTimestamp) {
            this.partnerReadTimestamp = partnerReadTimestamp;
            notifyDataSetChanged();
        }
    }

    public void submitMessages(@NonNull List<ChatMessage> newMessages) {
        messages.clear();
        messages.addAll(newMessages);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        if (!TextUtils.isEmpty(currentUserId) && currentUserId.equals(message.getSenderId())) {
            return TYPE_OUTGOING;
        }
        return TYPE_INCOMING;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_OUTGOING) {
            View view = inflater.inflate(R.layout.item_chat_message_outgoing, parent, false);
            return new OutgoingViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_chat_message_incoming, parent, false);
            return new IncomingViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage currentMessage = messages.get(position);

        boolean showTimestampInitially = false;
        if (position == messages.size() - 1) {
            showTimestampInitially = true;
        } else {
            ChatMessage nextMessage = messages.get(position + 1);
            if (!TextUtils.equals(currentMessage.getSenderId(), nextMessage.getSenderId()) ||
                (nextMessage.getTimestamp() - currentMessage.getTimestamp() > FIVE_MINUTES_IN_MILLIS)) {
                showTimestampInitially = true;
            }
        }

        if (holder instanceof OutgoingViewHolder) {
            boolean isLastOutgoingMessage = position == findLastOutgoingMessagePosition();
            ((OutgoingViewHolder) holder).bind(currentMessage, showTimestampInitially, isLastOutgoingMessage, partnerReadTimestamp, partnerPhotoUrl);
        } else if (holder instanceof IncomingViewHolder) {
            ((IncomingViewHolder) holder).bind(currentMessage, showTimestampInitially, partnerPhotoUrl);
        }
    }

    private int findLastOutgoingMessagePosition() {
        if (currentUserId == null) return -1;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (currentUserId.equals(messages.get(i).getSenderId())) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class OutgoingViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout messageBubble;
        private final TextView messageText;
        private final TextView timeText;
        private final ImageView seenAvatar;

        OutgoingViewHolder(@NonNull View itemView) {
            super(itemView);
            messageBubble = itemView.findViewById(R.id.message_bubble);
            messageText = itemView.findViewById(R.id.chat_message_text);
            timeText = itemView.findViewById(R.id.chat_message_time);
            seenAvatar = itemView.findViewById(R.id.seen_avatar);
        }

        void bind(@NonNull ChatMessage message, boolean showTimestampInitially, boolean isLastOutgoingMessage, long partnerReadTimestamp, @Nullable String partnerPhotoUrl) {
            messageText.setText(message.getText());
            timeText.setText(formatTime(message.getTimestamp()));

            // Reset state
            timeText.setVisibility(View.GONE);
            seenAvatar.setVisibility(View.GONE);
            messageBubble.setOnClickListener(null); // Clear previous listeners

            final boolean isSeen = isLastOutgoingMessage && message.getTimestamp() <= partnerReadTimestamp;

            if (isSeen) {
                // State: Last message is seen, show avatar initially
                seenAvatar.setVisibility(View.VISIBLE);
                if (partnerPhotoUrl != null) {
                    Glide.with(itemView.getContext()).load(partnerPhotoUrl).apply(RequestOptions.circleCropTransform()).into(seenAvatar);
                }

                messageBubble.setOnClickListener(v -> {
                    // Toggle between avatar and time
                    if (seenAvatar.getVisibility() == View.VISIBLE) {
                        seenAvatar.setVisibility(View.GONE);
                        timeText.setVisibility(View.VISIBLE);
                    } else {
                        timeText.setVisibility(View.GONE);
                        seenAvatar.setVisibility(View.VISIBLE);
                    }
                });

            } else {
                // State: Not the last seen message
                if (showTimestampInitially) {
                    timeText.setVisibility(View.VISIBLE);
                }
                // Click to toggle time visibility
                messageBubble.setOnClickListener(v -> timeText.setVisibility(timeText.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
            }
        }
    }

    static class IncomingViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout messageBubble;
        private final TextView messageText;
        private final TextView timestamp;
        private final ImageView avatarView;

        IncomingViewHolder(@NonNull View itemView) {
            super(itemView);
            messageBubble = itemView.findViewById(R.id.message_bubble);
            messageText = itemView.findViewById(R.id.chat_message_text);
            timestamp = itemView.findViewById(R.id.chat_message_time);
            avatarView = itemView.findViewById(R.id.chat_message_sender_avatar);
        }

        void bind(@NonNull ChatMessage message, boolean showTimestampInitially, @Nullable String photoUrl) {
            messageText.setText(message.getText());
            timestamp.setText(formatTime(message.getTimestamp()));

            timestamp.setVisibility(showTimestampInitially ? View.VISIBLE : View.GONE);

            messageBubble.setOnClickListener(v -> timestamp.setVisibility(timestamp.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));

            if (!TextUtils.isEmpty(photoUrl)) {
                Glide.with(avatarView.getContext()).load(photoUrl).placeholder(R.drawable.welcome_person_2).into(avatarView);
            } else {
                avatarView.setImageResource(R.drawable.welcome_person_2);
            }
        }
    }

    private static String formatTime(long timestamp) {
        if (timestamp <= 0) {
            return "";
        }
        DateFormat format = DateFormat.getTimeInstance(DateFormat.SHORT);
        return format.format(new Date(timestamp));
    }
}
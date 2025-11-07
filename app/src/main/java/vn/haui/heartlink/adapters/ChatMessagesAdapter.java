package vn.haui.heartlink.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import vn.haui.heartlink.R;
import vn.haui.heartlink.models.ChatMessage;

/**
 * Adapter responsible for rendering chat messages with incoming and outgoing layouts.
 */
public class ChatMessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_OUTGOING = 1;
    private static final int TYPE_INCOMING = 2;

    private final List<ChatMessage> messages = new ArrayList<>();
    @Nullable
    private String currentUserId;
    @Nullable
    private String partnerPhotoUrl;

    public void setCurrentUserId(@Nullable String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setPartnerPhotoUrl(@Nullable String partnerPhotoUrl) {
        this.partnerPhotoUrl = partnerPhotoUrl;
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
        }
        View view = inflater.inflate(R.layout.item_chat_message_incoming, parent, false);
        return new IncomingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        if (holder instanceof OutgoingViewHolder) {
            ((OutgoingViewHolder) holder).bind(message);
        } else if (holder instanceof IncomingViewHolder) {
            ((IncomingViewHolder) holder).bind(message, partnerPhotoUrl);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class OutgoingViewHolder extends RecyclerView.ViewHolder {

        private final TextView messageText;
        private final TextView timestamp;

        OutgoingViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.chat_message_text);
            timestamp = itemView.findViewById(R.id.chat_message_time);
        }

        void bind(@NonNull ChatMessage message) {
            messageText.setText(message.getText());
            timestamp.setText(formatTime(message.getTimestamp()));
        }
    }

    static class IncomingViewHolder extends RecyclerView.ViewHolder {

        private final TextView messageText;
        private final TextView timestamp;
        private final ImageView avatarView;

        IncomingViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.chat_message_text);
            timestamp = itemView.findViewById(R.id.chat_message_time);
            avatarView = itemView.findViewById(R.id.chat_message_sender_avatar);
        }

        void bind(@NonNull ChatMessage message, @Nullable String photoUrl) {
            messageText.setText(message.getText());
            timestamp.setText(formatTime(message.getTimestamp()));

            if (!TextUtils.isEmpty(photoUrl)) {
                Glide.with(avatarView.getContext())
                        .load(photoUrl)
                        .placeholder(R.drawable.welcome_person_2)
                        .into(avatarView);
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

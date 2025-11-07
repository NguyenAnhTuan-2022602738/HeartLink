package vn.haui.heartlink.adapters;

import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.ArrayList;
import java.util.List;

import vn.haui.heartlink.R;

/**
 * Adapter showing the list of chat threads with last message previews.
 */
public class MessageThreadsAdapter extends RecyclerView.Adapter<MessageThreadsAdapter.ThreadViewHolder> {

    private final List<ThreadItem> items = new ArrayList<>();
    @NonNull
    private final OnThreadClickListener listener;

    public MessageThreadsAdapter(@NonNull OnThreadClickListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<ThreadItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ThreadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_thread, parent, false);
        return new ThreadViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ThreadViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public interface OnThreadClickListener {
        void onThreadClicked(@NonNull ThreadItem item);
    }

    public static class ThreadItem {
        private final String chatId;
        private final String partnerUid;
        private final String displayName;
        private final String photoUrl;
        private final String preview;
        private final long lastTimestamp;
        private final boolean hasUnread;

        public ThreadItem(@NonNull String chatId,
                          @NonNull String partnerUid,
                          @NonNull String displayName,
                          String photoUrl,
                          @NonNull String preview,
                          long lastTimestamp,
                          boolean hasUnread) {
            this.chatId = chatId;
            this.partnerUid = partnerUid;
            this.displayName = displayName;
            this.photoUrl = photoUrl;
            this.preview = preview;
            this.lastTimestamp = lastTimestamp;
            this.hasUnread = hasUnread;
        }

        @NonNull
        public String getChatId() {
            return chatId;
        }

        @NonNull
        public String getPartnerUid() {
            return partnerUid;
        }

        @NonNull
        public String getDisplayName() {
            return displayName;
        }

        public String getPhotoUrl() {
            return photoUrl;
        }

        @NonNull
        public String getPreview() {
            return preview;
        }

        public long getLastTimestamp() {
            return lastTimestamp;
        }

        public boolean hasUnread() {
            return hasUnread;
        }
    }

    static class ThreadViewHolder extends RecyclerView.ViewHolder {

        private final ImageView avatarView;
        private final TextView nameView;
        private final TextView previewView;
        private final TextView timestampView;
        private final TextView unreadBadgeView;
    private final View onlineDotView;
        @NonNull
        private final OnThreadClickListener listener;

        ThreadViewHolder(@NonNull View itemView,
                         @NonNull OnThreadClickListener listener) {
            super(itemView);
            this.listener = listener;
            avatarView = itemView.findViewById(R.id.thread_avatar);
            nameView = itemView.findViewById(R.id.thread_name);
            previewView = itemView.findViewById(R.id.thread_last_message);
            timestampView = itemView.findViewById(R.id.thread_timestamp);
            unreadBadgeView = itemView.findViewById(R.id.thread_unread_badge);
            onlineDotView = itemView.findViewById(R.id.thread_avatar_online_dot);
        }

        void bind(@NonNull ThreadItem item) {
            nameView.setText(item.getDisplayName());
            previewView.setText(item.getPreview());

            if (item.getLastTimestamp() > 0) {
                CharSequence relative = DateUtils.getRelativeTimeSpanString(
                        item.getLastTimestamp(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE);
                timestampView.setText(relative);
                timestampView.setVisibility(View.VISIBLE);
            } else {
                timestampView.setText(null);
                timestampView.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(item.getPhotoUrl())) {
                Glide.with(avatarView.getContext())
                        .load(item.getPhotoUrl())
                        .placeholder(R.drawable.welcome_person_2)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(avatarView);
            } else {
                avatarView.setImageResource(R.drawable.welcome_person_2);
            }

            if (item.hasUnread()) {
                unreadBadgeView.setText("1");
                unreadBadgeView.setVisibility(View.VISIBLE);
            } else {
                unreadBadgeView.setVisibility(View.GONE);
            }

            onlineDotView.setVisibility(View.GONE);

            itemView.setOnClickListener(v -> listener.onThreadClicked(item));
        }
    }
}

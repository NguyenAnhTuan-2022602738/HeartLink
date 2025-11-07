package vn.haui.heartlink.adapters;

import android.text.TextUtils;
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
 * Adapter displaying horizontally scrolling matched users considered "active" for messaging.
 */
public class ActiveUsersAdapter extends RecyclerView.Adapter<ActiveUsersAdapter.ActiveUserViewHolder> {

    private final List<PartnerItem> items = new ArrayList<>();
    @NonNull
    private final OnActiveUserClickListener listener;

    public ActiveUsersAdapter(@NonNull OnActiveUserClickListener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<PartnerItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ActiveUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_active_user, parent, false);
        return new ActiveUserViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ActiveUserViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public interface OnActiveUserClickListener {
        void onActiveUserClicked(@NonNull PartnerItem item);
    }

    public static class PartnerItem {
        private final String userId;
        private final String displayName;
        private final String photoUrl;

        public PartnerItem(@NonNull String userId,
                           @NonNull String displayName,
                           String photoUrl) {
            this.userId = userId;
            this.displayName = displayName;
            this.photoUrl = photoUrl;
        }

        @NonNull
        public String getUserId() {
            return userId;
        }

        @NonNull
        public String getDisplayName() {
            return displayName;
        }

        public String getPhotoUrl() {
            return photoUrl;
        }
    }

    static class ActiveUserViewHolder extends RecyclerView.ViewHolder {

        private final ImageView avatarView;
        private final TextView nameView;
        private final View onlineDotView;
        @NonNull
        private final OnActiveUserClickListener listener;

        ActiveUserViewHolder(@NonNull View itemView,
                             @NonNull OnActiveUserClickListener listener) {
            super(itemView);
            this.listener = listener;
            avatarView = itemView.findViewById(R.id.active_user_avatar);
            nameView = itemView.findViewById(R.id.active_user_name);
            onlineDotView = itemView.findViewById(R.id.active_user_online_dot);
        }

        void bind(@NonNull PartnerItem item) {
            nameView.setText(item.getDisplayName());

            if (!TextUtils.isEmpty(item.getPhotoUrl())) {
                Glide.with(avatarView.getContext())
                        .load(item.getPhotoUrl())
                        .placeholder(R.drawable.welcome_person_1)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(avatarView);
            } else {
                avatarView.setImageResource(R.drawable.welcome_person_1);
            }

            onlineDotView.setVisibility(View.GONE);

            itemView.setOnClickListener(v -> listener.onActiveUserClicked(item));
        }
    }
}

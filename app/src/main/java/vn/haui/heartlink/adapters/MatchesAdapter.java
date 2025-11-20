package vn.haui.heartlink.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import vn.haui.heartlink.R;

/**
 * RecyclerView adapter displaying grouped match cards with headers.
 */
public class MatchesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_CARD = 1;

    private final List<ListItem> items = new ArrayList<>();
    private final MatchActionListener listener;

    public MatchesAdapter(@NonNull MatchActionListener listener) {
        this.listener = listener;
    }

    public void submitItems(@NonNull List<? extends ListItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void removeCard(@NonNull String partnerUid) {
        Iterator<ListItem> iterator = items.iterator();
        boolean changed = false;
        while (iterator.hasNext()) {
            ListItem item = iterator.next();
            if (item.getItemViewType() == TYPE_CARD) {
                CardItem card = (CardItem) item;
                if (partnerUid.equals(card.getPartnerUid())) {
                    iterator.remove();
                    changed = true;
                }
            }
        }
        if (changed) {
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getItemViewType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_matches_section_header, parent, false);
            return new HeaderViewHolder(view);
        }
        View view = inflater.inflate(R.layout.item_match_card, parent, false);
        return new CardViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ListItem item = items.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((HeaderItem) item);
        } else if (holder instanceof CardViewHolder) {
            ((CardViewHolder) holder).bind((CardItem) item);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public interface MatchActionListener {
        void onCardTapped(@NonNull CardItem item);

        void onPrimaryAction(@NonNull CardItem item);

        void onSecondaryAction(@NonNull CardItem item);

        void onUnlikeAction(@NonNull CardItem item);
    }

    public abstract static class ListItem {
        abstract int getItemViewType();
    }

    public static class HeaderItem extends ListItem {
        private final String title;

        public HeaderItem(@NonNull String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }

        @Override
        int getItemViewType() {
            return TYPE_HEADER;
        }
    }

    public static class CardItem extends ListItem {
        private final String partnerUid;
        private final String displayName;
        private final String photoUrl;
        private final long timestamp;
        private final String statusLabel;
        private final InteractionState state;
        private final String interactionType;

        public CardItem(@NonNull String partnerUid,
                        @NonNull String displayName,
                        String photoUrl,
                        long timestamp,
                        String statusLabel,
                        @NonNull InteractionState state,
                        @Nullable String interactionType) {
            this.partnerUid = partnerUid;
            this.displayName = displayName;
            this.photoUrl = photoUrl;
            this.timestamp = timestamp;
            this.statusLabel = statusLabel;
            this.state = state;
            this.interactionType = interactionType;
        }

        public String getPartnerUid() {
            return partnerUid;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getPhotoUrl() {
            return photoUrl;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getStatusLabel() {
            return statusLabel;
        }

        @NonNull
        public InteractionState getState() {
            return state;
        }

        @Nullable
        public String getInteractionType() {
            return interactionType;
        }

        @Override
        int getItemViewType() {
            return TYPE_CARD;
        }
    }

    public enum InteractionState {
        MUTUAL_MATCH,
        INCOMING_LIKE,
        SENT_LIKE
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleView;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.matches_section_title);
        }

        void bind(@NonNull HeaderItem item) {
            titleView.setText(item.getTitle());
        }
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        private final ImageView photoView;
        private final TextView nameView;
        private final TextView statusView;
        private final FrameLayout dismissButton;
        private final FrameLayout openButton;
        private final ImageView primaryIcon;
        private final ImageView secondaryIcon;
        private final View actionDivider;
        private final MatchActionListener listener;

        CardViewHolder(@NonNull View itemView, @NonNull MatchActionListener listener) {
            super(itemView);
            this.listener = listener;
            photoView = itemView.findViewById(R.id.match_card_photo);
            nameView = itemView.findViewById(R.id.match_card_name);
            statusView = itemView.findViewById(R.id.match_card_status);
            dismissButton = itemView.findViewById(R.id.match_card_dismiss);
            openButton = itemView.findViewById(R.id.match_card_open);
            primaryIcon = itemView.findViewById(R.id.match_card_primary_icon);
            secondaryIcon = itemView.findViewById(R.id.match_card_secondary_icon);
            actionDivider = itemView.findViewById(R.id.match_card_action_divider);
        }

        void bind(@NonNull CardItem item) {
            nameView.setText(item.getDisplayName());

            if (!TextUtils.isEmpty(item.getStatusLabel())) {
                statusView.setText(item.getStatusLabel());
                statusView.setVisibility(View.VISIBLE);
            } else {
                statusView.setText(null);
                statusView.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(item.getPhotoUrl())) {
                Glide.with(photoView.getContext())
                        .load(item.getPhotoUrl())
                        .placeholder(R.drawable.welcome_person_1)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(photoView);
            } else {
                photoView.setImageResource(R.drawable.welcome_person_1);
            }

            configureActions(item);
        }

        private void configureActions(@NonNull CardItem item) {
            itemView.setOnClickListener(v -> listener.onCardTapped(item));

            switch (item.getState()) {
                case MUTUAL_MATCH:
                    dismissButton.setVisibility(View.GONE);
                    actionDivider.setVisibility(View.GONE);
                    openButton.setVisibility(View.VISIBLE);
                    primaryIcon.setImageResource(R.drawable.ic_home_nav_messages);
                    primaryIcon.setContentDescription(openButton.getContext().getString(R.string.matches_action_message));
                    openButton.setContentDescription(openButton.getContext().getString(R.string.matches_action_message));
                    openButton.setOnClickListener(v -> listener.onPrimaryAction(item));
                    break;
                case INCOMING_LIKE:
                    openButton.setVisibility(View.VISIBLE);
                    dismissButton.setVisibility(View.VISIBLE);
                    actionDivider.setVisibility(View.VISIBLE);
                    primaryIcon.setImageResource(R.drawable.ic_home_like);
                    primaryIcon.setContentDescription(openButton.getContext().getString(R.string.matches_action_like_back));
                    openButton.setContentDescription(openButton.getContext().getString(R.string.matches_action_like_back));
                    openButton.setOnClickListener(v -> listener.onPrimaryAction(item));
                    secondaryIcon.setImageResource(R.drawable.ic_home_dislike);
                    secondaryIcon.setContentDescription(dismissButton.getContext().getString(R.string.matches_action_skip));
                    dismissButton.setContentDescription(dismissButton.getContext().getString(R.string.matches_action_skip));
                    dismissButton.setOnClickListener(v -> listener.onSecondaryAction(item));
                    break;
                case SENT_LIKE:
                    openButton.setVisibility(View.GONE);
                    actionDivider.setVisibility(View.GONE);
                    dismissButton.setVisibility(View.VISIBLE);
                    secondaryIcon.setImageResource(R.drawable.ic_home_dislike);
                    secondaryIcon.setContentDescription(dismissButton.getContext().getString(R.string.matches_action_unlike));
                    dismissButton.setContentDescription(dismissButton.getContext().getString(R.string.matches_action_unlike));
                    dismissButton.setOnClickListener(v -> listener.onUnlikeAction(item));
                    break;
            }
        }
    }
}

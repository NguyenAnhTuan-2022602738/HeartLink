package vn.haui.heartlink.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import vn.haui.heartlink.R;
import vn.haui.heartlink.models.DiscoveryProfile;

/**
 * Adapter for the discovery card stack.
 */
public class DiscoveryCardAdapter extends RecyclerView.Adapter<DiscoveryCardAdapter.CardViewHolder> {

    private final List<DiscoveryProfile> items = new ArrayList<>();

    public void submitList(@NonNull List<DiscoveryProfile> profiles) {
        items.clear();
        items.addAll(profiles);
        notifyDataSetChanged();
    }

    public DiscoveryProfile getItem(int position) {
        if (position < 0 || position >= items.size()) {
            return null;
        }
        return items.get(position);
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_discovery_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        DiscoveryProfile profile = items.get(position);

        String distanceText = formatDistance(profile.getDistanceKm());
        holder.distanceText.setText(distanceText);
        holder.nameText.setText(profile.getDisplayName());
        holder.subtitleText.setText(getSeekingDisplayString(profile.getSubtitle()));

        if (profile.getPhotoUrl() != null && !profile.getPhotoUrl().isEmpty()) {
            Glide.with(holder.profileImage.getContext())
                    .load(profile.getPhotoUrl())
                    .placeholder(R.drawable.welcome_person_1)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(holder.profileImage);
        } else {
            holder.profileImage.setImageResource(R.drawable.welcome_person_1);
        }

    holder.likeOverlay.setAlpha(0f);
    holder.passOverlay.setAlpha(0f);
    holder.superlikeOverlay.setAlpha(0f);
    }

    private String getSeekingDisplayString(String seekingType) {
        if (seekingType == null) {
            return "";
        }
        switch (seekingType) {
            case "friend":
                return "Một người bạn";
            case "chat":
                return "Tìm người trò chuyện";
            case "no_strings":
                return "Không ràng buộc";
            case "later":
                return "Để sau";
            default:
                return seekingType;
        }
    }


    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder {
        final ImageView profileImage;
        final TextView distanceText;
        final TextView nameText;
        final TextView subtitleText;
        final FrameLayout likeOverlay;
        final FrameLayout passOverlay;
        final FrameLayout superlikeOverlay;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.card_profile_image);
            distanceText = itemView.findViewById(R.id.card_distance_text);
            nameText = itemView.findViewById(R.id.card_user_name);
            subtitleText = itemView.findViewById(R.id.card_user_interest);
            likeOverlay = itemView.findViewById(R.id.card_like_overlay);
            passOverlay = itemView.findViewById(R.id.card_pass_overlay);
            superlikeOverlay = itemView.findViewById(R.id.card_superlike_overlay);
        }
    }

    private String formatDistance(double distanceKm) {
        if (Double.isNaN(distanceKm) || Double.isInfinite(distanceKm)) {
            return "--";
        }
        if (distanceKm < 1) {
            return String.format(Locale.getDefault(), "%dm", Math.round(distanceKm * 1000));
        }
        return String.format(Locale.getDefault(), "%.0f km", distanceKm);
    }
}

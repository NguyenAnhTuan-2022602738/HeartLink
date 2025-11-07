package vn.haui.heartlink.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.ArrayList;
import java.util.List;

import vn.haui.heartlink.R;

/**
 * Simple adapter rendering the user's photo gallery in the profile detail screen.
 */
public class ProfilePhotoAdapter extends RecyclerView.Adapter<ProfilePhotoAdapter.PhotoViewHolder> {

    private final List<String> photoUrls = new ArrayList<>();

    public void submitPhotos(@NonNull List<String> urls) {
        photoUrls.clear();
        photoUrls.addAll(urls);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_profile_photo, parent, false);
        return new PhotoViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        String url = photoUrls.get(position);
        if (url == null || url.trim().isEmpty()) {
            holder.bind(null);
        } else {
            holder.bind(url);
        }
    }

    @Override
    public int getItemCount() {
        return photoUrls.size();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        private final ImageView photoView;

        PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            photoView = itemView.findViewById(R.id.profilePhotoImage);
        }

        void bind(String url) {
            if (url == null || url.isEmpty()) {
                photoView.setImageResource(R.drawable.welcome_person_1);
                return;
            }
            Glide.with(photoView.getContext())
                    .load(url)
                    .placeholder(R.drawable.welcome_person_1)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(photoView);
        }
    }
}

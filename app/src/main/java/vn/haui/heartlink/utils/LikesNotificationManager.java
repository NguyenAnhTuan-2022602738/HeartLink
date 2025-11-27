package vn.haui.heartlink.utils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

import vn.haui.heartlink.R;
import vn.haui.heartlink.activities.MainActivity;

/**
 * Helper for posting high-priority notifications when someone likes the current user.
 */
public final class LikesNotificationManager {

    private static final String CHANNEL_ID = "heartlink_incoming_likes";

    private LikesNotificationManager() {
        // Utility class
    }

    /**
     * Hiển thị notification khi có ai đó like user hiện tại.
     *
     * @param context Context của application
     * @param like Thông tin về like mới
     */
    public static void showLikeNotification(@NonNull Context context,
                                            @NonNull MatchRepository.IncomingLike like) {
        // Wrap context with user's preferred language
        context = LocaleHelper.wrapContext(context);
        
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        ensureChannel(context, manager);

        String displayName = !TextUtils.isEmpty(like.getDisplayName())
                ? like.getDisplayName()
                : context.getString(R.string.notification_incoming_like_unknown);
        String title = context.getString(R.string.notification_incoming_like_title);
        String message = context.getString(R.string.notification_incoming_like_message, displayName);

        Intent matchesIntent = new Intent(context, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntentWithParentStack(matchesIntent);

        int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = stackBuilder.getPendingIntent(
                buildNotificationId(like),
                pendingIntentFlags
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_home_nav_matches)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        // Kiểm tra permission trước khi hiển thị notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return; // Không có permission, không hiển thị notification
            }
        }

        if (manager.areNotificationsEnabled()) {
            manager.notify(buildNotificationId(like), builder.build());
        }
    }

    /**
     * Tạo unique notification ID dựa trên liker UID và timestamp.
     *
     * @param like Thông tin về like
     * @return Unique notification ID
     */
    private static int buildNotificationId(@NonNull MatchRepository.IncomingLike like) {
        String key = like.getLikerUid() + ":" + like.getLikedAt();
        return key.hashCode();
    }

    /**
     * Đảm bảo notification channel được tạo (cho Android O+).
     *
     * @param context Context của application
     * @param manager NotificationManagerCompat instance
     */
    private static void ensureChannel(@NonNull Context context,
                                      @NonNull NotificationManagerCompat manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_incoming_like_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(context.getString(R.string.notification_incoming_like_channel_description));
            manager.createNotificationChannel(channel);
        }
    }
}

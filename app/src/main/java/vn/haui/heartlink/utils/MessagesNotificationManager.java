package vn.haui.heartlink.utils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import vn.haui.heartlink.R;
import vn.haui.heartlink.activities.MainActivity;
import vn.haui.heartlink.utils.ChatRepository.NewMessage;

/**
 * Helper for posting notifications for new messages.
 */
public final class MessagesNotificationManager {

    private static final String CHANNEL_ID = "heartlink_new_messages";

    private MessagesNotificationManager() {
        // Utility class
    }

    /**
     * Shows a notification for a new incoming message.
     *
     * @param context The application context.
     * @param message The new message data.
     */
    public static void showMessageNotification(@NonNull Context context, @NonNull NewMessage message) {
        // Wrap context with user's preferred language
        context = LocaleHelper.wrapContext(context);
        
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        ensureChannel(context, manager);

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("NAVIGATE_TO", "MESSAGES"); // Navigate to messages tab
        intent.putExtra("CHAT_ID", message.getChatId());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                message.getChatId().hashCode(), // Unique request code for each chat
                intent,
                pendingIntentFlags
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_home_nav_messages)
                .setContentTitle(message.getSenderName())
                .setContentText(message.getText())
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        if (manager.areNotificationsEnabled()) {
            manager.notify(message.getChatId().hashCode(), builder.build());
        }
    }

    private static void ensureChannel(@NonNull Context context, @NonNull NotificationManagerCompat manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_new_message_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            manager.createNotificationChannel(channel);
        }
    }
}

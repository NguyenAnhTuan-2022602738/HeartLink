package vn.haui.heartlink;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.google.GoogleEmojiProvider;

import vn.haui.heartlink.utils.ChatRepository;
import vn.haui.heartlink.utils.LikesNotificationManager;
import vn.haui.heartlink.utils.MatchRepository;
import vn.haui.heartlink.utils.MessagesNotificationManager;

public class HeartLinkApplication extends Application implements SharedPreferences.OnSharedPreferenceChangeListener {

    private ChildEventListener likesListener;
    private ChildEventListener messagesListener;
    private String currentUserId;

    @Override
    public void onCreate() {
        super.onCreate();
        EmojiManager.install(new GoogleEmojiProvider());
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        setupNotificationListeners();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ("notifications_enabled".equals(key)) {
            setupNotificationListeners();
        }
    }

    private void setupNotificationListeners() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);
            if (notificationsEnabled) {
                startListeners(currentUser.getUid());
            } else {
                stopListeners();
            }
        } else {
            stopListeners();
        }
    }

    private void startListeners(String userId) {
        if (userId.equals(currentUserId)) {
            return; // Listeners are already running for this user
        }
        stopListeners(); // Stop any existing listeners
        currentUserId = userId;

        likesListener = MatchRepository.getInstance().listenForIncomingLikes(userId,
                like -> LikesNotificationManager.showLikeNotification(this, like));

        messagesListener = ChatRepository.getInstance().listenForNewMessages(userId,
                message -> MessagesNotificationManager.showMessageNotification(this, message));
    }

    private void stopListeners() {
        if (currentUserId != null) {
            if (likesListener != null) {
                MatchRepository.getInstance().removeIncomingLikeListener(currentUserId, likesListener);
            }
            if (messagesListener != null) {
                ChatRepository.getInstance().removeMessagesListener(currentUserId, messagesListener);
            }
        }
        likesListener = null;
        messagesListener = null;
        currentUserId = null;
    }
}

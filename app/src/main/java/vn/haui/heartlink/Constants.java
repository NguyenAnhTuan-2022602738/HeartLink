package vn.haui.heartlink;

/**
 * Central place to store Firebase collection names and intent extras to avoid typos.
 */
public final class Constants {

    private Constants() {
        // Utility class
    }

    public static final String USERS_NODE = "Users";
    public static final String MATCHES_NODE = "Matches";
    public static final String MATCH_INTERACTIONS_NODE = "MatchInteractions";
    public static final String CHATS_NODE = "Chats";
    public static final String REPORTS_NODE = "Reports";

    public static final String EXTRA_USER_ID = "extra_user_id";
    public static final String EXTRA_MATCH_ID = "extra_match_id";
    public static final String EXTRA_CHAT_ID = "extra_chat_id";
    public static final String EXTRA_MATCH_PARTNER_NAME = "extra_match_partner_name";
    public static final String EXTRA_MATCH_PARTNER_PHOTO_URL = "extra_match_partner_photo_url";
    public static final String EXTRA_MATCH_SELF_PHOTO_URL = "extra_match_self_photo_url";
}

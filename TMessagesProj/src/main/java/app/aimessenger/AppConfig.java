package app.aimessenger;

/**
 * Central configuration for the AI Messenger single-bot client.
 * All single-chat restrictions reference this class; do not hardcode
 * the bot username or chat id anywhere else.
 */
public final class AppConfig {

    public static final String APP_NAME = "Apex";

    // Fixed title shown in the bot chat's action bar (replaces the bot's real name).
    public static final String ASSISTANT_TITLE = "Apex";

    public static final boolean FORCE_SINGLE_CHAT_MODE = true;

    // Username of the only bot this client is allowed to talk to. No leading @.
    public static final String ALLOWED_BOT_USERNAME = "whateslewillitbe_bot";

    // Optional fallback. Positive user id of the bot. Use 0 to resolve by username.
    public static final long ALLOWED_CHAT_ID = 0L;

    // Send "/start" automatically the first time the bot chat is opened.
    public static final boolean AUTO_SEND_START_IF_NEW = true;


    // Remove every way to react to messages: the emoji strip above the
    // long-press menu and the double-tap quick-reaction gesture.
    public static final boolean DISABLE_REACTIONS = true;

    // Remove every way to forward messages out of the chat: long-press menu
    // entries and the multi-select action-mode button.
    public static final boolean HIDE_FORWARD = true;

    // Audio player sheet: hide "+ Add to Profile" and the three-dot options menu.
    public static final boolean MINIMAL_AUDIO_PLAYER = true;

    // Keep the bot-commands "Menu" pill permanently collapsed to icon-only.
    public static final boolean BOT_MENU_ICON_ONLY = true;

    private AppConfig() {
    }

    public static boolean isSingleChatModeEnabled() {
        return FORCE_SINGLE_CHAT_MODE;
    }
}

package app.aimessenger;

import android.content.Context;
import android.text.TextUtils;

import org.telegram.messenger.ApplicationLoader;

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

    // Compiled-in default bot username (no leading @). Used until an admin
    // overrides it at runtime via a signed ADB broadcast (see AdminConfigReceiver).
    // Always read the effective value through getAllowedBotUsername().
    public static final String ALLOWED_BOT_USERNAME = "whateslewillitbe_bot";

    // --- Runtime admin override (signed ADB broadcast) ---------------------
    // SharedPreferences file holding the admin-set bot + replay nonce.
    public static final String ADMIN_PREFS = "apex_admin";
    public static final String KEY_ADMIN_BOT = "allowed_bot";
    public static final String KEY_ADMIN_NONCE = "bot_nonce";

    // RSA-2048 public key (X.509 SubjectPublicKeyInfo, Base64). The matching
    // PRIVATE key is held only by the admin and is NOT in this repo; it is used
    // to sign "<username>|<nonce>" so only the admin can change the bound bot.
    public static final String ADMIN_PUBLIC_KEY_B64 =
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArLtLXiiaLIvrw3dP6292oHiVqs05+/4FyqmONCwbLB1AC6mp33wkzUkVQSNNR6xvSqdwDQVc5heCcrh17aoyawkbEhytyilwNTGspHgs5wxGhfSKRXOYpcg6rfFtNpYVSr9L35TNP0PZwlWdAUPj4hDEL/bHIhwZsWmiK3UVtIl+P/Zsq5L34UE/+Aqo+Fr5yjeIfUDoF8MisEUuUCny7q7jiDAmWH/4BCpvMs6pOt6ytkorj1cWSjn6lKCeKV4DojtNna0tVdRmajvNdQt8ZayPcTMPa2O7LhXhw4GIAyGQVbR16LMScS1WhNZy9T/LFf31wyqxu0QSad+zXRKshwIDAQAB";

    /**
     * The effective allowed-bot username: the admin-set value if present and
     * valid, otherwise the compiled-in default. Never returns null/empty.
     */
    public static String getAllowedBotUsername() {
        try {
            Context ctx = ApplicationLoader.applicationContext;
            if (ctx != null) {
                String v = ctx.getSharedPreferences(ADMIN_PREFS, Context.MODE_PRIVATE)
                        .getString(KEY_ADMIN_BOT, null);
                if (!TextUtils.isEmpty(v)) {
                    return v;
                }
            }
        } catch (Throwable ignore) {
        }
        return ALLOWED_BOT_USERNAME;
    }

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

    // Draw all message bubbles as uniform rounded cards with no tail.
    public static final boolean UNIFORM_BUBBLES = true;

    private AppConfig() {
    }

    public static boolean isSingleChatModeEnabled() {
        return FORCE_SINGLE_CHAT_MODE;
    }
}

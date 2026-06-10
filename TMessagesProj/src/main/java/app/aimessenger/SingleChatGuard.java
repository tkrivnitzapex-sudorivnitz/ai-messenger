package app.aimessenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.CallLogActivity;
import org.telegram.ui.ChannelCreateActivity;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.ContactsActivity;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.GroupCreateActivity;
import org.telegram.ui.GroupCreateFinalActivity;
import org.telegram.ui.MainTabsActivity;
import org.telegram.ui.ProfileActivity;
import org.telegram.ui.SettingsActivity;
import org.telegram.ui.TopicsFragment;

import java.util.ArrayList;
import java.util.function.LongConsumer;

/**
 * Central enforcement for single-bot mode. Every chat-opening navigation,
 * dialog list, deep link and outgoing message is checked here, against
 * the bot configured in {@link AppConfig}.
 */
public final class SingleChatGuard {

    private static final String PREFS = "aimessenger";

    private SingleChatGuard() {
    }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Restrictions apply only when the mode is on and the user is logged in. */
    public static boolean isActive() {
        return AppConfig.isSingleChatModeEnabled()
                && UserConfig.getInstance(UserConfig.selectedAccount).isClientActivated();
    }

    /** The bot's dialog id, from config or from the cached username resolution. 0 if unknown yet. */
    public static long getAllowedBotId(int account) {
        if (AppConfig.ALLOWED_CHAT_ID != 0) {
            return AppConfig.ALLOWED_CHAT_ID;
        }
        return prefs().getLong("bot_id_" + account + "_" + AppConfig.getAllowedBotUsername().toLowerCase(), 0);
    }

    public static void setResolvedBotId(int account, long botId) {
        prefs().edit().putLong("bot_id_" + account + "_" + AppConfig.getAllowedBotUsername().toLowerCase(), botId).apply();
    }

    public static boolean isAllowedDialog(int account, long dialogId) {
        if (dialogId == 0) {
            return false;
        }
        long allowed = getAllowedBotId(account);
        if (allowed != 0) {
            return dialogId == allowed;
        }
        if (dialogId > 0) {
            TLRPC.User user = MessagesController.getInstance(account).getUser(dialogId);
            if (user != null && hasAllowedUsername(user)) {
                setResolvedBotId(account, dialogId);
                return true;
            }
        }
        return false;
    }

    private static boolean hasAllowedUsername(TLRPC.User user) {
        String allowed = AppConfig.getAllowedBotUsername();
        if (user.username != null && user.username.equalsIgnoreCase(allowed)) {
            return true;
        }
        if (user.usernames != null) {
            for (int i = 0; i < user.usernames.size(); i++) {
                TLRPC.TL_username u = user.usernames.get(i);
                if (u != null && u.username != null && u.username.equalsIgnoreCase(allowed)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isAllowedDeepLink(String username) {
        return !TextUtils.isEmpty(username) && username.equalsIgnoreCase(AppConfig.getAllowedBotUsername());
    }

    /** Keeps only the allowed bot's dialog in any dialog list shown by the UI. */
    public static ArrayList<TLRPC.Dialog> filterDialogs(int account, ArrayList<TLRPC.Dialog> dialogs) {
        if (!isActive() || dialogs == null || dialogs.isEmpty()) {
            return dialogs;
        }
        ArrayList<TLRPC.Dialog> filtered = new ArrayList<>(1);
        for (int i = 0; i < dialogs.size(); i++) {
            TLRPC.Dialog dialog = dialogs.get(i);
            if (dialog != null && isAllowedDialog(account, dialog.id)) {
                filtered.add(dialog);
            }
        }
        return filtered;
    }

    /**
     * Navigation gatekeeper, called from ActionBarLayout for every fragment
     * that is presented or added to the stack.
     *
     * @return the fragment to actually navigate to: the original one if allowed,
     * a substitute (bot chat or gate) when redirecting, or null to cancel.
     */
    public static BaseFragment checkFragment(BaseFragment fragment) {
        if (fragment == null || !isActive()) {
            return fragment;
        }
        int account = fragment.getCurrentAccount();
        if (fragment instanceof MainTabsActivity) {
            // The normal "home" of the app: replace with the bot chat.
            return homeFragment(account);
        }
        if (fragment instanceof DialogsActivity) {
            Bundle args = fragment.getArguments();
            boolean onlySelect = args != null && args.getBoolean("onlySelect", false);
            if (onlySelect) {
                // Share/forward picker; its content is filtered to the bot only.
                return fragment;
            }
            return homeFragment(account);
        }
        if (fragment instanceof ChatActivity) {
            long dialogId = dialogIdFromArgs(fragment.getArguments());
            if (isAllowedDialog(account, dialogId)) {
                return fragment;
            }
            showBlockedToast();
            return redirectToBot(account);
        }
        if (fragment instanceof TopicsFragment) {
            showBlockedToast();
            return redirectToBot(account);
        }
        if (fragment instanceof ProfileActivity) {
            // Allow only the bot's own profile-less chat; block self-profile (== settings) too.
            Bundle args = fragment.getArguments();
            long userId = args != null ? args.getLong("user_id", 0) : 0;
            long chatId = args != null ? args.getLong("chat_id", 0) : 0;
            if (chatId == 0 && userId != 0 && isAllowedDialog(account, userId)) {
                return fragment;
            }
            showBlockedToast();
            return null;
        }
        if (fragment instanceof SettingsActivity
                || fragment instanceof ContactsActivity
                || fragment instanceof CallLogActivity
                || fragment instanceof GroupCreateActivity
                || fragment instanceof GroupCreateFinalActivity
                || fragment instanceof ChannelCreateActivity) {
            showBlockedToast();
            return null;
        }
        return fragment;
    }

    private static long dialogIdFromArgs(Bundle args) {
        if (args == null) {
            return 0;
        }
        long userId = args.getLong("user_id", 0);
        if (userId != 0) {
            return userId;
        }
        long chatId = args.getLong("chat_id", 0);
        if (chatId != 0) {
            return -chatId;
        }
        if (args.getInt("enc_id", 0) != 0) {
            return Long.MIN_VALUE; // secret chats are never the bot
        }
        return 0;
    }

    /** The fragment that replaces the chats list / main tabs as the app's home. */
    public static BaseFragment homeFragment(int account) {
        long botId = getAllowedBotId(account);
        if (botId != 0 && peerLoaded(account, botId)) {
            return botChatFragment(botId);
        }
        return new SingleChatGateFragment();
    }

    /** Redirect target when a blocked chat was about to open: the bot chat, unless one is needed to resolve first. */
    private static BaseFragment redirectToBot(int account) {
        long botId = getAllowedBotId(account);
        if (botId != 0 && peerLoaded(account, botId)) {
            return botChatFragment(botId);
        }
        return new SingleChatGateFragment();
    }

    private static boolean peerLoaded(int account, long botId) {
        if (botId > 0) {
            return MessagesController.getInstance(account).getUser(botId) != null;
        }
        return MessagesController.getInstance(account).getChat(-botId) != null;
    }

    public static BaseFragment botChatFragment(long botId) {
        Bundle args = new Bundle();
        if (botId > 0) {
            args.putLong("user_id", botId);
        } else {
            args.putLong("chat_id", -botId);
        }
        return new ChatActivity(args);
    }

    /**
     * Resolves the allowed bot to a dialog id, by configured id or username.
     * Calls back with 0 on failure. Callback runs on the UI thread.
     */
    public static void resolveAllowedBot(int account, LongConsumer callback) {
        long configured = getAllowedBotId(account);
        if (configured != 0 && peerLoaded(account, configured)) {
            callback.accept(configured);
            return;
        }
        if (TextUtils.isEmpty(AppConfig.getAllowedBotUsername())) {
            callback.accept(0);
            return;
        }
        MessagesController.getInstance(account).getUserNameResolver().resolve(AppConfig.getAllowedBotUsername(), peerId -> {
            if (peerId == null || peerId == 0) {
                callback.accept(0);
            } else {
                setResolvedBotId(account, peerId);
                callback.accept(peerId);
            }
        });
    }

    /** Sends "/start" once if the bot conversation does not exist yet. */
    public static void maybeAutoStart(int account, long botId) {
        if (!AppConfig.AUTO_SEND_START_IF_NEW || botId == 0) {
            return;
        }
        String key = "start_sent_" + account + "_" + botId;
        if (prefs().getBoolean(key, false)) {
            return;
        }
        if (MessagesController.getInstance(account).getDialog(botId) != null) {
            prefs().edit().putBoolean(key, true).apply();
            return;
        }
        prefs().edit().putBoolean(key, true).apply();
        SendMessagesHelper.getInstance(account).sendMessage(SendMessagesHelper.SendMessageParams.of("/start", botId));
    }

    /** Data-level enforcement: outgoing messages may only target the allowed bot. */
    public static boolean canSendTo(int account, long dialogId) {
        if (!isActive()) {
            return true;
        }
        if (isAllowedDialog(account, dialogId)) {
            return true;
        }
        showBlockedToast();
        return false;
    }

    public static void showBlockedToast() {
        AndroidUtilities.runOnUIThread(() -> {
            try {
                Toast.makeText(ApplicationLoader.applicationContext,
                        "Only the " + AppConfig.APP_NAME + " bot chat is available in this app", Toast.LENGTH_SHORT).show();
            } catch (Exception ignored) {
            }
        });
    }

    public static void showResolveFailedToast() {
        AndroidUtilities.runOnUIThread(() -> {
            try {
                Toast.makeText(ApplicationLoader.applicationContext,
                        "Unable to open AI bot. Please check the configured bot username.", Toast.LENGTH_LONG).show();
            } catch (Exception ignored) {
            }
        });
    }
}

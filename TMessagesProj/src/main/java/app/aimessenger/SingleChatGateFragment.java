package app.aimessenger;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RadialProgressView;

/**
 * Shown instead of the chats list while the allowed bot is being resolved.
 * On success it replaces itself with the bot's ChatActivity as the root
 * fragment; on failure it shows an error with a retry button.
 */
public class SingleChatGateFragment extends BaseFragment {

    private RadialProgressView progressView;
    private TextView statusText;
    private TextView retryButton;
    private boolean resolving;

    @Override
    public View createView(Context context) {
        actionBar.setTitle(AppConfig.APP_NAME);
        actionBar.setCastShadows(false);

        FrameLayout frame = new FrameLayout(context);
        frame.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        progressView = new RadialProgressView(context);
        progressView.setSize(AndroidUtilities.dp(32));
        layout.addView(progressView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 16));

        statusText = new TextView(context);
        statusText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        statusText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        statusText.setGravity(Gravity.CENTER);
        statusText.setText("Connecting to your AI bot…");
        layout.addView(statusText, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 32, 0, 32, 0));

        retryButton = new TextView(context);
        retryButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        retryButton.setTypeface(AndroidUtilities.bold());
        retryButton.setTextColor(Theme.getColor(Theme.key_featuredStickers_addButton));
        retryButton.setGravity(Gravity.CENTER);
        retryButton.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(8));
        retryButton.setText("Retry");
        retryButton.setVisibility(View.GONE);
        retryButton.setOnClickListener(v -> startResolving());
        layout.addView(retryButton, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 16, 0, 0));

        frame.addView(layout, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        fragmentView = frame;
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        startResolving();
    }

    private void startResolving() {
        if (resolving) {
            return;
        }
        resolving = true;
        if (progressView != null) {
            progressView.setVisibility(View.VISIBLE);
            statusText.setText("Connecting to your AI bot…");
            retryButton.setVisibility(View.GONE);
        }
        SingleChatGuard.resolveAllowedBot(currentAccount, botId -> AndroidUtilities.runOnUIThread(() -> {
            resolving = false;
            if (botId == 0) {
                showError();
            } else {
                openBotChat(botId);
            }
        }));
    }

    private void openBotChat(long botId) {
        if (getParentLayout() == null) {
            return;
        }
        presentFragment(SingleChatGuard.botChatFragment(botId), true);
        SingleChatGuard.maybeAutoStart(currentAccount, botId);
    }

    private void showError() {
        SingleChatGuard.showResolveFailedToast();
        if (progressView != null) {
            progressView.setVisibility(View.GONE);
            statusText.setText("Unable to open AI bot.\nPlease check the configured bot username.");
            retryButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean isSwipeBackEnabled(android.view.MotionEvent event) {
        return false;
    }
}

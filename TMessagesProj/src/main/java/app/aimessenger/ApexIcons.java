package app.aimessenger;

import org.telegram.messenger.R;

/** Maps stock Telegram context-menu icons to the Apex stroke set. */
public final class ApexIcons {

    private ApexIcons() {
    }

    public static int remap(int icon) {
        if (icon == R.drawable.menu_reply) return R.drawable.apex_menu_reply;
        if (icon == R.drawable.msg_copy) return R.drawable.apex_menu_copy;
        if (icon == R.drawable.msg_delete) return R.drawable.apex_menu_delete;
        if (icon == R.drawable.msg_select) return R.drawable.apex_menu_select;
        if (icon == R.drawable.msg_pin || icon == R.drawable.msg_unpin) return R.drawable.apex_menu_pin;
        if (icon == R.drawable.msg_edit) return R.drawable.apex_menu_edit;
        if (icon == R.drawable.msg_download || icon == R.drawable.msg_gallery) return R.drawable.apex_menu_save;
        return icon;
    }
}

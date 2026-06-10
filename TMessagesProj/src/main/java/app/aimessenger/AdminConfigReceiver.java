package app.aimessenger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Base64;

import org.telegram.messenger.FileLog;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

/**
 * Receives a signed ADB broadcast that rebinds this install to a different
 * single bot, without recompiling. Only the holder of the admin PRIVATE key
 * (which is never shipped in the APK) can produce a valid signature, so the
 * binding cannot be changed by an attacker who only has the APK or device.
 *
 * <p>The signed payload is {@code "<username>|<nonce>"} where {@code nonce}
 * is a strictly increasing integer (a Unix timestamp works). The nonce must
 * exceed the last accepted nonce, which prevents replaying an older command
 * to roll the binding back to a previously authorised bot.
 *
 * <p>Usage (private key on the admin's machine; helper script in tools/):
 * <pre>
 *   adb shell am broadcast -n com.example.aimessenger/app.aimessenger.AdminConfigReceiver \
 *     -a app.aimessenger.SET_BOT \
 *     --es bot some_bot --es nonce 1733865600 --es sig "&lt;base64-signature&gt;"
 * </pre>
 * The receiver reports the outcome via the broadcast result so it shows up in
 * the {@code am broadcast} output (e.g. {@code data="OK: bot set to some_bot"}).
 */
public class AdminConfigReceiver extends BroadcastReceiver {

    public static final String ACTION_SET_BOT = "app.aimessenger.SET_BOT";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null || !ACTION_SET_BOT.equals(intent.getAction())) {
            return;
        }

        String bot = intent.getStringExtra("bot");
        String sig = intent.getStringExtra("sig");
        String nonceStr = intent.getStringExtra("nonce");

        if (TextUtils.isEmpty(bot) || TextUtils.isEmpty(sig) || TextUtils.isEmpty(nonceStr)) {
            reject(context, "REJECTED: missing bot/nonce/sig");
            return;
        }

        long nonce;
        try {
            nonce = Long.parseLong(nonceStr.trim());
        } catch (Exception e) {
            reject(context, "REJECTED: bad nonce");
            return;
        }
        if (nonce <= 0) {
            reject(context, "REJECTED: bad nonce");
            return;
        }

        // Normalise the username the same way it will be stored/compared.
        bot = bot.trim();
        if (bot.startsWith("@")) {
            bot = bot.substring(1);
        }
        bot = bot.toLowerCase();
        if (TextUtils.isEmpty(bot)) {
            reject(context, "REJECTED: empty bot");
            return;
        }

        final String payload = bot + "|" + nonce;
        if (!verify(payload, sig)) {
            reject(context, "REJECTED: bad signature");
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(AppConfig.ADMIN_PREFS, Context.MODE_PRIVATE);
        long lastNonce = prefs.getLong(AppConfig.KEY_ADMIN_NONCE, 0);
        if (nonce <= lastNonce) {
            reject(context, "REJECTED: stale nonce (replay)");
            return;
        }

        prefs.edit()
                .putString(AppConfig.KEY_ADMIN_BOT, bot)
                .putLong(AppConfig.KEY_ADMIN_NONCE, nonce)
                .apply();

        try {
            FileLog.d("AdminConfigReceiver: bound bot set to " + bot + " (nonce " + nonce + ")");
        } catch (Throwable ignore) {
        }

        try {
            setResultCode(1);
            setResultData("OK: bot set to " + bot + " — restart the app to apply");
        } catch (Throwable ignore) {
        }
    }

    private void reject(Context context, String reason) {
        try {
            FileLog.d("AdminConfigReceiver: " + reason);
        } catch (Throwable ignore) {
        }
        try {
            setResultCode(0);
            setResultData(reason);
        } catch (Throwable ignore) {
        }
    }

    private boolean verify(String payload, String sigB64) {
        try {
            byte[] keyBytes = Base64.decode(AppConfig.ADMIN_PUBLIC_KEY_B64, Base64.DEFAULT);
            PublicKey pub = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(keyBytes));
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(pub);
            signature.update(payload.getBytes(StandardCharsets.UTF_8));
            byte[] sigBytes = Base64.decode(sigB64, Base64.DEFAULT);
            return signature.verify(sigBytes);
        } catch (Throwable t) {
            return false;
        }
    }
}

# Apex — rebinding the single bot at runtime (admin only)

Apex normally talks to one hard-coded bot (`AppConfig.ALLOWED_BOT_USERNAME`).
You can rebind a specific install to a *different* bot **without recompiling**,
using a cryptographically signed ADB broadcast. This lets you ship the same APK
to several users and point each install at its own bot.

## Why it is secure

- The APK contains only the **RSA-2048 public key** (`AppConfig.ADMIN_PUBLIC_KEY_B64`).
- The matching **private key is never in the repo or the APK**. It lives only on
  the admin's machine (default `~/android_dev/telegram_app/apex-admin-private-key.pem`).
- A command is the string `"<username>|<nonce>"` signed with the private key
  (`SHA256withRSA`). The app verifies the signature with the public key, so an
  attacker who only has the APK or the device cannot forge a valid command.
- The `nonce` must strictly increase. An old captured command cannot be replayed
  to roll the binding back to a previously authorised bot.

## How to rebind

```bash
# 1) Generate the command (signs with your private key):
docs/admin/apex-sign-bot.sh some_other_bot

# 2) Run the printed line (or pipe straight to a shell):
docs/admin/apex-sign-bot.sh some_other_bot | sh
```

The receiver answers through the broadcast result, e.g.:

```
Broadcast completed: result=1, data="OK: bot set to some_other_bot — restart the app to apply"
```

Rejections (`result=0`) include `bad signature`, `stale nonce (replay)`, and
`missing bot/nonce/sig`. **Restart the app** after a successful rebind so the
new bot is resolved.

## Rotating the key

1. `openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out apex-admin-private-key.pem`
2. `openssl rsa -in apex-admin-private-key.pem -pubout -outform DER | base64 -w0`
3. Paste the output into `AppConfig.ADMIN_PUBLIC_KEY_B64`, rebuild, redistribute.

Keep the private key offline and backed up. Anyone with it can rebind any
install that trusts the matching public key.

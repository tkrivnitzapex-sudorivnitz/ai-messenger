# AI Messenger (Nekogram fork)

A single-bot Telegram client based on [Nekogram](https://github.com/Nekogram/Nekogram).
After normal Telegram login, the app only allows conversation with one configured bot.
All other chats are hidden and all navigation to them is blocked and redirected back to the bot.

## Configuration

Everything is driven by one file:

`TMessagesProj/src/main/java/app/aimessenger/AppConfig.java`

| Constant | Meaning |
|---|---|
| `APP_NAME` | Display name used in toasts/gate screen |
| `FORCE_SINGLE_CHAT_MODE` | Master switch. `false` restores normal Nekogram behavior |
| `ALLOWED_BOT_USERNAME` | The only bot the user may talk to (no `@`) |
| `ALLOWED_CHAT_ID` | Optional numeric fallback (positive bot user id). `0` = resolve by username |
| `AUTO_SEND_START_IF_NEW` | Send `/start` once when the bot chat does not exist yet |

To change the bot: edit `ALLOWED_BOT_USERNAME`, rebuild, reinstall. Cached
resolution is stored per-username in shared prefs, so changing the username
automatically re-resolves.

### Rebinding the bot at runtime (no rebuild)

You can point an already-installed app at a different bot with a cryptographically
signed ADB command — useful when shipping the same APK to several users. Only the
holder of the admin private key (never shipped in the APK) can authorise a change:

```
docs/admin/apex-sign-bot.sh <bot_username> | sh   # signs + runs the adb broadcast
```

Full details, security model, and key rotation are in
[`docs/admin/README.md`](docs/admin/README.md).

## How enforcement works

- `app/aimessenger/SingleChatGuard.java` — central policy:
  - `checkFragment(...)` is called from `ActionBarLayout.presentFragment(...)` and
    `ActionBarLayout.addFragmentToStack(...)` (the only two ways any screen opens).
    The chats list / main tabs are replaced with the bot chat (or the resolve gate),
    chats/profiles other than the bot are cancelled or redirected, and
    contacts / calls / group- and channel-creation screens are blocked.
  - `filterDialogs(...)` is applied in `DialogsActivity.getDialogsArray(...)`, so any
    dialog list that still renders (e.g. the share/forward picker) only contains the bot.
  - `canSendTo(...)` is enforced in `SendMessagesHelper` (both new messages and
    forwards), so messages can only ever be sent to the bot.
  - Notifications for other chats are dropped in `NotificationsController.processNewMessages`.
- `app/aimessenger/SingleChatGateFragment.java` — root screen shown while the bot
  username is resolved; replaces itself with the bot `ChatActivity`, sends `/start`
  once if the conversation is new, shows error + Retry if resolution fails.
- Deep links: `LaunchActivity.runLinkRequest(...)` rejects every `t.me`/`tg://` link
  that is not the allowed bot.
- Settings (incl. Logout) are reachable from the bot chat: top-right menu → Settings.

No data is deleted or modified server-side; other chats are only hidden/blocked in this client.

## Build

Requirements (installed automatically by CI/dev setup): JDK 21 (full JDK),
Android SDK platform 36, build-tools 36.1.0, NDK 27.3.13750724, CMake 4.0+.

`local.properties` (not committed) must contain:

```
sdk.dir=/path/to/Android/Sdk
apiId=<your api id from https://my.telegram.org>
apiHash=<your api hash>
sentryDsn=
mapsApiKey=
```

The repo currently builds with the public Telegram test credentials (apiId=4).
Replace them with your own for production use — the test pair is shared and
heavily rate-limited.

`TMessagesProj_App/google-services.json` is a placeholder (package
`com.example.aimessenger`). Push notifications via FCM require a real Firebase
project; replace this file with the real one if you need push.

### CLI

```
./gradlew :TMessagesProj_App:assembleDebug
```

APK output: `TMessagesProj_App/build/outputs/apk/debug/`

Note: the debug build is currently restricted to the `arm64-v8a` ABI for build
speed (see `splits.abi` in `TMessagesProj_App/build.gradle`; the original ABI
list is in a comment on the same line).

### Android Studio

Open (do not import) the project root, let it sync, then run the
`TMessagesProj_App` configuration or Build → Build APK(s).

## Branding

- App label: `AppNameNeko` in `TMessagesProj/src/main/res/values/strings_neko.xml`
- Application id: `APP_PACKAGE` in `gradle.properties` (currently `com.example.aimessenger`)
- Launcher icon: `@mipmap/ic_launcher` in `TMessagesProj/src/main/res/mipmap-*`
  (still the Nekogram icon as placeholder — replace those files to rebrand)

## License

This fork remains under GPL v2+ (see LICENSE), as required by Telegram/Nekogram
licensing. If you distribute the APK, you must make the modified source available.

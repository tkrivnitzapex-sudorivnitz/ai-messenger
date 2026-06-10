# Apex Round 2 — Design

Five workstreams: rename, attach-button bug fix, long-press menu trim, Clean Light menu restyle, and the bot "Menu" button. All toggles follow the existing `AppConfig` gate pattern.

## 1. Rename everything to Apex

| Location | Now | Becomes |
|---|---|---|
| Launcher label — `AppNameNeko` in `values/strings_neko.xml` **and** the debug-variant override | AI Messenger | **Apex** |
| `AppConfig.APP_NAME` | AI Messenger | **Apex** |
| Chat header — `AppConfig.ASSISTANT_TITLE` | Apex Assistant | **Apex** |
| Intro screen text | "**AI Messenger** — your private AI chat, powered by Telegram." | "**Apex** — your private AI assistant." |

Plus a sweep for any other user-visible "AI Messenger" / leftover "Nekogram" strings on screens you can actually reach (intro, settings header). Package id stays `com.example.aimessenger` — invisible to you, and changing it would force a re-login.

## 2. Attach button jump fix

Root cause (verified in code): when you start typing, Telegram animates the attach 📎 button away (shrink + fade) to make room for the send button. Our emoji-button removal changed the input bar's layout math, and the attach button has no scaling anchor (pivot), so during the animation it visually slides up/out toward the action bar instead of shrinking in place.

**Fix:** anchor the button (explicit pivot + right margin consistent with its container) so it shrinks in place, in `ChatActivityEnterView.java`. I'll reproduce the glitch on the emulator first, apply the fix, and re-screenshot to confirm — the exact one-liner depends on what the reproduction shows, but it's contained to this one animation path.

## 3. Long-press message menu — content

- **Reactions: completely gone.** The emoji strip above the popup is suppressed, and the double-tap-to-react gesture is disabled too (otherwise reactions sneak back in through the back door).
- **Forward: gone** — from the popup *and* from the multi-select bottom bar, so there's no second path.
- **Everything else stays**: Reply, Copy, Delete, Select, Pin, Edit, Save media, etc. — untouched.
- Implemented as two new `AppConfig` flags (`DISABLE_REACTIONS`, `HIDE_FORWARD`) checked in `ChatActivity`, same pattern as the existing single-bot lockdowns.

## 4. Long-press message menu — Clean Light restyle + simple icons

Goal: the popup should read as Apex's own menu, not Telegram's.

- **Card**: flat white, 16dp rounded corners, hairline `#E5E7EB` border, soft single shadow — no blur, no translucency.
- **Rows**: slightly taller (~46dp), text `#111827`, pressed state a flat `#F3F4F6`.
- **Icons**: replaced with a minimal stroke-style set (simple 1.5dp-line vector glyphs, single color `#3B82F6` blue) for the items that actually appear in your bot chat: Reply, Copy, Delete, Select, Pin, Edit, Save. Think thin-outline icons, no filled Telegram glyphs.
- The dimmed-background + message-highlight behavior stays (it's functional), but without Telegram's blur effect, consistent with the Clean Light theme.

## 5. Bot "Menu" button in the message bar

That pill on the left ( ☰ Menu ) is Telegram's bot-commands button. My call, since you left it open: **keep the function, lose the Telegram look** — collapse it permanently to icon-only (the view already supports this natively), drop the "Menu" text, and restyle it as a small flat Clean Light blue glyph. It still opens the bot's command list when tapped.

**Decision (Jacob, 2026-06-10): icon-only restyle confirmed.**

## 6. App-wide custom font: Inter

Swapping off Roboto is the single biggest "different app" signal. Approach:

- Bundle Inter (Regular, Medium, SemiBold, Italic) TTFs under `assets/fonts/`.
- Telegram resolves most custom typefaces through `AndroidUtilities.getTypeface(...)` — patch that one resolver to map the Roboto names (`rmedium`, `rcondensedbold`, italic, mono stays mono) to the Inter equivalents.
- Regular body text mostly uses the system default typeface rather than the resolver; route the visible surfaces (message cells, action bar/title, input field, popup menus, intro) through the same helper so they render Inter too.
- Acceptance: all primary surfaces you actually see (chat, header, long-press menu, input bar, intro) are Inter. Deep Nekogram settings screens may lag behind — acceptable, they're locked away anyway.

## 7. Message bubbles: no tails, uniform rounded cards

- Remove the bubble tail from `Theme.MessageDrawable` for both incoming and outgoing messages — every bubble becomes a uniform rounded card, ChatGPT-style.
- Uniform corner radius ~18dp on all four corners (reusing the existing `SharedConfig.bubbleRadius` mechanism where possible), including in grouped/consecutive messages — no more small "joined corner" variations.
- Colors stay as the Clean Light theme already defines them.

## Verification

Build the debug APK, install on the `testavd` emulator, and screenshot-verify each change: launcher name, chat header, typing animation (attach button), long-press menu (no reactions strip, no Forward, new style/icons), the collapsed Menu button, Inter rendering on chat + header + menu, and tail-less uniform bubbles.

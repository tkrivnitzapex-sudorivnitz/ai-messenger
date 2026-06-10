# AI Messenger — Clean Light Redesign

Date: 2026-06-09 · Status: approved by Jacob (visual companion session)

## Goal

Make the app feel like a simple, modern messaging app rather than Telegram: flat
colors, no glassy/blur effects, simple background, no "bot" labeling. UI/UX
structure stays identical — this is a re-skin plus small chrome removals.

## Decisions (user-approved)

1. **Profile status**: the "bot" subtitle under "Apex Assistant" in
   `ProfileActivity` is replaced with the normal "online" status text.
2. **No glass**: chat blur and liquid glass are force-disabled in code
   (`SharedConfig.chatBlurEnabled()` → false, `FLAG_CHAT_BLUR` /
   `FLAG_LIQUID_GLASS` off). Headers render as flat solid surfaces.
3. **Colors — "Clean Light" (style A)**:
   - Default chat background: solid `#F3F4F6` (replaces 4-color gradient +
     doodle pattern in `Theme.createDefaultWallpaper`).
   - Surfaces: white; hairline `#E5E7EB` separators.
   - Accent: flat blue `#3B82F6` replacing Telegram blue in the default theme.
   - Bubbles: incoming white, outgoing flat blue with white text.
   - Dark themes remain selectable but untouched (later pass).
4. **Chat layout — "Flat classic" (option 1)**: bubble shapes/tails/timestamps
   and all message types unchanged; colors only.
5. **Input bar**: emoji/sticker panel button removed. Bot menu (☰), attachment
   clip, voice/video record, and all other capabilities kept.
6. **App icon**: icons8 "AI" sparkle glyph (id `6IKnbEq33e5V`), black, centered
   on `#F6B092` peach; adaptive icon, all densities + round variant.
   License note: icons8 free tier wants attribution; buy/redraw before any
   store publish.

## Approach

Hardcode in the fork (default theme colors in `ThemeColors.java`, default
wallpaper in `Theme.java`, blur kill-switch, `ChatActivityEnterView` button
removal, mipmap swap). A bundled theme file was rejected: it cannot remove
blur, buttons, or change the icon.

## Out of scope

Dark theme restyle, message cell structural changes, removing other Telegram
features (handled separately by existing AppConfig lockdown).

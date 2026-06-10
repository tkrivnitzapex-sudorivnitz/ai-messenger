#!/usr/bin/env bash
#
# Sign an Apex "rebind the single bot" command and print the adb broadcast to run.
#
# Security model: the app ships only the RSA *public* key. This script signs
# "<username>|<nonce>" with the matching *private* key (never committed). Only
# someone holding that private key can authorise a bot change. The nonce must
# strictly increase, so an old signed command cannot be replayed to roll back.
#
# Requirements: openssl, adb, and the admin private key.
#   APEX_ADMIN_KEY  path to the private key PEM
#                   (default: ~/android_dev/telegram_app/apex-admin-private-key.pem)
#   APEX_APP_ID     installed applicationId (default: com.example.aimessenger)
#
# Usage:
#   ./apex-sign-bot.sh <bot_username> [nonce]
#   ./apex-sign-bot.sh my_new_bot | sh      # sign and run in one go
#
set -euo pipefail

KEY="${APEX_ADMIN_KEY:-$HOME/android_dev/telegram_app/apex-admin-private-key.pem}"
APP_ID="${APEX_APP_ID:-com.example.aimessenger}"

if [ $# -lt 1 ]; then
  echo "usage: $0 <bot_username> [nonce]" >&2
  exit 1
fi
if [ ! -f "$KEY" ]; then
  echo "error: private key not found at $KEY (set APEX_ADMIN_KEY)" >&2
  exit 1
fi

BOT="$(printf '%s' "$1" | sed 's/^@//' | tr '[:upper:]' '[:lower:]')"
NONCE="${2:-$(date +%s)}"
PAYLOAD="${BOT}|${NONCE}"
SIG="$(printf '%s' "$PAYLOAD" | openssl dgst -sha256 -sign "$KEY" | base64 -w0)"

echo "adb shell am broadcast -n ${APP_ID}/app.aimessenger.AdminConfigReceiver -a app.aimessenger.SET_BOT --es bot '${BOT}' --es nonce '${NONCE}' --es sig '${SIG}'"

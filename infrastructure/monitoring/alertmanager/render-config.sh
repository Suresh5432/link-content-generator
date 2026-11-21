#!/bin/sh
set -eu

: "${ALERT_SMTP_HOST:?ALERT_SMTP_HOST is required}"
: "${ALERT_SMTP_PORT:?ALERT_SMTP_PORT is required}"
: "${ALERT_SMTP_FROM:?ALERT_SMTP_FROM is required}"
: "${ALERT_SMTP_USERNAME:?ALERT_SMTP_USERNAME is required}"
: "${ALERT_EMAIL_TO:?ALERT_EMAIL_TO is required}"

case "$ALERT_SMTP_HOST" in
  *[!A-Za-z0-9.-]*|'') echo "ALERT_SMTP_HOST contains unsupported characters" >&2; exit 1 ;;
esac
case "$ALERT_SMTP_PORT" in
  *[!0-9]*|'') echo "ALERT_SMTP_PORT must be numeric" >&2; exit 1 ;;
esac
case "$ALERT_SMTP_FROM$ALERT_SMTP_USERNAME$ALERT_EMAIL_TO" in
  *[!A-Za-z0-9._+@,-]*|'') echo "Alert email settings contain unsupported characters" >&2; exit 1 ;;
esac

sed \
  -e "s|\${ALERT_SMTP_HOST}|${ALERT_SMTP_HOST}|g" \
  -e "s|\${ALERT_SMTP_PORT}|${ALERT_SMTP_PORT}|g" \
  -e "s|\${ALERT_SMTP_FROM}|${ALERT_SMTP_FROM}|g" \
  -e "s|\${ALERT_SMTP_USERNAME}|${ALERT_SMTP_USERNAME}|g" \
  -e "s|\${ALERT_EMAIL_TO}|${ALERT_EMAIL_TO}|g" \
  /etc/alertmanager/alertmanager.yml > /tmp/alertmanager-rendered.yml

exec /bin/alertmanager --config.file=/tmp/alertmanager-rendered.yml

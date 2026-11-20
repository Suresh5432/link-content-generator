#!/bin/bash
set -Eeuo pipefail

# Explicit, non-destructive reconciliation for an existing Postgres volume.
# Updates role passwords only; does not drop databases or alter data.
# Run only after reviewing the target and exporting the five *_DB_PASSWORD values.
for service in link extraction summary user notification; do
  user="${service}_user"
  password_variable="${service^^}_DB_PASSWORD"
  [[ "$service" == summary ]] && password_variable=SUMMARY_DB_PASSWORD
  password="${!password_variable:?${password_variable} must be set}"
  psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
    --set=service_user="$user" --set=service_password="$password" <<'SQL'
SELECT format('ALTER ROLE %I WITH PASSWORD %L', :'service_user', :'service_password')\gexec
SQL
done
echo 'Service database role passwords reconciled; databases and data were preserved.'

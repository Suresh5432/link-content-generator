#!/bin/bash
set -Eeuo pipefail

# Runs only when Postgres initializes an empty data directory. This creates
# per-service databases with per-service deployment-managed passwords; it does not
# alter databases in an existing volume.
for service in link extraction summary user notification; do
  user="${service}_user"
  database="${service}_db"
  password_variable="${service^^}_DB_PASSWORD"
  [[ "$service" == summary ]] && password_variable=SUMMARY_DB_PASSWORD
  service_password="${!password_variable:?${password_variable} must be set}"
  if [[ "$service" == "summary" ]]; then password_variable="SUMMARY_DB_PASSWORD"; fi
  service_password="${!password_variable:?${password_variable} must be set}"
  psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
    --set=service_password="$service_password" \
    --set=service_user="$user" --set=service_database="$database" <<'SQL'
SELECT format('CREATE USER %I WITH PASSWORD %L', :'service_user', :'service_password')\gexec
SELECT format('CREATE DATABASE %I OWNER %I', :'service_database', :'service_user')\gexec
SQL
done

# Operations and troubleshooting

## Routine checks

```sh
docker compose ps
docker compose logs --tail=200 api-gateway link-service extraction-service summarization-service user-service notification-service
```

Readiness from inside a service container:

```sh
docker compose exec link-service wget -qO- http://127.0.0.1:8081/actuator/health/readiness
```

Use the matching internal port: Gateway 8084, Link 8081, Extraction 8082, Summarization 8083, User 8085, Notification 8086. Management endpoints should not be exposed publicly. Compose only publishes the Gateway application port; confirm network restrictions before changing that.

## Metrics, traces, alerts

- Prometheus: `http://localhost:9090`; inspect Targets and confirm each service scrape is `UP`.
- Grafana: `http://localhost:3000`; configure/use a Prometheus data source and dashboards. The Compose file persists Grafana data but does not provision a project dashboard.
- Jaeger: `http://localhost:16686`; submit a traced request and check for service spans and propagated trace context across HTTP and RabbitMQ boundaries.
- Alertmanager: `http://localhost:9093`; configure the SMTP sender, recipient, and mounted app-password secret before expecting email alerts. The `.env.example` alert addresses are placeholders.

Configured containers and ports alone do not prove scrape, trace, or alert delivery. Verify actual targets, spans, alert firing, and a test notification after startup.

## PostgreSQL backups and upgrades

Back up all five service databases before deployment changes. Example from the Compose project directory:

```sh
mkdir -p backups
for db in link_db extraction_db summary_db user_db notification_db; do
  docker compose exec -T postgres pg_dump -U "$POSTGRES_USER" "$db" > "backups/${db}-$(date +%Y%m%d%H%M%S).sql"
done
```

`POSTGRES_USER` must be available in the shell (for example, source a protected local env file without printing it). Store backups outside the repository in production and test restoration to a separate database/host. Never remove `postgres_data` to fix a migration or password mismatch. Init scripts run only when PostgreSQL creates a new data directory. Existing role passwords may differ from changed `.env` values; inspect and follow `infrastructure/postgres/reconcile-service-credentials.sh` only after backup and a maintenance plan. It changes role passwords, not stored data.

## Troubleshooting

| Symptom | Checks |
|---|---|
| Service repeatedly restarts | `docker compose logs <service>`; validate required env vars, DB credentials, issuer metadata reachability, and health status of dependencies |
| Migration/DB authentication fails | Check service DB URL/user/password against the existing volume’s roles; take backups before credential reconciliation; do not reset volumes |
| 401 from Gateway | Compare token issuer and audience with Keycloak and `.env`; check token expiry and signing metadata reachability |
| Internal lookup denied | Check client-credentials token, audience/scope, and `INTERNAL_USER_LOOKUP_CLIENT_IDS` against actual client ID |
| Workflow queue stalls | Check RabbitMQ health, queue depth, consumer count, bindings, retries, and DLQs; preserve event IDs before recovery |
| AI summary fails | Verify `AI_PROVIDER`; for Ollama, check model-init completion and model availability; for Gemini, configure a valid `GEMINI_API_KEY` and network access |
| Email remains pending | Check user verified email, preferences, SMTP credentials/connectivity, delivery attempts, and worker logs |
| No traces or metrics | Check Prometheus Targets; confirm service OTLP endpoint resolves to Jaeger and inspect Jaeger for actual request traces |

## Runtime acceptance on a Docker host

Run the deployment guide’s startup procedure, then verify all service readiness endpoints, all five Flyway histories on the existing databases, Keycloak user and service tokens, RabbitMQ topology/retry/DLQ behavior, Ollama model availability or Gemini calls, SMTP delivery/retry, Gateway rate limiting, Prometheus targets, Grafana query access, Jaeger cross-service spans, restart recovery, and backup/restore. No persistent volume should be deleted as part of these checks.

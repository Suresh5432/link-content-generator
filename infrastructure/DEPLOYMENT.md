# Local Docker Compose deployment

This guide describes the Compose topology in the repository. For operational checks, backup/recovery and troubleshooting, see [Operations](OPERATIONS.md). Keycloak clients and audiences are described in [Keycloak setup](KEYCLOAK.md); the current API inventory is in [Architecture](ARCHITECTURE.md).

## Prerequisites and configuration

- Docker Engine with Compose v2 and enough memory for PostgreSQL, Keycloak, six JVM services, Prometheus/Grafana, Jaeger, RabbitMQ, Redis, and Ollama. Ollama model storage and memory requirements depend on the selected model.
- Java/Maven are not required for container startup; each image builds from the repository root.
- Copy `.env.example` to `.env`; replace all `replace_with_*` and example values with locally managed credentials. Do not commit `.env` or mounted secret files.
- Configure Keycloak realm `link-context`, issuer, audiences, and service clients as described in [Keycloak setup](KEYCLOAK.md).
- For Alertmanager email, create `infrastructure/monitoring/alertmanager/secrets/gmail-app-password` with the SMTP app password and configure sender/recipient values. Do not commit it.

The Keycloak issuer returned in tokens must exactly equal `KEYCLOAK_ISSUER_URI_DOCKER`, default `http://keycloak:8080/realms/link-context`, and resolve from backend containers. A Keycloak container being healthy does not mean realm, audience, or clients have been provisioned.

## Validate and start

```sh
cp .env.example .env
# Edit .env; configure Keycloak and all service credentials.
docker compose config --quiet
docker compose up --build -d
docker compose ps
```

Compose health dependencies gate application startup on PostgreSQL, RabbitMQ, Redis, and Keycloak. Prometheus waits for application health. Summarization additionally waits for the Ollama model-init task to complete. The initial Ollama model pull requires network access and may take time; model data persists in `ollama_data`.

Only Gateway is published as an application API at `http://localhost:8084`. Keycloak and infrastructure/monitoring ports are bound to host loopback. Internal service ports are only exposed on Compose networks.

| Component | Local address |
|---|---|
| Gateway | `http://localhost:8084` |
| Keycloak | `http://localhost:8080` |
| RabbitMQ management | `http://localhost:15672` |
| Prometheus | `http://localhost:9090` |
| Alertmanager | `http://localhost:9093` |
| Grafana | `http://localhost:3000` |
| Jaeger | `http://localhost:16686` |

Check an application readiness probe:

```sh
docker compose exec link-service wget -qO- http://127.0.0.1:8081/actuator/health/readiness
```

Replace `link-service` and port `8081` with the desired service: Gateway 8084, Extraction 8082, Summarization 8083, User 8085, or Notification 8086. Use `docker compose logs <service>` to diagnose startup failures.

## Data safety

Named volumes persist PostgreSQL, RabbitMQ, Redis, Keycloak, Prometheus, Grafana, and Ollama data. Use `docker compose down` for routine shutdown; **do not use `docker compose down -v` for upgrades or troubleshooting**. PostgreSQL initialization scripts only execute when the data directory is initialized. Existing database users/passwords are not automatically reconciled with changed environment values. Back up databases before changing credentials and review `infrastructure/postgres/reconcile-service-credentials.sh`; it changes role passwords only. Follow [backup instructions](OPERATIONS.md#postgresql-backups-and-upgrades) and test restore before upgrades.

## Verification available in this workspace

The following static/local checks have been run during this work:

- `docker compose --env-file .env.example config --quiet` rendered the Compose configuration successfully. Placeholder credentials are only used for config validation.
- `mvn -q -DskipTests package` compiled and packaged the Maven modules.
- All six backend Dockerfiles were inspected for a Maven build stage, Java 21 runtime, non-root user, memory-aware JVM settings, and an Actuator health check.
- `git diff --check` completed without whitespace errors.

No containers or persistent volumes were started or changed in this documentation batch. Docker-host checks still required include image builds, service startup/readiness, Flyway migrations against actual PostgreSQL volumes, credential compatibility, token validation against configured Keycloak, RabbitMQ confirms/retries/DLQs, Redis rate limiting, Ollama/Gemini execution, SMTP delivery, Prometheus scrape health, trace propagation, alert delivery, and restore/restart behavior. The runtime acceptance list is in [Operations](OPERATIONS.md#runtime-acceptance-on-a-docker-host).

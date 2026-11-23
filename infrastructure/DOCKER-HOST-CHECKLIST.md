# Docker-host integration and deployment checklist

Use this checklist on a machine where `docker info` succeeds and the Compose project has network access. These steps start test/application containers. They do not require deleting volumes; do **not** run `docker compose down -v`.

## 1. Safe preflight

- Confirm `docker info` works for the current user and `docker compose version` reports Compose v2.
- Prepare `.env` from `.env.example`, replace every placeholder, and configure the Keycloak realm/client/audience setup in [KEYCLOAK.md](KEYCLOAK.md).
- Configure SMTP/AI credentials only if those integrations are being exercised.
- If this host has an existing Compose deployment, record `docker compose ps`, back up all five databases, and plan a maintenance window before changing images or database roles. Never reset existing PostgreSQL data to make a test pass.
- Validate interpolation before startup: `docker compose config --quiet`.

## 2. Docker-backed tests

Run the repository-backed integration suites first. Testcontainers creates and removes its own ephemeral test containers; it does not use the Compose named data volumes.

```sh
mvn -pl backend/api-gateway -Dtest=RateLimitServiceIntegrationTest test
mvn -pl backend/summarization-service -am -Dtest=SummaryRepositoryIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl backend/user-service -Dtest=UserProfileRepositoryIntegrationTest test
```

The summary and user repository tests use PostgreSQL and Flyway. The Gateway test starts a disposable Redis container. These tests verify repository/query behavior and Redis limit behavior; they do not verify Keycloak, RabbitMQ, SMTP, AI providers, or the cross-service workflow. Dedicated RabbitMQ Testcontainers coverage and an automated authenticated end-to-end workflow are still absent.

Run the focused non-Docker suite and the local-stub Gateway summary test separately:

```sh
mvn -Dtest='!**/*ApplicationTests,!**/*IntegrationTest' -DfailIfNoTests=false test
mvn -pl backend/api-gateway -Dtest=SummaryGatewayIntegrationTest test
```

The Gateway summary test stubs JWT decoding and its downstream HTTP service. It checks route forwarding and role authorization but does not verify Keycloak signature/JWKS validation.

## 3. Build and start the deployment

```sh
mvn -q -DskipTests package
docker compose build
docker compose up -d
docker compose ps
```

Wait for health conditions and inspect startup logs:

```sh
docker compose logs --tail=200 postgres rabbitmq redis keycloak ollama ollama-model-init
```

Then inspect each application and monitoring service:

```sh
docker compose ps
```

Expected published API entry is Gateway `http://localhost:8084`. Management and infrastructure ports are loopback-bound in the local Compose file.

## 4. Readiness and migrations

Run readiness checks inside each application container:

```sh
for spec in 'api-gateway 8084' 'link-service 8081' 'extraction-service 8082' 'summarization-service 8083' 'user-service 8085' 'notification-service 8086'; do
  set -- $spec
  docker compose exec -T "$1" wget -qO- "http://127.0.0.1:$2/actuator/health/readiness"
done
```

Check database migration history and service schema validation without editing old migration files. Verify upgrades with a backed-up existing volume and a separate clean disposable database. Never use volume deletion as a migration/recovery procedure.

## 5. Authenticated API and workflow checks

- Provision Keycloak and test valid, missing, expired, wrong-issuer, wrong-audience, missing-role, and allowed/denied service-client tokens.
- Through Gateway, test Link create/idempotency/list/status/search/get/retry/delete and cross-user denial.
- Through Gateway, test User `/me`, Notification list/read/preferences, and Summary list/get-by-ID/get-by-link. Verify cross-user summary IDs return the same not-found response as missing IDs.
- Submit a controlled public HTTPS page and trace it through Link outbox, extraction persistence, content event, summary persistence/outbox, Link status update, notification creation, and email delivery.
- Exercise invalid/private/redirecting URLs, duplicate and out-of-order messages, AI timeout/provider errors, unavailable SMTP, and restart during processing. Inspect RabbitMQ retry/DLQ queues and confirm attempts remain bounded.
- Verify Redis rate limiting, CORS, timeout behavior, correlation ID propagation, Prometheus scrape targets, Jaeger spans across HTTP/RabbitMQ, Grafana queries, and an Alertmanager test notification.

## 6. Recovery and teardown

- Restart one service at a time while work is queued and verify outbox/consumer recovery and persistent state.
- Back up each database with `pg_dump`, restore to a separate database or host, and validate row counts/application reads.
- Stop the Compose stack with `docker compose down` when finished. Do not append `-v`; retain named volumes unless a separately approved data-retention action explicitly requires removal.

## Current workspace result

- Docker CLI 29.9.3, Compose 5.5.1, Docker Engine 29.9.3. The ordinary sandbox could not access Docker Desktop's socket; approved elevated Docker commands were used.
- Compose config validation passed with local `.env`; the checked-in `.env.example` remains placeholders and is not a live credential file.
- Four missing PostgreSQL role passwords were generated and applied transactionally to the existing roles; all five service-role/database TCP connections verified. Existing PostgreSQL data was preserved.
- Created confidential Keycloak service clients `extraction-service` and `notification-service`, both client-credentials only. Their only application scopes are `extraction.read` and `users.read`; token audiences are `extraction-service` and `user-service`, respectively. Removed unrelated default scopes and restricted User Service lookup allowlist to `notification-service`. Client tokens verified expected `azp`, audience, and scope. Secrets remain only in ignored `.env` with restrictive file permissions and were not printed.
- All six service images built. The first build revealed missing sibling Maven reactor modules in Dockerfiles; all six were corrected to copy `backend/` before Maven package. Image builds used `-DskipTests`.
- `docker compose --env-file .env up -d --no-recreate api-gateway link-service extraction-service summarization-service user-service notification-service` started all app services. A Notification Service health-check mismatch was fixed by changing its Docker health probe from aggregate `/actuator/health` to `/actuator/health/readiness`; only that app container was recreated to apply the image change.
- Docker health status is **healthy** for all six app services and the PostgreSQL, RabbitMQ, Redis, Keycloak, and Ollama dependencies. Each in-container readiness endpoint returned `{"status":"UP"}`; Gateway host readiness at port 8084 also returned `UP`.
- `qwen2.5:3b` is present in Ollama (1.9 GB), and the model-init container exited 0. Compose created the Ollama named volume. No existing data volumes were deleted, reset, or removed; existing infra containers were not recreated.
- The initial SMTP-missing observation above was superseded after SMTP configuration: the authenticated workflow later persisted an email delivery as `SENT`, and the relay accepted a separate STARTTLS test. The mailbox itself was not independently inspected.

Earlier Docker-backed Testcontainers suites also passed:

- Gateway Redis/Testcontainers: `RateLimitServiceIntegrationTest` — **2 tests, 0 failures, 0 errors, 0 skipped**.
- Summarization PostgreSQL/Testcontainers: `SummaryRepositoryIntegrationTest` — **1 test, 0 failures, 0 errors, 0 skipped**. Flyway applied the test schema migrations to the disposable database.
- User PostgreSQL/Testcontainers: `UserProfileRepositoryIntegrationTest` — **2 tests, 0 failures, 0 errors, 0 skipped**. Flyway applied both user profile migrations to the disposable database.

Those tests used disposable test containers and did not touch the application volumes.

### Latest runtime update — 2026-11-21

- After the user applied the Docker Desktop memory increase and restarted Desktop, `docker context show` returned `desktop-linux` and `docker info --format 'engine={{.Name}} memory_bytes={{.MemTotal}}'` returned `engine=docker-desktop memory_bytes=12537860096` (~11.68 GiB).
- Desktop restart stopped Keycloak with exit code 143. `docker compose --env-file .env up -d keycloak` started the existing service in place. OIDC discovery returned HTTP 200; all six application containers and Postgres, RabbitMQ, Redis, Keycloak, Ollama, Prometheus and Alertmanager returned healthy. No persistent volume was removed or reset.
- Each app's in-container `/actuator/health/readiness` endpoint returned `{"status":"UP"}`; Gateway host readiness returned HTTP 200. Prometheus reported 7/7 targets up; Alertmanager readiness and Jaeger UI returned HTTP 200.
- Local `qwen2.5:3b` inference exited 0 and returned `READY`.
- Reran Testcontainers: Gateway Redis 2/2, Summary PostgreSQL/Flyway 1/1, User PostgreSQL/Flyway 2/2; zero failures, errors, or skips.
- Live probes confirmed anonymous/malformed bearer requests are rejected with 401, configured-origin CORS preflight returns 200, and an unlisted origin returns 403. RabbitMQ had 18 application/retry/DLQ queues, with 0 messages ready and 0 unacknowledged.
- Previously verified workflow, cross-user denial, duplicate-message restart recovery, HTTP/Rabbit consumer trace probes, controlled Alertmanager relay acceptance, and isolated five-database logical restore remain as recorded in `COMPLETION-REPORT.md`.
- Still open: broker outage, unroutable mandatory publish and DLQ replay tests; runtime out-of-order delivery; normal outbox trace continuity; full JWT issuer/audience/expiry/roles matrix; production Alertmanager recipient and inbox receipt; restart during in-flight work; off-host backup/PITR; sustained load; and a repeatable automated authenticated end-to-end suite. No RabbitMQ Testcontainers test exists in the repository.

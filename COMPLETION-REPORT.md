# Backend completion report and prioritized checklist

**Status: backend implementation is not yet accepted as production-complete.** Source, packaging, and Compose configuration have received substantial work, but the full runtime acceptance criteria have not been demonstrated. This report separates inspected/implemented source from commands actually run and checks that require a Docker-capable runtime plus configured external services.

## Priority checklist

| Priority | Area | Status | Evidence / remaining work |
|---|---|---|---|
| P0 | Docker image build and six-service startup | **Not runtime-verified** | Compose graph validates, but Docker images/containers were not run in this workspace. Build images and verify health/readiness on a Docker host. |
| P0 | PostgreSQL persistence, migrations, existing-volume credentials | **Configured; runtime-verified pending** | Five services have Flyway migrations and `ddl-auto: validate`; Compose has first-boot DB initialization and a role-password reconciliation script. Verify clean-database migration and upgrade against a backed-up existing volume without deleting data. |
| P0 | Authenticated URL-to-summary end-to-end flow | **Partially implemented; not end-to-end verified** | Link, extraction, summarization, and notification code/configuration exist. A real token, broker, databases, extractor target, AI provider, email, and full workflow have not been exercised together. |
| P0 | Public summary retrieval | **Gap** | Gateway contains a summaries route, but Summarization Service does not expose a public read controller. Implement/resolve before claiming all required public APIs work. |
| P0 | Production identity setup and authorization | **Source configured; runtime verification pending** | JWT issuer/audience checks, service client config, user ownership checks, and internal client allowlisting are present. Provision realm/clients/mappers/secrets and test allowed and denied tokens/calls. No realm bootstrap artifact is present. |
| P1 | Link API and ownership | **Implemented in source; focused tests exist** | CRUD/list/retry, pagination/filter/search, idempotency, ownership and status transition code are present. Focused unit tests exist; the integration behavior/concurrency matrix remains to run. No link PATCH route is implemented. |
| P1 | Extraction SSRF and event duplicate handling | **Implemented in source; focused tests exist** | URL safety/fetch and event processor code plus URL safety and Rabbit config tests are present. Run adversarial DNS/redirect tests against real network conditions and verify pinned transport/TLS behavior on runtime. |
| P1 | Summary provider, validation, persistence, retries | **Implemented in source; partial unit coverage** | AI validators and mock provider tests exist; Ollama/Gemini calls, timeout/fallback behavior, provider limits and persistence against PostgreSQL remain runtime/integration checks. |
| P1 | User profiles and internal lookup | **Implemented in source; focused tests exist** | `/me`, verified email handling, internal lookup security and ownership tests are present. Test Keycloak user/client tokens and database constraints on the running stack. |
| P1 | Notification APIs and email delivery | **Implemented in source; partial unit coverage** | Notification API, preferences, event processing, and email worker code/tests exist. SMTP retry/recovery, worker contention/locking, atomic event handling, and duplicate-delivery window need runtime tests. |
| P1 | Messaging confirms, retries, DLQs, recovery | **Configured in source; broker behavior unverified** | Outbox/confirm/mandatory and retry/DLQ declarations exist. RabbitMQ management topology, unroutable publisher returns, broker interruption, consumer restart, bounded attempts and safe replay are not verified. |
| P1 | Gateway JWT, routes, Redis limiting, correlation/errors | **Configured in source; runtime verification pending** | Security/routing/filter changes and validator/rate-limit tests exist. Verify JWT matrix, CORS, timeout behavior, Redis rate limit under concurrency, and ensure no internal paths are routed. Summary route remains a gap. |
| P1 | Focused tests across services | **Added; execution evidence incomplete** | Test files exist across all six services. This report does not mark tests passed without command output. Run the exact service test suite and classify failures requiring Docker/external services. |
| P1 | Repository, integration, and end-to-end tests | **Incomplete** | User Service has a PostgreSQL Testcontainers repository test and Gateway has a Redis Testcontainers test. No complete RabbitMQ/Keycloak/SMTP/AI integration suite or end-to-end workflow suite was identified. Add and run these before acceptance. |
| P2 | Observability | **Configured; runtime proof pending** | Actuator, Prometheus scrape config, Grafana, Jaeger/OTLP, structured correlation filters and alerts are present. Verify readiness/liveness, scrape targets, emitted metrics, cross-service HTTP and Rabbit trace context, alert firing and delivery. Dashboard provisioning is not present. |
| P2 | Docker hardening and deployability | **Compose/Dockerfiles present; runtime checks pending** | Six multi-stage Java 21 Dockerfiles, non-root runtime setup, health checks, resource limits, named volumes and network segmentation are present by inspection. Verify actual image builds, graceful shutdown, restart recovery and resource behavior. Production secret management and per-service broker least privilege need deployment review. |
| P2 | API and operational documentation | **Partially complete** | Root README, architecture/API inventory, Keycloak, messaging, deployment and operations guides were added. OpenAPI/Swagger is not configured; add generated API contracts if required by acceptance. |
| P2 | Backups and recovery | **Instructions documented; restore unverified** | Backup guidance is in `infrastructure/OPERATIONS.md`. Perform and verify restore on an isolated database/host before production. |

## Verified in this workspace

The following commands completed successfully in the latest recorded documentation batch:

- `docker compose --env-file .env.example config --quiet`
- `mvn -q -DskipTests package`
- `git diff --check`

The command output for the Maven build contained only a JDK `Unsafe` deprecation warning from the Maven runtime. The Maven command explicitly skipped tests, so it is package/build evidence only. Compose config validation checks interpolation and syntax; it does not contact or start services. Dockerfile architecture was inspected, but images were not built.

The repository contains focused test source files for Link state/service, Extraction URL safety and Rabbit config, Summary input/output validation and provider behavior, User controller/service/filter plus a PostgreSQL Testcontainers repository test, Notification state/processor, and Gateway JWT/rate-limit behavior plus a Redis Testcontainers integration test. Presence of those files is not a test result. No test is marked passed in this report without an executed result.

## Docker-host and external-service runtime checks

Run these only after configuring non-placeholder credentials and backing up any existing database volumes. Do not use `docker compose down -v` or otherwise reset persistent data.

1. `docker compose up --build -d`; inspect all container health and logs. Check each of the six service readiness endpoints from inside its container.
2. On a clean disposable database, verify all five Flyway migration histories and schema validation. On the existing data volume, verify role/database connectivity and forward-only migration behavior after backup. Exercise the credential reconciliation script only if required and with a reviewed maintenance plan.
3. Configure Keycloak realm `link-context`, issuer, audience mappers and confidential service clients. Exercise valid/expired/wrong-issuer/wrong-audience tokens; check authorization and user ownership. Test both allowlisted and denied client-credentials lookups.
4. Verify actual RabbitMQ exchanges, bindings, queues, retry TTLs and DLQs. Test publisher confirm, mandatory unroutable return, broker outage, duplicate and out-of-order delivery, bounded retries, consumer restart, ack behavior, and DLQ replay.
5. Exercise Redis-backed Gateway limits under concurrent requests, request correlation across HTTP and RabbitMQ, timeouts, CORS, and denial of internal routes.
6. Test Extraction with public and prohibited destinations, DNS changes, redirects to private addresses, oversized responses, invalid content types and slow endpoints. Confirm the transport retains hostname/TLS validation while connecting to the validated IP.
7. Run Summarization against the configured Ollama model or a valid Gemini key; verify output validation, provider timeouts, configured retry/fallback behavior, and persisted summary. (Summary retrieval API remains a source gap.)
8. Configure a real SMTP relay and test verified/missing user email, disabled preferences, transient/permanent SMTP failures, retry backoff/attempt limit, concurrent workers, recovery of stuck work, and the documented SMTP duplicate-delivery window.
9. Confirm Prometheus targets and service metrics; query Grafana; inspect Jaeger spans across Gateway → services and Rabbit consumers; fire an alert and verify Alertmanager delivery.
10. Restart services during work in flight and verify outbox/consumer recovery and database state. Take database backups and restore to a separate target. Verify shutdown and resource limits under representative workload.

## Release recommendation

Do not describe the backend as fully complete or production-ready until the P0 items are closed and the P1 runtime reliability/security checks have evidence. The most direct source-level blocker is summary retrieval. The largest validation gap is that the Docker deployment, migrations against the actual volume, identity setup, broker failure handling, email/AI integrations, and complete workflow have not been run end to end.

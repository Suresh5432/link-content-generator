# Link Context Generator

Link Context Generator accepts a webpage URL, extracts its content, and generates an AI summary. The repository is a Java 21 / Spring Boot monorepo containing independent Maven services. This README is the project entry point; operational detail is in [`infrastructure/`](infrastructure/).

## Repository map

| Path | Responsibility |
|---|---|
| `backend/api-gateway` | Public HTTP entry point, JWT checks, routing, Redis rate limiting |
| `backend/link-service` | Link ownership, lifecycle, idempotent submission and event publication |
| `backend/extraction-service` | Safe webpage fetching, content extraction and persistence |
| `backend/summarization-service` | AI provider calls and summary persistence |
| `backend/user-service` | User profile APIs and secured internal profile lookup |
| `backend/notification-service` | In-app notification APIs and asynchronous email delivery |
| `backend/common/event-contracts` | Shared event types and routing-key contracts |
| `infrastructure/` | Compose deployment, PostgreSQL initialization and monitoring configuration |

Each service owns its database. Do not connect one service directly to another service’s schema. The URL-to-summary workflow uses RabbitMQ; see [architecture and API status](infrastructure/ARCHITECTURE.md) and [messaging operations](infrastructure/MESSAGING.md).

## Local deployment

Requirements: Docker Engine with Compose v2, network access for image/model pulls, and enough memory for the application and observability containers. Copy `.env.example` to `.env`, set unique local secrets and configure Keycloak as described in [Keycloak setup](infrastructure/KEYCLOAK.md). Keep `.env` and credential files out of source control.

```sh
cp .env.example .env
# Edit .env before starting services.
docker compose config --quiet
docker compose up --build -d
docker compose ps
```

Only Gateway is published as an application API at `http://localhost:8084`; Compose binds infrastructure and monitoring ports to loopback for local use. To stop without removing data, run `docker compose down`. **Do not use `docker compose down -v` for routine shutdown or upgrades.** PostgreSQL initialization scripts apply only to a fresh data directory. Existing database role credentials are not reset by Compose; see the backup and credential notes in [deployment operations](infrastructure/DEPLOYMENT.md).

## Build and tests

```sh
mvn -q -DskipTests package
mvn test
```

The package command compiles and packages Maven modules without running tests. Tests that use Docker/Testcontainers require a working Docker daemon. Service `HELP.md` files contain service-specific notes; current implementation and known gaps are summarized in [architecture and API status](infrastructure/ARCHITECTURE.md).

## Service and operational documentation

- [Backend completion report and prioritized checklist](COMPLETION-REPORT.md)
- [OpenAPI specification for public Gateway APIs](docs/openapi.yaml)
- [Architecture, APIs, and current implementation status](infrastructure/ARCHITECTURE.md)
- [Keycloak realm, clients, audiences, and scopes](infrastructure/KEYCLOAK.md)
- [RabbitMQ exchanges, queues, and event flow](infrastructure/MESSAGING.md)
- [Deployment, configuration, backup, verification, and troubleshooting](infrastructure/DEPLOYMENT.md)
- [Docker-host integration and deployment checklist](infrastructure/DOCKER-HOST-CHECKLIST.md)

## Current limitations

OpenAPI/Swagger endpoints are not configured in the current Maven projects. All six Compose services now start and report readiness, and service-client token scopes/audiences were verified. Remaining runtime checks include user-token authorization cases, SMTP delivery, actual AI summary generation, RabbitMQ failure/DLQ handling, end-to-end tracing, and the full URL-to-notification workflow; see the deployment guide.

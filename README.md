# Link Context Generator

Link Context Generator accepts a webpage URL, extracts its content, and generates an AI summary. The repository is a Java 21 / Spring Boot monorepo containing independent Maven services. This README is the project entry point; operational detail is in [`infrastructure/`](infrastructure/).

## Repository map

| Path | Responsibility |
|---|---|
| `backend/api-gateway` | Public HTTP entry point, JWT checks, routing, Redis rate limiting |
| `backend/link-service` | Link ownership, lifecycle, idempotent submission and event publication |
| `backend/extraction-service` | Safe webpage fetching, content extraction and persistence |
| `backend/summarization-service` | AI provider calls, summaries, grounded privacy assistant and AI request drafts |
| `backend/user-service` | User profile APIs, My Digital Footprint inventory, privacy requests, browser metadata, export/delete |
| `backend/notification-service` | In-app notification APIs and asynchronous email delivery |
| `backend/common/event-contracts` | Shared event types and routing-key contracts |
| `frontend/` | React/TypeScript workspace including **My Digital Footprint** |
| `browser-extension/` | Optional Manifest V3 extension that syncs cookie **metadata** only |
| `infrastructure/` | Compose deployment, PostgreSQL initialization and monitoring configuration |

Each service owns its database. Do not connect one service directly to another service’s schema. The URL-to-summary workflow uses RabbitMQ; see [architecture and API status](infrastructure/ARCHITECTURE.md) and [messaging operations](infrastructure/MESSAGING.md).

## My Digital Footprint

The workspace now includes a privacy dashboard where a signed-in user can track websites and category-level personal-data records, sync cookie/tracker metadata through an optional browser extension, discover public privacy-request channels, generate and approve deletion/access requests, track responses and reminders, ask a grounded AI assistant about their own records, and export or delete their footprint data.

Safety rules are enforced in code and tests: no passwords, raw cookie values, session tokens, full card numbers or identity numbers are stored; AI receives category-level context only; the user must approve the exact request message before submission; a website's deletion claim is recorded as a claim, not proof; and deleting cookie metadata never claims to delete server-side data.

Full feature architecture, API, lifecycle, security rules, limits and test evidence: [My Digital Footprint](docs/DIGITAL-FOOTPRINT.md).

## Local deployment

Requirements: Docker Engine with Compose v2, network access for image/model pulls, and enough memory for the application and observability containers. Copy `.env.example` to `.env`, set unique local secrets and configure Keycloak as described in [Keycloak setup](infrastructure/KEYCLOAK.md). Keep `.env` and credential files out of source control.

```sh
cp .env.example .env
# Edit .env before starting services.
docker compose config --quiet
docker compose up --build -d
docker compose ps
```

The React frontend is available at `http://localhost:5173` by default. Its public browser settings are supplied at image build time through `FRONTEND_API_BASE_URL`, `FRONTEND_KEYCLOAK_URL`, `FRONTEND_KEYCLOAK_REALM`, and `FRONTEND_KEYCLOAK_CLIENT_ID` in `.env`. Configure the Keycloak public client redirect URI and web origin for `http://localhost:5173/*` and `http://localhost:5173`. Compose uses `http://auth.localhost:8080` for both browser sign-in and backend token validation, with a network alias that lets both sides resolve the same issuer. Gateway CORS uses `CORS_ALLOWED_ORIGINS` and defaults to `http://localhost:5173`.

Only Gateway is published as an application API at `http://localhost:8084`; Compose binds infrastructure and monitoring ports to loopback for local use. To stop without removing data, run `docker compose down`. **Do not use `docker compose down -v` for routine shutdown or upgrades.** PostgreSQL initialization scripts apply only to a fresh data directory. Existing database role credentials are not reset by Compose; see the backup and credential notes in [deployment operations](infrastructure/DEPLOYMENT.md).

## Build and tests

```sh
mvn -q -DskipTests package
mvn test
```

The package command compiles and packages Maven modules without running tests. Tests that use Docker/Testcontainers require a working Docker daemon. Service `HELP.md` files contain service-specific notes; current implementation and known gaps are summarized in [architecture and API status](infrastructure/ARCHITECTURE.md).

## Service and operational documentation

- [Backend completion report and prioritized checklist](COMPLETION-REPORT.md)
- [My Digital Footprint feature guide](docs/DIGITAL-FOOTPRINT.md)
- [OpenAPI specification for public Gateway APIs](docs/openapi.yaml)
- [Architecture, APIs, and current implementation status](infrastructure/ARCHITECTURE.md)
- [Keycloak realm, clients, audiences, and scopes](infrastructure/KEYCLOAK.md)
- [RabbitMQ exchanges, queues, and event flow](infrastructure/MESSAGING.md)
- [Deployment, configuration, backup, verification, and troubleshooting](infrastructure/DEPLOYMENT.md)
- [Docker-host integration and deployment checklist](infrastructure/DOCKER-HOST-CHECKLIST.md)

## Current limitations

OpenAPI/Swagger endpoints are not configured in the current Maven projects. All six Compose services now start and report readiness, and service-client token scopes/audiences were verified. Remaining runtime checks include user-token authorization cases, SMTP delivery, actual AI summary generation, RabbitMQ failure/DLQ handling, end-to-end tracing, and the full URL-to-notification workflow; see the deployment guide.

# Architecture and API status

## Runtime flow

1. A client obtains a Keycloak access token and calls the Gateway.
2. Gateway authenticates the JWT, applies its request filters, and routes supported public paths to their owning service.
3. Link Service stores the caller-owned link and publishes `link.submitted.v1` through its outbox.
4. Extraction Service validates and fetches the target, stores extracted content, and publishes `content.extracted.v1`.
5. Summarization Service retrieves content through Extraction Service’s internal endpoint, calls the configured AI provider, persists the summary, and publishes `summary.generated.v1`.
6. Link Service consumes summary/failure events to update its status. Notification Service consumes summary/failure events to create in-app notifications and queue email delivery.

Services use separate PostgreSQL databases, RabbitMQ for asynchronous work, Redis for Gateway rate limiting, Keycloak for bearer tokens and service credentials, and Jaeger/OTLP plus Prometheus for telemetry. The Compose network is not a substitute for service authentication: internal user and content APIs have service security controls.

## Current HTTP endpoints

All public paths below are intended to be called through `http://localhost:8084` and require a valid bearer token. Exact claims, audiences, and clients are described in [Keycloak setup](KEYCLOAK.md).

| Method and path | Behavior |
|---|---|
| `POST /api/v1/links` | Submit URL; provide an `Idempotency-Key` header |
| `GET /api/v1/links` | List caller-owned links; supports page, size, status, `q`, sortBy, direction |
| `GET /api/v1/links/{id}` | Read caller-owned link |
| `POST /api/v1/links/{id}/retry` | Retry an eligible failed link |
| `DELETE /api/v1/links/{id}` | Soft-delete caller-owned link |
| `GET /api/v1/users/me` | Read or create the authenticated user’s profile |
| `PATCH /api/v1/users/me` | Update allowed profile fields |
| `GET /api/v1/notifications` | List caller-owned notifications (page, size, direction) |
| `GET /api/v1/notifications/unread-count` | Return unread count |
| `PATCH /api/v1/notifications/{id}/read` | Mark one owned notification read |
| `PATCH /api/v1/notifications/read-all` | Mark caller’s notifications read |
| `GET /api/v1/notifications/preferences` | Read notification preferences |
| `PUT /api/v1/notifications/preferences` | Replace notification preferences |

The following internal endpoints are not routed by Gateway and should only be called by authorized services:

| Method and path | Caller/purpose |
|---|---|
| `GET /internal/v1/contents/{contentId}` | Summarization Service retrieves extracted content using OAuth2 client credentials |
| `GET /internal/v1/users/{userId}` | Allowlisted service clients perform user lookup, including notification email delivery |

### Known API gaps

- Gateway config contains `/api/v1/summaries/**`, but there is no summary read controller in Summarization Service. No summary retrieval API is currently available.
- No `PATCH /api/v1/links/{id}` endpoint is implemented.
- Springdoc/Swagger dependencies and OpenAPI endpoint configuration are absent. This file is a hand-maintained API inventory, not a generated OpenAPI contract.
- Readiness endpoints are intended for container health checks; management exposure and network reachability should be verified on a running deployment.

## Data ownership

Compose provisions `link_db`, `extraction_db`, `summary_db`, `user_db`, and `notification_db` with separate service roles. Flyway migrations are stored under each service’s `src/main/resources/db/migration`. Migrations are forward-only: do not edit a migration that has already run in an environment. Back up each database before schema upgrades, and never delete named volumes as a migration strategy.

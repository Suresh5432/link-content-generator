# Link Context Generator

Link Context turns a webpage URL into an AI-generated summary and gives users a private workspace to understand and manage their digital footprint.

The application is a Java 21 / Spring Boot microservices monorepo with a React and TypeScript frontend. Services communicate through authenticated HTTP APIs and asynchronous RabbitMQ events; each service owns its database.

## What you can do

- Submit a webpage URL and follow its extraction and summarization status.
- Review generated summaries and receive in-app notifications.
- Track websites, accounts you remember creating, and category-level personal-data notes.
- Inspect cookie and tracker **metadata** with the optional browser extension.
- Prepare access, deletion, and other privacy requests; review and submit them yourself.
- Ask a grounded assistant questions about your own saved footprint records.
- Export or delete Link Context footprint records.
- Optionally connect Gmail to find **possible** website-account matches from recent message headers.

### Privacy by design

- Gmail discovery is off unless an operator configures it. It requests read-only metadata access, scans at most 100 recent messages, and never requests or stores message bodies. It cannot find every account or retrieve data held by websites.
- Email matches are low-confidence possibilities—not proof of an account—and require your confirmation.
- Cookie sync sends metadata only, never cookie values, passwords, or session tokens.
- Privacy-request messages are not sent automatically; users review and submit them through the website's official channel.
- A website's deletion claim is not treated as proof that all copies were erased.

See the [Digital Footprint guide](docs/DIGITAL-FOOTPRINT.md) for detailed behavior, API routes, provider setup, data handling, and limitations.

## Repository layout

| Path | Purpose |
|---|---|
| `backend/api-gateway` | Public API entry point, JWT validation, routing, and rate limiting |
| `backend/link-service` | Link ownership, lifecycle, and event publication |
| `backend/extraction-service` | Safe webpage fetching, extraction, and Website Intelligence |
| `backend/summarization-service` | AI summaries, grounded privacy assistant, and request drafts |
| `backend/user-service` | User profiles and My Digital Footprint data |
| `backend/notification-service` | In-app notifications and email delivery |
| `backend/common/event-contracts` | Shared event and routing-key contracts |
| `frontend/` | React/TypeScript web application |
| `browser-extension/` | Optional Manifest V3 cookie-metadata extension |
| `infrastructure/` | Keycloak, database, monitoring, and deployment configuration |
| `docs/` | Public API contract and feature documentation |

## Run locally with Docker Compose

### Requirements

- Docker Engine with Docker Compose v2
- Network access to pull container images and the configured AI model
- Enough memory for the application, databases, and monitoring services

### Start the stack

```sh
cp .env.example .env
```

Edit `.env`: replace every placeholder with a unique local secret, configure Keycloak as described in [Keycloak setup](infrastructure/KEYCLOAK.md), and provide any credentials required by the services you plan to use. Do not commit `.env` or real credentials.

```sh
docker compose config --quiet
docker compose up --build -d
docker compose ps
```

By default, the web app is at `http://localhost:5173` and the public API gateway is at `http://localhost:8084`. Sign in through the configured Keycloak realm. The frontend's public Keycloak/API settings are passed at image build time; see `.env.example` and [deployment operations](infrastructure/DEPLOYMENT.md).

To stop the stack without deleting persistent data:

```sh
docker compose down
```

Avoid `docker compose down -v` for routine shutdown: it deletes named volumes, including local database data. PostgreSQL initialization scripts apply only when the database data directory is first created.

## Optional Gmail discovery

Gmail discovery is disabled by default and is not simulated when provider configuration is missing. To enable it, an operator must configure a Google OAuth web application, enable Gmail API access, register the exact redirect URI, and complete any consent-screen verification required by Google:

```text
http://localhost:8084/api/v1/digital-footprint/connected-email/oauth/google/callback
```

Configure `GOOGLE_EMAIL_DISCOVERY_ENABLED`, `GOOGLE_EMAIL_DISCOVERY_ENCRYPTION_KEY`, `GOOGLE_EMAIL_DISCOVERY_FRONTEND_URL`, `GOOGLE_OAUTH_CLIENT_ID`, `GOOGLE_OAUTH_CLIENT_SECRET`, and `GOOGLE_OAUTH_REDIRECT_URI` in the deployment environment. The encryption key must be Base64 encoding of exactly 32 random bytes; store it as a secret and do not rotate it without re-encrypting stored tokens.

The user explicitly consents before connecting. OAuth state is single-use, short-lived, and bound to the initiating user; refresh tokens are encrypted at rest. Disconnect attempts provider revocation and deletes locally stored credentials and imported discoveries. See the [provider setup and limits](docs/DIGITAL-FOOTPRINT.md#accounts-cookies-browser-integration) for details.

## Development and tests

### Backend

Java 21 and Maven are required. From the repository root:

```sh
mvn -DskipTests package
mvn test
```

Some backend tests use Testcontainers and require a working Docker daemon. To build or test an individual service and its required modules:

```sh
mvn -pl backend/user-service -am -DskipTests package
mvn -pl backend/user-service -am test
```

### Frontend

Node.js and npm are required:

```sh
cd frontend
npm ci
npm run dev
```

Available checks:

```sh
npm test
npm run typecheck
npm run lint
npm run build
```

### Browser extension

Run its metadata and behavior tests with:

```sh
npm test --prefix browser-extension
```

Extension permissions and the data it syncs are documented in the [Digital Footprint guide](docs/DIGITAL-FOOTPRINT.md#browser-extension).

## Architecture and documentation

The main URL-to-summary workflow is asynchronous: the Gateway authenticates requests, Link Service records submissions, Extraction Service fetches and stores content, and Summarization Service generates summaries. RabbitMQ events update link status and notify users. PostgreSQL databases are service-owned; Redis supports Gateway rate limiting; Keycloak provides user and service authentication.

- [Architecture and API status](infrastructure/ARCHITECTURE.md)
- [Digital Footprint guide](docs/DIGITAL-FOOTPRINT.md)
- [OpenAPI contract](docs/openapi.yaml)
- [Keycloak setup](infrastructure/KEYCLOAK.md)
- [Messaging operations](infrastructure/MESSAGING.md)
- [Deployment, configuration, backup, and troubleshooting](infrastructure/DEPLOYMENT.md)
- [Docker-host deployment checklist](infrastructure/DOCKER-HOST-CHECKLIST.md)

## Scope and limitations

Link Context organizes information users provide or features observe; it does not have universal access to accounts or third-party website databases. Website analysis, AI-generated content, public privacy-contact discovery, and email-based account matches can be incomplete or incorrect. Verify findings with the relevant website. Privacy requests must use that website's official process, and this application is not legal advice.

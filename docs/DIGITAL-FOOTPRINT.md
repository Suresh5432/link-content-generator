# My Digital Footprint

**Status:** implemented and covered by automated tests in this repository. This document is the source of truth for the feature's scope, guarantees and deliberate limits. It is not legal advice.

## What the feature does

Users can maintain a private inventory of websites where they have accounts or shared information, record category-level personal-data records, inspect browser cookie/tracker metadata, discover public privacy-request channels, generate and approve privacy requests, track responses and reminders, ask a grounded AI assistant about their own records, and export or delete their footprint data.

The product keeps these distinct at all times:

- data in the user's browser vs data on a website's servers;
- data shared with third parties vs first-party data;
- data declared in a policy vs observed on a website vs confirmed by the user vs inferred by AI;
- a request that was merely prepared vs submitted vs acknowledged vs claimed completed by a website vs independently verified by the user.

## Service ownership

| Concern | Owner |
|---|---|
| Footprint, personal-data, accounts, cookies, requests, preferences, browser installations, export/delete | `user-service` (`user_db`) |
| Public privacy-page discovery during Website Intelligence | `extraction-service` (`extraction_db`) |
| Grounded privacy assistant and AI request drafts | `summarization-service` (`summary_db`) |
| In-app/email notifications for request updates and reminders | `notification-service` (`notification_db`) |
| Public routing, JWT validation, Redis rate limiting | `api-gateway` |

Services never read another service's database. Cross-service communication uses versioned RabbitMQ events in `common/event-contracts`.

## Database

Migration `backend/user-service/src/main/resources/db/migration/V5__expand_digital_footprint.sql` adds/extends:

- `tracked_websites` — category, risk, account status, policy URLs, inspection time, data categories, tracking technologies, deletion status, source analysis and first-recorded time.
- `personal_data_records` — category, type, description, confirmation/observation/policy/inference flags, date shared, evidence source, verification time.
- `privacy_requests` — full lifecycle timestamps, contacts, message, submission method/reference, expected response, website response, claim/verification flags, reminder time.
- `user_website_accounts`, `cookie_inventory`, `privacy_request_evidence`, `privacy_request_history`, `website_privacy_contacts`, `user_privacy_preferences`, `browser_integrations`, `browser_sync_history`.

`notification-service` migration `V2__privacy_notifications.sql` makes `link_id` nullable and adds `request_id` so a notification can belong to a privacy request.

Hibernate runs with `ddl-auto=validate`; the Flyway migrations are exercised by Testcontainers-backed context tests in user, notification, extraction, summarization and link services.

## Public API

All routes are behind the gateway and require a Keycloak JWT. Every user-scoped query is filtered by the token `sub`; a request for another user's record returns `404` or `403`, never the record.

### Websites

- `POST /api/v1/digital-footprint/websites`
- `POST /api/v1/digital-footprint/websites/import` (from a completed Website Analysis)
- `GET /api/v1/digital-footprint/websites` (paged)
- `GET /api/v1/digital-footprint/websites/search?q=&category=&riskLevel=&deletionStatus=&sort=`
- `GET /api/v1/digital-footprint/websites/metrics`
- `GET|PATCH|DELETE /api/v1/digital-footprint/websites/{id}`

Removing a website from the dashboard is documented in the API/UI as removing the tracking record only; it does not delete anything held by the website.

### Personal data

- `POST|GET /api/v1/digital-footprint/websites/{websiteId}/data-records`
- `GET /api/v1/digital-footprint/data-records`
- `PATCH|DELETE /api/v1/digital-footprint/data-records/{id}`

`SensitiveDataGuard` rejects passwords, card/CVV/bank-secret fields, session/access tokens and 13+ digit sequences. Records store categories and descriptions, not secret values.

### Accounts, cookies, browser integration

- `GET|POST /api/v1/digital-footprint/accounts`; `PATCH|DELETE /api/v1/digital-footprint/accounts/{id}`
- `GET /api/v1/cookies`, `GET /api/v1/cookies/domains`, `GET /api/v1/cookies/tracking`
- `DELETE /api/v1/cookies/inventory/{cookieId}` — deletes **metadata only** and returns a message explaining that the browser cookie remains until removed in the browser.
- `POST /api/v1/browser-integrations/register`, `POST /api/v1/browser-integrations/sync`
- `GET /api/v1/browser-integrations/{id}/history`, `DELETE /api/v1/browser-integrations/{id}`

`CookieMetadata` has no `value` field by design. Sync accepts metadata only and is capped at 5,000 cookies per request.

### Privacy contacts and preferences

- `PUT|GET /api/v1/digital-footprint/websites/{id}/privacy-contact`
- `GET /api/v1/digital-footprint/privacy-contacts`
- `GET|PUT /api/v1/privacy-preferences`

Preferences are the user's own defaults. The UI states that changing a preference here does not change consent on another website.

### Privacy requests

- `POST|GET /api/v1/privacy-requests`; `GET|PATCH /api/v1/privacy-requests/{id}`
- `GET /api/v1/privacy-requests/reminders`, `GET /api/v1/privacy-requests/review`
- `POST /api/v1/privacy-requests/{id}/approve|submit|cancel|response|verify|reminder-sent`
- `PATCH /api/v1/privacy-requests/{id}/status`
- `POST|GET /api/v1/privacy-requests/{id}/evidence`
- `GET /api/v1/privacy-requests/{id}/history`

Request types: access, deletion, account deletion, correction, marketing opt-out, tracking opt-out, cookie removal, consent withdrawal, portability.

Lifecycle: `DRAFT → READY_FOR_REVIEW → AWAITING_USER_APPROVAL → SUBMITTED → ACKNOWLEDGED → IN_PROGRESS → NEEDS_VERIFICATION → COMPLETED`, plus `REJECTED`, `EXPIRED`, `FAILED`, `CANCELLED`. Invalid transitions return `409`.

Guardrails enforced in code and tests:

- a message is required before approval;
- submission is rejected until the user has approved the exact message;
- duplicate open requests for the same website/type are rejected;
- raw secrets are rejected in request text;
- `COMPLETED` requires independent user verification (a website claim is recorded separately);
- every transition writes `privacy_request_history`.

### Export and deletion

- `GET /api/v1/digital-footprint/export`
- `DELETE /api/v1/digital-footprint/account-data`

The delete endpoint removes footprint-owned rows and returns counts plus a notice about legal/operational retention. It is scoped to the authenticated subject.

### AI

- `POST /api/v1/privacy-assistant` — grounded Q&A over the user's records.
- `POST /api/v1/privacy-request-drafts` — opt-in AI draft of a request message.

Both prompts forbid inventing records, require evidence labels, and forbid secrets. The draft endpoint receives only request type, website name and category-level data categories (never raw values). The UI labels AI drafts as suggestions; the user must edit and approve before submission. No request is ever auto-sent.

## Browser discovery and Website Intelligence

`WebsiteAnalysisInspectionRunner.detectPrivacyDiscovery` extracts findings from public pages: privacy policy, data deletion, account deletion, data access, cookie management, consent management, DPO contact, and privacy contact email (mailto or page text). Each finding stores its source URL as evidence. Heuristics live in `PrivacyDiscoveryClassifier` and are unit-tested. Findings are evidence for the user to verify, not a legal determination.

## Browser extension

`browser-extension/` is a Manifest V3 extension:

- permissions: `cookies`, `activeTab`, `storage`; no `webRequest`, `history`, `tabs`, `downloads`, `management` or `proxy`;
- uploads only name, domain, host-only, secure, HttpOnly, SameSite, expiration and derived category/tracking flags;
- never reads/uploads `cookie.value`, path, storeId, session identifiers or tokens;
- cookie removal happens locally via `chrome.cookies.remove` only after an explicit user action;
- the UI explains that removal can sign the user out/reset preferences;
- manual mode documents browser-specific deletion for users who do not install the extension.

Run the extension permission/metadata tests with:

```sh
node --test browser-extension/manifest.test.mjs
```

## Frontend

`frontend/src/DigitalFootprintPage.tsx` adds **My Digital Footprint** with tabs for Dashboard, My websites, Personal data, Cookies & trackers, Remove my data, Preferences, Privacy assistant, and Export & delete. It supports import from an analysis, filters/sorting, privacy-contact discovery, AI drafting, approval/submission, response/verification, history, evidence and account export/deletion. Website identity badges use a local initial instead of fetching third-party favicons, so domains are not leaked to an image service.

## Events and reminders

- `privacy.request.status.changed.v1` → notification-service in-app update (request-linked).
- `privacy.request.reminder.v1` → notification-service reminder.
- user-service publishes after commit via a transactional event relay and outbox-style resilient publisher; a scheduler emits a follow-up reminder for requests pending 14 days (configurable `digital-footprint.reminders.*`).

## Security and privacy controls

- Keycloak JWT on every user route; ownership enforced in services, not only controllers.
- Gateway routes only the public API; no `/internal/**` route.
- No passwords, raw cookie values, session tokens, full card numbers or identity numbers are stored.
- AI calls are explicit, category-level and never include secrets; the assistant is grounded and must say when data is not recorded.
- No automated submission of requests and no CAPTCHA/auth bypass. The user submits through the official channel and records the reference/evidence.
- Audit trail: request history plus evidence rows with source URLs.
- Export/delete for the authenticated user.

## Tests and verification (2026-11-23)

Backend module test runs (all green):

| Module | Tests |
|---|---|
| link-service | 17 |
| extraction-service | 27 |
| summarization-service | 33 |
| user-service | 50 |
| notification-service | 9 |
| api-gateway | 13 |

Frontend:

```sh
cd frontend
npx tsc -b --force
npm test        # 22 tests
npm run lint    # clean
npm run build   # production build
```

Extension:

```sh
node --test browser-extension/manifest.test.mjs   # 4 tests
```

Coverage includes website registration, data inventory, ownership, request generation, approval, submission, duplicates, transitions, reminders, cookie sync/removal messaging, cross-user denial, sensitive-data rejection, AI assistant/draft prompts, privacy notifications, migrations, and frontend flows.

### Environment note

A single unfiltered `mvn test` in this workspace is resource-sensitive: with the full Compose stack and other processes running, the pre-existing `ExtractionRetryPublisherRabbitMQIntegrationTest` occasionally loses its Testcontainers RabbitMQ connection (`connection reset`/`postmaster exit`). The same module and class pass when run alone, and every module passes sequentially. This is an environment-capacity flake, not a code failure; run with more free memory (or stop unused local stacks) for a single full-reactor invocation.

## Deliberate limitations

- No connected-email OAuth discovery (registration/verification email scanning) is implemented. Manual account records are supported. Any future implementation must use read-only scopes, never mailbox deletion, extract only registration evidence, and mark results as possible until confirmed.
- No automatic request submission. The safe path is: prepared message + official channel + user action + recorded reference/evidence.
- No file-upload evidence; evidence is textual/URL references. Secure upload can be added later with malware scanning, size/type limits and private storage.
- Deleting cookie metadata does not delete browser cookies; the extension or the user's browser does.
- AI is not used to infer health/identity facts, and AI output is never treated as confirmed.

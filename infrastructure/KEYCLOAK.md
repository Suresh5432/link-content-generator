# Keycloak setup for local Compose

The service configuration expects the realm `link-context`. Create/configure it in Keycloak at `http://localhost:8080` before relying on protected API calls. No realm import file is currently supplied, so realm bootstrap and client setup are operator tasks.

## Issuer and audiences

The realm’s token `iss` claim must exactly equal `KEYCLOAK_ISSUER_URI_DOCKER`, defaulting to `http://keycloak:8080/realms/link-context` for the services. The same issuer must be resolvable by any host-based client that validates these tokens; use a consistent hostname and local host mapping if needed. Do not issue tokens with `localhost` as issuer while backend validators expect `keycloak`.

Create/configure audiences matching the Compose defaults:

| Consumer | Audience env | Default audience |
|---|---|---|
| Gateway | `KEYCLOAK_EXPECTED_AUDIENCE` | `link-context-gateway` |
| Link Service | `LINK_SERVICE_EXPECTED_AUDIENCE` | `link-service` |
| Extraction Service | `EXTRACTION_SERVICE_EXPECTED_AUDIENCE` | `extraction-service` |
| Summarization Service | `SUMMARIZATION_SERVICE_EXPECTED_AUDIENCE` | `summarization-service` |
| User Service | `USER_SERVICE_EXPECTED_AUDIENCE` | `user-service` |
| Notification Service | `NOTIFICATION_SERVICE_EXPECTED_AUDIENCE` | `notification-service` |

Set token audience mappers/roles so user tokens include the Gateway audience accepted by Gateway, and each backend accepts the audience configured for that service. The exact access-token content must be confirmed from issued tokens; a successful Keycloak health check does not prove JWT validation is configured correctly.

## Service clients

Configure confidential service clients with service accounts and client-credentials flow. Store client secrets in `.env` or a deployment secret store, never in Git. The local Compose realm has dedicated `extraction-service` and `notification-service` clients; each is client-credentials only and has one service-specific API scope.

- `EXTRACTION_CLIENT_ID=extraction-service` and `EXTRACTION_CLIENT_SECRET`: used by Summarization Service to call Extraction Service. Its default client scope is only `extraction.read`, which emits the `extraction-service` audience.
- `NOTIFICATION_CLIENT_ID=notification-service` and `NOTIFICATION_CLIENT_SECRET`: used by Notification Service for authorized user lookup. Its default client scope is only `users.read`, which emits the `user-service` audience. `INTERNAL_USER_LOOKUP_CLIENT_IDS` is set to `notification-service`.
- Keep User Service’s `INTERNAL_USER_LOOKUP_CLIENT_IDS` limited to callers that need profile lookup; local Compose allows only `notification-service`.

The target service must accept the client’s audience and required scope. Confirm the application authorities/claims from actual client-credentials tokens and exercise both allowed and denied lookups before deployment. Client IDs or secrets present in `.env.example` are placeholders/default names, not working credentials.

## Human users

Create the browser/client application according to the chosen frontend’s OAuth flow (the frontend is outside the current backend delivery). User profile email synchronization trusts only a verified email claim. Test tokens with missing, unverified, and verified email claims; missing/unverified email must not be treated as verified contact data.

## Smoke checks

1. Confirm `GET http://localhost:8080/realms/link-context/.well-known/openid-configuration` advertises the configured issuer.
2. Obtain a user token and inspect `iss`, `aud`, `sub`, and email verification claims without logging or sharing the token.
3. Call a supported Gateway API with the token; verify no-token, wrong-issuer, and wrong-audience requests are rejected.
4. Obtain client-credentials tokens for internal clients; verify allowed calls succeed and non-allowlisted clients are rejected.

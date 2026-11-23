# Keycloak setup for local Compose

The service configuration expects the realm `link-context`. The Keycloak administration console is available at `http://localhost:8080/auth` in local Compose; browser authentication is reverse-proxied at `http://localhost:5173/auth`. No realm import file is currently supplied, so realm bootstrap and client setup are operator tasks.

## Issuer and audiences

The realm’s token `iss` claim must exactly equal `KEYCLOAK_ISSUER_URI_DOCKER`, defaulting to `http://localhost:5173/auth/realms/link-context`. The frontend reverse-proxies Keycloak under `/auth`, keeping authentication on the frontend origin. Backend JWT decoders validate that public issuer and retrieve signing keys over the Docker network from `http://keycloak:8080/auth/realms/link-context/protocol/openid-connect/certs`. The frontend redirect URI and web origin remain `http://localhost:5173`.

Keycloak must be configured with HTTP relative path `/auth`, and both the frontend and Keycloak must share the `identity-network` so Nginx can proxy the authentication endpoints. If the hostname, path, or issuer changes, recreate Keycloak, frontend, and every JWT-validating service:

```sh
docker compose up -d --force-recreate keycloak api-gateway link-service extraction-service summarization-service user-service notification-service
```

Create/configure audiences matching the Compose defaults:

| Consumer | Audience env | Default audience |
|---|---|---|
| Gateway | `KEYCLOAK_EXPECTED_AUDIENCE` | `link-context-gateway` |
| Link Service | `LINK_SERVICE_EXPECTED_AUDIENCE` | `link-service` |
| Extraction Service | `EXTRACTION_SERVICE_EXPECTED_AUDIENCE` | `extraction-service` |
| Summarization Service | `SUMMARIZATION_SERVICE_EXPECTED_AUDIENCE` | `summarization-service` |
| User Service | `USER_SERVICE_EXPECTED_AUDIENCE` | `user-service` |
| Notification Service | `NOTIFICATION_SERVICE_EXPECTED_AUDIENCE` | `notification-service` |

Set token audience mappers/roles so user tokens include the Gateway and routed API service audiences accepted by the backend. Running `python3 infrastructure/keycloak/set-login-theme.py` ensures the `link-context-web` default audience scope includes the configured Gateway, Link, Summarization, User, and Notification service audiences. The exact access-token content must be confirmed from issued tokens; a successful Keycloak health check does not prove JWT validation is configured correctly.

## Service clients

Configure confidential service clients with service accounts and client-credentials flow. Store client secrets in `.env` or a deployment secret store, never in Git. The local Compose realm has dedicated `extraction-service` and `notification-service` clients; each is client-credentials only and has one service-specific API scope.

- `EXTRACTION_CLIENT_ID=extraction-service` and `EXTRACTION_CLIENT_SECRET`: used by Summarization Service to call Extraction Service. Its default client scope is only `extraction.read`, which emits the `extraction-service` audience.
- `NOTIFICATION_CLIENT_ID=notification-service` and `NOTIFICATION_CLIENT_SECRET`: used by Notification Service for authorized user lookup. Its default client scope is only `users.read`, which emits the `user-service` audience. `INTERNAL_USER_LOOKUP_CLIENT_IDS` is set to `notification-service`.
- Keep User Service’s `INTERNAL_USER_LOOKUP_CLIENT_IDS` limited to callers that need profile lookup; local Compose allows only `notification-service`.

The target service must accept the client’s audience and required scope. Confirm the application authorities/claims from actual client-credentials tokens and exercise both allowed and denied lookups before deployment. Client IDs or secrets present in `.env.example` are placeholders/default names, not working credentials.

## Human users

The React app uses a branded Keycloak login theme so hosted sign-in and password recovery use Link Context typography, colors, layout, and recovery copy. The setup helper selects the theme, enables Keycloak's self-service password reset, and configures realm SMTP from `SMTP_USERNAME` and `SMTP_PASSWORD` in `.env`, using `SMTP_HOST`/`SMTP_PORT` or the Gmail defaults (`smtp.gmail.com:587`, STARTTLS). The SMTP username is used as the sender address. Password reset messages are sent by Keycloak, not the application notification service. Compose mounts the theme at `/opt/keycloak/themes`. Apply it to the realm after restarting Keycloak to load the mount:

```sh
docker compose up -d --force-recreate keycloak
python3 infrastructure/keycloak/set-login-theme.py
```

The theme styles the hosted page while the browser still uses the Authorization Code + PKCE flow; passwords remain handled by the identity provider. The `/auth` reverse proxy keeps the browser on the frontend origin during authentication.

The browser client is public OIDC client `link-context-web`. Enable Standard Flow, disable Direct Access Grants, and configure redirect/web origins for the frontend's actual port (Compose defaults to 5173). The setup helper ensures the built-in `roles` and API audience scopes are default client scopes, creates the `USER` realm role if needed, makes it a default for new accounts, and grants it to existing human accounts; service accounts are excluded. The API Gateway requires the `USER` role from the access token's realm role claim for application routes. Enable registration if `/signup` should be available. User profile email synchronization trusts only a verified email claim.

## Smoke checks

1. Confirm `GET http://localhost:5173/auth/realms/link-context/.well-known/openid-configuration` advertises the configured issuer.
2. Obtain a user token and inspect `iss`, `aud`, `sub`, and email verification claims without logging or sharing the token.
3. Call a supported Gateway API with the token; verify no-token, wrong-issuer, and wrong-audience requests are rejected.
4. Obtain client-credentials tokens for internal clients; verify allowed calls succeed and non-allowlisted clients are rejected.

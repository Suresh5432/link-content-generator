# Link Context Generator frontend

Responsive React + TypeScript application built with Vite. It uses the public API Gateway at `http://localhost:8084` and Keycloak for browser sign-in.

## Requirements

- Node.js 20 or newer and npm
- The project API Gateway, Keycloak realm, and browser client configured and running

## Configure Keycloak

Create a **public** OIDC client (default ID `link-context-web`) in realm `link-context`. Enable Standard Flow, configure the frontend origin as a Web Origin, and add redirect URIs for the actual frontend port (Compose defaults to 5173; Vite may fall back to 5174). Keep Direct Access Grants/password grant disabled. The client secret is not used by this SPA. Set the realm Login theme to `link-context` after restarting Keycloak to load the custom theme mounted by Compose; see [`../infrastructure/KEYCLOAK.md`](../infrastructure/KEYCLOAK.md).

Configure user tokens with the `USER` realm role and the `link-context-gateway` audience required by the API Gateway. Keycloak issuer and audience must match the backend configuration; see [`../infrastructure/KEYCLOAK.md`](../infrastructure/KEYCLOAK.md). Local Compose proxies Keycloak under the frontend origin at `/auth`. The branded login route embeds the same-origin hosted sign-in flow, so the address bar stays on `/login` while Authorization Code + PKCE completes. Login and logout results return to the workspace or the signed-out login screen.

## Run locally

```sh
cd frontend
cp .env.example .env.local
npm install
npm run dev
```

Set these environment values before starting Vite. Only public browser configuration belongs here; never put service credentials or client secrets in `VITE_*` variables.

| Variable | Default | Purpose |
|---|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8084` | Public API Gateway base URL |
| `VITE_KEYCLOAK_URL` | `<frontend origin>/auth` | Same-origin Keycloak proxy base URL. Vite proxies `/auth` to local Keycloak during development. |
| `VITE_KEYCLOAK_REALM` | `link-context` | Keycloak realm |
| `VITE_KEYCLOAK_CLIENT_ID` | `link-context-web` | Public SPA client ID |

The app uses OIDC Authorization Code flow with PKCE S256 through `keycloak-js`; access tokens are attached to Gateway calls and refreshed before expiry. No password grant or hardcoded credential is used. Configure the Gateway's CORS allowed origins to include the frontend origin.

## Available scripts

```sh
npm run dev        # Start the development server
npm run lint       # ESLint
npm run typecheck  # TypeScript project check
npm test           # Vitest and Testing Library
npm run build      # Typecheck and production build
```

## First workflow

- Sign in through Keycloak, then land on the protected workspace dashboard.
- Use `/login` for both sign-in and sign-out: it shows the appropriate action for the current session. Legacy `/signin` and `/logout` URLs redirect to `/login`. Use `/signup` for Keycloak registration and `/forgot-password` (also `/forgetpassword`) to begin Keycloak password recovery. The realm must allow user registration and password reset for those actions to be available. The recovery page sends only an optional email `login_hint`; users complete reset on Keycloak’s hosted page.
- Submit an HTTP(S) URL; the app includes an idempotency key and shows processing status.
- Browse/search links, inspect an individual link, retry a failed link, and open completed summaries with key takeaways.
- View and mark notifications read, edit the current user profile, and update notification preferences.

The typed API client uses only routes in [`../docs/openapi.yaml`](../docs/openapi.yaml): links, summaries, current user profile, notifications, and notification preferences. Records are owner scoped by the backend. A summary detail is resolved through the summary endpoints; a 404 for a link's summary while processing is treated as a normal not-ready state.

## Backend dependencies and known contract notes

- All application data requires a valid Keycloak token, the `USER` role, and Gateway audience. The Gateway is the only public API base.
- Summary results are asynchronous. The app refreshes on navigation/reload; the current API offers no live event or polling contract.
- The link list API supports pagination, status and URL search. The UI requests up to 100 entries; pagination beyond those results is not yet exposed.
- `GET /api/v1/notifications` returns Spring Data `Page` JSON; nested `pageable` and `sort` payload fields may vary by framework version. The client uses the documented stable content and page fields.
- `GET /api/v1/users/me` returns email only when Keycloak marks it verified. Profile display name, bio, and avatar URL are editable.
- The API documents no delete action in the first workflow UI. The backend does have `DELETE /api/v1/links/{id}`; deletion is intentionally not exposed in this initial frontend.

For deployment, build the frontend and serve `dist/` from a static web host with SPA fallback to `index.html`. Supply the four public environment variables at build time and allow the deployed origin in Keycloak and Gateway CORS.

## Docker Compose

The repository Compose file builds the multi-stage `frontend/Dockerfile` image and serves it through Nginx at `http://localhost:5173` by default (configurable with `FRONTEND_PORT`). Nginx proxies `/auth` to Keycloak, and the SPA derives the Keycloak URL from the current frontend origin. Set `FRONTEND_API_BASE_URL`, `FRONTEND_KEYCLOAK_REALM`, and `FRONTEND_KEYCLOAK_CLIENT_ID` in the repository `.env` before building. Changing public settings embedded in the static bundle requires rebuilding the frontend image. Set `CORS_ALLOWED_ORIGINS` to the exact frontend origin.

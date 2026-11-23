# Link Context browser extension

Optional companion for **My Digital Footprint**. It lets a user review cookie metadata
for domains they choose, inspect supported browser storage names/counts, remove selected
browser data **in their own browser profile**, and sync **cookie metadata only** to Link Context.

## Security guarantees

- `background.js` never reads or returns `cookie.value`, session identifiers, tokens, or storage values.
- Cookie metadata uploaded to Link Context is limited to: `name`, `domain`, `hostOnly`, `secure`,
  `httpOnly`, `sameSite`, `expirationDate`, `firstParty`, `trackingRelated`, `category`, `purpose`.
- Storage inspection/removal operates on names/counts and supported browser APIs only; values are not read or uploaded.
- Cookie/storage removal happens locally after explicit confirmation and affects only the current browser profile.
- The access token is pasted by the user for the current popup session; it is not persisted by the extension.
  Production should replace this with an OAuth PKCE flow and a verified app/extension handshake.

## Permissions

- `cookies`: read cookie metadata and remove selected cookies locally.
- `activeTab`: inspect/clear supported storage for the active http(s) tab after user action.
- `storage`: store extension installation ID and authorized API origin allowlist locally.
- `scripting`: inject `content.js` on demand for storage inspection/clear.
- `optional_host_permissions`: requested at runtime only when the user authorizes a Link Context API origin for sync.

## Browser storage management

- Supported where browser APIs allow: `localStorage`, `sessionStorage`, IndexedDB databases (name enumeration/removal when available),
  service worker registrations, and Cache Storage keys.
- Unsupported/unavailable APIs are reported explicitly.
- Clearing browser data does **not** delete server-side account data; use the privacy request workflow for that.

## Load locally (Chrome/Edge/Brave)

1. Open `chrome://extensions` and enable **Developer mode**.
2. Choose **Load unpacked** and select this `browser-extension/` folder.
3. Open the popup, enter the API base URL, authorize the origin, and paste a short-lived access token.
4. Scan a domain and review metadata. Delete selected cookies, clear domain cookies, inspect/clear browser storage, or sync metadata.

## Limitations

- A web page and a server API cannot delete arbitrary browser cookies/storage; only the browser extension or browser settings can.
- Deleting cookies/storage may sign the user out or reset preferences.
- Clearing browser storage does **not** delete server-side account data; use the privacy request workflow for that.
- Connected-email discovery and automated privacy request submission are intentionally not implemented.

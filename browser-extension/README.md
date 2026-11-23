# Link Context browser extension (scaffold)

Optional companion for **My Digital Footprint**. It lets a user review cookie metadata
for domains they choose, categorise trackers, remove selected cookies **in their own
browser**, and sync **metadata only** to Link Context.

## Security guarantees

- `background.js` never reads or returns `cookie.value`, session identifiers or tokens.
- The only fields uploaded are: `name`, `domain`, `hostOnly`, `secure`, `httpOnly`,
  `sameSite`, `expirationDate`, `firstParty`, `trackingRelated`, `category`, `purpose`.
- Cookie removal happens locally with `chrome.cookies.remove` after explicit confirmation.
- The access token is kept in the popup for the session; production should replace this
  with an OAuth PKCE flow and a verified `externally_connectable` handshake with the app.

## Load locally (Chrome/Edge/Brave)

1. Open `chrome://extensions` and enable **Developer mode**.
2. Choose **Load unpacked** and select this `browser-extension/` folder.
3. Open the popup, enter the API base URL (e.g. `http://localhost:8084`) and a short-lived
   access token from your signed-in Link Context session.
4. Scan a domain and review metadata. Select cookies and delete locally, or sync metadata.

## Limitations (by design)

- A web page and a server API cannot delete arbitrary cookies; only the browser extension
  or browser settings can.
- Deleting a cookie may sign the user out or reset preferences.
- Clearing browser storage does **not** delete server-side account data; use the privacy
  request workflow for that.

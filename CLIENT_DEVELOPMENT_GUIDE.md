# Port Royal Client Development Guide

This guide covers the REST API integration points shared by browser/mobile clients and a
Telegram Bot. The API is frontend-agnostic: the server owns match state and rules, and every
client reads the current match snapshot before deciding what to display or submit.

For game rules, move details, card behavior, and the full endpoint reference, see
[`GAME_RULES_AND_API.md`](GAME_RULES_AND_API.md). While developing against a running server, use
Swagger UI at `/swagger-ui/index.html` or the OpenAPI document at `/api/v1/api-docs`.

## 1. API basics

The API base URL is the server root plus `/api/v1`, for example:

```text
https://api.example.com/api/v1
```

Unless listed as public below, API requests require HTTP Basic authentication. Send JSON request
bodies with `Content-Type: application/json`. JSON fields use `camelCase`; multiword query
parameters use `underscores`. Most successful API responses use an envelope with `status`,
`message`, and `data`; some catalog endpoints return arrays directly. Errors return a consistent
JSON object with `status`, `error`, `errorCode`, `message`, `errorContext`, `errorLinks`, and
`timestamp`, plus optional `errorDetails`, `description`, or `suggestion`. The HTTP status line is
authoritative and matches the `status` field. Resource creation returns `201 Created`; queued OTP
requests return `202 Accepted`.

| Endpoint group | Authentication | Purpose                                                                              |
|----------------|----------------|--------------------------------------------------------------------------------------|
| `/public/**`   | None           | JSON registration/OTP; HTML email confirmation at server-root `/public/confirmEmail` |
| `/users/**`    | Basic          | User profile and identity operations                                                 |
| `/matches/**`  | Basic          | Create, join, start, close, retrieve matches, and access move history                |
| `/cards/**`    | Basic          | Read the card and contract catalogs                                                  |
| `/sentinel/**` | Basic          | Subscribe to match notifications                                                     |

## 2. Choose an identity model

### Normal web or app client

Each human player authenticates with their own username and password. Register an account through
`POST /public/users`:

```json
{
  "username": "alice42",
  "password": "use-a-user-supplied-secret",
  "email": "alice@example.com",
  "firstName": "Alice",
  "lastName": "Example"
}
```

Usernames must be 5-30 characters and contain only letters, digits, `_`, or `-`. Email is used for
account confirmation and Telegram identity-unification flows. After registration, the client can
use the account's Basic credentials on protected endpoints:

```sh
curl -u alice42:your-password \
  "https://api.example.com/api/v1/users/me"
```

For a browser-based client, call the API over HTTPS and do not persist passwords or encoded Basic
credentials in `localStorage`. A backend-for-frontend (BFF) can keep credentials out of browser
JavaScript entirely. If the browser calls this API directly, keep credentials in memory where
practical, handle CORS preflight requests, and never place credentials in URLs or logs.

Example browser request using credentials entered by the user at runtime:

```js
const apiBase = "https://api.example.com/api/v1";
const bytes = new TextEncoder().encode(`${username}:${password}`);
let binary = "";
bytes.forEach((byte) => {
  binary += String.fromCharCode(byte);
});

const response = await fetch(`${apiBase}/users/me`, {
  headers: {
    Authorization: `Basic ${btoa(binary)}`,
  },
});

if (!response.ok) {
  throw new Error(`API request failed: ${response.status}`);
}

const currentUser = await response.json();
```

Do not hardcode `username` or `password` in the application bundle.

### Telegram Bot client

The Telegram Bot authenticates with one technical account with the `BOT` role. It does **not**
create a backend account for every Telegram user. For supported player actions, the bot sends the
human player's Telegram numeric ID in the `tgId` request header:

```sh
curl -u "$BOT_USERNAME:$BOT_PASSWORD" \
  -H "tgId: 123456789" \
  "https://api.example.com/api/v1/matches/current"
```

Use the same bot credentials for each Telegram user, but set `tgId` to the Telegram user represented
by that particular request. Do not expose the bot credentials to Telegram users, browser code, or
mobile clients. Provision bot credentials as a server-side secret; the development seed account
must not be used in production.

The header applies when the API must resolve the real player for an action, including hosting,
joining, starting or closing a match, managing the host's AI players, retrieving a player's current
match status, and submitting a move. If a bot-mediated action omits `tgId`, the request is rejected.
`GET /matches/{match-key}` retrieves a match by key and does not require the header.

Register a Telegram-only player with the bot account using `POST /users/telegram-accounts`:

```json
{
  "username": "telegram-player",
  "telegramId": "123456789"
}
```

This registration body contains the Telegram ID; it is distinct from the `tgId` header used on
player actions.

### Linking web and Telegram identities

If a player already has a web account, keep using that account from the web client. To link it to
Telegram, request an OTP using the public `POST /public/telegram-unification-requests` endpoint
with an `email` and optional `telegramId` JSON body, then have the bot submit
`POST /users/identity-unifications` using its `BOT` credentials:

```sh
curl -X POST -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","telegramId":"123456789"}' \
  "https://api.example.com/api/v1/public/telegram-unification-requests"
```

```json
{
  "email": "alice@example.com",
  "telegramId": "123456789",
  "token": "OTP_FROM_EMAIL"
}
```

The email account must be confirmed and the Telegram identity must not already be linked. Once
unified, web requests authenticate as the user's account and bot-mediated requests use the same
person's Telegram ID; the server associates both with one player record.

## 3. Typical match lifecycle

The host creates a match, other players join before it starts, and the host starts it. The
following requests show the flow for a normal client; use the bot authentication and `tgId` header
described above when the bot performs one of these player actions.

```sh
BASE="https://api.example.com/api/v1"

# Host a match. The request body is optional; this selects configuration ID 1.
curl -u alice42:your-password -H "Content-Type: application/json" \
  -d '{"id":1,"name":"default"}' "$BASE/matches"

# A second player joins using the keyCode returned by the host request.
curl -u bob42:your-password -X POST "$BASE/matches/ABC123/players"

# The host starts the match.
curl -u alice42:your-password -X POST "$BASE/matches/current/start"

# Read the authoritative current state.
curl -u alice42:your-password "$BASE/matches/ABC123"
```

The match response's `data` contains the current snapshot. Use that snapshot to render the board
and determine the valid player, phase, and indices for a move. Do not infer indices from an earlier
snapshot: harbor, expedition, player, and contract-board ordering can change as the match
progresses. The server validates each move against the live game state.

To add an AI player before starting, the host calls `POST /matches/current/ai-players` with a difficulty of
`EASY`, `MEDIUM`, or `HARD`, and optionally a display name:

```json
{
  "difficulty": "MEDIUM",
  "name": "Rocco the Pirate"
}
```

AI players are match participants controlled by the backend; they are not the Telegram Bot account
and do not authenticate.

## 4. Submitting moves and refreshing state

Submit a move to `POST /matches/current/moves`. The required `move` field selects the action; optional fields
are used only by moves that need them.

```sh
curl -u alice42:your-password \
  -H "Content-Type: application/json" \
  -d '{"move":"DISCOVER"}' \
  "$BASE/matches/current/moves"
```

For a bot acting as Alice, add `-H "tgId: 123456789"` and authenticate as the bot technical
account. A move request may include fields such as `parameterIndex`, `pickPlayerIndex`, and
`expeditionEmployeesList`; their meaning depends on the move. For example, indices are zero-based
and must refer to the current snapshot, not catalog IDs. See the move table in
[`GAME_RULES_AND_API.md`](GAME_RULES_AND_API.md) for the complete set of move names and parameters.

On success, the move response can include a `mainEvent` and `sideEvents`. Process both: a single
move may affect players other than the one who submitted it. A successful response describes the
submitted move, not necessarily any automatic AI turns that followed it. Refresh the match
snapshot after a move or notification to display the latest state.

Useful read endpoints:

| Method and path                                | Use                                                                               |
|------------------------------------------------|-----------------------------------------------------------------------------------|
| `GET /matches/{match-key}`                                         | Read a specific match snapshot                                                    |
| `GET /matches/{match-key}?move_number=N`                           | Return `"No new moves"` with no match data when `N` equals the current move count |
| `GET /matches/current`                                             | Read the authenticated player's current match, if any                             |
| `GET /matches`                                                     | List matches, with optional filters and pagination                                |
| `GET /matches/{match-key}/moves/{move-number}`                     | Retrieve one persisted move                                                       |
| `GET /matches/{match-key}/moves`                                   | Retrieve paged move history                                                       |

For bot requests to `/matches/current`, include `tgId` so the API can resolve which player's match to
return. The `move_number` option is a lightweight unchanged-state check, not a substitute for
processing move events or reading the full snapshot when state changes.

## 5. Live updates

Sentinel notifications tell a client that a match changed; they are not a full state snapshot or
move history. When an alert arrives, refresh `/matches/{match-key}` (or `/matches/current`) and render the
returned state.

| Transport                | Endpoint                                                       | Behavior                                                                |
|--------------------------|----------------------------------------------------------------|-------------------------------------------------------------------------|
| Server-Sent Events (SSE) | `GET /sentinel/subscriptions/sse?match_key=...&seconds=3600`        | Open a stream for repeated alerts until it closes or times out          |
| Long polling             | `GET /sentinel/subscriptions/long-polling?match_key=...&seconds=120`| Wait for one alert, then subscribe again; minimum timeout is 10 seconds |
| Webhook callback         | `POST /sentinel/subscriptions/callbacks`                           | Register a URL to receive alerts for a match                            |

SSE is suitable for web/app clients that can keep a connection open. Long polling works for
clients that prefer a request/response loop. SSE and long-polling subscriptions are held in the
serving API instance; deployments with multiple instances need sticky routing for these
connections. The callback subscription is database-backed and is the preferred option when
notifications must work across instances.

Callback request body:

```json
{
  "matchKeyCode": "ABC123",
  "url": "https://client.example.com/port-royal/alerts",
  "secret": "optional-client-managed-secret"
}
```

Protect callback endpoints as you would any public webhook receiver. The subscription is associated
with the authenticated API principal; callback registration does not take a `tgId` header.
Notifications are delivered once per alert, so clients that need to recover after disconnects
should refresh the match snapshot rather than relying on notifications as a durable event log.

## 6. Client implementation notes

- Treat the API's match snapshot as authoritative; do not implement game rules or locally accept
  moves as valid before the server confirms them.
- On a `400` response, show the returned error message, refresh the snapshot, and let the player
  choose an action valid in the new phase.
- Handle `401` by requesting or refreshing the user's credentials. A `403` indicates the
  authenticated role is not permitted to perform the operation.
- Keep Basic credentials and bot secrets out of URLs, client logs, analytics, and error reports.
- Use `GET /cards` and `GET /cards/contracts` to load card and contract definitions rather than
  hardcoding catalog content.
- Encode query parameter values such as match keys, usernames, and Telegram IDs.
- Do not treat timeout, lost connection, or a client retry as proof a move failed. Read the match
  state and move history before retrying to avoid duplicating an action.

## 7. Endpoint reference

API paths below are relative to `/api/v1`; the HTML email-confirmation page is the exception and
is served at `/public/confirmEmail` from the server root.

| Method            | Path                                                                           | Notes                                                                             |
|-------------------|--------------------------------------------------------------------------------|-----------------------------------------------------------------------------------|
| `POST`            | `/public/users`                                                                | Register a standard username/password account; public                            |
| `POST`            | `/public/email-confirmations`                                                  | Request an email confirmation using a JSON body; public                          |
| `GET`             | `SERVER_ROOT/public/confirmEmail?token=...`                                    | Confirm an email token; public browser flow                                       |
| `POST`            | `/public/telegram-unification-requests`                                        | Request an OTP using a JSON body; public                                          |
| `GET`             | `/users/me`                                                                    | Get the authenticated API account; bot auth sees the technical bot account       |
| `GET`             | `/users?username=...` or `/users?telegram_id=...` or `/users?email=...`         | Look up a user by username, Telegram ID, or email                                |
| `PATCH`           | `/users/{username}`                                                            | Partially update profile fields                                                  |
| `DELETE`          | `/users/{username}`                                                            | Delete a user; requires `ADMIN` authorization                                     |
| `PATCH`           | `/users/me/password`                                                           | Change the authenticated user's password                                         |
| `POST`            | `/users/telegram-accounts`                                                    | Create a Telegram-linked user; requires `BOT` role                               |
| `POST`            | `/users/identity-unifications`                                                | Unify a Telegram identity and email account; requires `BOT` role                 |
| `POST`            | `/matches`                                                                     | Create a match                                                                   |
| `GET`             | `/matches`                                                                     | List matches; multiword filters use underscore names                             |
| `POST`            | `/matches/{match-key}/players`                                                 | Join a match before it starts                                                    |
| `POST`            | `/matches/current/start`                                                       | Start the current hosted match                                                  |
| `DELETE`          | `/matches/current`                                                             | Close the current unstarted match                                               |
| `POST` / `DELETE` | `/matches/current/ai-players` and `/matches/current/ai-players/{ai-player-username}` | Add or remove an AI player before match start                          |
| `GET`             | `/matches/current`                                                             | Retrieve the authenticated player's current match                               |
| `GET`             | `/matches/{match-key}`                                                         | Retrieve a match by key                                                         |
| `POST`            | `/matches/current/moves`                                                       | Submit one move                                                                 |
| `GET`             | `/matches/{match-key}/moves/{move-number}` or `/matches/{match-key}/moves`     | Read one move or paged move history                                             |
| `GET`             | `/cards` or `/cards/contracts`                                                 | Read the card or contract catalog                                               |
| `GET` / `POST`    | `/sentinel/subscriptions/...`                                                  | Subscribe to match alerts                                                       |

See the live OpenAPI definition for request and response schemas, and
[`GAME_RULES_AND_API.md`](GAME_RULES_AND_API.md) for detailed match, move, and notification
semantics.

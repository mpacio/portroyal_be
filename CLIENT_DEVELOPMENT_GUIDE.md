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

Unless listed as public below, API requests require an OAuth2 bearer access token. Send JSON request
bodies with `Content-Type: application/json`. Human applications sign in using OpenID Connect
Authorization Code with PKCE; the Telegram bot uses its server-side client-credentials grant. Most successful API responses use an envelope with
`status`, `message`, and `data`; some catalog endpoints return arrays directly. Errors use HTTP
status codes and generally return an object with `status`, `error`, and `message`, with optional
`errorDetails`, `description`, or `suggestion`.

| Endpoint group | Authentication | Purpose                                                                               |
|----------------|----------------|---------------------------------------------------------------------------------------|
| `/public/**`   | None           | JSON registration/OTP; HTML email confirmation at server-root `/public/confirm-email` |
| `/user/**`     | Bearer token   | User profile and identity operations                                                  |
| `/match/**`    | Bearer token   | Create, join, start, close, and retrieve matches                                      |
| `/game/**`     | Bearer token   | Submit moves and read move history                                                    |
| `/card/**`     | Bearer token   | Read the card and contract catalogs                                                   |
| `/sentinel/**` | Bearer token   | Subscribe to match notifications                                                      |

## 2. Choose an identity model

### Normal web or app client

Each human player authenticates with their own username and password. Register an account through
`POST /public/register`. This public web/app endpoint requires `username`, `email`, and `password`;
`firstName` and `lastName` are optional, and a `telegramId` is not required. Omitting the password
is rejected.

```json
{
  "username": "alice42",
  "password": "use-a-user-supplied-secret",
  "email": "alice@example.com",
  "firstName": "Alice",
  "lastName": "Example"
}
```

Usernames must be 5-30 characters and contain only letters, digits, `_`, or `-`. Email is used for account confirmation and Telegram identity-unification flows. After registration
and email confirmation, use Authorization Code + PKCE to obtain an access token:

```sh
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  "https://api.example.com/api/v1/user/me"
```

Profile changes use `PATCH /user/update?username=...` with only editable name fields. The query
username must match the authenticated user unless the caller is an administrator:

```sh
curl -H "Authorization: Bearer $ACCESS_TOKEN" -X PATCH \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Alice"}' \
  "https://api.example.com/api/v1/user/update?username=alice42"
```

For a browser-based client, use OIDC Authorization Code with PKCE (`S256`) and validate `state` and
`nonce`. Keep access and refresh tokens in memory only; never place tokens or passwords in
`localStorage`, `sessionStorage`, URLs, logs, analytics, or error reports. A full page reload means
signing in again (the authorization server may have an authenticated browser session). The SPA
calls the API over HTTPS with its short-lived bearer access token and the API allows only explicitly
configured CORS origins. Prefer a BFF if persistent browser sessions are required or if tokens must
not be available to browser JavaScript.

Example browser request after PKCE sign-in:

```js
const apiBase = "https://api.example.com/api/v1";
const response = await fetch(`${apiBase}/user/me`, {
  headers: {
    Authorization: `Bearer ${accessToken}`,
  },
});

if (!response.ok) {
  throw new Error(`API request failed: ${response.status}`);
}

const currentUser = await response.json();
```

Do not hardcode a client secret in a browser application. Browser clients are public OIDC clients
and use PKCE, not a client secret.

### Telegram Bot client

The Telegram Bot authenticates using its confidential OAuth client with the `client_credentials`
grant; its client secret is a server-side secret and must be rotated. The resulting access token
identifies the single technical account with the `BOT` role. The bot does **not** create a backend
account for every Telegram user. For supported player actions, it sends the human player's Telegram
numeric ID in the `tgId` request header:

```sh
curl -H "Authorization: Bearer $BOT_ACCESS_TOKEN" \
  -H "tgId: 123456789" \
  "https://api.example.com/api/v1/match/status"
```

Use short-lived bot access tokens for each Telegram user, but set `tgId` to the Telegram user
represented by that particular request. Do not expose the bot client secret or access token to
Telegram users, browser code, or mobile clients. Provision bot credentials as server-side secrets;
the development account must not be used in production.

The header applies when the API must resolve the real player for an action, including hosting,
joining, starting or closing a match, managing the host's AI players, retrieving a player's current
match status, and submitting a move. If a bot-mediated action omits `tgId`, the request is rejected.
`GET /match/retrieve?key_code=...` retrieves a match by key and does not require the header.

Register a Telegram-only player with the bot account using `POST /user/register/telegram`:

```json
{
  "username": "telegram-player",
  "telegramId": "123456789"
}
```

This registration body contains the Telegram ID; it is distinct from the `tgId` header used on
player actions. Unlike public web/app registration, this BOT-only endpoint does not require an
email or password. The request must authenticate as the technical account with the `BOT` role, and
its `tgId` header must match the `telegramId` in the body. If no password is supplied, the server
stores a generated internal password that is not returned; Telegram players do not sign in with
those credentials. They access player operations through the authenticated bot, which supplies their
Telegram ID in `tgId`.

### Unifying web and Telegram accounts

Account unification always finishes through the Telegram bot: the bot submits `POST /user/unify`
with the Telegram user's `tgId`, the email address, and the OTP received by email. The OTP can be
requested in either of two ways, depending on where the user starts:

#### The user starts from the web

This flow is for a user who already has a web account and wants to use that same account through
Telegram:

1. The signed-in user requests an OTP from the web client with
   `POST /user/new-tg-unify-email-otp`, authenticated as the web account. The JSON email must match
   that account's email. The email must be confirmed, and the web account must not already have a
   Telegram ID.
2. The API emails an OTP to that address.
3. The user switches to Telegram, provides the email and OTP to the bot, and the bot calls
   `POST /user/unify`.
4. If the OTP is valid and has not expired, the Telegram-only account is merged into the web
   account. The web account remains the account used for email/password sign-in, and Telegram
   requests resolve to that same user.

Web client's OTP request:

```http
POST /api/v1/user/new-tg-unify-email-otp
Authorization: Bearer <web-account-access-token>
Content-Type: application/json
```

```json
{
  "email": "alice@example.com"
}
```

#### The user starts from Telegram

This flow is for a user who has already been registered by the bot and wants to link that
Telegram-only account to an existing web account:

1. The user asks the bot to unify accounts and provides the email address of the existing web
   account.
2. The bot calls `POST /user/new-tg-unify-email-otp` using its `BOT` bearer token, with the user's
   Telegram ID in the `tgId` header and the supplied email in the JSON body.
3. The API emails an OTP to the supplied address.
4. The user provides the email and OTP to the bot. The bot calls `POST /user/unify` with the same
   `tgId` and those values.
5. If the OTP is valid and has not expired, the Telegram-only account is merged into the existing
   web account. The web account remains the account used for email/password sign-in, and Telegram
   requests resolve to that same user.

Bot's OTP request:

```http
POST /api/v1/user/new-tg-unify-email-otp
Authorization: Bearer <bot-access-token>
tgId: 123456789
Content-Type: application/json
```

```json
{
  "email": "alice@example.com"
}
```

#### Completing unification from Telegram

For either flow, the bot makes the final request using its `BOT` bearer token and the Telegram ID
of the user who is unifying. The email and token in the request body must match the address and
OTP used for the request:

```http
POST /api/v1/user/unify
Authorization: Bearer <bot-access-token>
tgId: 123456789
Content-Type: application/json
```

```json
{
  "email": "alice@example.com",
  "token": "OTP_FROM_EMAIL"
}
```

The Telegram-only account must already be registered and not already linked. OTPs expire after
24 hours; requesting another OTP for the same email replaces the previous one. The bot must keep
its credentials server-side and must set `tgId` to the Telegram user represented by each request.

## 3. Typical match lifecycle

The host creates a match, other players join before it starts, and the host starts it. The
following requests show the flow for a normal client; use the bot authentication and `tgId` header
described above when the bot performs one of these player actions.

```sh
BASE="https://api.example.com/api/v1"

# Host a match. The request body is optional; this selects configuration ID 1.
curl -H "Authorization: Bearer $ACCESS_TOKEN" -H "Content-Type: application/json" \
  -d '{"id":1,"name":"default"}' "$BASE/match/host"

# A second player joins using the keyCode returned by the host request.
curl -H "Authorization: Bearer $BOB_ACCESS_TOKEN" -X POST "$BASE/match/join?key_code=ABC123"

# The host starts the match.
curl -H "Authorization: Bearer $ACCESS_TOKEN" -X POST "$BASE/match/start"

# Read the authoritative current state.
curl -H "Authorization: Bearer $ACCESS_TOKEN" "$BASE/match/retrieve?key_code=ABC123"
```

The match response's `data` contains the current snapshot. Use that snapshot to render the board
and determine the valid player, phase, and indices for a move. Do not infer indices from an earlier
snapshot: harbor, expedition, player, and contract-board ordering can change as the match
progresses. The server validates each move against the live game state.

To add an AI player before starting, the host calls `POST /match/ai-player` with a difficulty of
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

Submit a move to `POST /game/move`. The required `move` field selects the action; optional fields
are used only by moves that need them.

```sh
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"move":"DISCOVER"}' \
  "$BASE/game/move"
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

| Method and path                                  | Use                                                                               |
|--------------------------------------------------|-----------------------------------------------------------------------------------|
| `GET /match/retrieve?key_code=...`               | Read a specific match snapshot                                                    |
| `GET /match/retrieve?key_code=...&move_number=N` | Return `"No new moves"` with no match data when `N` equals the current move count |
| `GET /match/status`                              | Read the authenticated player's current match, if any                             |
| `GET /match/retrieve-all`                        | List matches, with optional filters and pagination                                |
| `GET /game/move?key_code=...&move_number=N`      | Retrieve one persisted move                                                       |
| `GET /game/moves?key_code=...`                   | Retrieve paged move history                                                       |

For bot requests to `/match/status`, include `tgId` so the API can resolve which player's match to
return. The `move_number` option is a lightweight unchanged-state check, not a substitute for
processing move events or reading the full snapshot when state changes.

## 5. Live updates

Sentinel notifications tell a client that a match changed; they are not a full state snapshot or
move history. When an alert arrives, refresh `/match/retrieve` (or `/match/status`) and render the
returned state.

| Transport                | Endpoint                                                        | Behavior                                                                |
|--------------------------|-----------------------------------------------------------------|-------------------------------------------------------------------------|
| Server-Sent Events (SSE) | `GET /sentinel/sse/subscribe?key_code=...&seconds=3600`         | Open a stream for repeated alerts until it closes or times out          |
| Long polling             | `GET /sentinel/long-polling/subscribe?key_code=...&seconds=120` | Wait for one alert, then subscribe again; minimum timeout is 10 seconds |
| Webhook callback         | `POST /sentinel/callback/subscribe`                             | Register a URL to receive alerts for a match                            |

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
  "secret": "at-least-32-random-bytes-from-a-secret-generator"
}
```

Use an HTTPS callback on port 443 with a publicly routable host. The server validates the URL when
registering and again before delivery; it rejects private, loopback, link-local and metadata
destinations, IP literals, and redirects. The callback secret is encrypted at rest by the API and is
never included in the JSON body. Each delivery includes:

```text
X-Callback-Timestamp: <unix-seconds>
X-Callback-Event-Id: <unique-event-id>
X-Callback-Signature: v1=<hex HMAC-SHA-256>
```

To verify, compute HMAC-SHA-256 using the shared secret over the exact raw request bytes:
`timestamp + "." + event-id + "." + raw-body`, then compare the lowercase hex digest in constant
time. Reject timestamps outside a short window (for example, five minutes) and remember event IDs
for that window to prevent replay. The subscription is associated with the authenticated API
principal; callback registration does not take a `tgId` header. Notifications are delivered once
per alert, so clients that need to recover after disconnects should refresh the match snapshot rather
than relying on notifications as a durable event log.

## 6. Client implementation notes

- Treat the API's match snapshot as authoritative; do not implement game rules or locally accept
  moves as valid before the server confirms them.
- On a `400` response, show the returned error message, refresh the snapshot, and let the player
  choose an action valid in the new phase.
- Handle `401` by requesting or refreshing the user's credentials. A `403` indicates the
  authenticated role is not permitted to perform the operation.
- Keep access/refresh tokens and bot client secrets out of URLs, client logs, analytics, and error
  reports. Do not store browser tokens in persistent Web Storage.
- Use `GET /card` and `GET /card/contract` to load card and contract definitions rather than
  hardcoding catalog content.
- Encode query parameter values such as match keys, usernames, and Telegram IDs.
- Do not treat timeout, lost connection, or a client retry as proof a move failed. Read the match
  state and move history before retrying to avoid duplicating an action.

## 7. Endpoint reference

API paths below are relative to `/api/v1`; the HTML email-confirmation page is the exception and
is served at `/public/confirm-email` from the server root.

| Method            | Path                                         | Notes                                                                                                        |
|-------------------|----------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| `POST`            | `/public/register`                           | Register a standard username/password account; public                                                        |
| `GET`             | `/user/new-email-confirmation?email=...`     | Request an email confirmation; authentication required                                                       |
| `GET`             | `SERVER_ROOT/public/confirm-email?token=...` | Confirm an email token; public browser flow                                                                  |
| `POST`            | `/user/new-tg-unify-email-otp`               | Request an unification OTP as the email account, or as `BOT` with the user's `tgId`; body: `{"email":"..."}` |
| `GET`             | `/user/me`                                   | Get the authenticated API account; for bot auth this is the technical bot account                            |
| `GET`             | `/user/retrieve?username=...`                | Administrator-only lookup by username, Telegram ID, or email; use `telegram_id` for Telegram ID              |
| `PATCH`           | `/user/update?username=...`                  | Update first/last name only; users may update their own profile, ADMIN may update others                     |
| `POST`            | `/public/password-reset/request`             | Request a reset code; response does not disclose whether the email is registered                             |
| `POST`            | `/public/password-reset/confirm`             | Reset password using the emailed one-time code                                                               |
| `POST`            | `/user/register/telegram`                    | Create a Telegram-linked user; requires `BOT` role                                                           |
| `POST`            | `/user/unify`                                | Complete unification with `{"email":"...","token":"..."}`; requires `BOT` role and the user's `tgId` header  |
| `POST`            | `/match/host`                                | Host a match                                                                                                 |
| `POST`            | `/match/join?key_code=...`                   | Join a match before it starts                                                                                |
| `POST`            | `/match/start`                               | Start the hosted match                                                                                       |
| `POST`            | `/match/close`                               | Close an unstarted match                                                                                     |
| `POST` / `DELETE` | `/match/ai-player`                           | Add or remove an AI player before match start                                                                |
| `GET`             | `/match/status`                              | Retrieve the authenticated player's current match                                                            |
| `GET`             | `/match/retrieve?key_code=...`               | Retrieve a match by key                                                                                      |
| `POST`            | `/game/move`                                 | Submit one move                                                                                              |
| `GET`             | `/game/move` or `/game/moves`                | Read one move or paged move history                                                                          |
| `GET`             | `/card` or `/card/contract`                  | Read the card or contract catalog                                                                            |
| `GET` / `POST`    | `/sentinel/...`                              | Subscribe to match alerts                                                                                    |

See the live OpenAPI definition for request and response schemas, and
[`GAME_RULES_AND_API.md`](GAME_RULES_AND_API.md) for detailed match, move, and notification
semantics.

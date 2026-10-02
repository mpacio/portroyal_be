# Port Royal Game Rules and API Guide

This guide describes the rules implemented by `portroyal_lib` and how to play a match through the
`portroyal_be` REST API. The server is authoritative: clients request moves, and the game engine
validates and applies each move.

For the exact live API schema, use Swagger UI at `/swagger-ui/index.html` or the OpenAPI document
at `/api-docs` on a running server.

## 1. Game at a glance

- A match has **2-5 players**, counting human and AI players together. It cannot be started with
  fewer than two.
- Each player begins with the configured number of coins (3 in the default configuration).
- Players take turns exploring the shared harbor, then trading ships and hiring employees.
- Players earn points from employees, completed expeditions, and contracts.
- The first player to have **at least 12 points and at least one expedition** triggers the final
  turn when they end their turn. The game ends when turn rotation reaches the first active player
  again. The winner is chosen from players meeting the final-turn condition (12 or more points and
  at least one expedition). The qualifying player with the most points wins; ties are broken by
  coins.

The default game configuration enables the **Just One More Contract (JOMC)** expansion. It adds
additional employees, cargo ships, and a contracts board. Match configuration also controls starting
coins, tax behavior, the large expedition player threshold, cargo-ship bonus handling, and contract
limits.

## 2. Turn sequence and phases

The `active player` owns the current turn. The `running player` is the player currently expected to
submit a move. During the active player's part of the turn, these are the same; later, other players
may get one opportunity to use the harbor.

### Discover

The active player may discover cards one at a time, or finish discovering to move to trade and hire.
The player can also commit a qualifying expedition or sign an eligible manual contract during their
active turn, including while the engine is still in the Discover phase.

- **Ship:** A ship enters the harbor unless it repeats a ship color already there. If the active
  player's total power is at least the ship's power, the player may repel or accept it. Repelling
  discards the ship and records its color as repelled; accepting a repeated color causes a bust.
- **Bust:** The newly accepted ship and the entire harbor are discarded, the active player's turn
  ends, and each player's Jester effect pays one coin per Jester. Contract requirements are checked.
- **Employee:** Added to the harbor for players to hire later.
- **Expedition:** Added to the shared expedition row, where a player may claim it by committing the
  required employees.
- **Tax:** Applied immediately and discarded; see [Tax cards](#tax-cards).

Once the active player finishes discovering, their trading capacity is based on the number of
distinct ship colors in the harbor: `max(1, distinct colors - 2)`, plus one for each Governor.

### Trade, hire, and end turn

The active player can use their trading capacity to take harbor actions:

- **Trade a ship:** Discard it and gain its printed coins. Matching-color Merchants grant additional
  coins. The JOMC cargo ship has an additional coin benefit for another player (or the poorest
  player when that configuration option is enabled).
- **Hire an employee:** Pay its cost, then add it to your employee display. Each Mademoiselle reduces
  hiring costs by one coin. The active player does not pay the active-player fee.
- **End turn:** The active player ends their harbor actions. Other players are then offered a
  harbor action in order, if there is a ship to trade or an employee they can afford. Each player's
  sub-turn starts with one action; card effects such as a matching Clerk can add capacity. A
  non-active player pays one coin to the active player when trading or hiring. A player may end
  their sub-turn without taking an action. After the sub-turns, any cards left in the harbor are
  discarded and the next active player's turn begins.

The active player can also commit expeditions and sign manual contracts during the trade/hire
phase. An expedition or contract action does not consume trading capacity.

## 3. Cards and scoring

The card catalog is the source of truth for card IDs, points, costs, colors, power, and expedition
requirements. Fetch it with `GET /api/v1/card`; use `GET /api/v1/card/contract` for contract
descriptions, types, rewards, and requirements.

### Employees

- **Sailors and Pirates** add their printed power to the owner's total, which is used to repel ships.
- **Captains, Priests, and Settlers** are used to commit expeditions. When committed, the selected
  employees are discarded along with the expedition card.
- **Handymen** can substitute for any missing required expedition employee.
- **Merchants** provide one extra coin for each Merchant matching the color of a ship the owner
  trades.
- **Mademoiselles** reduce the owner's employee hiring cost by one coin each.
- **Jesters** pay one coin each to their owner whenever a bust occurs.
- **Governors** add one trading capacity each after the active player finishes discovering.
- JOMC expansion employees have these effects:
  - **Admirals:** When a harbor action is taken while the harbor has at least five cards, gain two
    coins per Admiral.
  - **Deputies:** When a harbor action is taken while the harbor has three or four cards, gain one
    coin per Deputy.
  - **Gunners:** When a harbor action is taken with more than one ship in the harbor, gain one coin
    per Gunner for each ship beyond the first.
  - **Clerks:** Trading a ship grants one additional trading capacity for each Clerk matching that
    ship's color.

Employee points count toward the final score. Employees used for an expedition are removed from
the owner's display and discarded.

### Expeditions

Each expedition specifies its points, coin reward, and required combination of Captains, Priests,
and Settlers. A player may commit if they can meet all requirements using employees they own;
Handymen may substitute. When the player owns a Handyman and could satisfy the expedition in more
than one way, the move must explicitly list which employee types to use. The player receives the
expedition's points and coins, and the used employees and expedition are removed from play.

### Tax cards

On revealing either tax card, every player holding **more coins than the configured `taxedMoney`**
loses `coins / taxRate` coins (integer division). The minimum-points tax then gives one coin to each
player tied for the lowest positive point total; the maximum-power tax gives one coin to each player
tied for the highest positive power. Default configuration values are 12 and 2 respectively.

### JOMC contracts

The expansion deals a configured number of contracts onto the board. Each contract exposes its own
requirements, available slots, and reward for each slot in the card catalog and current match state.
Manual contracts may be signed by an eligible player during their turn if they meet the
requirements, have not signed that contract already, and have not reached the configured contract
limit. The reward depends on the slot claimed. Automatic contracts are checked at the end of a turn
and after a bust; eligible players are signed automatically. Each signed contract adds one point and
its reward coins.

## 4. API basics

Set `BASE` to the service root, for example `http://localhost:8080/api/v1`. Unless noted as public,
endpoints require **HTTP Basic authentication**. Send JSON with `Content-Type: application/json`.
Bot-mediated calls use the bot account's Basic credentials plus a `tgId` header containing the
human player's Telegram ID. AI players are match participants, not API identities.

Example authenticated request:

```sh
curl -u alice:password "$BASE/user/me"
```

The server returns JSON response wrappers for most operations. Validation failures, illegal moves,
missing resources, and authentication or authorization failures are reported as HTTP errors. Use the
returned match snapshot as the authoritative source for player ordering, phase, harbor indices,
expedition indices, contract indices, and current state.

## 5. Register and identify players

| Method and path | Purpose |
|---|---|
| `POST /public/register` | Create a standard account; public. |
| `GET /public/newEmailConfirmation?email=...` | Request email confirmation; public. |
| `GET /public/confirmEmail?token=...` | Confirm an email token; public browser flow. |
| `GET /public/newTgUnifyEmailOtp?email=...&telegramId=...` | Request the email OTP to link a Telegram identity; public. The email must be confirmed and not already linked. |
| `POST /user/register/telegram` | Create a Telegram-linked player; requires `BOT` authentication. |
| `POST /user/unify` | Link the Telegram ID to the email account using the OTP; requires `BOT` authentication. |
| `GET /user/me` | Get the authenticated account. |
| `GET /user/retrieve?username=...` | Look up a user by username, Telegram ID, or email. |
| `PUT /user/update?username=...` | Update user profile; requires `ADMIN` or `BOT` authorization. |
| `POST /user/changePsw` | Change the authenticated user's password. |
| `DELETE /user/delete?username=...` | Delete a user; requires `ADMIN` authorization. |

Standard registration body:

```json
{
  "username": "alice42",
  "password": "replace-with-a-secret",
  "email": "alice@example.com",
  "firstName": "Alice",
  "lastName": "Example"
}
```

Usernames must be 5-30 characters and contain letters, digits, `_`, or `-`. Telegram IDs are
numeric strings. Bot registration requires a username and Telegram ID, for example:

```json
{
  "username": "telegram-player",
  "telegramId": "123456789"
}
```

Do not use development seed credentials in a deployed environment.

For bot requests to match and game endpoints, include, for example:

```sh
curl -u shaslabot:replace-with-secret -H "tgId: 123456789" ...
```

The API resolves that Telegram ID to the real player for supported bot-mediated operations. It is
not an optional identity hint for a bot request.

## 6. Create and manage a match

| Method and path | Purpose |
|---|---|
| `POST /match/host` | Host a match. Optional JSON body identifies a known configuration by `id` and `name`; omitting it selects default configuration ID 1. |
| `PUT /match/join?keyCode=...` | Join an open match before it starts. |
| `POST /match/ai-player` | Host adds an AI before the match starts. Body requires `difficulty` (`EASY`, `MEDIUM`, or `HARD`) and may include `name`. |
| `DELETE /match/ai-player?aiPlayerUsername=...` | Host removes an AI before the match starts. |
| `PUT /match/start` | Host starts the match; requires at least two total players. |
| `PUT /match/close` | Close an unstarted match the caller participates in. |
| `GET /match/status` | Get the caller's current match, if any. |
| `GET /match/retrieve?keyCode=...` | Get the match snapshot. |
| `GET /match/retrieve?keyCode=...&moveNumber=N` | If `N` equals the current move count, returns "No new moves"; otherwise returns the snapshot. |
| `GET /match/retrieve-all` | List matches; supports `username`, `ended`, `pageNumber`, `pageSize`, `sortField`, and `sortDirection`. |

Typical sequence:

```sh
# Host (save the keyCode returned in the response)
curl -u alice:password -H "Content-Type: application/json" \
  -d '{"id":1,"name":"default"}' "$BASE/match/host"

# Another player joins with that keyCode
curl -u bob:password -X PUT "$BASE/match/join?keyCode=ABC123"

# Host starts
curl -u alice:password -X PUT "$BASE/match/start"

# Read state before choosing a move
curl -u alice:password "$BASE/match/retrieve?keyCode=ABC123"
```

The host is automatically a player. A player cannot join multiple open matches. Human and AI
players share a maximum capacity of five. The match composition is locked once started.

## 7. Submit game moves

All moves use `POST /game/move`. Only the current running player can move. The required `move`
field is case-insensitive; the other fields are optional and default to `-1` or an empty list when
omitted.

| Move | `parameterIndex` | `pickPlayerIndex` | `expeditionEmployeesList` |
|---|---|---|---|
| `DISCOVER` | Not used | Not used | Not used |
| `REPEL`, `ACCEPT`, `FINISH_DISCOVER` | Not used | Not used | Not used |
| `TRADE` | Index of a ship in the harbor | Cargo-ship bonus recipient; otherwise `-1` | Not used |
| `TRADE_RENOUNCE` | Index of a ship in the harbor | Cargo-ship bonus recipient; otherwise `-1` | Not used |
| `HIRE` | Index of an employee in the harbor | Not used | Not used |
| `END_TURN` | Not used | Not used | Not used |
| `COMMIT_EXPEDITION` | Index in the current expedition list | Not used | Employee types to consume, if needed |
| `SIGN_CONTRACT` | Index on the current contracts board | Not used | Not used |

For `TRADE` / `TRADE_RENOUNCE`, the cargo-ship recipient is the zero-based player index shown in
the match snapshot and cannot be the running player. Use `-1` if the selected ship is not a cargo
ship. For a normal `HIRE`, `parameterIndex` is the employee's current harbor index.

Example discover:

```sh
curl -u alice:password -H "Content-Type: application/json" \
  -d '{"move":"DISCOVER"}' "$BASE/game/move"
```

Example expedition commit:

```json
{
  "move": "COMMIT_EXPEDITION",
  "parameterIndex": 0,
  "expeditionEmployeesList": ["CAPTAIN", "CAPTAIN", "SETTLER"]
}
```

If a player has a Handyman and there are multiple valid employee selections, send the exact
selection. Available employee names are `CAPTAIN`, `PRIEST`, `SETTLER`, and `HANDYMAN`.

`GET /game/move?keyCode=ABC123&moveNumber=12` retrieves one move.
`GET /game/moves?keyCode=ABC123` retrieves paged history; optional parameters include
`pageNumber` (0-based, default 0), `pageSize` (default 10), `sortField` (default `TIME_INDEX`),
and `sortDirection` (`ASC` or `DESC`, default `ASC`).

The move response includes the move and generated events. A human move may trigger automatic AI
turns; the API persists those AI moves as well, but the immediate move response describes the
submitted move. Retrieve the match again to see the latest state.

## 8. Card catalog and notifications

| Method and path | Purpose |
|---|---|
| `GET /card` | List base-game and JOMC card definitions. |
| `GET /card/{id}` | Get one card by catalog ID. |
| `GET /card/contract` | List contract definitions and descriptions. |
| `GET /card/contract/{id}` | Get one contract by catalog ID. |
| `POST /sentinel/callback/subscribe` | Subscribe a callback URL for a one-time alert. JSON includes `matchKeyCode`, `url`, and optional `secret`. |
| `GET /sentinel/long-polling/subscribe?keyCode=...&seconds=120` | Wait for the next alert, delivered once; minimum timeout is 10 seconds. |
| `GET /sentinel/sse/subscribe?keyCode=...&seconds=3600` | Receive alerts as Server-Sent Events until timeout. |
| `DELETE /sentinel/subscription?keyCode=...` | Remove the caller's subscription. |

Callback alerts are database-backed. Long-polling and SSE subscriptions are held by the serving
API instance, so deployments using multiple instances should use sticky routing for these
connections. Clients can always poll `/match/retrieve` for match state.

## 9. Client integration checklist

1. Authenticate each protected request with HTTP Basic; use `tgId` when a bot acts on behalf of a
   human player.
2. Host or join a match, and wait for the host to start it.
3. Read the match snapshot before a move. Use its current player ordering and board lists to
   resolve all indices.
4. Submit one atomic move at a time. If the engine rejects a move, refresh the match snapshot and
   follow the current phase.
5. Poll or subscribe for updates; refresh after any AI turn or notification.
6. Read the catalog endpoints to render exact card and contract values instead of hardcoding card
   IDs or effects.

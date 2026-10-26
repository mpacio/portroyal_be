# Port Royal — Back End

A digital, server-authoritative back end for the tabletop card game **Port Royal**. It exposes a
REST API that lets any number of clients — web/app front ends, CLI tools, and a Telegram Bot — host
matches, join them, and play moves, while the game rules themselves are entirely encapsulated in a
standalone Java library.

This repository is a **Maven multi-module** project:

| Module                           | Type                                | Responsibility                                                                                                                                                       |
|----------------------------------|-------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [`portroyal_lib`](portroyal_lib) | Plain Java library (Java 21)        | The **game engine**: all Port Royal rules, cards, phases, effects and match/move validation logic. No web, no persistence, no framework dependency.                  |
| [`portroyal_be`](portroyal_be)   | Spring Boot 3 application (Java 21) | The **REST API**: authentication, persistence (PostgreSQL/JPA), match/user orchestration, real-time notifications. Delegates every rule decision to `portroyal_lib`. |

The root [`pom.xml`](pom.xml) is a `pom`-packaged aggregator; building it builds both modules in the
correct order (the library is built/installed before the API, since `portroyal_be` depends on it).

---

## Table of contents

- [Where the game logic lives](#where-the-game-logic-lives)
- [Designed for many kinds of clients](#designed-for-many-kinds-of-clients)
  - [Autonomous (AI) players](#autonomous-ai-players)
- [Designed to scale horizontally](#designed-to-scale-horizontally)
- [Requirements](#requirements)
- [Configuration / environment variables](#configuration--environment-variables)
- [Database](#database)
- [Build & run](#build--run)
- [API overview](#api-overview)
- [Production roadmap](docs/PRODUCTION_ROADMAP.md)
- [OAuth client operations](docs/OAUTH_CLIENT_OPERATIONS.md)
- [Operations runbook draft](docs/OPERATIONS_RUNBOOK.md)
- [Operations runbook draft](docs/OPERATIONS_RUNBOOK.md)
- [Game rules and API guide](docs/GAME_RULES_AND_API.md)
- [Client development guide](docs/CLIENT_DEVELOPMENT_GUIDE.md)
- [Testing](#testing)
- [Development notes](#development-notes)

---

## Where the game logic lives

**All of the Port Royal rules and game engine code live in the [`portroyal_lib`](portroyal_lib)
module**, under `com.matteopaciolla.portroyal`:

- `core` — `Match`, `Player`, `Table`, `Deck`, `ContractsBoard`, `MoveRecord`, `Event`, plus
  `phases`/`effects` sub-packages implementing the game's turn/phase state machine.
- `confs` — deck configuration/dictionaries, including the base game (`BaseDeckDictionary`) and the
  `JOMC` expansion (`JOMC_ExpansionDeckDictionary`), and the tunable `Configuration` for a match.
- `cards`, `exceptions`, `facades`, `util` — card definitions, game-specific exception hierarchy,
  CSV import/export helpers for move logs, and misc utilities.

`portroyal_be` never re-implements or duplicates a rule: it only creates/loads a `Match` object from
`portroyal_lib`, forwards player input as a `MoveRecord`, and asks the library to validate/apply it
(`Match#executeMove`, `Match#discover`, ...). If the move is illegal, the library throws a
`UserInputException` subtype which the API translates into an HTTP error. This separation means the
engine can be reused (and is reused, via `MainClass`/`GamerUI`, in `portroyal_lib`) completely
independently of the web API, e.g. for CLI simulations or automated tests replaying CSV move logs.

The API depends on a specific library version through the `prlib.version` property in
[`portroyal_be/pom.xml`](portroyal_be/pom.xml), and it re-exposes that version to the running
application at `portroyal.lib.version` (used, among other things, to tag stored moves with the
engine version that produced them — see `moves.lib_version` column).

## Designed for many kinds of clients

The API is deliberately **frontend-agnostic**: the same match/user data must be reachable, at the
same time and consistently, from **N different kinds of clients** — a JS/mobile web app, a CLI/
scripting client, and, notably, a **Telegram Bot**. This isn't an afterthought: it's baked into the
authentication model.

- The Telegram integration uses **one confidential OAuth client** for the BOT, not a technical
  `UserEntity` and not one backend account per Telegram player.
- The BOT authenticates with its client credentials for narrowly scoped account onboarding/linking.
  To act as a linked human, it exchanges the Telegram sender ID at the custom Telegram delegation
  grant. The resulting bearer token has the human's stable database ID as `sub` and records the BOT
  client as `actor_client_id`.
- Ordinary match and game requests use that delegated bearer token and do not send `tgId`. The
  backend resolves the acting human from the token subject, and persisted matches/moves remain
  associated with that human `UserEntity`.
- A player can be registered directly through the bot (`POST /api/v1/user/register/telegram`, BOT onboarding scope)
  using a username and Telegram id without an email or password, or through a normal web/app
  registration (`POST /api/v1/public/register`) using username/email/password. Telegram onboarding
  requires a BOT-authorized access token and matching `tgId` header and `telegramId` body values.
- The two identities can later be **unified**: a Telegram-only user requests a one-time code sent to
  an email address (`POST /api/v1/user/new-tg-unify-email-otp`), then confirms the pairing through
  the BOT onboarding scope (`POST /api/v1/user/unify`, backed by the OTP/`temporary_tokens` table). After unification,
  the same physical player can keep playing the very same match indifferently from the web front end
  or from Telegram — the API always resolves to the same `UserEntity` regardless of which channel is
  used to reach it.

Because moves/matches are keyed by the domain user (via username or Telegram id), not by client type
or session, any mix of frontends can interleave calls against the same match with no special
handling required on the API side.

### Autonomous (AI) players

A match host can also fill empty seats with autonomous, backend-controlled AI players so humans and
AI players can play side by side. This is distinct from the confidential Telegram OAuth client,
which represents a server-side integration; an AI player is a per-match, never-authenticating
participant computed by the backend.

- AI players are **not** stored in the `users` table. Each one is an `AIPlayerEntity` row in a
  dedicated `ai_players` table, owned by its `MatchEntity` (`ON DELETE CASCADE`-style cascading:
  adding/removing an AI player, or deleting the match, creates/deletes its `ai_players` rows
  automatically). They never authenticate and have no password, email, or role list of their own.
- The API still exposes AI players through the same `UserDto` shape used for real accounts (a
  virtual `UserRole.AI` tag), so clients can keep rendering one uniform player list; `MatchEntity`
  combines humans (`players`) and AI players (`aiPlayers`) — in that order — to build the library's
  ordered player list and to resolve `moves.active_player_username`/`running_player_username` by
  index.
- `POST /match/ai-player` (host-only, match not yet started) adds an AI player with a chosen
  `BotDifficulty` (`EASY`/`MEDIUM`/`HARD`) and an optional display name; `DELETE
  /match/ai-player?ai_player_username=...` removes one before the match starts.
- Once the match is running, `GameService#autoPlayAiTurns` plays out every consecutive AI player
  turn by calling `Match#calculateNextMoveRecord(BotDifficulty)` (already implemented and tested in
  `portroyal_lib`) and persisting the resulting moves exactly like a human move — no game-rule logic
  is added to `portroyal_be`. This runs right after a human's move is inserted and right after the
  match is started (in case the library randomly picks an AI player as the first player), so AI
  turns are always fully played out before control returns to the API caller.

## Designed to scale horizontally

Every `portroyal_be` instance is meant to be **stateless, disposable and independently replicable**:
you can run any number of them behind a load balancer, and a client can be served by a different
instance on every single call while still getting coherent, atomic answers — including mid-match,
even though the game engine keeps an in-memory object graph (`Match`) while a match is running.

This works because of a specific design:

1. **PostgreSQL is the single source of truth.** Every accepted move is persisted (`moves` table,
   linked to `matches`) before the API responds. No match state is considered authoritative unless
   it is durably stored.
2. **Each instance keeps only a local, disposable projection of that state.** `MatchRetainer` uses a
   Caffeine `LoadingCache<String, Match>` to avoid replaying the full move history for every request.
   The cache is purely a performance optimization, never a source of truth: before returning a
   cached `Match`, `MatchRetainer#getMatch` compares the number of moves in the cached object
   against the number of moves persisted for that match in the database. On any mismatch it
   **invalidates and rebuilds** the `Match` from scratch (`MatchCreator.createMatch(...)`, replaying
   every persisted `MoveRecord` through `portroyal_lib`).
   - Practical consequence: if a player's previous request was handled by instance A (which cached
     the match) and their next request lands on instance B, instance B independently reconstructs the
     exact same match state from the database and will keep it consistent going forward — no
     shared cache, sticky sessions or distributed lock is required for the core game/match API.
3. **API requests are stateless bearer-token requests.** The Resource Server verifies the signed
   access token and its issuer, audience, expiry, and persisted authorization on every call.
   Authorization Code with PKCE is used by public clients; only the authorization/login pages use a
   browser session.
4. **Outbound side effects are queued/derived from the DB, not kept in memory.** For example emails
   are written to an `emails_queue` table (`EmailSenderFacade`) rather than sent synchronously from
   request-handling memory, keeping request handling itself stateless.

**Known exception — real-time push notifications.** The `sentinel` long-polling
(`GET /sentinel/long-polling/subscribe`) and Server-Sent-Events
(`GET /sentinel/sse/subscribe`) endpoints keep their pending subscriptions
(`SentinelService#matchSubsMap`, holding `DeferredResult`/`SseEmitter` instances) **in local JVM
memory of the instance that accepted the subscription**. If a move is processed by a *different*
instance than the one holding a given subscription, that subscription will not be notified
in-process. Deploying these two endpoints behind a load balancer therefore requires **sticky
routing/session affinity** for the lifetime of a subscription, or accepting that push notifications
may be missed when instances change mid-subscription (the client can always fall back to polling
`GET /match/retrieve?key_code=...&move_number=...`, which is instance-agnostic by design). The
webhook-style alternative, `POST /sentinel/callback/subscribe`, has no such limitation: it stores the
callback URL in the database (`callbacks` table) and is invoked by whichever instance happens to
process the triggering move, so it works correctly with any load-balancing strategy.

## Requirements

- Java 21
- Maven 3.9+ (or the Maven Wrapper, if present locally)
- PostgreSQL (any recent version; schema is generated by Hibernate, see [Database](#database))

## Configuration / environment variables

Configuration lives in [`portroyal_be/src/main/resources/application.properties`](portroyal_be/src/main/resources/application.properties).
The following are the properties you are expected to provide via environment variables (or a
Spring `application-*.properties`/`application.yml` override) when running the service:

| Property / env var                             | Required | Default                                                                                                | Description                                                                                                                                                                          |
|------------------------------------------------|----------|--------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `SPRING_DATASOURCE_URL`                        | yes      | —                                                                                                      | JDBC URL of the PostgreSQL database, e.g. `jdbc:postgresql://host:5432/prdb`                                                                                                         |
| `SPRING_DATASOURCE_USERNAME`                   | yes      | —                                                                                                      | Database user                                                                                                                                                                        |
| `SPRING_DATASOURCE_PASSWORD`                   | yes      | —                                                                                                      | Database password                                                                                                                                                                    |
| `APP_CORS_ALLOWED_ORIGINS`                     | no       | empty (no cross-origin access)                                                                          | Comma-separated exact origins allowed by CORS, for example `https://client.example.com`; credentials are not allowed                                                                 |
| `NPM_INTERNAL_PROXY_REGEX`                     | Docker   | loopback only                                                                                            | Exact Tomcat trusted-proxy address regex; set to the NPM container's reserved address on the proxy network                                                                           |
| `OAUTH2_ISSUER`                                | production | —                                                                                                    | Stable public issuer URL for the Authorization Server                                                                                                                                |
| `OAUTH2_BOT_CLIENT_ID`                         | production | —                                                                                                   | Registered confidential Telegram OAuth client ID                                                                                                                                    |
| `OAUTH2_BOT_CLIENT_SECRET`                     | operator only | —                                                                                                 | High-entropy BOT client secret used by the BOT and operator tools; do not pass it to the API container                                                                              |
| `OAUTH2_SIGNING_KEY_PATH`                      | production | —                                                                                                   | Path to a durable PKCS#12 RSA signing keystore                                                                                                                                       |
| `OAUTH2_SIGNING_KEY_PASSWORD` or `OAUTH2_SIGNING_KEY_PASSWORD_FILE` | production | —                                                           | Keystore password supplied through the deployment secret store                                                                                                                       |
| `OAUTH2_SIGNING_KEY_ALIAS`                     | production | `portroyal-signing`                                                                                 | Alias containing the RSA private key and certificate                                                                                                                                 |
| `OAUTH2_SIGNING_KEY_FILE`                      | Docker only | —                                                                                                   | Host path mounted into the API container as a Docker secret                                                                                                                          |
| `DEV_SEED_ADMIN_PASSWORD`                      | dev only | —                                                                                                      | Required when the `dev` profile creates its local admin seed account; no default is provided                                                                                         |
| `ADMIN_BOOTSTRAP_USERNAME` and `ADMIN_BOOTSTRAP_PASSWORD` | operator only | —                                                                    | One-time bootstrap inputs for `AdminBootstrapTool`; remove after provisioning                                                                                                        |
| `server.port`                                  | no       | `8080`                                                                                                 | HTTP port the API listens on                                                                                                                                                         |
| `be_app.baseurl`                               | no       | `prbe.mightyshaman.com`                                                                                | Public base URL used to build links sent in emails (e.g. email confirmation links)                                                                                                   |
| `be_app.user.telegram_unification.enabled`     | no       | `true`                                                                                                 | Feature flag for the Telegram/email account unification flow                                                                                                                         |
| `be_app.user.telegram_unification.max_retries` | no       | `3`                                                                                                    | Max allowed OTP attempts for Telegram unification                                                                                                                                    |
| `be_app.cache.match.seconds-to-expire`         | no       | `60` (set in `application.properties`; falls back to `300` in code if the property is absent entirely) | TTL, in seconds, of the **per-instance** in-memory `Match` cache described above (`MatchRetainer`). This is purely a local performance cache, safe to tune per instance/environment. |
| `springdoc.api-docs.path`                      | no       | `/api-docs`                                                                                            | Springdoc mapping suffix; the effective raw OpenAPI URL is `/api/v1/api-docs` because REST controllers receive the API prefix                                                        |
| `portroyal.lib.version`                        | derived  | from `pom.xml`                                                                                         | Injected automatically at build time from the Maven `prlib.version` property; not meant to be set manually                                                                           |

Logging is configured via [`logback-spring.xml`](portroyal_be/src/main/resources/logback-spring.xml),
and HTTP request/response logging is provided by [Logbook](https://github.com/zalando/logbook)
(`logbook.filter.enabled`, `logbook.format.style`).

## Database

The API uses PostgreSQL through Spring Data JPA/Hibernate, with Flyway migrations under
[`portroyal_be/src/main/resources/db/migration`](portroyal_be/src/main/resources/db/migration).
`V1` establishes the existing schema and `V2` creates the OAuth authorization tables. Flyway applies
these migrations to an empty database; Hibernate is set to `ddl-auto=validate` and does not create
or update the live schema. Hibernate's reference schema export is limited to the `dev` profile.

For an existing database without Flyway history, first back it up and verify that its schema matches
the `V1` baseline. Then enable `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` for one controlled startup
to record version 1 and apply later migrations. Unset that override after Flyway creates its history
table. Do not baseline an unverified or partially migrated database.

In Docker Compose, PostgreSQL is attached only to the private backend network. The API is reachable
by NPM over `npm_main_net` but does not publish a host port. Reserve NPM's address on that network,
set `NPM_INTERNAL_PROXY_REGEX` to that exact address, and configure NPM to overwrite forwarded
scheme/host/client headers rather than pass through client-supplied values. Set
`APP_CORS_ALLOWED_ORIGINS` to comma-separated, exact browser origins; leaving it empty denies
cross-origin browser requests.

Key tables (see [`create-schema.sql`](create-schema.sql) for the authoritative, generated DDL):

- `users` — human accounts; unique on `username`, `email`, `telegram_id`; `roles` is a Postgres
  array of `ADMIN`/`USER` (the Telegram BOT is an OAuth client, and autonomous AI players are
  **not** stored here — see
  [Autonomous (AI) players](#autonomous-ai-players)).
- `ai_players` — one row per autonomous per-match AI player, FK'd to its owning `matches` row
  (cascade-deleted with the match); holds `username`, `display_name`, and `difficulty`
  (`EASY`/`MEDIUM`/`HARD`). See [Autonomous (AI) players](#autonomous-ai-players).
- `matches` — one row per hosted match, keyed by a short, shareable `key_code`; tracks `started`,
  `ended`, `host_user_id`, `winner_user_id`, timestamps.
- `match_user` — join table between `matches` and `users` (the players of a match).
- `moves` — append-only log of every move ever played, linked to `matches`; the check constraint on
  `move_name` mirrors the `MoveAction` enum from `portroyal_lib`
  (`DISCOVER, REPEL, ACCEPT, FINISH_DISCOVER, TRADE_HIRE, TRADE, HIRE, TRADE_RENOUNCE, END_TURN,
  COMMIT_EXPEDITION, SIGN_CONTRACT`). This table is the durable source of truth described in
  [Designed to scale horizontally](#designed-to-scale-horizontally).
- `game_configurations` — key/value match configuration properties (per configuration id).
- `temporary_tokens` — OTP tokens for email confirmation and Telegram unification flows.
- `callbacks` — webhook subscriptions for match notifications (see `sentinel` API).
- `emails_queue` — outbound transactional emails, queued for asynchronous delivery.
- `oauth2_registered_client`, `oauth2_authorization`, and `oauth2_authorization_consent` —
  PostgreSQL-backed OAuth client, authorization, token, and consent state.

## Build & run

Build everything (library + API) from the repository root:

```bash
mvn clean install
```

Run the API module directly:

```bash
cd portroyal_be
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The `dev` profile uses an ephemeral signing key and seeds only the local administrator. Set
`DEV_SEED_ADMIN_PASSWORD` to a non-empty development secret. Do not use this profile in production.

Run the standalone CLI against the Spring API running on localhost:8080. It accepts an OAuth bearer
token obtained through the registered client's flow:

```bash
cd portroyal_be
mvn exec:java -Dexec.mainClass=com.matteopaciolla.prbe.cli.PortRoyalCli
```

This CLI can inspect the current user, register accounts, request an account-unification OTP,
complete Telegram/email account unification, play matches, and issue HTTP requests. It does not
implement the interactive OIDC login flow; obtain a token with a standards-compliant OIDC client.
See [OAuth client operations](docs/OAUTH_CLIENT_OPERATIONS.md) for operator registration and the
Telegram delegated-token flow.

or run the packaged jar:

```bash
cd portroyal_be
mvn clean package
java -jar target/portroyal_be-*.jar
```

Build a container image (via Spring Boot's Cloud Native Buildpacks integration, Paketo builder):

```bash
cd portroyal_be
mvn spring-boot:build-image
```

`portroyal_lib` also ships its own [`Dockerfile`](portroyal_lib/Dockerfile), which compiles the
library and runs its CLI demo entry point (`com.matteopaciolla.portroyal.MainClass`) — useful for
exercising/replaying the game engine (from pre-recorded CSV move sets in `csv_files/`) without
standing up the full API/database stack.

## API overview

- For an end-to-end description of the rules, move payloads, and match workflow, see the
  [Game Rules and API Guide](docs/GAME_RULES_AND_API.md).
- For the staged production hardening and go-live criteria, see the [Production Roadmap](docs/PRODUCTION_ROADMAP.md).
- For OAuth client setup and Telegram delegation, see [OAuth client operations](docs/OAUTH_CLIENT_OPERATIONS.md).
- For operational health and incident procedures, see the [Operations runbook draft](docs/OPERATIONS_RUNBOOK.md).
- Base path: `/api/v1`
- Interactive docs: Swagger UI at `/swagger-ui/index.html`, raw OpenAPI spec at `/api/v1/api-docs`
  (see `springdoc.api-docs.path`).
- Authentication: OAuth 2.0 bearer tokens protect `/api/v1/**`; public clients use Authorization
  Code with PKCE and the Telegram integration uses a confidential BOT client plus the delegated
  token grant. `tgId` is accepted only for BOT-authorized account onboarding/linking and token
  exchange. `AI` denotes autonomous per-match players that never authenticate.

| Tag         | Base path          | Purpose                                                                                                     |
|-------------|--------------------|-------------------------------------------------------------------------------------------------------------|
| Public      | `/api/v1/public`   | Public JSON registration/OTP; HTML email confirmation at server-root `/public/confirm-email`                 |
| User        | `/api/v1/user`     | Current-user lookup, retrieve/update/delete users, password change, bot registration & Telegram unification |
| Match       | `/api/v1/match`    | Host/join/start/close a match, list matches, poll match status/state                                        |
| Move (Game) | `/api/v1/game`     | Play a move, fetch a single move or paged move history                                                      |
| Cards       | `/api/v1/card`     | Read-only catalog of cards and contract cards from `portroyal_lib`'s deck dictionaries                      |
| Sentinel    | `/api/v1/sentinel` | Real-time match notifications: webhook callback, long polling, Server-Sent Events, unsubscribe              |

Selected endpoints:

Paths in this table are relative to `/api/v1`.

| Method & path                                      | Description                                                                                                                             |
|----------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| `POST /public/register`                            | Register a new user (username/email/password, web/app flow)                                                                             |
| `POST /user/register/telegram`                     | Register a new user from the bot (Telegram id only) — BOT `bot:onboard` scope                                                         |
| `POST /user/new-tg-unify-email-otp`                | Request an OTP to unify a Telegram identity with an email account                                                                        |
| `POST /user/unify`                                 | Confirm Telegram/email unification with the OTP — BOT `bot:onboard` scope                                                               |
| `GET /user/me`                                     | Get the currently authenticated user                                                                                                    |
| `PATCH /user/update?username=...`                  | Partially update a user profile                                                                                                          |
| `POST /match/host`                                 | Host a new match (optionally with a custom configuration)                                                                               |
| `POST /match/join?key_code=...`                     | Join an open, not-yet-started match                                                                                                     |
| `POST /match/start`                                | Start the match you are hosting (requires ≥ 2 players)                                                                                  |
| `POST /match/close`                                | Close an unstarted match the caller participates in                                                                                     |
| `POST /match/ai-player`                            | Add an autonomous AI player (`EASY`/`MEDIUM`/`HARD`) to the match you are hosting, before it starts                                     |
| `DELETE /match/ai-player?ai_player_username=...`     | Remove a previously added AI player from the match you are hosting, before it starts                                                    |
| `GET /match/status`                                | Get the match currently being played by the caller, if any                                                                              |
| `GET /match/status?move_number=...`                 | If `move_number` equals the current move count, returns "No new moves"; otherwise returns the current match                              |
| `GET /match/retrieve?key_code=...&move_number=...`   | Get full match state; poll with `move_number` to cheaply check "any new moves?"                                                          |
| `POST /game/move`                                  | Play a move (`DISCOVER`, `TRADE`, `HIRE`, `COMMIT_EXPEDITION`, `SIGN_CONTRACT`, `END_TURN`, ...); validated entirely by `portroyal_lib` |
| `GET /game/moves?key_code=...`                      | Paged move history of a match                                                                                                           |
| `GET /card`, `GET /card/contract`                  | Static card catalog                                                                                                                     |
| `POST /sentinel/callback/subscribe`                | Register a webhook URL to be called on match alerts (works across any instance)                                                         |
| `GET /sentinel/long-polling/subscribe?key_code=...` | Long-poll for the next match alert (requires sticky routing at scale)                                                                   |
| `GET /sentinel/sse/subscribe?key_code=...`          | Subscribe to match alerts via Server-Sent Events (requires sticky routing at scale)                                                     |

Only BOT-authorized account onboarding/linking and the Telegram delegation token exchange accept a
`tgId`. Ordinary game and match requests resolve the human user from the delegated bearer token.

## Testing

Both modules use JUnit 5, AssertJ and Mockito. Run all tests from the root:

```bash
mvn test
```

or scoped to a single module, e.g. `mvn -pl portroyal_lib test` for the game engine's rule tests.

## Development notes

- Startup account seeding is disabled unless the `dev` Spring profile is active. When enabled,
  the local `admin` account requires a non-empty `DEV_SEED_ADMIN_PASSWORD`. The Telegram BOT is an
  OAuth client, never a seeded `UserEntity`. No default credentials are provided.
  Production must not activate the `dev` profile.
- CORS uses an environment-configured exact origin allowlist
  (`WebSecurityConfig#corsConfigurationSource`); set `APP_CORS_ALLOWED_ORIGINS` to the browser
  origins that should be allowed. An empty value denies cross-origin requests.

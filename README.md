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
- [Game rules and API guide](GAME_RULES_AND_API.md)
- [Client development guide](CLIENT_DEVELOPMENT_GUIDE.md)
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

- There's a first-class `BOT` role (`UserRole.BOT`) alongside `USER` and `ADMIN`. A Telegram Bot
  process authenticates against the API with **its own single technical account** (an example is
  seeded at startup as `shaslabot`, see [Development notes](#development-notes)) — it does not
  create one BE account per Telegram user.
- Instead, the bot **mediates**: every request it makes on behalf of a human player carries a
  mandatory `tgId` header (see `CommonConstants.BOT_MANDATORY_HEADER`) containing *that player's*
  Telegram id. Server-side, `AuthenticationUtils.isBotUser()` detects that the caller authenticated
  as the bot account, and the controllers/services then resolve the actual acting user via
  `userService.getUserEntityByTelegramId(tgId)` instead of via the authenticated principal — see
  `MatchController#getUserEntity`, `GameController#move` (`insertMoveWithTelegramId`), and the
  corresponding `MatchService`/`GameService` methods. In other words: **one bot identity, credentialed
  once, safely drives moves and matches for many different real players**, each still tracked and
  authorized as themselves at the domain level (moves/matches remain associated with the human
  player's `UserEntity`, not with the bot).
- A player can be registered directly through the bot (`POST /api/v1/user/register/telegram`, `hasRole(BOT)`)
  using only their Telegram id, or through a normal web/app registration
  (`POST /api/v1/public/register`) using username/email/password.
- The two identities can later be **unified**: a Telegram-only user requests a one-time code sent to
  an email address (`GET /api/v1/public/new-tg-unify-email-otp`), then confirms the pairing through the bot
  (`POST /api/v1/user/unify`, `hasRole(BOT)`, backed by the OTP/`temporary_tokens` table). After unification,
  the same physical player can keep playing the very same match indifferently from the web front end
  or from Telegram — the API always resolves to the same `UserEntity` regardless of which channel is
  used to reach it.

Because moves/matches are keyed by the domain user (via username or Telegram id), not by client type
or session, any mix of frontends can interleave calls against the same match with no special
handling required on the API side.

### Autonomous (AI) players

A match host can also fill empty seats with autonomous, backend-controlled AI players so humans and
AI players can play side by side. This is a distinct concept from the single Telegram `BOT`
technical account described above: that `BOT` role identifies one authenticated proxy account that
mediates real human users, while an AI player is a per-match, never-authenticating participant
computed by the backend.

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
3. **Authentication is per-request, not session-based, for the API itself.** All game/match/user API
   endpoints use HTTP Basic Auth (`httpBasic()` in `WebSecurityConfig`), which is verified against
   the database on every call — there is no server-side session state a load balancer would need to
   pin a client to. (The small Thymeleaf web login pages under `/login` do use a servlet session for
   the browser UI experience, which is unrelated to the JSON API.)
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

The API uses PostgreSQL through Spring Data JPA/Hibernate. There is **no Flyway/Liquibase**
migration tool in the project: the schema is derived directly from the JPA entities. In development,
Hibernate is configured to export the schema it would generate to the repository-root
[`create-schema.sql`](create-schema.sql) / [`drop-schema.sql`](drop-schema.sql) files on every
startup (`spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=drop-and-create`)
— these files are a convenient, always-up-to-date **reference** of the current schema, not something
you need to run by hand for a real deployment (`ddl-auto` is left to Hibernate's normal behavior for
actually creating/updating the live schema).

Key tables (see [`create-schema.sql`](create-schema.sql) for the authoritative, generated DDL):

- `users` — accounts; unique on `username`, `email`, `telegram_id`; `roles` is a Postgres array of
  `ADMIN`/`USER`/`BOT` (autonomous AI players are **not** stored here — see
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

## Build & run

Build everything (library + API) from the repository root:

```bash
mvn clean install
```

Run the API module directly:

```bash
cd portroyal_be
mvn spring-boot:run
```

Run the standalone CLI tester against the Spring API running on localhost:8080:

```bash
cd portroyal_be
mvn exec:java -Dexec.mainClass=com.matteopaciolla.prbe.cli.PortRoyalCli
```

This CLI can register users, inspect the current user, list/host/join/start matches, add/remove
autonomous AI players before starting a match, issue arbitrary HTTP calls to the REST API, and play
a match interactively without starting the Spring app in the same JVM.

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
  [Game Rules and API Guide](GAME_RULES_AND_API.md).
- Base path: `/api/v1`
- Interactive docs: Swagger UI at `/swagger-ui/index.html`, raw OpenAPI spec at `/api/v1/api-docs`
  (see `springdoc.api-docs.path`).
- Authentication: HTTP Basic Auth on essentially every endpoint under `/api/v1/**` (see
  [Designed to scale horizontally](#designed-to-scale-horizontally)); roles are `ADMIN`, `USER`,
  `BOT` (see [Designed for many kinds of clients](#designed-for-many-kinds-of-clients) for how `BOT`
  callers mediate for other users via the `tgId` header) and `AI` (autonomous, per-match AI players
  that never authenticate — see [Autonomous (AI) players](#autonomous-ai-players)).

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
| `POST /user/register/telegram`                     | Register a new user from the bot (Telegram id only) — `BOT` role only                                                                   |
| `GET /public/new-tg-unify-email-otp`               | Request an OTP to unify a Telegram id with an email account                                                                             |
| `POST /user/unify`                                 | Confirm Telegram/email unification with the OTP — `BOT` role only                                                                       |
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

Bot-mediated calls (`MatchController`, `GameController`, `UserController#registerTelegramPlayer`,
`UserController#unifyTelegramAndEmailAccounts`) require the `tgId` request header, set to the
Telegram id of the human player the bot is acting for.

## Testing

Both modules use JUnit 5, AssertJ and Mockito. Run all tests from the root:

```bash
mvn test
```

or scoped to a single module, e.g. `mvn -pl portroyal_lib test` for the game engine's rule tests.

## Development notes

- On startup, `Main.java` seeds two default accounts if they don't already exist: an `admin` user
  (`ADMIN`+`USER` roles) and a `shaslabot` user (`BOT` role) — both with the **hardcoded password
  `"password"`**. This is a convenience for local development only; **rotate or remove these
  credentials before exposing an instance publicly**.
- CORS is currently configured with a wildcard origin pattern and credentials enabled
  (`WebSecurityConfig#corsConfigurationSource`); restrict `allowedOriginPatterns` to your real front
  end origins for any non-local environment.

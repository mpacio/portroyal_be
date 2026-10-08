# Copilot instructions — Port Royal Back End

## Repository shape

Maven **multi-module** project (root `pom.xml` is a `pom`-packaged aggregator):

- [`portroyal_lib`](../portroyal_lib) — plain Java library (Java 21), package
  `com.matteopaciolla.portroyal`. The **entire game engine**: rules, cards, phases, effects,
  match/move validation. No web, no persistence, no framework dependency.
- [`portroyal_be`](../portroyal_be) — Spring Boot 3 application (Java 21). The **REST API**:
  auth, persistence (PostgreSQL/JPA), match/user orchestration, real-time notifications.

`portroyal_be` depends on `portroyal_lib` via the `prlib.version` property in
`portroyal_be/pom.xml`; the library must be built/installed before the API (root
`mvn clean install` handles ordering automatically).

## Commit policy

COPILOT IS NEVER ALLOWED TO GIT COMMIT WITHOUT ASKING BEFORE. Any git commit, amend, squash,
rebase, or branch creation that writes repository history must be explicitly approved by the user
before execution. Do not create commits as part of routine work, testing, or cleanup unless the user
has clearly asked for it.

## Golden rule: rules live only in `portroyal_lib`

**Never implement or duplicate game-rule logic inside `portroyal_be`.** The API only:

1. creates/loads a `Match` object from `portroyal_lib`,
2. forwards player input as a `MoveRecord`,
3. asks the library to validate/apply it (`Match#executeMove`, `Match#discover`, ...).

If a move is illegal, the library throws a `UserInputException` subtype, which the API translates
into an HTTP error. When asked to add or change game behavior, make the change in `portroyal_lib`
(under `core`, `confs`, `cards`, `exceptions`, `facades`, `util`) and keep `portroyal_be` a thin
orchestration layer.

## Multi-client / BOT authentication model

The API is frontend-agnostic (web/app, CLI, Telegram Bot). Keep this model intact when touching
auth or user-resolution code:

- Roles: `UserRole.USER`, `ADMIN`, `BOT`. A Telegram Bot authenticates as **one single technical
  account** (seeded as `shaslabot`), not one account per Telegram user.
- Bot-mediated requests carry a mandatory `tgId` header (`CommonConstants.BOT_MANDATORY_HEADER`).
  `AuthenticationUtils.isBotUser()` detects the bot caller; controllers/services then resolve the
  real acting user via `userService.getUserEntityByTelegramId(tgId)` instead of the authenticated
  principal (see `MatchController#getUserEntity`, `GameController#move`).
- Registration: `POST /api/v1/users` (web/app, username/email/password) vs.
  `POST /api/v1/users/telegram` (`BOT` role only, Telegram id only).
- Identity unification: `POST /api/v1/telegram-account-verifications` + `PUT
  /api/v1/users/telegram-account` (`BOT` role only, OTP via `temporary_tokens`) merges a
  Telegram-only user with an email account into one `UserEntity`.
- Moves/matches are always keyed by the domain `UserEntity`, never by client type or session.

## Statelessness / horizontal scaling

Every `portroyal_be` instance must remain stateless and disposable:

- **PostgreSQL is the single source of truth.** Every accepted move is persisted (`moves` table)
  before the API responds.
- **`MatchRetainer`** keeps a Caffeine `LoadingCache<String, Match>` purely as a performance
  optimization. Before returning a cached `Match`, it compares the cached move count against the
  persisted move count and **invalidates/rebuilds** (`MatchCreator.createMatch(...)`) on mismatch.
  Never treat this cache as authoritative state.
- **Auth is per-request** (HTTP Basic, `WebSecurityConfig`), not session-based, for all JSON API
  endpoints — do not introduce server-side session state for `/api/v1/**`.
- **Outbound side effects are queued via DB tables**, not sent synchronously in-memory (e.g.
  `emails_queue` / `EmailSenderFacade`).
- **Known exception:** `sentinel` long-polling and SSE endpoints hold subscriptions
  (`SentinelService#matchSubsMap`) in local JVM memory — these need sticky routing at scale. The
  webhook alternative (`POST /matches/{keyCode}/subscriptions`, backed by the `callbacks` table) has no
  such limitation. Prefer the webhook/DB-backed pattern for any new cross-instance notification
  feature; if extending long-polling/SSE, keep in mind their sticky-session requirement.

## Database

- Spring Data JPA/Hibernate against PostgreSQL. **No Flyway/Liquibase** — schema is derived from
  JPA entities. `create-schema.sql` / `drop-schema.sql` at the repo root are generated on every
  startup for reference only; don't hand-edit them or expect them to drive real migrations.
- Key tables: `users`, `matches`, `match_user`, `moves` (append-only; `move_name` check constraint
  mirrors the `MoveAction` enum from `portroyal_lib`), `game_configurations`, `temporary_tokens`,
  `callbacks`, `emails_queue`.
- When adding a new `MoveAction`, keep the `portroyal_lib` enum and the `moves.move_name` DB check
  constraint in sync.

## API conventions

- Base path `/api/v1`; Swagger UI at `/swagger-ui/index.html`, OpenAPI JSON at `/api/v1/api-docs`.
- HTTP Basic Auth on essentially every endpoint under `/api/v1/**`.
- Use lowercase, plural resource names and kebab-case path segments. Use HTTP methods to express
  the operation; avoid verb-named path segments and keep nesting shallow. Do not restore replaced
  route aliases: the current `/api/v1` routes are intentionally canonical even though the change
  breaks old clients.
- Keep JSON property names in camelCase. Use underscore-separated names for compound query
  parameters (for example, `move_number`, `page_number`, `page_size`, `sort_field`,
  `sort_direction`, `telegram_id`). Keep query parameters for list filtering, sorting, and paging.
- REST groups are resources rather than controller prefixes: `/users`, `/matches`, `/cards`,
  `/contract-cards`, `/email-confirmations`, and `/telegram-account-verifications`. Moves and
  notifications are nested under `/matches/{keyCode}`.
- Use suitable success and error HTTP status codes. REST errors, including API authentication and
  authorization failures, use JSON with `status`, machine-readable `code`, human-readable
  `message`, optional `details`, and `timestamp`; do not return internal exception messages in 5xx
  responses.
- Public JSON endpoints are `POST /api/v1/users`, `POST /api/v1/email-confirmations`, and
  `POST /api/v1/telegram-account-verifications`. The HTML email confirmation page remains at
  `/public/confirmEmail` outside the JSON API.
- Bot-only registration and unification endpoints are `POST /api/v1/users/telegram` and
  `PUT /api/v1/users/telegram-account`. Bot-mediated match and move endpoints (`MatchController`,
  `GameController`) require the `tgId` header where they resolve the acting human; always preserve
  resolution through `userService.getUserEntityByTelegramId(tgId)`.
- Rate limiting is not currently implemented. Do not add per-instance-only limits to this
  horizontally scaled service; introduce it only with an explicit, shared/distributed strategy.

## Build & test

```bash
mvn clean install              # build library + API, root
cd portroyal_be && mvn spring-boot:run     # run API
mvn test                       # all tests (JUnit 5, AssertJ, Mockito)
mvn -pl portroyal_lib test     # engine tests only
```

Requires Java 21, Maven 3.9+, PostgreSQL. Config lives in
`portroyal_be/src/main/resources/application.properties`, primarily overridden through
`SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`.

## Local dev credentials (do not carry into production advice)

On startup, `Main.java` seeds an `admin` (`ADMIN`+`USER`) and a `shaslabot` (`BOT`) account, both
with hardcoded password `"password"`, and CORS is configured with a wildcard origin pattern. These
are dev-only conveniences — flag them if asked about production hardening, don't propagate them as
a template for new environments.

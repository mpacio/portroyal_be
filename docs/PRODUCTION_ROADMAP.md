# Production Roadmap

## Purpose and scope

This document turns the proposed professionalization work for Port Royal BE into an ordered,
verifiable roadmap. It describes the intended production target; it is not a claim that these
controls have already been implemented.

The backend is intended to be a public, frontend-agnostic API. Any supported client should be able
to let a player create an account and play, while the API remains the authority for identity,
authorization, and persisted game state. The production deployment uses Nginx Proxy Manager (NPM)
for HTTPS termination. The Docker environment on the LAN is the pre-production integration
environment.

There are currently no production datasets or deployed clients. The roadmap therefore favors a
clean authentication cutover and versioned schema baseline before opening production, rather than
maintaining compatibility with an established production contract.

The selected authentication direction is an **OIDC/OAuth 2.0 Authorization Server integrated into
the Spring Boot project**. It will issue tokens consumed by the API's Resource Server. The
Authorization Server and Resource Server may run in the same Spring Boot deployment, but their
security filter chains, endpoints, responsibilities, and tests must be clearly separated.

All game rules remain in `portroyal_lib`. This roadmap changes no game behavior: `portroyal_be`
continues to authenticate callers, resolve the domain `UserEntity`, orchestrate operations, and
persist accepted moves in PostgreSQL.

## Initial baseline

The following is a snapshot from before implementation began in this working tree. The status of
changes made against it is summarized in [Implementation status](#implementation-status).

The repository currently has:

- Spring Boot 3 / Java 21, Spring Security, PostgreSQL/JPA, and no database migration framework.
- HTTP Basic on protected API endpoints, plus form login and sessions with
  `SessionCreationPolicy.IF_REQUIRED`.
- CORS configuration that accepts all origin patterns, methods, and headers while allowing
  credentials.
- A `Main` startup runner that seeds `admin` and `shaslabot` with the hard-coded password
  `password`.
- Logbook request logging configured at `TRACE`; email confirmation and Telegram unification token
  values are also logged in `UserService`.
- JPA schema-export settings in `application.properties`; generated schema scripts are reference
  output, not a versioned production migration process.
- Telegram mediation through one technical `BOT` identity and a mandatory `tgId` header on
  bot-mediated operations. This model must be preserved.
- A match cache intended only as an optimization; PostgreSQL remains the source of truth.

## Target architecture and security invariants

1. The application hosts its own OAuth 2.0 Authorization Server and OIDC Provider, using a
   Spring Authorization Server release compatible with the supported Spring Boot/Spring Security
   line.
2. The same project also protects the game API as a Resource Server. Game API requests use
   `Authorization: Bearer <access-token>` and do not depend on an HTTP session.
3. PostgreSQL is the durable source of truth for users, registered OAuth clients, authorizations,
   refresh-token state, and other revocable identity state. Signing keys are durable and securely
   managed outside ephemeral container storage.
4. Public clients never contain a client secret. Authorization Code with PKCE using `S256` is the
   interactive login flow for browser/native clients. Do not implement the deprecated Resource
   Owner Password Credentials grant.
5. The Telegram integration remains one confidential OAuth BOT client, not one API credential per
   Telegram player and not a technical `UserEntity`. The BOT authenticates with client credentials
   and exchanges the Telegram sender ID for a short-lived delegated access token whose subject is
   the linked human `UserEntity` and whose claims retain the BOT client as the actor. Normal game
   API calls use this token and do not send `tgId`. The API trusts the authenticated BOT's
   assertion of the Telegram sender ID; `tgId` is accepted only in the delegated-token exchange
   and BOT-authorized account onboarding/linking flows. Preserve the existing match/move ownership
   semantics.
6. Authorization decisions are made by the server. Client-supplied roles or identity headers are
   never trusted as authentication. Every protected operation continues to enforce ownership and
   role requirements.
7. Instances remain disposable and horizontally replicable. Restarts or routing to another
   instance must not lose valid authorization state or change signing keys unexpectedly.

## Phased implementation plan

### Phase 0 — Define the public service contract

- Define supported client categories: browser SPA, native/mobile, CLI, Telegram BOT, and any
  server-to-server integration.
- Define how public OAuth clients obtain a `client_id` and register exact redirect URIs. Use
  controlled registration through a guided script that an authorized operator invokes manually.
  Do not enable unrestricted Dynamic Client Registration
  without a documented abuse and support model.
- Define the Telegram BOT delegated-token flow. The BOT first authenticates as a confidential
  OAuth client, then exchanges the Telegram sender ID for a short-lived access token representing
  the linked human account. The standard `client_credentials` grant alone represents only the
  BOT client, so implement a narrowly scoped token-exchange/custom-grant flow. Accept `tgId` only
  at this trusted exchange and at BOT-authorized account onboarding/linking endpoints, not on
  ordinary game API calls. Under this trust model, the API relies on the authenticated BOT to
  report the sender ID received from Telegram.
- Define the public API's acceptable-use rules, privacy notice, account/data deletion policy,
  support contact, and initial rate/usage limits. Track unapproved content in
  [`PUBLIC_API_POLICIES_DRAFT.md`](PUBLIC_API_POLICIES_DRAFT.md); this draft must not be published,
  and production go-live remains blocked until its TBD values and text are approved.
- Publish the authentication and registration flows in the client guide and OpenAPI documentation.

**Exit criteria:** each client type has a documented grant/flow, and client registration,
redirect-URI, account-registration, and recovery policies are decided.

### Phase 1 — Remove development-only security hazards

- Move seeded accounts and development credentials behind a development-only profile. Production
  startup must not create an administrator or BOT account with a known password.
- Provision the production administrator through a controlled bootstrap procedure. Provision the
  Telegram BOT as a confidential OAuth client, not as a seeded `UserEntity`; keep its high-entropy
  client secret in a secret-management mechanism, define rotation, and do not store secrets in
  source control, images, or committed Compose files.
- Remove or redact passwords, `Authorization` headers, access/refresh tokens, OTPs, email
  confirmation tokens, Telegram IDs, and other sensitive values from logs. Turn off TRACE-level
  HTTP logging in non-development environments; log event IDs and outcomes rather than secrets.
- Keep client-visible errors useful but avoid disclosing whether a username or email exists where
  that enables account enumeration.
- Confirm that registration behavior is explicit: web/app registration requires username, email,
  and password; Telegram-only registration is restricted to the authenticated BOT client and uses
  the Telegram ID only in the onboarding flow. Never let the BOT-only passwordless path become a
  public registration bypass.

**Exit criteria:** a production-profile startup creates no predictable account; automated checks
find no secrets in logs; public and BOT registration tests prove the intended required fields and
authorization boundaries.

### Phase 2 — Establish versioned database changes

- Adopt one migration tool (Flyway is the recommended default for this project; Liquibase is an
  alternative, not an additional tool).
- Create an initial baseline representing the current schema, then add every later schema change
  as a reviewed, ordered migration. Include the tables/indexes needed by the Authorization Server.
- Keep the `moves.move_name` database constraint synchronized with the `portroyal_lib` move enum.
- Restrict schema export/generation to development. Configure production Hibernate to validate the
  schema rather than create or update it implicitly.
- Define backup retention and test restoration to a clean database before relying on the database
  in production.

**Exit criteria:** a fresh PostgreSQL instance can be created entirely from migrations; the
application starts with schema validation; an upgrade and a restore have been exercised in the
LAN test environment.

### Phase 3 — Integrate the Authorization Server and Resource Server

- Add and configure Spring Authorization Server in the Spring Boot project. Keep Authorization
  Server endpoints/configuration separate from the Resource Server API security chain, even if
  both run in the same process.
- Implement OIDC discovery, authorization, token, JWKS, and logout/revocation behavior as
  appropriate to the chosen Spring Authorization Server version. Set a stable issuer URL per
  environment; validate issuer, signature, expiry, and audience at the API.
- Support Authorization Code + PKCE (`S256`) for public browser/native clients. Use exact
  registered redirect URIs. Do not issue client secrets to browser or mobile bundles.
- Use client credentials for the confidential Telegram BOT integration. Since a client-credentials
  token represents the client rather than a human, add a narrowly scoped token-exchange/custom
  grant: after authenticating the BOT client, accept the Telegram sender ID, resolve the linked
  `UserEntity`, and issue a short-lived delegated token with a stable human subject and the BOT
  client recorded as its actor. The API trusts the authenticated BOT's assertion; `tgId` is not a
  credential and must not be accepted as proof on its own. For first-time Telegram registration or
  account linking, retain BOT-client-authorized onboarding/linking operations that accept the
  Telegram ID; once the human account exists, normal API calls use its delegated token without a
  `tgId` header. Give the BOT only the authority required for these flows and ensure it cannot
  acquire `ADMIN` or unrestricted user authority. Do not use the BOT grant for human clients.
- Preserve the domain identity model. Define a stable mapping from the authenticated OIDC subject
  to `UserEntity`; do not use mutable usernames as the permanent token subject. Preserve email
  confirmation, Telegram/email account unification, role boundaries, and `tgId` resolution.
- Adapt authentication and controller/service resolution so delegated-token requests resolve the
  acting `UserEntity` from the token's stable subject. Track the BOT client separately for
  authorization and audit; do not use a synthetic BOT `UserEntity` or a `tgId` header to identify
  the actor on ordinary game API calls. Ordinary tokens must not be able to claim BOT or select a
  different user by supplying a header or role value.
- Issue short-lived access tokens. Rotate refresh tokens, detect reuse where supported, and
  implement revocation/logout. Access-token revocation must take effect immediately, including
  after logout or credential changes; the Resource Server must check persisted authorization state
  on each request rather than relying on JWT expiry alone.
- Persist registered client definitions, authorization/refresh state, and consent as needed in
  PostgreSQL. Use a durable signing-key source with access controls and a documented rotation
  process; never generate a new production signing key on every start or store it only in a
  disposable container.
- Keep API resource requests stateless. Authorization/login pages may use a browser session, but
  preserve CSRF protection for session/cookie-based login and consent forms. Do not disable CSRF
  indiscriminately across every security chain.
- Replace Basic Auth for protected API clients and update the CLI and all examples. Since no
  production clients exist, prefer a clean migration; if Basic is temporarily retained for
  development, keep it development-only and give it a removal condition.

**Exit criteria:** public-client PKCE, BOT client authentication, and delegated-token exchange work
end to end; delegated tokens identify one linked human and retain the BOT client as actor; ordinary
game API calls require no `tgId` header; access tokens are accepted only by the intended
issuer/audience; refresh rotation and revocation behave as documented; no production API route
depends on Basic Auth or an API session; authorization state and signing keys survive restart and
instance changes.

### Phase 4 — Harden registration, account recovery, and API abuse controls

- Enforce request validation at the boundary: mandatory fields, length/format rules, password
  policy, and per-field limits. Align DTO validation, service behavior, OpenAPI, and examples.
- Complete account lifecycle flows: email verification before account use, authenticated password
  change, secure password reset, and session/token revocation after credential changes.
- Make one-time tokens cryptographically unpredictable, purpose-bound, expiring, single-use, and
  stored safely (prefer hashes for bearer-like secrets). Apply attempt limits to short numeric OTPs,
  invalidate replaced/consumed tokens, and return generic responses where appropriate.
- Add rate limits at both NPM and the application for registration, login/token issuance, refresh,
  email/OTP operations, password reset, match creation/join, and resource-intensive polling.
  Application-level limits must still protect the service if traffic bypasses NPM.
- Set explicit maximum page sizes, request-body limits, and sensible request/async timeouts. Keep
  long-polling/SSE limits explicit; remember that the current in-memory subscriptions require sticky
  routing, while the DB-backed webhook pattern is suitable across instances.
- Audit object-level authorization for user, match, move, and callback operations. Authentication
  alone must not grant access to another player's data or match.

**Exit criteria:** automated negative tests cover missing/invalid fields, unauthorized access,
cross-user access, replayed/expired OTPs, rate-limit behavior, and oversized inputs; recovery and
credential changes invalidate the appropriate refresh/session state.

### Phase 5 — Configure browser policy and reverse-proxy deployment

- Replace the overlapping permissive CORS configuration with one explicit policy per environment.
  Allow the headers and methods clients need, including `Authorization` and `tgId`. CORS is not an
  authentication control. If bearer tokens are sent in headers and cookies are not used for API
  calls, do not enable credentialed cross-origin requests. If a cookie-based flow is used, list
  exact trusted origins and pair it with CSRF protection.
- Configure Docker networks so NPM can reach the API while the API and PostgreSQL ports are not
  published to the public Internet. Restrict database access to the application network and
  administrator paths.
- Configure NPM to redirect HTTP to HTTPS, manage certificates, pass the correct forwarded
  scheme/host/client metadata, and apply appropriate security headers. Configure Spring to trust
  forwarded headers only from the known proxy/network; do not trust client-supplied forwarding
  headers from arbitrary sources.
- Keep public issuer/base URLs, CORS origins, database credentials, email credentials, BOT client
  secrets, and signing-key material in environment-specific secrets. Do not put production values
  in image layers or source-controlled files.
- Use separate `dev`, LAN-test, and production configuration. Ensure test and production differ
  only where intended and that production cannot accidentally inherit development seeds or schema
  generation.

**Exit criteria:** external scans cannot reach the application or database ports directly; the
public host serves HTTPS with a correct issuer URL; spoofed forwarding headers are ignored;
preflight and authenticated requests succeed only according to the documented CORS policy.

### Phase 6 — Add operations, observability, and public API governance

- Add health/readiness and metrics support. Expose only minimal liveness/readiness information
  publicly; keep detailed health, metrics, configuration, and administrative endpoints private or
  authenticated.
- Emit structured logs with request/correlation IDs and redaction. Monitor API error rates and
  latency, authentication failures, database pool saturation, email queue backlog, rate-limit
  events, and callback delivery failures.
- Define alert thresholds and operational runbooks for database outage, failed email delivery,
  key rotation, credential compromise, and restore from backup.
- Update OpenAPI to describe OIDC/OAuth2 Authorization Code + PKCE, BOT client authentication and
  delegated-token exchange, bearer API authorization, required scopes/roles, `401`/`403` responses,
  rate limits, token lifetimes, registration, recovery, and BOT-authorized onboarding/linking.
- Publish API versioning/deprecation policy, acceptable-use rules, privacy/retention details, and
  a contact path for security reports. Keep the public API documentation available without
  exposing internal operational details.

**Exit criteria:** operators can determine whether the service is healthy without exposing secrets;
alerts cover the main failure modes; OpenAPI and client documentation match deployed behavior and
the published policy is reachable.

### Phase 7 — Verify in LAN, then promote to production

- Add unit and integration coverage for Authorization Server configuration, login/registration,
  PKCE including verifier failures, issuer/audience/signature validation, expiry, refresh rotation,
  refresh reuse/revocation, client registration boundaries, role checks, and BOT delegated-token
  exchange. Verify that only the authenticated BOT client can request a delegated token, that the
  token subject resolves to the intended linked user and records the BOT actor, that an ordinary
  client cannot select another subject, and that normal game API calls work without a `tgId`
  header. Test Telegram onboarding/linking separately at the BOT-authorized boundary.
- Test account confirmation, reset, Telegram/email unification, and credential changes with the
  email queue and PostgreSQL in the test deployment.
- Test security-filter separation: API bearer requests are stateless; login/consent forms retain
  CSRF protections; public endpoints are intentionally public; administrative endpoints are not.
- Run migrations from an empty database and from the latest development schema; test backup and
  restore. Restart the app and route requests between multiple instances to confirm OAuth state,
  key stability, match persistence, and cache rebuild behavior.
- Deploy the same image/configuration shape into the Docker LAN environment behind a representative
  NPM configuration. Exercise browser/native PKCE, BOT requests, CORS preflight, throttling,
  HTTPS/forwarded headers, health checks, and logs.
- Promote only after the LAN acceptance tests pass. Before opening public traffic, verify secrets,
  certificate renewal, firewall/network rules, backups, alert delivery, public policy links, and
  rollback instructions.

**Go-live criteria:** all prior phase exit criteria pass; no default credentials or secrets remain;
no unresolved critical/high security issue remains; the production database can be restored;
monitored operations and incident contacts are in place.

## Suggested execution order

The dependency order is:

1. Define client and account flows (Phase 0).
2. Remove development secrets and sensitive logging (Phase 1).
3. Introduce schema migrations and durable persistence (Phase 2).
4. Integrate the Authorization Server/Resource Server and replace Basic Auth (Phase 3).
5. Complete lifecycle and abuse controls (Phase 4).
6. Lock down CORS, Docker networking, NPM, and secrets (Phase 5).
7. Establish observability and public API governance (Phase 6).
8. Prove the complete deployment in the LAN environment, then promote (Phase 7).

Phases may overlap when work is independent, but a phase's exit criteria must pass before its
dependent production behavior is enabled.

## Implementation status

| Phase | Status |
|---|---|
| 0 — Public service contract | In progress. Controlled client registration and immediate token revocation are selected. `PUBLIC_API_POLICIES_DRAFT.md` intentionally contains TBD policy text and limits; publication and go-live are blocked until approved. |
| 1 — Development security hazards | Implemented in source: development-only admin seeding, one-time admin bootstrap, no BOT `UserEntity` seed, redacted request/response logs, and generic public registration conflicts. Backend unit tests pass. |
| 2 — Versioned database changes | Flyway V1 baseline and V2 Authorization Server tables are present. The existing PostgreSQL schema was baselined at V1, V2 applied, and Hibernate schema validation succeeded. A separate empty schema also migrated through V1/V2 and passed validation. Backup/restore verification remains. |
| 3 — Authorization and resource servers | Authorization Server/Resource Server chains, PKCE client registration, durable rotating JWK source, Telegram delegation grant, stable human subjects, per-request authorization revocation checks, and bearer-only game routes are implemented. A PostgreSQL-backed BOT onboarding/delegation/revocation smoke flow passed; public PKCE consent/refresh and multi-instance key-rotation acceptance remain. |
| 4 — Lifecycle and abuse controls | Partial. Password-change/deletion revocation is wired as part of token lifecycle. User/email/Telegram payload validation and bounded page sizes are implemented; a password minimum/strength policy, request limits, reset flows, remaining boundary validation, and negative authorization coverage remain. |
| 5 — Proxy and container deployment | Partial. Exact-origin non-credentialed CORS, private PostgreSQL networking, no published API port, mounted signing-key secrets, and a configurable trusted-proxy address are configured. NPM must use the reserved trusted address and overwrite forwarded headers; HTTPS/security-header configuration and LAN verification remain. |
| 6 — Operations and governance | Partial. Minimal health/readiness probes, an ADMIN-protected Prometheus endpoint, and a runbook draft are present. Correlation IDs, alert delivery/thresholds, approved policies, and full OpenAPI governance remain. |
| 7 — LAN acceptance and go-live | Not complete. No production promotion is authorized; all preceding gates and acceptance checks must pass first. |

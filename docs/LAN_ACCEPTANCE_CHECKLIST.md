# LAN acceptance checklist

**Scope:** pre-production verification of Phases 2-7. Phase 0 policy approvals and production
promotion are explicitly out of scope. Use only disposable test accounts and a separate test
database. Do not run these steps against production data or production secrets.

## 1. Prerequisites and isolation

- [ ] Use a dedicated LAN test host with Docker Compose, PostgreSQL client tools, and an NPM test
  proxy host. The app and database services in Compose have project-scoped names and volumes; the
  test app uses its own `BE_APP_NETWORK_ALIAS`.
- [ ] Create an untracked `.env.lan-test` with unique `POSTGRES_USER`, `POSTGRES_PASSWORD`,
  `POSTGRES_DB`, `OAUTH2_ISSUER`, `OAUTH2_BOT_CLIENT_ID`, `OAUTH2_SIGNING_KEY_FILE`,
  `OAUTH2_SIGNING_KEY_PASSWORD_FILE`, `NPM_INTERNAL_PROXY_REGEX`, `APP_CORS_ALLOWED_ORIGINS`,
  `POSTGRES_TEST_DB_PORT=15432`, `BE_APP_NETWORK_ALIAS=portroyal-be-app-lan-test`, and a
  dedicated `APP_LOG_DIRECTORY`.
- [ ] Set these non-secret values in the PowerShell session to match `.env.lan-test`:

```powershell
$env:POSTGRES_USER = "prbe_lan_test"
$env:POSTGRES_DB = "prbe_lan_test"
$env:POSTGRES_TEST_DB_PORT = "15432"
$lanHost = "prbe-lan-test.example.test"
```
- [ ] Store `.env.lan-test`, signing keys, password files, and `PGPASSFILE` outside the repository
  with restrictive permissions. Do not paste credentials or tokens into shared logs.
- [ ] Ensure the NPM test host is attached to `npm_main_net`, has a test DNS name and certificate,
  and forwards to `portroyal-be-app-lan-test:8080`. NPM must overwrite forwarded client/protocol
  headers and its IP must match `NPM_INTERNAL_PROXY_REGEX`.
- [ ] Set the test hostname in the current PowerShell session:

```powershell
$lanHost = "prbe-lan-test.example.test"
```
- [ ] Keep the PostgreSQL test port bound only to loopback via the test override below. Never
  publish the database or application port on a public interface.

## 2. Start the isolated stack

Run on the dedicated test host:

```powershell
docker compose --env-file .env.lan-test -p prbe-lan-test `
  -f docker-compose.yaml -f docker-compose.lan-test.yaml config --quiet

docker compose --env-file .env.lan-test -p prbe-lan-test `
  -f docker-compose.yaml -f docker-compose.lan-test.yaml up --build -d

docker compose --env-file .env.lan-test -p prbe-lan-test `
  -f docker-compose.yaml -f docker-compose.lan-test.yaml ps
```

- [ ] Confirm PostgreSQL is healthy and the app is ready:

```powershell
docker compose --env-file .env.lan-test -p prbe-lan-test `
  -f docker-compose.yaml -f docker-compose.lan-test.yaml exec postgres `
  sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" pg_isready -h 127.0.0.1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'

curl.exe -i "https://$lanHost/actuator/health/readiness"
```

- [ ] Inspect Flyway history and verify V1 through V7 completed successfully:

```powershell
docker compose --env-file .env.lan-test -p prbe-lan-test `
  -f docker-compose.yaml -f docker-compose.lan-test.yaml exec postgres `
  sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" psql -h 127.0.0.1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT version, success FROM flyway_schema_history ORDER BY installed_rank"'
```

- [ ] Confirm app logs contain no credentials, bearer tokens, OTPs, email bodies, or reset tokens:

```powershell
docker compose --env-file .env.lan-test -p prbe-lan-test `
  -f docker-compose.yaml -f docker-compose.lan-test.yaml logs --tail 200 be_app
```

## 3. Database migrations and backup/restore

- [ ] Start from the isolated empty database and confirm Flyway created the complete schema with
  Hibernate validation enabled.
- [ ] For upgrade testing, restore a sanitized development backup into a second disposable empty
  database and verify Flyway advances it through V7. Do not use the source database as the target.
- [ ] Install `pg_dump` and `pg_restore` on the test host. Create a protected `PGPASSFILE` for the
  loopback-only test database, then set:

```powershell
$env:PGHOST = "127.0.0.1"
$env:PGPORT = $env:POSTGRES_TEST_DB_PORT
$env:PGDATABASE = $env:POSTGRES_DB
$env:PGUSER = $env:POSTGRES_USER
$env:PGPASSFILE = "C:\secure\prbe-lan-test.pgpass"
```

- [ ] Create and validate a backup on an encrypted, access-controlled destination:

```powershell
.\scripts\backup-postgres.ps1 `
  -BackupDirectory "E:\encrypted\prbe-lan-test-backups" `
  -EncryptedTargetConfirmed
```

- [ ] Create a separate empty restore database, restore the archive, then verify tables and Flyway
  history. The restore script intentionally refuses to target `PGDATABASE` and does not drop
  existing objects:

```powershell
docker compose --env-file .env.lan-test -p prbe-lan-test `
  -f docker-compose.yaml -f docker-compose.lan-test.yaml exec postgres `
  sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" createdb -h 127.0.0.1 -U "$POSTGRES_USER" prbe_restore_test'

.\scripts\restore-postgres.ps1 `
  -BackupFile "E:\encrypted\prbe-lan-test-backups\portroyal-$env:POSTGRES_DB-<UTC timestamp>.dump" `
  -TargetDatabase "prbe_restore_test" `
  -ConfirmSeparateEmptyDatabase
```

- [ ] Verify the restore includes the latest account-deletion tombstone ledger before exposing the
  restored app. This repository does not yet automate an external, durable deletion ledger, so this
  remains an acceptance blocker.
- [ ] Record the manual backup owner and RPO. The selected policy is manual backups with a 30-day
  maximum retention; this does not provide a fixed recovery-point objective.

## 4. OAuth/OIDC and account lifecycle

- [ ] Verify `/.well-known/openid-configuration` returns the configured test issuer and that the
  discovered authorization, token, JWKS, revocation, and logout endpoints use the test host.
- [ ] Register a test public client through the operator tool with exact redirect URIs. Complete
  Authorization Code + PKCE (`S256`) with a valid verifier, then repeat with an invalid verifier
  and confirm it is rejected. Verify no public-client secret exists.
- [ ] Verify consent and login require browser interaction, while JSON game API requests are
  stateless bearer-token requests. Confirm login/consent browser sessions retain CSRF protection.
- [ ] Verify access-token issuer, audience, signature, and expiry. Refresh once and confirm
  rotation; replay the consumed refresh token and verify the documented rejection/revocation
  behavior.
- [ ] Provision a disposable test BOT client. Verify `client_credentials` grants only
  `bot:onboard`, the custom Telegram grant requires that authenticated BOT, the delegated `sub` is
  the linked human, and normal game calls do not send `tgId`.
- [ ] Verify reset requests return the same generic response for existing and nonexistent emails.
  In the disposable DB, inspect that reset tokens are hashed, expire after one hour, are single-use,
  and revoke bearer authorizations and browser sessions after use. The external email consumer
  must be available to verify actual delivery.
- [ ] Verify Telegram OTP mismatch attempts increment and the token is invalid after the third
  failure. Verify a successful token cannot be replayed.
- [ ] Request account deletion with `confirm=true`. Verify it returns 202 while the account has an
  active match; after the match ends, verify the account is disabled/pseudonymized, authorizations
  and queued email are removed, move names/notes are pseudonymized, and the match history remains
  readable with no stale usernames from another app instance.
- [ ] Verify an account cannot be unified if both identities are associated with the same match;
  confirm the rejected unification does not consume its OTP. For non-overlapping histories,
  confirm match/move ownership transfers to the confirmed email account.

## 5. Input, rate-limit, and object-authorization checks

- [ ] Send a JSON request larger than 2 MiB to a public JSON endpoint and verify HTTP 413.
- [ ] Verify pagination rejects negative page numbers and page sizes outside 1-100. Verify long
  polling accepts 10-120 seconds and SSE accepts 1-3600 seconds.
- [ ] From one source IP, send six disposable registration requests within an hour; verify the
  sixth receives HTTP 429 and `Retry-After`. Verify login/token issuance (10/IP/minute), refresh
  (10/client+IP/minute), email sends (3/email+IP/hour), and OTP (three failures/token).
- [ ] With a disposable authenticated account, verify match creation and joining are each limited
  to 10/account/minute and match/move polling to 60/account/minute. Confirm NPM applies its
  configured coarse source-IP limits as well.
- [ ] Verify another user's token cannot submit moves, update/delete the account, or manipulate
  that user's callback subscription. Verify callbacks reject HTTP/private targets in the
  production-like profile, accept only dev/test localhost exceptions, do not follow redirects,
  and time out on slow endpoints.

Representative safe smoke requests (run only against the disposable stack):

```powershell
$base = "https://$lanHost/api/v1"

# A rejected JSON payload still exercises the registration IP bucket.
1..6 | ForEach-Object {
  & curl.exe -sS -o NUL -w "%{http_code} " -H "Content-Type: application/json" `
    --data-raw '{}' "$base/public/register"
}

# A nonexistent address must receive the same generic response on the first three requests;
# the fourth request from the same source IP should be rate limited.
$resetBody = '{"email":"nobody@example.test"}'
1..4 | ForEach-Object {
  & curl.exe -sS -o NUL -w "%{http_code} " -H "Content-Type: application/json" `
    --data-raw $resetBody "$base/public/password-reset"
}

# Send a JSON payload larger than 2 MiB; expected result: HTTP 413.
$largeBodyPath = Join-Path $env:TEMP 'prbe-body-limit-test.json'
$largeBody = '{"x":"' + ('x' * 2097150) + '"}'
[System.IO.File]::WriteAllText($largeBodyPath, $largeBody, [System.Text.UTF8Encoding]::new($false))
& curl.exe -sS -o NUL -w "%{http_code}" -H "Content-Type: application/json" `
  --data-binary "@$largeBodyPath" "$base/public/password-reset"
Remove-Item -LiteralPath $largeBodyPath -Force
```

The registration sequence should return five `400` responses for the intentionally invalid `{}` body,
then `429`; the email sequence should return three generic `200` responses, then `429`. Repeat
through NPM and directly from a disposable container on `npm_main_net` to distinguish proxy
enforcement from application enforcement. Rate buckets are fixed-window and persist in PostgreSQL,
so use a fresh test source or wait for the window to roll over before repeating.

## 6. NPM, proxy, and operations

- [ ] Confirm HTTP redirects to HTTPS, certificate validation succeeds, security headers are
  present, CORS preflight succeeds only for configured origins, and `X-Request-ID` is returned.
- [ ] Spoof `X-Forwarded-For` and forwarded scheme from a non-NPM client. Confirm the app does not
  trust those values; confirm NPM overwrites them.
- [ ] From a machine outside the Docker host, verify ports 8080 and 5432 are unreachable; only the
  HTTPS NPM host is exposed. Verify the test DB mapping is bound to `127.0.0.1`.
- [ ] Confirm `/actuator/health/liveness` and `/readiness` expose minimal status, Prometheus
  requires ADMIN, and the Prometheus alert rules load. Configure Alertmanager separately and send
  a test alert before production.
- [ ] Verify correlation IDs appear in console/file logs and match response headers. Confirm logs
  remain redacted.
- [ ] Run the full suite again after LAN tests and attach sanitized results to the handoff:

```powershell
mvn -pl portroyal_be -am test
```

## 7. Results to report

Record the test host/environment, Flyway versions, backup archive verification, issuer URL,
PKCE/refresh results, rate-limit outcomes, CORS/forwarded-header results, external port scan,
alert delivery, and any failed check. Do not include secrets, tokens, email bodies, or personal
data in the report. Production promotion is not authorized by this checklist.

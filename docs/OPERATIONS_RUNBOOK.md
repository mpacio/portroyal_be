# Operations runbook draft

**Status: operational draft. Assign owners, alert thresholds, and alert-delivery channels before
production go-live.**

## Health and metrics

- `GET /actuator/health/liveness` and `/actuator/health/readiness` expose minimal probe status.
  Detailed component health is disabled.
- `/actuator/prometheus` is protected by the `ADMIN` authority. Scraping credentials must be
  provisioned and stored outside the repository.
- Do not publish actuator ports directly; reach the API through the configured reverse proxy.
- Initial Prometheus alert expressions and thresholds are in
  [`deploy/monitoring/prometheus-alerts.yml`](../deploy/monitoring/prometheus-alerts.yml). Review
  them against LAN traffic before enabling paging; Alertmanager routing and delivery contacts are
  not configured in this repository.
- Every response includes a server-generated `X-Request-ID`; use it to correlate entries in the
  application logs. Client-supplied request IDs are not trusted.

## Request limits and NPM

- The application enforces shared fixed-window limits from PostgreSQL. `429` responses include
  `Retry-After`. The API body limit is 2 MiB; form bodies are also limited by the embedded Tomcat
  connector.
- Application bucket keys store hashes of operation/scope values, not raw IP addresses, email
  addresses, or account identifiers. PostgreSQL server time defines the fixed-window boundaries.
- NPM must apply source-IP rate limits to registration, login/authorization, and token endpoints.
  Define `limit_req_zone` in the Nginx `http` context and apply `limit_req` to the corresponding
  proxied routes with `limit_req_status 429`; provision the zones in NPM's HTTP-level custom
  configuration, not inside a proxy-host `location` block. Cover registration (5/hour), login and
  token issuance (10/minute), and email sends (3/hour per source IP as a coarse outer limit).
- The application remains authoritative for per-account match/read limits and per-email limits;
  NPM should provide an additional coarse IP shield. Preserve `Retry-After` and avoid caching
  `429` responses.
- Configure proxy read/send timeouts above the 120-second long-poll limit and 3600-second SSE
  limit, and ensure NPM overwrites forwarded client/protocol headers rather than appending
  untrusted values. Validate the actual NPM configuration in the LAN environment before production.
- Callback delivery validates production callback hosts against private/loopback ranges, refuses
  redirects, and has 5-second connect / 10-second read timeouts. Maintain an outbound network
  policy that also blocks private and link-local destinations; application DNS checks alone do not
  replace egress firewall controls.

Selected values (per source IP unless stated otherwise): registration 5/hour; login and token
issuance 10/minute; refresh 10 per OAuth client and IP/minute; email sends 3 per email and IP/hour;
numeric OTPs 3 failed attempts per token; match creation and join 10 per account/minute each;
resource-intensive reads 60 per account/minute.

## Database outage or migration failure

1. Check the API readiness probe and PostgreSQL availability without printing connection secrets.
2. Check application logs for the failed migration version and exception type; never retry a
   failed migration by manually changing `flyway_schema_history`.
3. Preserve the database and logs, restore service connectivity, then validate migration checksums
   and the live schema before restarting the API.
4. Use the documented backup procedure and restore to a separate database before replacing
   production data. Retention, escalation owner, and restore acceptance window: **TBD**.

## Email queue backlog

The API stores outbound messages in the PostgreSQL `emails_queue`. No email delivery consumer or
provider implementation is included in this repository, so queueing a message does not prove it was
delivered.

1. Check queue depth through `portroyal_email_queue_pending` without printing recipient addresses
   or message bodies.
2. Confirm the externally managed consumer and selected provider are reachable without exposing
   credentials.
3. After recovery, monitor queue progress and delivery failures; do not delete queued messages
   unless the service owner approves the data-loss impact.
4. Alert threshold and escalation owner: **proposed in the Prometheus rule file; delivery owner TBD**.

## OAuth key or client-secret compromise

1. For a BOT secret, update the secret in the BOT's secret manager and run the controlled
   `rotate-bot` registration operation. It revokes existing authorizations for that client.
2. For a signing key, use the overlapping-key rotation procedure in
   [`OAUTH_CLIENT_OPERATIONS.md`](OAUTH_CLIENT_OPERATIONS.md); keep the former public key available
   until existing token lifetimes have elapsed.
3. For a user credential compromise, reset the password through the approved account flow. Password
   changes revoke persisted authorizations immediately.
4. Record incident scope and notify the security contact once the currently TBD contact has been
   assigned.

## Backup and restore

Backups are manual only, have a selected maximum retention of 30 days, and must include PostgreSQL.
Create and verify a backup before migrations, maintenance, or other risky operations. Store backups
encrypted, keep credentials outside command arguments and repository files, and reapply account
deletions after a restore. A durable deletion ledger outside the backup set must be maintained for
this reapplication; it is not automated yet. Manual backups provide no fixed recovery-point
objective; storage location and restore ownership still need operational confirmation.
Docker/pg_dump restore acceptance remains unverified in this environment. Never use a live
production database as a restore target.

The repository scripts use `PGPASSFILE` instead of placing database credentials in command
arguments:

```powershell
.\scripts\backup-postgres.ps1 `
  -BackupDirectory "E:\encrypted\portroyal-backups" `
  -EncryptedTargetConfirmed

.\scripts\restore-postgres.ps1 `
  -BackupFile "E:\encrypted\portroyal-backups\portroyal-prdb-20261026-020000.dump" `
  -TargetDatabase "prdb_restore_test" `
  -ConfirmSeparateEmptyDatabase
```

Set `PGHOST`, `PGDATABASE`, `PGUSER`, and `PGPASSFILE` in the protected environment. The restore
script refuses to use `PGDATABASE` as its target and does not drop existing objects; the operator
must create a separate empty database. Reapply the durable account-deletion ledger before exposing
the restored service.

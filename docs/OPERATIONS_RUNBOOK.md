# Operations runbook draft

**Status: operational draft. Assign owners, alert thresholds, restore retention, and delivery
channels before production go-live.**

## Health and metrics

- `GET /actuator/health/liveness` and `/actuator/health/readiness` expose minimal probe status.
  Detailed component health is disabled.
- `/actuator/prometheus` is protected by the `ADMIN` authority. Scraping credentials must be
  provisioned and stored outside the repository.
- Do not publish actuator ports directly; reach the API through the configured reverse proxy.

## Database outage or migration failure

1. Check the API readiness probe and PostgreSQL availability without printing connection secrets.
2. Check application logs for the failed migration version and exception type; never retry a
   failed migration by manually changing `flyway_schema_history`.
3. Preserve the database and logs, restore service connectivity, then validate migration checksums
   and the live schema before restarting the API.
4. Use the documented backup procedure and restore to a separate database before replacing
   production data. Retention, escalation owner, and restore acceptance window: **TBD**.

## Email queue backlog

1. Check queue depth and the `EMAIL_SENDING_ENABLED`/provider configuration without printing
   recipient addresses or message bodies.
2. Confirm the email provider is reachable and that its credentials remain valid.
3. After recovery, monitor queue progress and failed deliveries; do not delete queued messages
   unless the service owner approves the data-loss impact.
4. Alert threshold and escalation owner: **TBD**.

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

Backups must include PostgreSQL and be restorable to a separate database before any production
promotion. Define backup frequency, encrypted storage, retention, restore ownership, and tested
recovery objective: **TBD**. Do not use a live production database as a restore target.

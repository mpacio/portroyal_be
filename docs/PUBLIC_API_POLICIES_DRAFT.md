# Public API policy drafts

**Status: draft only. These policies are not approved, effective, or ready to publish. Production
go-live remains blocked until the privacy notice and legal basis are reviewed, the email provider
and processing region are confirmed, and the operational contact is verified.**

The operational choices below were supplied by the project owner. They are proposed policy text,
not a legal determination or a statement that the controls are already implemented.

## Acceptable use

Port Royal is a personal, non-commercial game service operated under the project name **MSP
Games**. Browser, native/mobile, CLI, and Telegram BOT clients are in scope. Generic
server-to-server integrations are not supported in the initial public service.

Clients may automate normal gameplay, including CLI play, provided they respect the published
limits and do not impair the service. Users must not conduct bulk scraping, attempt unauthorized
access, interfere with other users' accounts or matches, evade access controls, or disrupt the
service. Abuse reports may be sent to the contact below. The operator may restrict or suspend
access to investigate abuse, protect users or service availability, or perform maintenance.

The service is provided on a best-effort basis, without a service-level availability guarantee.

## Privacy and data retention

### Operator and processing

The project owner identifies **MSP Games** as the public project/operator name. The individual's
legal identity, controller details, and the applicable legal basis have not yet been verified and
must be reviewed before this notice is published. The service operates from Italy.

The service processes account identifiers (username, email address, Telegram ID where linked),
password hashes, authorization/account metadata, and match and move data to provide account,
identity, and gameplay functions. Source IP addresses and request metadata may be processed for
security, abuse prevention, and enforcement of usage limits. Email is used for account
confirmation, account recovery, and Telegram/email account linking.

Hosting is provided through Hetzner in Germany. The outgoing email provider has not been selected;
Mailjet is not confirmed. No analytics, advertising, or third-party tracking services are planned.
Recipients, subprocessors, transfer details, and legal bases remain subject to privacy review.

### Retention and requests

Account data is retained until the account owner requests deletion. Technical security/access logs
are retained for 30 days. Pseudonymized match and move history may be retained for as long as it is
useful to operate the service. Backups are intended to have a maximum 30-day retention; after a
restore, account deletions must be applied again. These periods and the restoration procedure must
be verified operationally and approved in the privacy review.

Privacy and data-subject requests may be sent to [support@matteopaciolla.com](mailto:support@matteopaciolla.com).
The request-verification process and required legal disclosures remain subject to review.

## Account and data deletion

The intended self-service policy is that an account owner submits a deletion request using an
authenticated bearer token and an explicit confirmation. An email OTP is not required. Requests
are deferred while the account has active matches. Once those matches are complete, identifying
account data is to be removed within 30 days; historical match and move records may be retained in
pseudonymized form for as long as they are useful to the service. Backup copies may persist for up
to 30 days and must not be restored without reapplying deletions.

This is a target policy, not the behavior currently enforced by the API. The existing deletion
endpoint does not yet implement the confirmation, active-match deferral, or pseudonymization
requirements. Do not present it as a compliant self-service deletion flow until the implementation
is updated and verified.

## Support and security contact

The project owner selected **support@matteopaciolla.com** for both support requests and security
reports. Verify that the address is monitored and has an assigned operational owner before
publishing it.

## Initial usage and rate limits

The following initial limits were selected for the proposed public policy. They are not yet
enforced by the application or reverse proxy and must not be represented as active protections
until Phase 4 implementation and deployment verification are complete. Rate-limit responses are
intended to use HTTP 429 and `Retry-After`. Email-related responses must not reveal whether an
account exists.

| Operation | Limit | Window and scope | Response |
|---|---|---|---|
| Registration | 5 requests | Per source IP, per hour | HTTP 429 with `Retry-After` |
| Login and token issuance | 10 requests | Per source IP, per minute | HTTP 429 with `Retry-After` |
| Token refresh | 10 requests | Per OAuth client and source IP, per minute | HTTP 429 with `Retry-After` |
| Email confirmation, OTP, and password-reset sends | 3 sends | Per email address and source IP, per hour | HTTP 429 with `Retry-After`; generic response |
| Numeric OTP verification | 3 failed attempts | Per token; invalidate after the third failure | Generic invalid/expired response |
| Match creation | 10 requests | Per account, per minute | HTTP 429 with `Retry-After` |
| Match joining | 10 requests | Per account, per minute | HTTP 429 with `Retry-After` |
| Match/move polling and other resource-intensive reads | 60 requests | Per account, per minute | HTTP 429 with `Retry-After` |

## Recovery policy

Web/app passwords must contain at least 8 Unicode code points and at most 72 UTF-8 bytes; no
particular mix of character classes is required.

The intended password-reset flow is available only for web/app accounts with a confirmed email.
The public request returns a generic response regardless of whether the address belongs to an
account. For an eligible account, the service sends a single-use reset token that expires after
one hour. A successful reset revokes the user's existing sessions and tokens. This flow is not
implemented yet and must be described as planned until it is available and tested.

## Approval and publication checklist

- Confirm the data controller's legal identity and legal basis with an appropriate privacy review.
- Select the outgoing email provider and confirm its processing region and contractual terms.
- Verify that support@matteopaciolla.com is monitored for support and security reports.
- Verify retention, backup expiry, and deletion reapplication operationally.
- Implement the account recovery, deletion, and rate-limit controls before describing them as
  available.
- Approve the final notice and policy text before publication or production go-live.

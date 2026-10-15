# Security review

Original review: 2026-09-24
Mitigation verification: 2026-10-15

Scope: review of the backend application for common security flaws across authentication, authorization, input handling, and outbound integrations.

## Findings

| # | Status    | Severity    | File                                                                          | Lines                              | Finding                                                                                                                                                                                                                                                                                                      | Confidence |
|---|-----------|-------------|-------------------------------------------------------------------------------|------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------|
| 1 | MITIGATED | 🔴 CRITICAL | `controller/UserController.java`                                              | current user-profile endpoints     | User lookup is administrator-only; profile edits are self-only except ADMIN; deletion is self-or-admin. API authorization is checked from bearer-token roles and subject.                                                                                                                                    | 9/10       |
| 2 | MITIGATED | 🟠 HIGH     | `service/SentinelService.java`, `security/callback/CallbackUrlValidator.java` | callback registration and delivery | Callback URLs are checked at registration and delivery, public DNS addresses are pinned into the outbound HTTP client, redirects are disabled, and delivery is bounded by timeouts. Payloads use timestamped HMAC-SHA-256; callback secrets are encrypted at rest. Egress firewall rules remain recommended. | 9/10       |
| 3 | MITIGATED | 🟠 HIGH     | `Main.java`, `config/OAuthClientProvisioner.java`                             | provisioning                       | No default admin password is created. Legacy `admin/password` rows are disabled; the BOT account has no password and uses a configured OAuth client secret.                                                                                                                                                  | 9/10       |
| 4 | MITIGATED | 🟡 MEDIUM   | `config/WebSecurityConfig.java`                                               | CORS configuration                 | Wildcard credentialed CORS was replaced with explicit configured origins and credentials are disabled for bearer-based browser API access. OIDC browser origins use the same allowlist.                                                                                                                      | 9/10       |

## Authentication changes

- Protected `/api/v1/**` endpoints now accept stateless OAuth2 bearer tokens; HTTP Basic is removed.
- Human web/CLI clients use OIDC Authorization Code + PKCE. Public clients receive short-lived access
  tokens and rotating refresh tokens only with `offline_access`; refresh-token reuse revokes the
  authorization family.
- The Telegram bot uses client credentials, keeps its secret server-side, and retains the BOT plus
  `tgId` acting-user model.
- Password hashes are upgraded from BCrypt to Argon2id on successful login; login attempts are
  rate-limited in PostgreSQL. Email-confirmation, reset and Telegram OTP values are not logged.
- Browser token storage is memory-only, not a BFF. This reduces persistent exposure but does not
  protect tokens from XSS while the application is running. MFA/passkeys and external client
  migrations remain follow-up work.

## Notes

- The SSRF controls validate each DNS resolution and use those validated addresses at connect time, reducing DNS-rebinding risk. Network egress restrictions provide defense in depth.
- Database at-rest encryption and TLS termination/proxy configuration remain deployment responsibilities; keep OAuth signing and callback-encryption keys outside PostgreSQL and source control.

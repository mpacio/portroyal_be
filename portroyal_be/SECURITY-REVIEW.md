# Security review

Date: 2026-09-24

Scope: review of the backend application for common security flaws across authentication, authorization, input handling, and outbound integrations.

## Findings

| # | Status | Severity | File | Lines | Vulnerability | Confidence |
|---|--------|----------|------|-------|---------------|------------|
| 1 | SOLVED | 🔴 CRITICAL | src/main/java/com/matteopaciolla/prbe/controller/UserController.java | 53-64 | Arbitrary user deletion and lookup via /api/v1/user/{username} without ownership checks or admin enforcement; any authenticated user can target other accounts. | 9/10 |
| 2 || 🟠 HIGH | src/main/java/com/matteopaciolla/prbe/service/SentinelService.java | 57-80, 183-230 | Callback subscription accepts arbitrary external URLs and then POSTs alert payloads to them. This creates an SSRF/vector for outbound request abuse and sensitive data exfiltration. | 9/10 |
| 3 || 🟠 HIGH | src/main/java/com/matteopaciolla/prbe/Main.java | 49-75 | Startup creates default privileged accounts (admin and bot) with the hardcoded password "password", creating predictable credentials for a reachable deployment. | 8/10 |
| 4 || 🟡 MEDIUM | src/main/java/com/matteopaciolla/prbe/config/WebSecurityConfig.java | 81-88 | CORS is configured with wildcard origins while credentials are enabled, permitting cross-origin credentialed requests from untrusted origins and increasing CSRF/XSS impact. | 8/10 |

## Notes

- The most actionable risk is the broken access control around account management: the controller exposes user data and deletion by username without comparing the requested account to the currently authenticated user.
- The callback-based notification system is a classic SSRF sink because it trusts a user-supplied URL and performs an outbound HTTP POST with application data.
- The application does use BCrypt for password hashing, which is a strength, but the default hardcoded credentials undermine the default security posture.
- The global CORS configuration is also risky because it allows any origin with credentials enabled; it should be restricted to trusted origins.

# Security Audit

Audit date: 2026-08-26. Status legend: PASS, FIXED, LIMITATION.

| Check | Status | Finding | Action |
|---|---|---|---|
| Secrets | PASS | Runtime credentials use environment variables; committed values are explicitly local placeholders. | `.env` is ignored; `.env.example` documents rotation. |
| Passwords | PASS | BCrypt is used and no response contains `passwordHash`. | Registration constrains BCrypt-safe 8-72 character input. |
| SQL injection | PASS | All data access uses Spring Data/JPA criteria; there is no concatenated SQL. | None. |
| Authorization matrix | PASS | Creation has coarse role security; all resource actions use centralized owner/assigned-agent checks. | Permission tests exercise each distinct matrix rule. |
| IDOR | PASS | Direct ticket lookup is followed by `assertCanView`; another customer/agent receives 403. | None. |
| JWT | PASS | HS256 requires a 32-byte secret; each request verifies once, then uses a one-minute password-free principal cache. | Signed ID/email/role are matched after cache refresh; expired, tampered, stale-role, weak-secret, and invalid-token/no-lookup tests pass. |
| Request validation | PASS | All request bodies use validated DTOs; malformed enum JSON receives 400. | None. |
| Error leakage | PASS | Shared errors omit traces, SQL text, and internal exception names. | Unknown errors return a generic 500 message. |
| CORS | PASS | Origins are environment-controlled; credentials are disabled and no wildcard is used. | None. |
| Dependencies | PASS | Spring Boot 3.5.5 resolves Spring Security 6.5.3. | Version family reviewed during dependency resolution. |
| Login rate limiting | PASS | A thread-safe fixed window limits attempts per observed client address and returns 429 with `Retry-After`. | Limits/window are environment-configurable; use a trusted gateway or shared limiter for multiple app instances. |

The public role field on registration is an explicit assessment convenience; production agent provisioning requires privileged administration.

The JWT cache deliberately trades at most one minute of role/email/deletion
staleness for removal of the per-request user query. Password login never uses
this cache. After a changed user is reloaded, signed claim mismatch rejects the
old token; the user must authenticate again. A shorter TTL tightens this window
but increases PostgreSQL traffic.

# Security

## Auth mechanism

- Spring Security with a stateless JWT filter chain (no sessions, no
  CSRF tokens needed since there's no cookie-based session — set
  `.csrf(csrf -> csrf.disable())` and document *why* this is safe here:
  stateless bearer-token APIs aren't subject to CSRF the way cookie-session
  apps are).
- Password storage: BCrypt via `PasswordEncoder`.
- JWT signed with HS256, secret from an environment variable
  (`JWT_SECRET`), never hardcoded, never committed. Provide a `.env.example`
  with a placeholder.
- Token expiry: short-lived (e.g. 1 hour) is more correct, but for an
  assessment context a longer expiry (e.g. 24h) avoids friction during
  manual testing — pick one, document the tradeoff, don't silently pick the
  convenient option without saying so.
- `JwtAuthenticationFilter` extends `OncePerRequestFilter`, extracts and
  validates the token, loads the user, sets the `SecurityContext`.

## Roles

- `CUSTOMER`
- `AGENT`

No `ADMIN` role in scope — the brief doesn't call for one. If registration
being open (see `04-API-SPEC.md`) feels like it needs admin gating in a
real product, say so in the README's assumptions/limitations section rather
than building an admin system that isn't asked for.

## Permission matrix

This is the authoritative implementation of TASK-009. Every row must map to
an actual enforced check (via `@PreAuthorize` + the `PermissionService` from
`02-ARCHITECTURE.md`), not just to documentation.

| Action | Customer (own ticket) | Customer (other's ticket) | Agent (assigned) | Agent (unassigned/other's) |
|---|---|---|---|---|
| Create ticket | ✅ | n/a | ❌ | n/a |
| View ticket | ✅ | ❌ | ✅ | ❌* |
| Assign ticket | ❌ | ❌ | ✅ (reassign) | ✅ (claim) |
| Change status: OPEN→IN_PROGRESS | ❌ | ❌ | ✅ | ❌ |
| Change status: IN_PROGRESS→RESOLVED | ❌ | ❌ | ✅ | ❌ |
| Change status: RESOLVED→CLOSED | ✅ | ❌ | ✅ | ❌ |
| Change status: RESOLVED→REOPENED | ✅ | ❌ | ✅ | ❌ |
| Change status: REOPENED→IN_PROGRESS | ❌ | ❌ | ✅ | ❌ |
| Change status: IN_PROGRESS→OPEN | ❌ | ❌ | ✅ | ❌ |
| Change priority | ❌ | ❌ | ✅ | ❌ |
| Add comment | ✅ | ❌ | ✅ | ❌ |
| View comments | ✅ | ❌ | ✅ | ❌* |
| View history | ✅ | ❌ | ✅ | ❌* |

`*` — an agent must be the ticket's *assigned* agent to view or act on it
(assignment is via the "Assign ticket" row above, which any agent can do
to claim an unassigned ticket). This is the direct reading of US-002 —
"tickets assigned to **me**" — rather than an implied broader queue view,
which the brief doesn't ask for. If a wider "see all tickets" view turns
out to be needed later, that's a new requirement to raise, not something
to build speculatively now.

## Enforcement mechanism

- Method-level: `@PreAuthorize("hasRole('AGENT')")` etc. for the
  coarse-grained role check.
- Resource-level (the "own ticket" vs "other's ticket" distinction, which a
  role check alone can't express): explicit calls into `PermissionService`
  inside the service layer, e.g. `permissionService.assertCanView(actor,
  ticket)`, throwing `PermissionDeniedException` → mapped to `403` by the
  global exception handler.
- Do not rely on `@PreAuthorize` SpEL expressions trying to reach into the
  DB to check ticket ownership — that gets unreadable fast. Keep coarse
  role checks in annotations, keep resource-ownership checks as explicit,
  readable service-layer code. This split is itself a design decision worth
  a line in `11-DESIGN-DECISIONS-LOG.md`.

## Password & input hygiene

- Passwords: BCrypt, minimum length enforced via Bean Validation on
  registration (e.g. 8 chars minimum).
- All request DTOs validated via `jakarta.validation` annotations —
  this is also a direct defense against injection-adjacent and malformed-
  data issues, and it's what the security audit in
  `09-AUDIT-CHECKLISTS.md` will specifically check for.
- JPA/Hibernate parameterized queries throughout (Spring Data derived
  queries and `@Query` with named/positional params) — no string-concatenated
  SQL anywhere. This is a hard rule, not a preference; the security audit
  checklist explicitly greps for string-concatenated queries.

## CORS

If a frontend will call this API from a different origin, configure CORS
explicitly (allowed origins from an env var, not `*` in anything beyond
local dev) rather than leaving Spring's defaults or wildcarding — the
security audit will flag `*` origins with credentials as a finding if left
in.

# Post-Build Audits

Run these **after** the full build (Phase 12 in `06-BUILD-PLAN.md`),
against the actual finished codebase — not as a design-time exercise. The
output of each audit is a filled-in findings doc, not a checkbox that says
"passed." A checklist with every item silently checked and no findings text
reads as unreviewed, not as clean code — every item needs at least a one-
line note on what was checked and what was found, even if the note is
"reviewed, no issue."

Create three files as the actual output of this phase:
`audits/CODE_QUALITY_AUDIT.md`, `audits/SECURITY_AUDIT.md`,
`audits/PERFORMANCE_AUDIT.md`. Each follows the finding template at the
bottom of this doc.

---

## 1. Code Quality Audit

- [ ] **Layering respected**: no repository calls inside controllers; no
  business logic (branching on domain rules) inside controllers.
- [ ] **Naming consistency**: consistent verb choices across similar
  operations (e.g. not `getById` in one service and `fetchById` in
  another for the same kind of operation).
- [ ] **No dead code**: no unused imports, unused methods, commented-out
  blocks left in.
- [ ] **DRY on the transition/permission rules specifically**: confirm the
  transition table and permission matrix each exist in exactly one place
  in code (per `02-ARCHITECTURE.md`'s stated intent) — grep for any second
  copy of transition logic that might have crept into a controller.
- [ ] **Consistent exception handling**: every custom exception is
  actually routed through the global `@ControllerAdvice`; no controller
  has its own ad-hoc try/catch producing a different error shape than
  `04-API-SPEC.md`'s schema.
- [ ] **DTOs never leak entities**: confirm no controller method returns a
  JPA entity directly (check for lazy-loading serialization risk
  specifically — this is both a code-quality and a latent
  performance/bug issue).
- [ ] **Magic values**: no hardcoded strings for status/role/category
  values outside the enums themselves (grep for string literals that
  should be enum references).
- [ ] **Method/class size**: no service method doing more than one clearly
  nameable thing; flag (don't necessarily fix, but note) any method over
  ~40 lines as a candidate for extraction.
- [ ] **Consistent transaction boundaries**: `@Transactional` present on
  every service method with multi-step writes (per `02-ARCHITECTURE.md`'s
  status-change + history-write atomicity requirement) — verify this one
  specifically, it's a real correctness issue if missed, not just style.
- [ ] **Lint/formatter run**: if a formatter (e.g. Spotless,
  google-java-format) wasn't wired in from the start, run one now and note
  the diff size in findings (a large diff at audit time signals formatting
  wasn't consistent during development — worth noting honestly).
- [ ] **Javadoc/comments on non-obvious logic**: specifically the state
  machine and permission service — code that encodes a business rule
  table should have a comment pointing back to `01-REQUIREMENTS.md`'s
  transition table, so a future reader doesn't have to reverse-engineer
  intent from cases.

## 2. Security Audit

- [ ] **No hardcoded secrets**: grep the entire repo (including test
  files and any committed `application.yml`) for the JWT secret, DB
  password, or any credential — everything must come from env vars, with
  only placeholder/example values in committed config.
- [ ] **Password handling**: confirm BCrypt (or equivalent) is used, never
  plaintext or reversible encoding; confirm passwords are never included
  in any response DTO, log statement, or exception message.
- [ ] **SQL injection surface**: grep for any string-concatenated or
  `String.format`-built query — confirm zero instances; confirm all
  queries go through Spring Data derived methods or parameterized
  `@Query`.
- [ ] **Authorization completeness**: cross-check every row of the
  permission matrix in `05-SECURITY.md` against an actual enforced check
  in code — this is the single most important item in this audit, don't
  just spot-check, walk the full matrix row by row.
- [ ] **IDOR check**: specifically attempt (mentally or via a quick manual
  test) to access another customer's ticket by guessing/incrementing an
  ID — confirm `403`, not accidental success or an information-leaking
  `500`.
- [ ] **JWT handling**: confirm expired tokens are rejected; confirm
  tampered signatures are rejected; confirm the secret has adequate
  length/entropy for HS256 (not a short/guessable string even as a
  placeholder default).
- [ ] **Input validation coverage**: confirm every request DTO has Bean
  Validation annotations — no endpoint accepting raw unvalidated input.
- [ ] **Error message leakage**: confirm exception responses never
  include stack traces, internal class names, or SQL error text — the
  global handler should always return the clean shared error schema.
- [ ] **CORS configuration**: confirm no wildcard origin combined with
  credentials allowed, per `05-SECURITY.md`.
- [ ] **Dependency check**: run `mvn dependency:tree` or an equivalent and
  note the Spring Boot/Spring Security versions in use; flag if any are
  meaningfully behind current stable (a submission using a known-vulnerable
  old version is worth catching here rather than leaving as a silent risk).
- [ ] **Rate limiting / brute force on login**: note as a finding either
  way — if not implemented (reasonable to skip given assessment scope),
  say so explicitly as an accepted limitation rather than leaving it
  unaddressed and unmentioned.

## 3. Performance Audit

- [ ] **N+1 query check**: specifically inspect ticket-list and
  ticket-detail endpoints for N+1 patterns (e.g. lazy-loading
  `assignedAgent`/`customer` per ticket in a loop when listing). Use
  `spring.jpa.show-sql=true` locally and count actual queries fired for a
  `GET /api/tickets` call returning multiple tickets — if it's firing one
  query per ticket for related data, that's a finding, fix with a fetch
  join or `@EntityGraph`.
- [ ] **Pagination present**: confirm `GET /api/tickets` is paginated
  (per `04-API-SPEC.md`) and not returning an unbounded list — flag if any
  other list-returning endpoint (comments, history) lacks pagination and
  could plausibly grow large (a very long-running ticket could accumulate
  many comments/history entries).
- [ ] **Indexes exist**: confirm the indexes listed in `03-DATA-MODEL.md`
  are actually present in the Flyway migration, not just documented.
- [ ] **Connection pool sanity**: confirm HikariCP defaults are reasonable
  for the deployment context (default pool size is usually fine for this
  scale — note that it was checked, not necessarily changed).
- [ ] **Unnecessary eager fetching**: confirm `@ManyToOne`/`@OneToMany`
  associations default to `LAZY` (Hibernate's `@OneToMany` already
  defaults lazy; `@ManyToOne` defaults *eager* and is a common
  accidental-performance-bug source — explicitly check and set `LAZY`
  where the association isn't always needed).
- [ ] **Response payload size**: confirm response DTOs don't
  over-fetch/over-return relative to what the endpoint's actual use case
  needs (e.g. ticket-list responses probably don't need the full comment
  thread embedded).
- [ ] **Transactional scope**: confirm `@Transactional` boundaries aren't
  wrapping unnecessary work (e.g. an external call or heavy computation
  inside a transaction, holding a DB connection longer than needed) — not
  a major concern at this project's scope, but worth a one-line
  confirmation.
- [ ] **Startup/build sanity**: note actual measured `docker-compose up`
  cold-start time and typical endpoint response time from a manual
  curl/Postman check — concrete numbers are more convincing in the audit
  doc than "performance is good."

---

## Finding template (use this shape for every checklist item)

```markdown
### [Category] Short finding title

**Status**: ✅ Checked, no issue / ⚠️ Checked, minor issue / ❌ Checked, issue found and fixed / 📝 Checked, accepted limitation (not fixed, documented)

**What was checked**: <one line>

**Finding**: <what was found, even if "nothing notable">

**Action taken**: <fixed in commit X / left as documented limitation / n/a>
```

An audit doc made entirely of ✅ with no ⚠️/❌/📝 at all across three
audits on a from-scratch project is a signal the audit wasn't done
critically — real audits find *something*, even if it's minor. Look
honestly rather than defensively.

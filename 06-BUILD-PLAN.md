# Build Plan — Execution Order

This is the literal sequence to build in. Each phase lists the original
TASK-ID(s) it satisfies, so traceability back to the brief is never lost.
Commit after each phase with a message referencing the TASK-ID(s), e.g.
`feat(TASK-001): add Ticket entity and enums`. This makes the eventual
git history itself a piece of evidence that the brief was followed
systematically, which strengthens the submission independent of the README.

## Phase 0 — Scaffold (no TASK-ID, prerequisite)
- Spring Boot project: Web, Data JPA, Validation, Security, PostgreSQL
  driver, Lombok, Flyway.
- `application.yml` with profiles: `local` (Postgres via Docker),
  `test` (H2 or Testcontainers — prefer Testcontainers if time allows,
  since it exercises the real Postgres dialect in tests; H2 is an
  acceptable fallback, document which was chosen).
- Base package structure per `02-ARCHITECTURE.md`.

## Phase 1 — Domain foundation
**Satisfies: TASK-001 (Ticket model), TASK-003 (Category), TASK-004
(Priority)**
- Enums: `TicketStatus`, `TicketCategory`, `TicketPriority`, `UserRole`.
- Entities: `User`, `Ticket`, `Comment`, `StatusHistory` per
  `03-DATA-MODEL.md`.
- Flyway `V1__init_schema.sql`.
- Repositories for all four entities.

## Phase 2 — Security skeleton
**No single TASK-ID, but a hard prerequisite for TASK-009 and every
endpoint's role enforcement — build this before endpoints, not after.**
- `User` implements/adapts to `UserDetails` (or a dedicated
  `UserDetailsService` wraps it).
- `JwtService` (generate/validate/parse tokens).
- `JwtAuthenticationFilter`.
- `SecurityConfig` wiring the filter chain, `PasswordEncoder` bean,
  stateless session policy.
- `POST /api/auth/register`, `POST /api/auth/login`.
- Unit tests for `JwtService` (valid token round-trips, expired token
  rejected, tampered token rejected).

## Phase 3 — Ticket creation
**Satisfies: TASK-002 (Create API), TASK-005 US-001 (Validation)**
- `CreateTicketRequest` DTO with Bean Validation annotations.
- `TicketMapper` (entity ↔ DTO).
- `TicketService.create(...)`.
- `TicketController` — `POST /api/tickets`.
- Global exception handler (`@ControllerAdvice`) — build this now, since
  every subsequent phase's error cases depend on it existing.
- Unit tests: valid creation succeeds; each validation rule (blank title,
  missing category, invalid enum value) independently produces a `400`
  with the correct field error.

## Phase 4 — Read endpoints
**Supports US-002's "manage tickets assigned to me" premise; no single
TASK-ID but required before assignment/status tests are meaningful.**
- `GET /api/tickets/{id}`, `GET /api/tickets` (with the customer/agent
  visibility split and optional filters from `04-API-SPEC.md`).
- `PermissionService.assertCanView(...)`.
- Unit tests: customer sees only their own tickets; agent sees only
  tickets assigned to them; customer requesting another customer's ticket
  gets `403`; agent requesting a ticket assigned to someone else gets
  `403`.

## Phase 5 — Assignment
**Satisfies: TASK-005 (US-002, Assign ticket)**
- `PATCH /api/tickets/{id}/assign`.
- Writes a `StatusHistory` row with `eventType=ASSIGNMENT`.
- Unit tests: agent self-assigns; agent assigns to a named agent; customer
  attempting to assign gets `403`; assigning to a nonexistent agent gets
  `404`.

## Phase 6 — State machine + status transitions
**Satisfies: TASK-010 (Define allowed transitions), TASK-011 (Prevent
invalid transitions), TASK-006 (Update status)**
- `TicketStateMachine` component implementing the transition table from
  `01-REQUIREMENTS.md` / `03-DATA-MODEL.md`.
- `PATCH /api/tickets/{id}/status`, enforcing both the state machine
  *and* the permission matrix (including the customer
  `RESOLVED→CLOSED`/`RESOLVED→REOPENED` carve-out from `05-SECURITY.md`).
- Unit tests — this is the highest-value test target in the whole project,
  give it real coverage:
  - Every row in the transition table succeeds when attempted by an
    authorized actor.
  - Every non-listed (from, to) pair is rejected with `409`.
  - `CLOSED` accepts no outgoing transitions at all.
  - Each permission carve-out is independently tested (customer can do
    `RESOLVED→CLOSED`, cannot do `OPEN→IN_PROGRESS`).

## Phase 7 — Status history
**Satisfies: TASK-012 (Add status history)**
- `GET /api/tickets/{id}/history`.
- Confirm (via test, not just by inspection) that Phase 5's assignment
  writes and Phase 6's status writes both land correctly in this unified
  timeline, in the right order, with the right `eventType`.

## Phase 8 — Priority updates
**Satisfies: TASK-007 (Update priority)**
- `PATCH /api/tickets/{id}/priority`.
- Unit tests: agent updates priority; customer attempting this gets `403`;
  invalid enum value gets `400`.

## Phase 9 — Comments
**Satisfies: TASK-008 (Add comments)**
- `POST /api/tickets/{id}/comments`, `GET /api/tickets/{id}/comments`.
- Unit tests: ticket's customer and assigned agent can comment; an
  unrelated customer/agent cannot; comments list is chronological.

## Phase 10 — Permission consolidation pass
**Satisfies: TASK-009 (Validate permissions), as a dedicated review pass**
- Walk every endpoint against the permission matrix in `05-SECURITY.md`
  and confirm each row has a corresponding test (this is a *review* phase,
  not new feature work — its output is closing any gaps found, not new
  functionality).
- This is also the natural point to run the security audit checklist's
  "authorization" section early, rather than waiting for the formal audit
  at the very end — catching gaps here is cheaper than catching them in
  Phase 12.

## Phase 11 — Docker & DB backup
**Satisfies the Docker bonus and the Database bonus's backup requirement —
see `08-DOCKER-DB.md` for the full spec.**
- `docker-compose.yml` (app + Postgres).
- `Dockerfile` for the app (multi-stage build).
- Generate and commit the DB backup per `08-DOCKER-DB.md`.

## Phase 12 — Audits
**See `09-AUDIT-CHECKLISTS.md` for the full checklists. Run all three
against the finished codebase, not incrementally per-phase — they need the
complete picture.**
- Code quality audit.
- Security audit.
- Performance audit.
- Fix findings where reasonable within scope; document anything left open
  and why, in the audit doc itself and referenced from the README.

## Phase 13 — Documentation
- Fill in `11-DESIGN-DECISIONS-LOG.md` for anything not captured live
  during the phases above.
- Write the README from `10-README-TEMPLATE.md`.
- Final pass: confirm every TASK-ID from the original brief appears in the
  README's traceability table (see the template) pointing at real code.

## Traceability check (run this before calling it done)

Every TASK-ID below must map to committed code. Use this as a literal
checklist at the end:

- [ ] TASK-001 Ticket model — Phase 1
- [ ] TASK-002 Create API — Phase 3
- [ ] TASK-003 Category — Phase 1
- [ ] TASK-004 Priority — Phase 1
- [ ] TASK-005 (US-001) Validation — Phase 3
- [ ] TASK-005 (US-002) Assign ticket — Phase 5
- [ ] TASK-006 Update status — Phase 6
- [ ] TASK-007 Update priority — Phase 8
- [ ] TASK-008 Add comments — Phase 9
- [ ] TASK-009 Validate permissions — Phase 10 (+ enforced throughout)
- [ ] TASK-010 Define allowed transitions — Phase 6
- [ ] TASK-011 Prevent invalid transitions — Phase 6
- [ ] TASK-012 Add status history — Phase 7

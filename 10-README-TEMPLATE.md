# README Template

Copy this structure into the actual `README.md` at the project root and
fill in every section — this maps directly to assessment rules #1–9, so
skipping a section is a direct, visible gap against the brief, not a minor
omission.

```markdown
# Customer Support Ticketing System

A Spring Boot REST API for creating, assigning, and managing customer
support tickets, with enforced status-transition rules and full audit
history.

## Tech Stack

- Java 21, Spring Boot 3.x (Web, Data JPA, Security, Validation)
- PostgreSQL 16
- Flyway (migrations)
- JWT (Spring Security)
- JUnit 5 + Mockito
- Docker & docker-compose

## Quick Start (Docker — recommended)

\`\`\`bash
git clone <repo-url>
cd <repo-dir>
docker-compose up --build
\`\`\`

API available at `http://localhost:8080`.

## Quick Start (local, without Docker)

\`\`\`bash
# 1. Start Postgres locally or via: docker run -e POSTGRES_DB=support_ticketing ...
# 2. Set environment variables (see .env.example)
# 3. Run:
./mvnw spring-boot:run
\`\`\`

## Restoring the database backup

A schema + demo-data backup is included at `db/backup.sql`.

\`\`\`bash
docker exec -i <postgres_container_name> psql -U app -d support_ticketing < db/backup.sql
\`\`\`

Demo accounts (if seeded):
| Role | Email | Password |
|---|---|---|
| Customer | customer@example.com | <password> |
| Agent | agent@example.com | <password> |

## Running tests

\`\`\`bash
./mvnw test
\`\`\`

Coverage report (if JaCoCo configured): `target/site/jacoco/index.html`.

## API Documentation

Full endpoint list: `docs/04-API-SPEC.md`. Summary:

| Method | Path | Purpose |
|---|---|---|
| POST | /api/auth/register | Register a user |
| POST | /api/auth/login | Log in, receive JWT |
| POST | /api/tickets | Create a ticket |
| GET | /api/tickets | List tickets (scoped by role) |
| GET | /api/tickets/{id} | Get a ticket |
| PATCH | /api/tickets/{id}/assign | Assign a ticket |
| PATCH | /api/tickets/{id}/status | Change ticket status |
| PATCH | /api/tickets/{id}/priority | Change ticket priority |
| POST | /api/tickets/{id}/comments | Add a comment |
| GET | /api/tickets/{id}/comments | List comments |
| GET | /api/tickets/{id}/history | View status/assignment history |

## Architecture & Design Patterns

<Summarize from `02-ARCHITECTURE.md`: layering, and the patterns used —
State Machine for transitions, centralized PermissionService, DTO+Mapper,
global exception handler — each with a one-line "why," not just a name
drop. Also briefly note what was deliberately *not* used and why (CQRS,
event sourcing, microservices) — this shows judgment, not just knowledge
of pattern names.>

## Data Model

<Summarize from `03-DATA-MODEL.md`, include the ERD and the status
transition diagram — Mermaid renders natively on GitHub.>

## Task Traceability

Every task from the original brief, mapped to where it's implemented:

| Task | Description | Implementation |
|---|---|---|
| TASK-001 | Ticket model | `domain/entity/Ticket.java` |
| TASK-002 | Create API | `controller/TicketController.java#createTicket` |
| TASK-003 | Category | `domain/enums/TicketCategory.java` |
| TASK-004 | Priority | `domain/enums/TicketPriority.java` |
| TASK-005 (US-001) | Validation | `dto/request/CreateTicketRequest.java` |
| TASK-005 (US-002) | Assign ticket | `controller/TicketController.java#assignTicket` |
| TASK-006 | Update status | `controller/TicketController.java#updateStatus` |
| TASK-007 | Update priority | `controller/TicketController.java#updatePriority` |
| TASK-008 | Add comments | `controller/CommentController.java` |
| TASK-009 | Validate permissions | `security/PermissionService.java` |
| TASK-010 | Define allowed transitions | `domain/statemachine/TicketStateMachine.java` |
| TASK-011 | Prevent invalid transitions | `domain/statemachine/TicketStateMachine.java` |
| TASK-012 | Add status history | `domain/entity/StatusHistory.java` |

*(Fill in actual file paths/method names as built — don't leave this
table pointing at planned-but-nonexistent paths.)*

## Assumptions & Decisions

<Pull from `11-DESIGN-DECISIONS-LOG.md` — every "decide and document" item
flagged across `01-REQUIREMENTS.md` and `05-SECURITY.md` must appear here.
Examples of the kind of thing that belongs in this section:
- Default priority is MEDIUM when omitted.
- An agent must self-assign a ticket before changing its status/priority
  (rather than any agent being able to act on any ticket).
- Customers may transition RESOLVED→CLOSED or RESOLVED→REOPENED on their
  own tickets; all other status transitions are agent-only.
- Registration is open with a role field rather than admin-gated, given
  assessment scope.
- JWT expiry set to <X> for ease of manual review.
>

## Bonuses Implemented

- ✅ **Database**: PostgreSQL, with backup at `db/backup.sql` (restore
  instructions above).
- ✅ **Docker**: `docker-compose up --build` brings up the full system.
- ✅ **Unit Tests**: JUnit 5 + Mockito, see `docs/07-TESTING-PLAN.md` for
  scope/rationale. Run via `./mvnw test`. Coverage: <fill in actual
  numbers for service/security/statemachine packages>.

## Audits

Three audits were performed after the build was complete:
- [Code Quality Audit](audits/CODE_QUALITY_AUDIT.md)
- [Security Audit](audits/SECURITY_AUDIT.md)
- [Performance Audit](audits/PERFORMANCE_AUDIT.md)

<One or two sentences summarizing the most notable finding from each,
even if minor — don't just link and say nothing.>

## Incomplete / Deviated Requirements

<Per assessment rule #9. If everything in the brief was completed, say so
explicitly: "All tasks in the brief (TASK-001 through TASK-012) were
completed as specified; see the Task Traceability table above." If
anything was scoped down, cut, or interpreted (like the US-002 TASK-005
numbering collision, or the "who can act on unassigned tickets" decision),
list it here plainly with the reasoning, even if it's already mentioned
elsewhere — this section is where a reviewer will look first for gaps, so
don't make them hunt through the rest of the README for it.>
\`\`\`

## Notes on filling this in

- Every `<...>` placeholder above must be replaced with real content
  reflecting what was actually built — a placeholder left in the final
  README is worse than a short honest answer.
- The Task Traceability table is the most important section for grading
  against the literal brief — get it right, and get it right *last*,
  after the code exists, so file paths are accurate rather than aspirational.
- Keep the Assumptions and Incomplete/Deviated sections honest and
  specific. Assessment rule #9 exists precisely so that a limitation,
  clearly stated, isn't penalized the way a silently-hidden gap would be.

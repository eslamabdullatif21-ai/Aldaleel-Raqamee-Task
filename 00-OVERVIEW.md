# Customer Support Ticketing System — Build Docs

This folder is the complete spec for building the Customer Support Ticketing
assessment. It's written so an AI coding agent (Codex) can execute it
end-to-end with minimal ambiguity. Read the docs in this order:

| # | File | Purpose |
|---|------|---------|
| 01 | `01-REQUIREMENTS.md` | The epic/stories/tasks rewritten as unambiguous acceptance criteria |
| 02 | `02-ARCHITECTURE.md` | Layering, design patterns used and why, package structure |
| 03 | `03-DATA-MODEL.md` | Entities, relationships, the status transition rule set, ERD |
| 04 | `04-API-SPEC.md` | Every REST endpoint: method, path, request/response, status codes |
| 05 | `05-SECURITY.md` | JWT auth flow, roles, permission matrix per endpoint |
| 06 | `06-BUILD-PLAN.md` | Task-by-task execution order, mapped to the original TASK IDs |
| 07 | `07-TESTING-PLAN.md` | What to unit test and to what depth (bonus requirement) |
| 08 | `08-DOCKER-DB.md` | docker-compose spec, DB backup/restore procedure (bonus requirements) |
| 09 | `09-AUDIT-CHECKLISTS.md` | Code quality, security, and performance audits — run **after** the build |
| 10 | `10-README-TEMPLATE.md` | Structure for the final submission README |
| 11 | `11-DESIGN-DECISIONS-LOG.md` | Living log — fill this in *while* building, not after |

## Ground rules that apply across every doc

- **Framework**: Spring Boot (Java). Non-negotiable per assessment rules.
- **Database**: PostgreSQL. A DB backup (`.sql` dump) must ship with the
  submission — see `08-DOCKER-DB.md`.
- **Docker**: `docker-compose.yml` bringing up the app + Postgres together.
- **Tests**: JUnit 5 + Mockito, targeting service-layer logic and the
  status-transition rules specifically (highest-value tests for this domain).
- **Auth**: Spring Security + JWT, two roles: `CUSTOMER` and `AGENT`.
- **Every original TASK-ID** (TASK-001 through TASK-012) must be traceable to
  a specific section of code and a specific commit. `06-BUILD-PLAN.md` is the
  authoritative mapping — don't lose this traceability, it's what makes the
  submission demonstrably complete against the brief.

## Execution order (high level)

1. Scaffold project (Spring Initializr equivalent: Web, JPA, Security,
   Validation, PostgreSQL driver, Lombok).
2. Data model + migrations (`03-DATA-MODEL.md`).
3. Security/JWT skeleton (`05-SECURITY.md`) — build this early since every
   endpoint depends on it, not last.
4. Domain logic + REST endpoints in TASK-ID order (`06-BUILD-PLAN.md`).
5. Unit tests alongside each service as it's built, not batched at the end.
6. Docker Compose + DB backup.
7. Run all three audits (`09-AUDIT-CHECKLISTS.md`) against the finished code.
8. Fill in `11-DESIGN-DECISIONS-LOG.md` retrospectively if anything was
   missed live, then write the README from `10-README-TEMPLATE.md`.

## What "done" looks like

- `docker-compose up` brings up a working system with one command.
- Every endpoint in `04-API-SPEC.md` exists and matches its documented
  contract.
- Every transition rule in `03-DATA-MODEL.md` is enforced and has a status
  history record.
- Every permission rule in `05-SECURITY.md` is enforced and tested.
- Three completed audit docs exist with findings and remediation notes (not
  just "audit passed" — see `09-AUDIT-CHECKLISTS.md` for what's expected).
- README follows `10-README-TEMPLATE.md` exactly, including an "Incomplete /
  Deviated Requirements" section — even if that section says "none."

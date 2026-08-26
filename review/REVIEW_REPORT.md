# Independent Reviewer-Style Test Report

**Review date:** 2026-08-25  
**Final score:** **94/100**
**Verdict:** Strong assessment submission. All required ticketing behavior passed live black-box testing after the defects found during review were corrected. PostgreSQL migration, data creation, `pg_dump`, clean restore, restored login, and restored API reads were also verified. Docker remains the main unexecuted delivery path.

## Executive Summary

The application was started as an HTTP service against a new isolated database and used through its public REST API with two customer accounts and two agent accounts. This was separate from the JUnit/Mockito test suite. The final live pass executed **63 checks and passed 63**. The Maven regression suite executed **39 tests and passed 39**. A clean Maven `verify` produced the executable JAR.

The live pass covered registration, login, invalid/tampered authentication, ticket creation and defaults, server-controlled ownership, pagination, combined filters, direct resource access, assignment/reassignment, priority changes, comments, every allowed transition edge, invalid and terminal transitions, customer resolution actions, append-only history, role/ownership isolation, validation bodies, and expected HTTP error codes.

## Evidence

| Verification | Result |
|---|---|
| Live black-box HTTP review | **63/63 passed** |
| JUnit/Mockito regression | **39/39 passed** |
| State-machine matrix | **25/25 status pairs tested**, plus explicit terminal-state test |
| Flyway | V1 migration validated and applied to an empty database |
| Hibernate | Schema validation passed with `ddl-auto=validate` |
| Clean build | `mvn clean verify` passed |
| Artifact | `target/support-ticketing-1.0.0.jar` produced successfully |
| Git whitespace check | Passed; only Windows line-ending notices |

The reproducible live harness is [e2e-review.ps1](e2e-review.ps1). It starts from no assumed data and creates its own review identities and tickets.

## Feature Coverage

### Authentication and errors

- Unauthenticated and tampered-token requests return 401.
- Customer and agent registration/login succeed; wrong password returns a clean 401.
- Duplicate email returns 409.
- Registration returns all field validation errors.
- Unknown routes return 404, unsupported methods return 405, and malformed UUIDs return 400.
- Error responses follow the shared schema without stack traces or internal details.

### US-001 - Customer ticket creation

- Customer creation returns 201 with server-generated ID and owner.
- A client-supplied `customerId` is ignored; authenticated identity controls ownership.
- Initial status is `OPEN`, assignment is null, and omitted priority becomes `MEDIUM`.
- Explicit category/priority values persist.
- Blank/missing fields, oversized values, and invalid enums return field-aware 400 responses.
- Agents cannot create customer tickets.

### US-002 - Agent ticket management

- Agent lists contain only tickets assigned to that agent.
- Any agent can self-assign or name another agent without changing status.
- Reassignment immediately transfers view/manage permission from the former agent to the new agent.
- Assignment to a customer is forbidden; an unknown target user returns 404.
- Only the assigned agent may update priority or agent-controlled status.
- Ticket customer and assigned agent can post/read comments; unrelated users cannot.
- Comments return chronologically with author identity and role.

### US-003 - Status transitions and history

- Live lifecycle exercised: `OPEN -> IN_PROGRESS -> OPEN -> IN_PROGRESS -> RESOLVED -> REOPENED -> IN_PROGRESS -> RESOLVED -> CLOSED`.
- The customer successfully performed both permitted resolution actions during the review lifecycle.
- `OPEN -> RESOLVED` returned 409 with current/target statuses.
- `CLOSED` rejected outgoing transitions.
- Rejected transitions did not append history.
- Creation, assignment, and status events formed one complete chronological timeline.

### Read/query behavior

- Customers list only their own tickets; agents list only assigned tickets.
- Pagination produced distinct pages and stable Spring DTO page metadata.
- Combined status, priority, and category filters returned the expected scoped result.
- Existing-but-forbidden resources returned 403; nonexistent tickets returned 404.

## Findings Corrected During Review

1. **Malformed UUID paths returned 500.** Added explicit type-mismatch handling; final result is 400.
2. **Unknown routes and unsupported HTTP methods returned 500 through the catch-all handler.** Added explicit 404 and 405 mappings.
3. **Spring warned that raw `PageImpl` JSON is unstable.** Enabled stable DTO page serialization and re-tested pagination.
4. **Invalid enum JSON returned a generic 400 without a field error.** Added enum-aware field errors naming the invalid field and allowed values.
5. **The PostgreSQL shorthand `TIMESTAMPTZ` prevented migration testing on H2 PostgreSQL mode.** Replaced it with equivalent `TIMESTAMP WITH TIME ZONE` in both Flyway and the backup; Flyway and Hibernate validation then passed.
6. **The initial Windows review harness did not read non-2xx bodies reliably.** Corrected response-stream parsing; this was a harness issue, not an API defect.

## Assumptions and Interpretations

- Omitted ticket priority defaults to `MEDIUM`.
- Tickets are created unassigned with status `OPEN`.
- Any agent may claim/reassign a ticket; all later agent operations require current assignment.
- A customer may perform `RESOLVED -> CLOSED` and `RESOLVED -> REOPENED` only on their own ticket.
- Creation is represented in history as `null -> OPEN`.
- Assignment and status events share one immutable chronological history endpoint.
- Registration accepts a caller-selected `CUSTOMER`/`AGENT` role for assessment usability. Production agent provisioning should be privileged.
- JWT expiry defaults to one hour and is environment-configurable.
- Comments are immutable because no update/delete contract was requested.
- Comment and history endpoints are intentionally unpaginated to preserve the supplied API response contract.
- The included SQL backup contains the schema but no seeded credentials or customer data.

## Environment Limitations

- The project and container remain targeted to **Java 21**, but the available review host had Java 17. Compilation/tests therefore used a temporary Maven `-Djava.version=17` override. No Java-17-only project change was committed.
- Docker was not installed, so `docker compose up --build` and the non-root container image could only be statically reviewed.
- PostgreSQL 16.9 was run directly. Flyway migrated a fresh database, demo data was created through the HTTP API, an actual `pg_dump` was restored into a second database, and restored authentication/ticket/comment/history access passed. Testcontainers CI would still make this repeatable across environments.

## Score Breakdown

| Area | Score | Reviewer rationale |
|---|---:|---|
| Functional completeness | 25/25 | All TASK-001 through TASK-012 behaviors passed live HTTP review. |
| Domain rules and permissions | 15/15 | Transition table, terminal state, assignment ownership, customer carve-outs, and audit atomicity behaved correctly. |
| API design and validation | 14/15 | Consistent errors, stable pagination, scoped filters, and field-aware validation; comment/history lists remain unbounded. |
| Security | 13/15 | JWT, BCrypt, ownership enforcement, safe errors, and environment secrets are solid; open agent self-registration and absent login throttling are production limitations. |
| Persistence | 10/10 | PostgreSQL 16.9, Flyway migration, schema validation, seeded data, actual `pg_dump`, clean restore, row counts, and restored API behavior were verified. |
| Automated testing | 8/10 | 39 unit tests plus a 63-check live harness provide strong risk-focused coverage; there is no checked-in Testcontainers full-stack suite and service line coverage is not uniformly high. |
| Code quality | 4/5 | Clear layering and centralized rules; `TicketService` has a broader responsibility surface than ideal and the user-details adapter/entity split could be simplified. |
| Documentation and delivery | 5/5 | README, assumptions, task traceability, Docker assets, SQL backup, decisions, audits, and this reproducible review are included. |
| **Total** | **94/100** | Strong and demonstrably complete, with remaining deductions concentrated in production hardening, repeatable Testcontainers coverage, and Docker runtime verification. |

## Recommended Next Steps

1. Add a Testcontainers PostgreSQL integration suite to CI and run the same lifecycle against PostgreSQL 16.
2. Move agent provisioning behind an administrator/identity provider and add login throttling at the gateway or shared rate limiter.
3. Paginate comments and history if the API contract can evolve.
4. Split query/timeline operations from `TicketService` if the domain grows.

# Design Decisions Log

## TASK-005 numbering collision

**Context**: TASK-005 appears under both US-001 (validation) and US-002 (assignment).

**Decision**: retain both labels and disambiguate them by user story.

**Rationale / trade-off**: exact traceability is worth the slightly awkward naming.

## Assigned-agent ownership

**Context**: the brief asks agents to manage tickets assigned to them.

**Decision**: any agent may claim/reassign; only the assigned agent may then view, update, comment, or view history.

**Rationale / trade-off**: this matches the story literally, at the cost of requiring an explicit claim before work.

## Unified append-only history

**Context**: status and assignment changes both require auditing.

**Decision**: store creation/status and assignment events in one table, distinguished by `eventType`.

**Rationale / trade-off**: one ordered timeline is easier to consume, though the table has nullable event-specific columns.

## Default priority

**Context**: creation priority is optional.

**Decision**: default to `MEDIUM`.

**Rationale / trade-off**: this is the neutral documented default; callers must explicitly choose LOW.

## Manual DTO mapping

**Context**: entities must not be exposed by REST endpoints.

**Decision**: use one explicit `TicketMapper` rather than MapStruct.

**Rationale / trade-off**: three mappings do not justify another annotation processor; new fields require manual updates.

## One-hour JWT expiry

**Context**: token lifetime balances security and reviewer convenience.

**Decision**: default to one hour, configurable through `JWT_EXPIRATION`.

**Rationale / trade-off**: reduces bearer-token exposure; a long review may require re-authentication.

## H2 test profile

**Context**: Docker/Testcontainers is unavailable on the verification host.

**Decision**: provide H2 PostgreSQL mode for lightweight contexts and use Mockito unit tests for business rules.

**Rationale / trade-off**: tests stay deterministic without infrastructure. PostgreSQL-specific migration, seeding, dump, and restore were verified separately on PostgreSQL 16.9; CI should automate the same check with Testcontainers.

## Exact enum vocabulary

**Context**: the documents permit adjusted categories/statuses only when justified.

**Decision**: retain every documented enum set unchanged.

**Rationale / trade-off**: no story requires more values; future vocabulary needs code and migration changes.

## Permission enforcement split

**Context**: role checks and ownership checks have different complexity.

**Decision**: use `@PreAuthorize` for coarse roles and `PermissionService` for resource rules.

**Rationale / trade-off**: policies remain readable without database-heavy SpEL; services make an explicit policy call.

## Accepted audit limitations

**Context**: comment/history pagination and login rate limiting are production concerns but not required by the API contract.

**Decision**: preserve chronological list responses and omit an in-process login limiter.

**Rationale / trade-off**: avoids changing the stated wire contract or adding a non-distributed limiter; production should add pagination and gateway/identity-provider throttling.

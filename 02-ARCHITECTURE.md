# Architecture & Design Patterns

This doc defines *how* the system is structured, and — critically, since the
user explicitly wants this documented — *why* each pattern is used. Don't
apply a pattern because it's a resume word; apply it because it solves a
concrete problem in this domain. Each section below states the problem
first, then the pattern.

## Layering

```
controller  → REST endpoints, request/response DTOs, no business logic
service     → business logic, transaction boundaries, orchestration
repository  → Spring Data JPA interfaces, no business logic
domain      → entities, enums, the state machine
security    → JWT filter, UserDetailsService, permission evaluation
exception   → custom exceptions + a global @ControllerAdvice handler
dto         → request/response shapes, kept separate from entities
mapper      → entity ↔ DTO conversion (MapStruct or manual, pick one and be
              consistent)
config      → SecurityConfig, OpenAPI config, etc.
```

Rule: controllers never touch repositories directly, and never contain `if`
statements deciding business outcomes. If a controller has branching logic
beyond "which DTO to return," that logic belongs in a service.

## Package structure

```
com.company.supportticketing
├── config/
├── controller/
├── domain/
│   ├── entity/
│   ├── enums/
│   └── statemachine/
├── dto/
│   ├── request/
│   └── response/
├── exception/
├── mapper/
├── repository/
├── security/
└── service/
    └── impl/
```

Package-by-layer (not package-by-feature) is the right call here given the
project's size — three user stories, ~a dozen tasks. Package-by-feature
earns its complexity at a larger scale; forcing it here would just be
indirection for its own sake. State this reasoning in
`11-DESIGN-DECISIONS-LOG.md` if asked to justify it later.

## Design patterns used, and why

### 1. State Machine (for status transitions — TASK-010/011)
**Problem**: transition validity rules (US-003) must live in exactly one
place, be easy to unit test in isolation, and be trivially extensible if a
new status is added later.
**Pattern**: a `TicketStateMachine` component holding the transition table
from `01-REQUIREMENTS.md` as a `Map<TicketStatus, Set<TicketStatus>>`,
exposing `boolean canTransition(from, to)` and `void validateTransition(from,
to)` (throws `InvalidTransitionException`). The service layer calls this
before persisting any status change. This is the single highest-value
pattern in the project because it's exactly what US-003 is asking for as a
first-class concern, not an incidental detail.

### 2. Strategy (for permission checks — TASK-009)
**Problem**: "who can do what to which ticket" varies per action
(create/assign/status/priority/comment) and per role, and the brief wants
this centralized rather than scattered as inline `if` checks in every
controller.
**Pattern**: a `PermissionService` with one method per action (e.g.
`canUpdateStatus(User actor, Ticket ticket)`), each implementing one row of
the permission matrix in `05-SECURITY.md`. This isn't a full Strategy-pattern
class hierarchy (that would be over-engineering for ~5 permission checks) —
it's a single service with clearly named methods, which is the pragmatic
version of the same idea. Document this as a deliberate choice: the pattern
name is Strategy in spirit, but the implementation is intentionally simple
rather than a `PermissionCheck` interface with five implementing classes.

### 3. Builder (via Lombok `@Builder`, for DTOs/entities with many optional
fields)
**Problem**: `Ticket` and response DTOs have several optional/nullable
fields (`assignedAgentId`, `priority` default, etc.); telescoping
constructors get unreadable fast.
**Pattern**: Lombok's `@Builder` on entities and response DTOs. Low
ceremony, standard in Spring Boot codebases, avoids hand-rolling boilerplate
that adds no value.

### 4. DTO + Mapper (separation of persistence model from wire model)
**Problem**: exposing JPA entities directly over REST couples the API
contract to the database schema and risks leaking fields (e.g. internal
audit columns) or causing lazy-loading serialization issues.
**Pattern**: every endpoint has a request DTO and response DTO, converted
via a mapper layer. Pick MapStruct if you want compile-time-generated
mapping (recommended — less error-prone than hand-written mappers as the
model grows) or hand-written mapper classes if you want to avoid an
annotation-processor dependency for a project this size. Either is
defensible; document which was chosen and why in
`11-DESIGN-DECISIONS-LOG.md`.

### 5. Global Exception Handler (`@ControllerAdvice`)
**Problem**: consistent error response shape across validation errors
(`400`), permission errors (`403`), state-conflict errors (`409`), and
not-found errors (`404`), without duplicating error-formatting logic in
every controller.
**Pattern**: one `@ControllerAdvice` class mapping custom exceptions
(`InvalidTransitionException`, `TicketNotFoundException`,
`PermissionDeniedException`, `MethodArgumentNotValidException`) to the
shared error schema in `04-API-SPEC.md`.

### 6. Repository (Spring Data JPA)
**Problem**: standard CRUD + a handful of derived queries (e.g. "tickets
assigned to agent X").
**Pattern**: `TicketRepository extends JpaRepository<Ticket, UUID>` with
derived query methods (`findByAssignedAgentId`, `findByCustomerId`). No
custom DAO layer needed — Spring Data covers this project's query
complexity without hand-rolled SQL, except possibly one `@Query` for a
combined filter (status + agent) if the "manage tickets assigned to me"
story implies filtering (see `04-API-SPEC.md`).

### 7. Filter Chain (Spring Security, for JWT — see `05-SECURITY.md`)
**Problem**: every request needs authentication resolved before it reaches
a controller, without each controller re-implementing token parsing.
**Pattern**: a custom `JwtAuthenticationFilter` in the standard Spring
Security filter chain, populating the `SecurityContext` so
`@PreAuthorize`/`Authentication` objects work normally downstream. This is
the idiomatic Spring approach, not a custom-rolled alternative.

## Patterns deliberately **not** used, and why not

Document these too — knowing what you left out, and why, is as much a sign
of engineering judgment as knowing what you included:

- **Full CQRS**: no read/write model split. The domain is small enough that
  a single model serves both; CQRS here would be complexity without payoff.
- **Event sourcing**: status history (TASK-012) *looks* event-sourcing-
  adjacent, but it's implemented as a straightforward append-only audit
  table, not a system where state is *derived* from replaying events. Full
  event sourcing would be substantial overkill for this scope.
- **Generic Repository/Specification pattern**: Spring Data's derived
  queries and, if needed, `JpaSpecificationExecutor` cover the filtering
  needs here without a hand-rolled specification abstraction on top.
- **Microservices**: single deployable Spring Boot app. Splitting this
  into services would add operational overhead (service discovery, network
  calls, distributed transactions) with no corresponding benefit at this
  scale — the entire domain is one bounded context.

## Transaction boundaries

Service methods that mutate state (create ticket, assign, status change,
priority change, add comment) are `@Transactional`. Status changes and their
corresponding `StatusHistory` insert happen in the *same* transaction — a
status change without a history record (or vice versa) is a data
consistency bug, not an acceptable partial success.

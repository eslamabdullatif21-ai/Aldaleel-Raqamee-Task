# Testing Plan (Unit Testing Bonus)

Goal: meaningful coverage of the parts of the system where correctness is
non-obvious, not 100% line coverage for its own sake. A support-ticket
system's real risk surface is the state machine and the permission rules —
test those exhaustively. CRUD plumbing (a repository `save()` call) needs
much less scrutiny.

## Tooling

- JUnit 5.
- Mockito (`@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`)
  for service-layer unit tests that isolate from Spring context.
- `@WebMvcTest` + `MockMvc` for controller-layer tests (request/response
  shape, status codes, validation wiring) without booting the full
  application context.
- `@SpringBootTest` + Testcontainers (Postgres) for a small number of true
  integration tests covering the full stack for at least the create-ticket
  and status-transition flows end to end. If Testcontainers is too heavy
  for the time budget, `@SpringBootTest` against the `test` profile's H2/
  embedded config is an acceptable fallback — document the choice.

## Priority order (highest value first — do these even if time runs short)

1. **`TicketStateMachineTest`** — every (from, to) pair in the transition
   table from `01-REQUIREMENTS.md`, both the allowed ones (assert success)
   and a representative sample of disallowed ones (assert
   `InvalidTransitionException`), plus an explicit "nothing transitions out
   of CLOSED" test. This is the single most important test class in the
   project — US-003 exists specifically to be enforced, and this is where
   that enforcement is proven.
2. **`PermissionServiceTest`** — one test per row of the permission matrix
   in `05-SECURITY.md`. Parameterize with `@ParameterizedTest` where the
   pattern repeats (e.g. "customer, other ticket, any action → denied")
   rather than writing near-duplicate test methods.
3. **`TicketServiceTest`** (Mockito, repository mocked) — creation
   defaults (`status=OPEN`, `priority=MEDIUM` if omitted), assignment
   writes the correct history event, status change writes the correct
   history event and calls the state machine, priority change rejects
   non-agents.
4. **Controller tests** (`@WebMvcTest`) — for each endpoint: happy path
   returns the documented status code and body shape; each documented
   error case (`400`/`401`/`403`/`404`/`409`) is actually produced.
5. **`JwtServiceTest`** — valid token issues and validates correctly;
   expired token is rejected; tampered/malformed token is rejected.
6. A handful of **integration tests** (`@SpringBootTest`) covering: full
   ticket lifecycle (create → assign → IN_PROGRESS → RESOLVED → CLOSED)
   as one flow, and one negative flow (attempt an invalid transition,
   confirm `409` and confirm no `StatusHistory` row was written for the
   rejected attempt — i.e. the transaction rolled back cleanly).

## What NOT to over-invest in

- Trivial getter/setter/Lombok-generated code — no test value.
- Repository interfaces with only Spring Data derived queries — Spring
  Data itself is well-tested; testing that `findByAssignedAgentId` "works"
  is testing the framework, not your code. (A custom `@Query` is different
  — that's your logic, and worth a test.)
- DTO/mapper classes with pure 1:1 field copying — cover these implicitly
  via the controller/service tests that exercise them, rather than writing
  standalone mapper tests with no independent value.

## Coverage target

Aim for meaningful coverage of `service/`, `security/`, and
`domain/statemachine/` packages (rough guideline: 80%+ line coverage in
those packages specifically) rather than chasing an aggregate project-wide
number that gets padded by trivial classes. If a coverage tool (JaCoCo) is
added, configure it to report per-package so this distinction is visible,
and mention the actual achieved numbers in the README rather than only the
target.

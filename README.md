# Customer Support Ticketing System

A Spring Boot REST API for creating, assigning, and managing support tickets, with ownership-based authorization, enforced status transitions, comments, and an append-only audit history.

## Tech Stack

- Java 21 and Spring Boot 3.5.5 (Web, Data JPA, Security, Validation)
- PostgreSQL 16 and Flyway migrations
- JWT (HS256) and BCrypt password hashing
- JUnit 5, Mockito, H2 test profile, and JaCoCo
- Docker and Docker Compose

## Quick Start (Docker - recommended)

```bash
git clone <repository-url>
cd support-ticketing
copy .env.example .env   # Windows; use: cp .env.example .env on macOS/Linux
# Replace the example secrets in .env
docker compose up --build
```

The API is available at `http://localhost:8080`. Flyway creates the schema automatically. Docker image builds skip tests because the verified test phase is separate.

## Quick Start (local)

Prerequisites: JDK 21, Maven 3.9+, and PostgreSQL 16.

```bash
# Create database support_ticketing and user app, then set:
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/support_ticketing
export SPRING_DATASOURCE_USERNAME=app
export SPRING_DATASOURCE_PASSWORD=your-password
export JWT_SECRET=a-random-secret-containing-at-least-32-bytes
mvn spring-boot:run
```

PowerShell uses `$env:VARIABLE_NAME='value'` instead of `export`.

## Restoring the Database Backup

A PostgreSQL schema backup is included at `db/backup.sql`. Flyway is the normal source of truth; the backup is the assessment artifact.

```bash
docker exec -i support-ticketing-postgres-1 psql -U app -d support_ticketing < db/backup.sql
```

No credentials are seeded. Register one `CUSTOMER` and one `AGENT` through `/api/auth/register`.

## Running Tests

```bash
mvn test
```

The implemented suite has 39 passing tests. JaCoCo output is generated at `target/site/jacoco/index.html`; measured line coverage is 100% for `TicketStateMachine`, 81% for `PermissionService`, 100% for `JwtService`, and 56% for `TicketService`. The tests prioritize transition and permission rules over trivial persistence plumbing.

## API Documentation

The full contract is [04-API-SPEC.md](04-API-SPEC.md). All endpoints except registration and login require `Authorization: Bearer <token>`.

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/auth/register` | Register a customer or agent |
| POST | `/api/auth/login` | Receive a JWT |
| POST | `/api/tickets` | Create a customer ticket |
| GET | `/api/tickets` | Paginated, role-scoped ticket list with filters |
| GET | `/api/tickets/{id}` | View an accessible ticket |
| PATCH | `/api/tickets/{id}/assign` | Assign or reassign to an agent |
| PATCH | `/api/tickets/{id}/status` | Apply a valid status transition |
| PATCH | `/api/tickets/{id}/priority` | Change priority |
| POST/GET | `/api/tickets/{id}/comments` | Add/list immutable comments |
| GET | `/api/tickets/{id}/history` | List creation, assignment, and status events |

## Architecture and Design Patterns

Controllers only handle HTTP mapping; services own transactions and orchestration; repositories isolate JPA; DTOs prevent persistence entities leaking over the wire. `TicketStateMachine` is the one transition-rule source. `PermissionService` is a deliberately small strategy-style policy component rather than a class hierarchy. A manual mapper keeps the mapping dependency surface small, and `@RestControllerAdvice` guarantees one error format.

CQRS, event sourcing, microservices, and a custom repository abstraction were intentionally excluded: this is one small bounded context, and each would add complexity without solving a requirement. History is an append-only audit log; current ticket state is not reconstructed from events.

```mermaid
erDiagram
    USER ||--o{ TICKET : creates
    USER ||--o{ TICKET : handles
    USER ||--o{ COMMENT : writes
    USER ||--o{ STATUS_HISTORY : triggers
    TICKET ||--o{ COMMENT : has
    TICKET ||--o{ STATUS_HISTORY : has
```

```mermaid
stateDiagram-v2
    [*] --> OPEN
    OPEN --> IN_PROGRESS
    IN_PROGRESS --> RESOLVED
    IN_PROGRESS --> OPEN
    RESOLVED --> CLOSED
    RESOLVED --> REOPENED
    REOPENED --> IN_PROGRESS
    CLOSED --> [*]
```

## Task Traceability

| Task | Implementation |
|---|---|
| TASK-001 Ticket model | `domain/entity/Ticket.java` |
| TASK-002 Create API | `TicketController#create`, `TicketService#create` |
| TASK-003 Category | `domain/enums/TicketCategory.java` |
| TASK-004 Priority | `domain/enums/TicketPriority.java` |
| TASK-005 (US-001) Validation | `dto/request/CreateTicketRequest.java`, `GlobalExceptionHandler` |
| TASK-005 (US-002) Assignment | `TicketController#assign`, `TicketService#assign` |
| TASK-006 Status update | `TicketController#status`, `TicketService#updateStatus` |
| TASK-007 Priority update | `TicketController#priority`, `TicketService#updatePriority` |
| TASK-008 Comments | `CommentController`, `TicketService#addComment/comments` |
| TASK-009 Permissions | `security/PermissionService.java`, method security |
| TASK-010 Allowed transitions | `domain/statemachine/TicketStateMachine.java` |
| TASK-011 Invalid-transition prevention | `TicketStateMachine#validateTransition`, HTTP 409 handler |
| TASK-012 Status history | `domain/entity/StatusHistory.java`, `TicketService#history` |

## Assumptions and Decisions

- Omitted creation priority defaults to `MEDIUM`; status always starts at `OPEN` and assignment starts null.
- Any agent may claim or reassign a ticket. Status, priority, viewing, comments, and history then require that agent to be assigned.
- A customer may change their own `RESOLVED` ticket to `CLOSED` or `REOPENED`; all other customer status changes are forbidden.
- Registration accepts a role for assessment usability. Agent provisioning would be admin-controlled in production.
- JWTs expire after one hour by default. The signing secret must be at least 32 bytes and comes from the environment.
- Assignment and status events share one immutable timeline. Creation records `null -> OPEN`.
- Comment/history list endpoints are chronological but unpaginated because the supplied API contract calls for lists; pagination is the documented scaling follow-up.
- H2 is used only for lightweight test configuration; unit tests isolate domain logic with Mockito. Production remains PostgreSQL-only.

## Bonuses Implemented

- Database: PostgreSQL with Flyway and `db/backup.sql`.
- Docker: multi-stage non-root image plus app/database Compose stack.
- Unit tests: 39 tests with JaCoCo reporting.

## Audits

- [Code quality audit](audits/CODE_QUALITY_AUDIT.md): layering and single-source rules passed; test coverage is intentionally risk-weighted rather than uniform.
- [Security audit](audits/SECURITY_AUDIT.md): authorization and secret handling passed; login rate limiting is an accepted out-of-scope production hardening item.
- [Performance audit](audits/PERFORMANCE_AUDIT.md): ticket listing is paginated/indexed; unpaginated comments/history and unavailable Docker runtime measurements are documented limitations.

## Incomplete / Deviated Requirements

All feature tasks TASK-001 through TASK-012 are implemented. Docker is not installed in the build environment, so the Compose stack and PostgreSQL restore could not be executed locally; their definitions were statically cross-checked. The installed JDK was Java 17, so the passing build was additionally run with a temporary `-Djava.version=17` verification override while the committed project and Docker image remain correctly targeted to Java 21.

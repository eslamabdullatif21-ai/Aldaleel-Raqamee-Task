# Assumptions, Decisions, and Tradeoffs

This is the consolidated record of every material interpretation and engineering
choice in the submitted implementation. It reflects the finished code rather
than the earlier planning documents archived under `not task/`.

## Scope and Requirement Interpretation

| Topic | Implemented decision | Reason and tradeoff |
|---|---|---|
| Framework | Java 21 with Spring Boot 3.5.5; no alternative application framework. | Required by the assessment. The host JDK is Java 17, so host tests use `-Djava.version=17`; the built container was verified on Temurin Java 21.0.12. |
| TASK-005 collision | Both TASK-005 items are implemented and identified as TASK-005 (US-001 validation) and TASK-005 (US-002 assignment). | Preserves traceability despite duplicate numbering in the brief. |
| Application boundary | One Spring Boot application and one PostgreSQL database. | The stories form one small bounded context. Microservices would introduce networking and distributed consistency without helping a requirement. |
| Supporting authentication | Registration and login endpoints were added even though they are not named tasks. | Authenticated customer/agent behavior cannot be exercised without identities. This adds only the minimum supporting capability. |
| Roles | Only `CUSTOMER` and `AGENT`; no administrator role. | No admin workflow was requested. Open agent registration is convenient for assessment review but would require privileged provisioning in production. |
| Added operational protections | Pagination, login throttling, JWT principal caching, optimistic locking, and bounded runtime settings are included. | They make the single instance safer without changing the ticketing domain. Their limits and single-instance implications are documented below. |

## Ticket and Workflow Rules

| Topic | Implemented decision | Reason and tradeoff |
|---|---|---|
| Identifiers | Users, tickets, comments, and history events use UUIDs. | Avoids guessable sequential IDs and works well across future deployments, at the cost of larger indexes than integer IDs. |
| Categories | `TECHNICAL`, `BILLING`, `ACCOUNT`, `GENERAL`. | Uses a constrained vocabulary instead of free text. Adding values requires a code and database-constraint migration. |
| Priorities | `LOW`, `MEDIUM`, `HIGH`, `URGENT`; omitted creation priority becomes `MEDIUM`. | `MEDIUM` is the neutral default. Customers cannot subsequently reprioritize tickets. |
| Initial ticket state | A ticket always starts `OPEN` and unassigned. The authenticated customer, not request data, becomes the owner. | Prevents forged ownership and keeps assignment/status changes explicit. |
| Input normalization | Ticket title, description, comment body, registration name, and registration email are trimmed; registration email is lowercased. | Produces consistent stored values. Passwords are intentionally not trimmed because whitespace may be intentional. |
| Assignment | Any agent may claim or reassign any ticket. Omitting `agentId` self-assigns. Assignment does not change status. | Supports team routing with a compact API. It also means assignment permission is broader than later management permission. |
| Assigned-agent boundary | Only the currently assigned agent may view, update status/priority, comment, or read history. | Directly follows “tickets assigned to me.” Agents must claim a ticket before working it and lose access immediately after reassignment. |
| Customer boundary | Customers may view, comment on, and read history for their own tickets only. | Keeps tenant ownership explicit. Another authenticated user receives `403`, while a nonexistent ticket receives `404`. |
| Customer status actions | The owner may perform `RESOLVED -> CLOSED` or `RESOLVED -> REOPENED`; no other customer status change is allowed. | Lets the customer confirm or reject a resolution without granting general workflow control. |
| Agent status actions | The assigned agent may use any transition allowed by the central state machine. | Role permission alone is insufficient; assignment and transition validity are both checked. |
| Allowed transitions | `OPEN -> IN_PROGRESS`; `IN_PROGRESS -> OPEN` or `RESOLVED`; `RESOLVED -> CLOSED` or `REOPENED`; `REOPENED -> IN_PROGRESS`. | This is the single implemented transition graph. A well-formed but invalid transition returns `409 Conflict`. |
| Terminal state | `CLOSED` has no outgoing transition. | Reopening is deliberately available from `RESOLVED`, not from a confirmed closed state. |
| Comments | Comments are trimmed, limited to 5,000 characters, chronological, and immutable; there are no edit/delete endpoints. | Preserves an audit-friendly conversation and stays within TASK-008. Corrections require a new comment. |
| Status notes | Status-change notes are optional and limited to 1,000 characters. | Gives audit context without allowing unbounded history payloads. |
| Priority audit | Priority changes update the ticket but do not create a history event. | TASK-012 requires status history; assignment was deliberately added to the same timeline, but expanding it into a universal change log was outside scope. |

## History Model

| Topic | Implemented decision | Reason and tradeoff |
|---|---|---|
| Unified timeline | Creation/status and assignment events share `status_history`, distinguished by `STATUS_CHANGE` and `ASSIGNMENT`. | One endpoint returns the complete operational timeline. Event-specific columns are necessarily nullable. |
| Creation event | Creation records `fromStatus=null`, `toStatus=OPEN`, with note `Ticket created`. | Makes the beginning of the lifecycle visible without inventing a pre-creation status. |
| Assignment event | Records previous/new agent IDs and does not pretend assignment is a status transition. | Preserves accurate semantics while satisfying assignment auditability. |
| Append-only behavior | No update/delete repository API or HTTP endpoint exists for history. | Supports TASK-012 audit integrity. Database administrators can still alter data; cryptographic/tamper-evident auditing was not requested. |
| Atomicity | Ticket mutations and their associated history insert use the same transaction. | Prevents a committed status/assignment change without its audit event. |

## API and Validation

| Topic | Implemented decision | Reason and tradeoff |
|---|---|---|
| Wire model | Controllers accept/return DTOs, never JPA entities. A manual mapper performs conversions. | Prevents persistence-field leakage and lazy serialization. Manual mapping avoids another annotation processor but must be updated when fields change. |
| Validation limits | Title 200; description/comment 5,000; status note 1,000; email 320; name 120; password 8-72 on registration and maximum 72 on login. | Matches database/storage constraints and BCrypt's useful input bound. |
| Enum errors | Invalid enum JSON returns a field error listing accepted values. | More reviewer/client-friendly than a generic malformed-body response. |
| Error contract | Expected failures use one JSON schema. Validation/malformed input is `400`, authentication `401`, permission `403`, absence `404`, state/duplicate conflict `409`, throttling `429`, unsupported method `405`, and unexpected errors `500`. | Separates malformed input from state conflict and avoids leaking stack traces or SQL details. |
| Ticket listing | Default page size 20, maximum 100, default sort `createdAt` descending; customer scope is ownership and agent scope is current assignment. Status, priority, and category equality filters can be combined. | Bounded pages protect memory. Offset pagination is simpler than cursors but becomes less stable/efficient for very deep datasets. |
| Comment/history listing | Default page size 20, maximum 100; server forces ascending timestamp then ID. Client sort is ignored. | Guarantees stable chronological audit pages, trading away caller-defined ordering. |
| Page representation | Spring Data pages use Spring's stable DTO serialization mode. | Avoids relying on unstable `PageImpl` JSON internals. |
| Null response fields | Jackson omits null properties. | Keeps event/ticket responses compact; clients must treat absent optional fields as null. |

## Persistence and Data

| Topic | Implemented decision | Reason and tradeoff |
|---|---|---|
| Production database | PostgreSQL 16 with Flyway. | SQL is optional in the brief but earns the database bonus and supplies transactions, constraints, and indexing. |
| Schema authority | `V1__init_schema.sql` creates the schema; Hibernate uses `ddl-auto=validate`. | Startup detects drift rather than silently changing production tables. Schema changes require explicit migrations. |
| Database backup | `db/backup.sql` is a PostgreSQL 16.9 plain SQL schema-and-data dump. It contains 2 users, 2 tickets, 3 comments, 7 history events, and Flyway history. | Satisfies the SQL backup requirement and gives reviewers demo data. Demo credentials are intentionally non-production. |
| Time handling | Entity timestamps use `Instant`; Hibernate JDBC timezone is UTC. | Avoids server-local timezone ambiguity. Clients receive ISO-8601 instants. |
| Concurrency | `Ticket.version` uses optimistic locking. | Prevents silent lost updates without holding long database locks; a concurrent collision may surface as a conflict/error and requires client retry. |
| Fetching | All `ManyToOne` relations are lazy and Open Session in View is disabled. | Prevents accidental N+1/lazy serialization and long-lived persistence contexts; mappings must occur inside service transactions. |
| Indexes | Unique email; ticket customer; ticket status; agent/status; comment ticket/time; history ticket/time. | Matches login, scoped listing/filtering, and chronological timeline reads, with additional write/index storage cost. |
| Referential behavior | Foreign keys enforce references; no cascade-delete workflow or ticket/user deletion endpoint exists. | Avoids accidental audit loss. Data-retention/deletion policy is intentionally outside scope. |
| Demo seeding | Fresh deployments start with migrations only; the supplied dump contains demo data. | Keeps normal startup deterministic and avoids production demo accounts, while restore remains a separate reviewer action. |

## Authentication and Security

| Topic | Implemented decision | Reason and tradeoff |
|---|---|---|
| Authentication | Stateless Spring Security bearer JWT; server sessions are disabled and CSRF is disabled. | CSRF protection is unnecessary for a non-cookie bearer API. Token theft remains possible, so HTTPS is required in deployment. |
| JWT signing | HMAC signing through JJWT with an environment secret of at least 32 bytes. No secret is committed. | Symmetric signing is small and sufficient for one service. Multiple independent issuers/verifiers would favor asymmetric keys. |
| JWT contents | Signed user ID, email, and role; default expiry one hour. | Lets the request filter authenticate efficiently. Tokens issued by the older format lack `uid` and require one re-login. |
| JWT parsing | Each request parses/verifies the token exactly once. | Removes duplicate signature work while still rejecting tampered, expired, malformed, or incomplete tokens. |
| Principal cache | A password-free user snapshot is cached by email for one minute by default. | Removes the database user lookup from most JWT requests. Role/email/deletion changes can remain stale for at most the TTL; after refresh, claim mismatch rejects the old token and requires login. |
| Password changes | Password login always loads current PostgreSQL state; passwords/hashes are never stored in the JWT cache. Existing JWTs are not individually revoked by a password change. | Keeps login correctness and avoids sensitive cache data. Immediate token revocation would require a token version/revocation store, which is outside scope. |
| Password storage | BCrypt; passwords never appear in response DTOs or errors. | Deliberately slow password hashing protects stored credentials, with CPU cost during login. |
| Login rate limit | Fixed window, default 10 login attempts per observed remote address per minute; all attempts, including successful ones, count. `429` includes `Retry-After`. | Small, dependency-free single-instance protection. NAT users share a bucket, process restart clears it, and proxy deployments must provide a trusted client-address strategy. |
| Authorization split | `@PreAuthorize` handles coarse role rules; `PermissionService` handles ticket ownership/assignment. | Avoids database-heavy SpEL and keeps resource policy readable, at the cost of explicit checks in service methods. |
| CORS | Explicit environment-controlled origins, only GET/POST/PATCH/OPTIONS and Authorization/Content-Type; credentials disabled. | Avoids wildcard credential exposure. New clients/methods require configuration changes. |
| Public role registration | Registration accepts `CUSTOMER` or `AGENT`. | Makes the assessment self-contained. Production must gate agent provisioning through administration or an identity provider. |
| Missing auth features | No refresh token, logout blacklist, MFA, password reset, account disable flag, or email verification. | None is required by the epic; adding them would expand the identity domain substantially. |

## Architecture and Code Structure

| Topic | Implemented decision | Reason and tradeoff |
|---|---|---|
| Organization | Package-by-layer: controller, service, repository, domain, DTO, mapper, security, exception, config. | Clear for this project size. Package-by-feature becomes more attractive if the bounded context grows. |
| Controllers | HTTP mapping and coarse annotations only; repositories are never called directly. | Keeps business and transaction rules testable in services. |
| State machine | One `TicketStateMachine` map is the sole transition source. | Exhaustive, isolated testing is simple. Adding a status requires updating code, database constraints, and tests. |
| Permission policy | One small `PermissionService`, strategy-like without an interface/implementation hierarchy. | Centralizes policy without five tiny strategy classes. It may be split if actions/roles multiply. |
| Repositories | Spring Data JPA plus `JpaSpecificationExecutor` for combined scoped filters. | Avoids custom DAO boilerplate; complex reporting could eventually justify dedicated queries/read models. |
| Mapping | Explicit `TicketMapper`; no MapStruct. | Fewer dependencies for a small model, with manual maintenance as the tradeoff. |
| Errors | One `@RestControllerAdvice` plus JSON Spring Security entry/access handlers. | Consistent API errors without controller try/catch blocks. |
| Boilerplate | Lombok supplies constructors/builders/getters/setters. | Keeps domain code compact but requires Lombok-aware tooling. |
| Transactions | Mutations are transactional; reads are marked `readOnly=true`. | Keeps write/history atomic and minimizes transaction intent ambiguity. |
| Deliberately omitted patterns | No CQRS, event sourcing, microservices, generic repository wrapper, or custom strategy hierarchy. | Each adds indirection or operations not justified by three user stories. History is an audit log, not the source from which ticket state is rebuilt. |
| API tooling | No Swagger/OpenAPI dependency. | Not requested; README provides the endpoint contract without expanding dependencies. |

## Single-Instance Performance Decisions

| Topic | Implemented decision | Reason and tradeoff |
|---|---|---|
| Deployment target | Optimized for one application instance, not horizontal scaling. | The in-memory login limiter and JWT user cache are intentionally process-local. Multiple instances require shared/gateway equivalents. |
| Database pool | Fixed Hikari pool of 16 with 5,000 ms acquisition timeout. | Bounds database concurrency and keeps connections warm. A larger pool can increase database contention; production sizing must be measured. |
| Servlet connector | Tomcat max connections 12,000, accept queue 2,000, keep-alive timeout 20 seconds, platform-thread maximum 300/minimum spare 30. | Absorbs larger bursts than defaults. Queuing can convert refused connections into longer latency, and OS socket limits still apply. |
| Java concurrency | Java 21 virtual threads enabled by default with application keep-alive. | Reduces the cost of blocking servlet/JPA work. The Docker load tests exercised the actual Java 21 runtime. |
| JVM memory | Example/Compose profile uses `-Xms1g -Xmx2g`. | Gives the single process an explicit capacity envelope. Hosts/containers need enough memory and may require lower values in small environments. |
| Collection bounds | Every growing collection endpoint is paginated and capped at 100. | Prevents unbounded memory and payload growth. Clients must request subsequent pages. |
| Active k6 profile | `review/k6-boundary.js` immediately starts constant VUs for 10 seconds, with one request per VU followed by a one-second think time. | Retained because it is the more demanding profile and has repeated measurements. It intentionally creates an unrealistic connection storm and is a saturation test, not normal user behavior. |
| Aggressive k6 results | On the internal Docker network: 1,000 VUs produced 10,001 requests, 0% failures, 951 requests/s, and 118.51 ms successful p95; 5,000 produced 47,765, 0%, 4,318 requests/s, and 519.4 ms; 10,000 produced 18,917, 33.96%, 1,238 requests/s, and 6.3 s. | The 1,000 and 5,000 tiers completed cleanly. The immediate 10,000-user storm crossed the single-instance saturation point and caused ten-second request timeouts, but the app had no restart/OOM and recovered immediately. |
| Gentle-profile comparison | The archived ramped profile gradually added users and used ten-second think time. It reached only about 68/342/685 average requests per second at 1,000/5,000/10,000 VUs and returned zero failures. | Those results describe many mostly-idle users, not the aggressive profile's roughly 961-975/4,541-4,548/4,324-6,980 attempted requests per second. The two profiles must not be presented as equivalent capacity evidence. |
| Load-test validity | Containerized k6, Java, and PostgreSQL shared one WSL 2 host; tests were short and read one two-ticket page. | Results are comparative local evidence only. Production sizing requires a separate load generator, longer steady states, realistic user mixes, and explicit SLOs. A Windows-host k6 run also showed that localhost-to-WSL forwarding can become the bottleneck, so reported Docker results use the internal network. |

## Testing and Verification Decisions

| Topic | Implemented decision | Reason and tradeoff |
|---|---|---|
| Unit-test focus | 55 JUnit 5/Mockito tests emphasize all 25 transition pairs, permission rules, service orchestration, JWT/cache/filter behavior, error mapping, pagination, and throttling. The suite passed both with the host Java 17 compatibility override and in a Temurin Java 21 Maven container. | Risk-weighted coverage provides more value than testing Lombok or trivial repository methods. Service coverage is not uniformly high. |
| Test database | H2 PostgreSQL mode for lightweight test configuration; Mockito isolates unit logic. | Fast and infrastructure-independent, but it cannot prove every PostgreSQL behavior. |
| PostgreSQL verification | PostgreSQL 16.9 migrations, Hibernate validation, demo creation, dump, clean restore, restored authentication, and API reads were exercised manually/automatically during review. | Strong local evidence, but not a checked-in Testcontainers integration suite. |
| Black-box review | A 64-check HTTP harness passed during review and is archived under `not task/review`. | It is useful evidence but not needed in the compact assessment submission; unit tests remain active submission tests. |
| Coverage | JaCoCo generates a report but does not enforce a global percentage gate. | Avoids gaming aggregate coverage with trivial DTO/entity tests. CI could introduce risk-based package gates later. |
| Docker build tests | Docker build uses `-DskipTests`; tests are run separately before image creation. | Speeds repeat image builds. CI must preserve the separate test step. |
| Runtime verification | Docker Engine 29.1.3 and Compose 2.40.3 built and ran the submitted stack in WSL 2. PostgreSQL 16.15 restored the dump, Flyway/Hibernate validated it, the Java 21 application passed all 64 HTTP checks, and the Maven test container passed all 55 tests on Java 21. | Docker Desktop itself was broken on the host, so an isolated Ubuntu WSL Docker Engine was used without deleting existing Docker data. Host Maven tests still use the Java 17 override. |

## Delivery and Operational Decisions

| Topic | Implemented decision | Reason and tradeoff |
|---|---|---|
| Docker | Multi-stage Java 21 image, non-root runtime user, PostgreSQL health dependency, and named volume. | Satisfies the Docker bonus and gives one-command startup. The image is larger/slower to build than distributing a bare JAR. |
| Configuration | Secrets and deployment values come from environment variables; `.env.example` contains placeholders/default tuning. | Keeps real secrets out of Git. Compose's development defaults must be replaced before deployment. |
| Backup vs migrations | Flyway starts a fresh database; `db/backup.sql` is a separate optional restore containing demo data. | Avoids coupling normal startup to a dump while satisfying the backup requirement. |
| Git history | The complete domain/API implementation precedes pagination/rate limiting, JWT/runtime performance improvements, and post-improvement k6 evidence. Work is prepared on `feature/support-ticketing-api` from `development`. | Matches the supplied feature-branch rules and makes the assessment evolution reviewable. The GitHub remote is configured but intentionally not pushed yet. |
| Commit convention | Assessment `TASK-*` identifiers are used where they map to functional work; conventional `feat`, `fix`, `test`, `perf`, `docs`, and `chore` prefixes cover supporting work. | The supplied rules refer to a HIS module and Azure task ID, but neither exists in this standalone brief. The user explicitly waived the rules file's 300-line limit; commits were still split by concern. |
| Archived material | Planning, audits, alternate load test, backup helper/manifest, scored review, and black-box harness live under `not task/`. | Keeps the evaluator-facing root compact without deleting review evidence. |

## Deliberately Out of Scope

The following were not requested and were intentionally not built: UI, email or
push notifications, attachments, SLA/escalation rules, public unassigned-ticket
queue, full-text search, tags, bulk operations, ticket/comment/history deletion,
comment editing, comprehensive field-level change audit, administrator portal,
agent provisioning workflow, refresh tokens, token blacklist, MFA, password
reset, email verification, shared/distributed rate limiting or caching,
multi-instance deployment, message queues, CQRS, and event sourcing.

## Incomplete or Environment-Limited Items

- All functional tasks TASK-001 through TASK-012 are implemented.
- GitHub upload is intentionally pending. The supplied remote is configured and
  the feature branch is prepared locally, but no push was made during preparation.
- The standard Docker Desktop installation on the review host was incomplete.
  Runtime verification therefore used an isolated Ubuntu WSL 2 Docker Engine;
  this tests the same Linux images and Compose file without altering old Docker data.
- A repeatable PostgreSQL Testcontainers suite is not included. PostgreSQL 16.9
  was nevertheless migrated, backed up, restored, queried, and load-tested
  directly during review.

# Requirements — Traceable Acceptance Criteria

Source: EPIC "Customer Support Ticketing", 3 user stories, 12 tasks. This doc
turns the short task names from the brief into concrete, testable
acceptance criteria. Nothing here should require Codex to guess intent.

---

## US-001 — As a customer, I want to create a support ticket

### TASK-001 — Ticket model
- A `Ticket` entity exists with at minimum: `id`, `title`, `description`,
  `category`, `priority`, `status`, `customerId`, `assignedAgentId`
  (nullable), `createdAt`, `updatedAt`.
- `status` defaults to `OPEN` on creation and cannot be set by the client at
  creation time (server-controlled).
- `assignedAgentId` is null on creation — assignment happens via US-002.

### TASK-002 — Create API
- `POST /api/tickets` creates a ticket for the authenticated customer.
- Request body: `title`, `description`, `category`, `priority`.
- Response: `201 Created` with the full ticket representation, including
  server-assigned `id`, `status=OPEN`, `createdAt`.
- Only users with role `CUSTOMER` may call this endpoint (see
  `05-SECURITY.md`).
- The `customerId` on the created ticket is taken from the authenticated
  principal, never from the request body — a customer cannot create a
  ticket on behalf of someone else.

### TASK-003 — Category
- `category` is a constrained enum, not free text. Suggested set (adjust if
  the assessment context implies otherwise, but document the choice):
  `TECHNICAL`, `BILLING`, `ACCOUNT`, `GENERAL`.
- Invalid/missing category on create → `400 Bad Request` with a field-level
  error message.

### TASK-004 — Priority
- `priority` is a constrained enum: `LOW`, `MEDIUM`, `HIGH`, `URGENT`.
- Default priority if omitted: `MEDIUM` (document this default explicitly
  in the README — it's an assumption, not a spec'd requirement).
- Priority can be changed later only via US-002 / TASK-007, not by editing
  the ticket directly post-creation.

### TASK-005 (US-001) — Validation
- Bean Validation (`jakarta.validation`) on the create DTO:
  - `title`: required, non-blank, max length (e.g. 200 chars).
  - `description`: required, non-blank, max length (e.g. 5000 chars).
  - `category`: required, must be a valid enum value.
  - `priority`: optional, must be a valid enum value if present.
- Validation failures return `400 Bad Request` with a consistent error body
  (see `04-API-SPEC.md` for the shared error schema) listing every failing
  field, not just the first one.

---

## US-002 — As a support agent, I want to manage tickets assigned to me

> Note: the brief numbers this story's tasks TASK-005 through TASK-009,
> which collides with TASK-005 above (Validation, under US-001). This is a
> numbering artifact in the source brief, not a duplicate requirement. Both
> are kept and are disambiguated everywhere else in these docs as
> **TASK-005 (US-001)** and **TASK-005 (US-002)**. Do not merge them.

### TASK-005 (US-002) — Assign ticket
- `PATCH /api/tickets/{id}/assign` assigns a ticket to an agent.
- Request body: `{ "agentId": "<uuid>" }`. If omitted, self-assign the
  authenticated agent (document this convenience behavior if implemented).
- Only role `AGENT` may call this endpoint.
- Assigning a ticket does **not** change its status automatically — a newly
  assigned ticket does not silently become `IN_PROGRESS`. Status changes are
  explicit, via TASK-006.
- Reassigning an already-assigned ticket is allowed (support teams
  reallocate work) and overwrites `assignedAgentId`. Record this in status
  history as an assignment event, not a status event (see
  `03-DATA-MODEL.md`).

### TASK-006 — Update status
- `PATCH /api/tickets/{id}/status` changes ticket status.
- Request body: `{ "status": "<NEW_STATUS>" }`.
- Must go through the transition rules defined in US-003 — this endpoint is
  the enforcement point, not a free-form field update.
- Only the assigned agent (or any agent, if unassigned tickets can transition
  — decide and document) may change status. A customer may **not** directly
  set status, with one exception: a customer may transition their own ticket
  to `CLOSED` if it's currently in `RESOLVED` (i.e. customer confirms
  resolution). Document this exception clearly since it's a permission
  carve-out, not a blanket customer permission.

### TASK-007 — Update priority
- `PATCH /api/tickets/{id}/priority` changes priority.
- Request body: `{ "priority": "<NEW_PRIORITY>" }`.
- Only an agent (assigned or any agent — decide and document, consistent
  with the status endpoint decision above) may change priority. Customers
  cannot re-prioritize their own tickets.

### TASK-008 — Add comments
- `POST /api/tickets/{id}/comments` adds a comment to a ticket.
- Request body: `{ "body": "<text>" }`.
- Both the ticket's customer and the assigned agent may comment. Other
  agents/customers may not comment on tickets that aren't theirs (see
  `05-SECURITY.md` permission matrix).
- `GET /api/tickets/{id}/comments` lists comments in chronological order,
  each with author, authorRole, body, createdAt.
- Comments are immutable once created (no edit/delete endpoint) unless
  explicitly requested otherwise — this keeps the scope bounded and is a
  reasonable assumption for a support-ticket audit trail.

### TASK-009 — Validate permissions
- Centralized permission enforcement (not scattered ad-hoc checks) — see
  `05-SECURITY.md` for the full matrix and the mechanism (method security /
  a dedicated `PermissionService`).
- Every mutating endpoint must reject unauthorized actors with `403
  Forbidden`, and every endpoint must reject unauthenticated requests with
  `401 Unauthorized`. A customer attempting to access another customer's
  ticket gets `403`, not `404` (document this choice — `404` would leak less
  information but `403` is more conventional for this brief; either is
  defensible, pick one and be consistent).

---

## US-003 — As the system, I want to enforce valid ticket status transitions

### TASK-010 — Define allowed transitions
- Status enum: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `REOPENED`
  (adjust only if you have a stronger reason — document if you deviate).
- Allowed transitions (authoritative table, also see `03-DATA-MODEL.md` for
  the state diagram):

  | From | To | Who |
  |---|---|---|
  | OPEN | IN_PROGRESS | Agent |
  | IN_PROGRESS | RESOLVED | Agent |
  | RESOLVED | CLOSED | Customer or Agent |
  | RESOLVED | REOPENED | Customer or Agent |
  | REOPENED | IN_PROGRESS | Agent |
  | IN_PROGRESS | OPEN | Agent (e.g. unassigned / bounced back) |

  Any transition not in this table is invalid. `CLOSED` is terminal — no
  transitions out of `CLOSED`. This table is the single source of truth;
  don't let it drift between code and docs.

### TASK-011 — Prevent invalid transitions
- Attempting an invalid transition (per the table above) returns `409
  Conflict` (state conflict, not a validation error — `400` is for
  malformed input, `409` is for a well-formed request that conflicts with
  current state) with a message naming the current status and the
  attempted status.
- This rule is enforced centrally (one place in code decides validity — see
  `02-ARCHITECTURE.md` for where this lives, likely a `TicketStateMachine`
  or equivalent), not duplicated across controllers/services.

### TASK-012 — Add status history
- Every status change (and, per the assign-ticket note under TASK-005
  US-002, every assignment change) creates an immutable `StatusHistory`
  record: `ticketId`, `fromStatus` (nullable, for creation), `toStatus`,
  `changedBy` (user id), `changedAt`, optional `note`.
- `GET /api/tickets/{id}/history` returns the full history in chronological
  order.
- History records are never updated or deleted — pure append-only audit
  log.

---

## Assumptions register

Anything marked "decide and document" above must be resolved by Codex during
the build and recorded in `11-DESIGN-DECISIONS-LOG.md`, then surfaced again
in the README's "Assumptions" section per `10-README-TEMPLATE.md`. The brief
explicitly allows for this under its "Incomplete Requirements" rule — an
undocumented silent assumption is worse than a documented one.

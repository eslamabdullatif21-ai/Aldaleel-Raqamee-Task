# API Specification

Base path: `/api`. All request/response bodies are JSON. All endpoints
except `/api/auth/*` require a valid JWT in the `Authorization: Bearer
<token>` header.

## Shared error schema

Every non-2xx response uses this shape:

```json
{
  "timestamp": "2026-08-25T10:15:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/tickets",
  "fieldErrors": [
    { "field": "title", "message": "must not be blank" }
  ]
}
```

`fieldErrors` is present only for `400` validation failures; omitted
otherwise.

---

## Auth

### `POST /api/auth/register`
- Body: `{ "email", "password", "name", "role" }` (`role` ∈
  `CUSTOMER`/`AGENT` — in a real product this would be admin-gated, but for
  this assessment's scope, open self-registration with a role field is a
  reasonable, documented simplification).
- `201 Created` → user representation (no password hash).
- `409 Conflict` if email already registered.

### `POST /api/auth/login`
- Body: `{ "email", "password" }`.
- `200 OK` → `{ "token": "<jwt>", "expiresAt": "..." }`.
- `401 Unauthorized` if credentials invalid.
- `429 Too Many Requests` after the configured number of attempts from the
  observed client address. The response includes `Retry-After` in seconds.

---

## Tickets

### `POST /api/tickets`
- Role: `CUSTOMER`.
- Body: `{ "title", "description", "category", "priority"? }`.
- `201 Created` → full ticket representation.
- `400` on validation failure (TASK-005 US-001).

### `GET /api/tickets/{id}`
- Role: `CUSTOMER` (own ticket only) or `AGENT` (any ticket).
- `200 OK` → full ticket representation.
- `403` if a customer requests a ticket that isn't theirs.
- `404` if the ticket doesn't exist.

### `GET /api/tickets`
- Role: `CUSTOMER` → returns only their own tickets.
- Role: `AGENT` → returns tickets assigned to them (per US-002: "tickets
  assigned to me").
- Query params (optional): `status`, `priority`, `category` — simple equality
  filters, applied within the caller's own scope above.
- `200 OK` → paginated list. Use Spring Data `Pageable`
  (`?page=&size=&sort=`) rather than hand-rolled pagination.

### `PATCH /api/tickets/{id}/assign`
- Role: `AGENT`.
- Body: `{ "agentId"? }` — omit to self-assign.
- `200 OK` → updated ticket.
- `403` if the caller isn't an agent.
- `404` if ticket or target agent doesn't exist.
- Creates a `StatusHistory` record with `eventType=ASSIGNMENT`.

### `PATCH /api/tickets/{id}/status`
- Role: per the permission matrix in `05-SECURITY.md` (agent-driven, with
  the one customer carve-out: `RESOLVED → CLOSED`).
- Body: `{ "status": "<TARGET_STATUS>", "note"? }`.
- `200 OK` → updated ticket.
- `409 Conflict` if the transition isn't allowed from the current status
  (TASK-011).
- `403` if the caller lacks permission for this specific transition.
- Creates a `StatusHistory` record with `eventType=STATUS_CHANGE`.

### `PATCH /api/tickets/{id}/priority`
- Role: `AGENT`.
- Body: `{ "priority": "<NEW_PRIORITY>" }`.
- `200 OK` → updated ticket.
- `403` if caller isn't an agent.

---

## Comments

### `POST /api/tickets/{id}/comments`
- Role: the ticket's customer, or its assigned agent.
- Body: `{ "body" }`.
- `201 Created` → comment representation.
- `403` if the caller is neither the ticket's customer nor its assigned
  agent.

### `GET /api/tickets/{id}/comments`
- Role: ticket's customer, or its assigned agent (same rule as posting a
  comment).
- Query params: `page` and `size` (default 20, maximum 100).
- `200 OK` → paginated comments in stable chronological order. Client-provided
  sorting is ignored so page boundaries cannot change the audit chronology.

---

## History

### `GET /api/tickets/{id}/history`
- Role: ticket's customer, or its assigned agent.
- Query params: `page` and `size` (default 20, maximum 100).
- `200 OK` → paginated `StatusHistory` records in stable chronological order
  (both event types, per `03-DATA-MODEL.md`).

---

## Status code summary

| Code | Meaning in this API |
|---|---|
| 200 | Successful read or update |
| 201 | Successful creation |
| 400 | Malformed/invalid request body (Bean Validation failures) |
| 401 | Missing/invalid/expired JWT |
| 403 | Authenticated, but not permitted for this action/resource |
| 404 | Resource doesn't exist |
| 409 | Well-formed request conflicts with current resource state (invalid transition, duplicate email) |
| 429 | Login attempt limit exceeded; retry after the response's `Retry-After` delay |
| 500 | Unhandled server error (should be rare — every expected failure mode above has a specific code) |

## API documentation

This document (`04-API-SPEC.md`) is the authoritative API reference and
is what the README's endpoint summary links back to. No Swagger/OpenAPI
dependency is required by the brief — don't add one unless it's already
part of your normal Spring Boot setup, since it isn't a stated
requirement.

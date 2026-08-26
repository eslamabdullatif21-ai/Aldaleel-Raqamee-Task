# Performance Audit

Audit date: 2026-08-25. Status legend: PASS, FIXED, LIMITATION.

| Check | Status | Finding | Action |
|---|---|---|---|
| N+1 queries | PASS | Ticket responses read relation identifiers only; associations are lazy and Open Session in View is disabled. | Scoped list filtering is executed in one criteria query plus the page count. |
| Pagination | LIMITATION | Ticket listing is paginated; comments/history follow the supplied unpaginated list contract. | Documented as a future cursor/page enhancement for long-lived tickets. |
| Indexes | PASS | Customer, status, composite agent/status, comment timeline, history timeline, and unique email indexes exist in migration and backup. | None. |
| Connection pool | PASS | Hikari defaults are suitable for this small single-instance deployment. | Environment tuning remains a deployment concern. |
| Fetch strategy | PASS | Every `ManyToOne` association explicitly uses `LAZY`. | None. |
| Payload size | PASS | Ticket responses do not embed comments/history or user entities. | Separate endpoints keep payloads bounded by use case. |
| Transaction scope | PASS | Transactions contain database/policy/mapping work only; there are no external calls. | Read methods use `readOnly=true`. |
| Runtime measurements | LIMITATION | Docker is unavailable on the verification host, so cold-start and live PostgreSQL latency were not measured. | Compose and Dockerfile were statically reviewed; run measurements in CI/host with Docker. |

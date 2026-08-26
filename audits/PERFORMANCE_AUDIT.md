# Performance Audit

Audit date: 2026-08-26. Status legend: PASS, FIXED, LIMITATION.

| Check | Status | Finding | Action |
|---|---|---|---|
| N+1 queries | PASS | Ticket responses read relation identifiers only; associations are lazy and Open Session in View is disabled. | Scoped list filtering is executed in one criteria query plus the page count. |
| Pagination | PASS | Ticket, comment, and history endpoints are paginated; collection page size is capped at 100. | Comment/history ordering is forced to timestamp then identifier for stable page boundaries. |
| Indexes | PASS | Customer, status, composite agent/status, comment timeline, history timeline, and unique email indexes exist in migration and backup. | None. |
| Connection pool | PASS | Hikari defaults are suitable for this small single-instance deployment. | Environment tuning remains a deployment concern. |
| Fetch strategy | PASS | Every `ManyToOne` association explicitly uses `LAZY`. | None. |
| Payload size | PASS | Ticket responses do not embed comments/history or user entities. | Separate endpoints keep payloads bounded by use case. |
| Transaction scope | PASS | Transactions contain database/policy/mapping work only; there are no external calls. | Read methods use `readOnly=true`. |
| Runtime measurements | PASS | Local PostgreSQL 16.9 k6 tests captured moderate and boundary concurrency behavior. | Docker runtime remains unavailable on the review host. |
| Single-instance high concurrency | LIMITATION | At 1,000/5,000/10,000 VUs, 9,071/43,220/71,435 requests succeeded while attempted throughput was approximately 867/4,191/6,751 requests per second; transport failure rates were 8.63%/6.91%/23.31%. | The JVM recovered and PostgreSQL had no rollback, deadlock, or temp spill. Use multiple instances and a load balancer for this traffic range; do not hide saturation by increasing pools without deployment measurements. |

The boundary profile used one authenticated paginated ticket-list request per VU
followed by a one-second think time for 10 seconds. The application remained
healthy after 10,000 VUs and returned a successful probe, but the error rates
make all three boundary tiers failures for reliable service. These are local,
single-host results, not production capacity figures.

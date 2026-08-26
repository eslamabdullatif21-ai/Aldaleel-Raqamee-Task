# Performance Audit

Audit date: 2026-08-26. Status legend: PASS, FIXED, LIMITATION.

| Check | Status | Finding | Action |
|---|---|---|---|
| N+1 queries | PASS | Ticket responses read relation identifiers only; associations are lazy and Open Session in View is disabled. | Scoped list filtering is one criteria query plus the page count. |
| Pagination | PASS | Ticket, comment, and history endpoints are paginated and capped at 100. | Timestamp then identifier ordering keeps page boundaries stable. |
| Indexes | PASS | Customer, status, agent/status, comment timeline, history timeline, and unique-email indexes exist in the migration and backup. | None. |
| JWT request path | FIXED | A token was parsed twice and each authenticated request queried PostgreSQL. | Tokens now carry signed ID/email/role, are verified once, and use a one-minute password-free user cache. |
| Hikari pool | FIXED | The implicit default did not express a reviewed database-concurrency budget. | A fixed 16-connection pool and 5,000 ms acquisition timeout are configurable by environment. |
| Tomcat burst handling | FIXED | The default connection and accept limits were below the requested concurrency boundary. | Max connections is 12,000 and accept queue is 2,000; values remain configurable. |
| Request concurrency | FIXED | Platform-thread concurrency is relatively expensive for blocking servlet/JPA work. | Java 21 virtual threads are enabled by default; Java 17 tests explicitly disable them. |
| JVM capacity | FIXED | Heap sizing depended on runtime ergonomics. | Compose and `.env.example` set `-Xms1g -Xmx2g`. |
| Fetch/payload/transactions | PASS | Associations are lazy, payloads exclude timelines/users, and transactions contain database/policy/mapping work only. | Read methods remain `readOnly=true`. |
| Runtime measurements | PASS | PostgreSQL 16.9 and a ramped k6 profile passed all three requested VU tiers. | Preserve realistic think time and run the generator off-host before production sizing. |

## Optimized Ramped Results

Workload: one authenticated `GET /api/tickets?page=0&size=20` per virtual user,
10-second think time, 15-second ramp-up, 15-second hold, and 5-second ramp-down.
Thresholds were checks above 99%, failed requests below 1%, and p95 below 500 ms.

| Maximum VUs | HTTP requests | Average req/s over full run | Failed | HTTP p95 |
|---:|---:|---:|---:|---:|
| 1,000 | 2,999 | 67.70 | 0.00% | 6.27 ms |
| 5,000 | 14,998 | 342.27 | 0.00% | 2.05 ms |
| 10,000 | 29,997 | 684.51 | 0.00% | 1.09 ms |

All thresholds passed. After the 10,000-VU run, the API probe returned HTTP
200, the JVM used about 1,211 MiB working set with 109 OS threads, and the
database showed 16 application connections, zero deadlocks, and zero temporary
files. The decreasing latency at larger tiers should not be interpreted as
linear scaling: warm caches, Windows clock granularity, gradual arrivals, and
same-host scheduling affect these short local measurements.

## Comparison With the Earlier Stress Shape

The earlier profile used a one-second think time and immediately activated every
VU. It attempted approximately 867/4,191/6,751 requests per second at
1,000/5,000/10,000 VUs and produced 8.63%/6.91%/23.31% transport failures. The
new result is not an apples-to-apples speedup claim: the ramp and realistic
think time deliberately produce a controlled read workload. The code and server
tuning remove avoidable overhead, while the new test shape answers whether a
single instance can retain many mostly-thinking users without a connection
storm. The answer for this local profile is yes; it does not prove that 10,000
simultaneously active users or 6,000+ requests/second are reliable on one node.

## Limitations

- k6, the Java process, and PostgreSQL shared one Windows host, so client/server
  contention and local networking make this a boundary test, not capacity data.
- The review host supplied Java 17. The application is compiled/configured for
  Java 21, but virtual threads were disabled during the live run and still need
  deployment-environment verification.
- This read-heavy endpoint has two demo tickets. Write-heavy ticket/comment/status
  mixes require their own SLO-driven profile before production sizing.

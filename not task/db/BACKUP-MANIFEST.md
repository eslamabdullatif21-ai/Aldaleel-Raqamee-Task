# PostgreSQL Backup Manifest

**Artifact:** `db/backup.sql`  
**Generated:** 2026-08-25 22:43 Africa/Cairo  
**Updated and restored:** 2026-08-27 10:53 Africa/Cairo  
**Source server:** PostgreSQL 16.9  
**Generator:** `pg_dump` 16.9, plain SQL format  
**SHA-256:** `2A37080FCDAF0B5E9C95E409E1AD8B9337BD88EE72D088C683C776191E35603A`

## Source and Restored Counts

| Table | Source rows | Restored rows |
|---|---:|---:|
| `users` | 3 | 3 |
| `tickets` | 2 | 2 |
| `comments` | 3 | 3 |
| `status_history` | 7 | 7 |

The dump also contains the successful Flyway V1 schema-history row.

## Generation Command

```powershell
pg_dump -h 127.0.0.1 -p 55432 -U app -d support_ticketing `
  --clean --if-exists --no-owner --no-privileges --format=plain `
  --encoding=UTF8 --file=db/backup.sql
```

## Verification Performed

1. Restored the dump with `psql -v ON_ERROR_STOP=1` into a new empty database named `support_ticketing_restore`.
2. Confirmed all four domain-table counts exactly matched the source.
3. Started the application against the restored database.
4. Confirmed Flyway recognized version 1 and reported the schema up to date.
5. Logged in as the demo customer and both demo agents.
6. Ran the complete 64-check HTTP harness, including self-claim, cross-agent denial, status/priority updates, and current-owner handoff.
7. Read comments and history through the restored API.

## Demo Data

| Role | Email | Password |
|---|---|---|
| Customer | `customer@example.com` | `Customer123!` |
| Agent 1 | `agent@example.com` | `Agent123!` |
| Agent 2 | `agent2@example.com` | `Agent123!` |

These credentials are intentionally demo-only and must not be reused in production.

# PostgreSQL Backup Manifest

**Artifact:** `db/backup.sql`  
**Generated:** 2026-08-25 22:43 Africa/Cairo  
**Source server:** PostgreSQL 16.9  
**Generator:** `pg_dump` 16.9, plain SQL format  
**SHA-256:** `F0A5FB07EF2A54821FFCBB16DAF7DC31DF7681952FC2E4AB9AD3948D2847E0AE`

## Source and Restored Counts

| Table | Source rows | Restored rows |
|---|---:|---:|
| `users` | 2 | 2 |
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
5. Logged in as both demo users.
6. Confirmed customer and assigned-agent ticket lists each returned both tickets.
7. Read comments and history through the restored API.

## Demo Data

| Role | Email | Password |
|---|---|---|
| Customer | `customer@example.com` | `Customer123!` |
| Agent | `agent@example.com` | `Agent123!` |

These credentials are intentionally demo-only and must not be reused in production.

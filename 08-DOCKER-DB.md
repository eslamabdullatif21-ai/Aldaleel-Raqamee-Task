# Docker & Database (Docker Bonus + Database Bonus)

## docker-compose.yml — required services

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: support_ticketing
      POSTGRES_USER: app
      POSTGRES_PASSWORD: ${DB_PASSWORD:-app_local_password}
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U app -d support_ticketing"]
      interval: 5s
      timeout: 5s
      retries: 5

  app:
    build: .
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/support_ticketing
      SPRING_DATASOURCE_USERNAME: app
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-app_local_password}
      JWT_SECRET: ${JWT_SECRET:-replace-me-in-real-deployments}
    ports:
      - "8080:8080"

volumes:
  pgdata:
```

Adjust exact env var names to whatever `application.yml` actually expects
— keep this file and `application.yml` in sync, that mismatch is a common
and embarrassing bug in submissions.

## Dockerfile — multi-stage build

```dockerfile
# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Use `-DskipTests` in the Docker build stage (tests already ran in CI/local
dev — re-running them inside the image build slows the build without added
value), but say so explicitly in the README so it doesn't read as "tests
were skipped" in a bad sense.

## One-command run

README must state, and this must actually work:

```bash
docker-compose up --build
```

Bringing up a fully working system (API + DB, migrations applied
automatically via Flyway on app startup) with that single command is the
actual bar for the Docker bonus — not just "a Dockerfile exists somewhere."

## Database backup (required if using SQL, per assessment rule #4)

The backup must be **included with the submission**, not just
generateable. Two things to commit:

1. **Schema + seed dump**, generated after the app has run migrations and
   (optionally) some demo data has been created:
   ```bash
   docker exec <postgres_container_name> pg_dump -U app -d support_ticketing --clean --if-exists > db/backup.sql
   ```
   Commit `db/backup.sql` to the repo.

2. **A restore instruction in the README** (also duplicate in
   `10-README-TEMPLATE.md`'s content, not just referenced):
   ```bash
   docker exec -i <postgres_container_name> psql -U app -d support_ticketing < db/backup.sql
   ```

Consider seeding `db/backup.sql` with a couple of demo users (one
`CUSTOMER`, one `AGENT`) and a sample ticket or two so a reviewer running
the restore can immediately explore the API without first registering
accounts by hand — this is a small effort that meaningfully improves
reviewer experience, which is part of what's being evaluated even though
it's not a literal checklist item.

## Flyway vs manual dump — how they relate

Flyway migrations (`03-DATA-MODEL.md`) are the source of truth for schema
and are what actually runs on `app` startup against a fresh `postgres`
container — this is what makes `docker-compose up` self-sufficient. The
`pg_dump` backup is a separate, additional artifact satisfying the
assessment's explicit "include a database backup" rule; it's not the
primary way the schema gets created in the running system. Don't rely on
the dump alone to bootstrap the working Docker setup — that would make the
one-command run fragile and dependent on manually keeping the dump in sync
with the Flyway migrations.

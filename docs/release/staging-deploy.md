# Easy Finance MVP - Staging Deployment

## Required Environment Variables

Use real staging secrets. Do not reuse local defaults.

```bash
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080

DB_URL=jdbc:postgresql://<host>:5432/<database>
DB_USERNAME=<database-user>
DB_PASSWORD=<database-password>

JWT_ISSUER=easy-finance
JWT_SECRET=<strong-random-secret-at-least-32-bytes>
JWT_EXPIRATION=PT1H

EXPENSE_IMPORT_MAX_FILE_SIZE_BYTES=5242880
EXPENSE_IMPORT_MAX_FILE_SIZE=5MB
EXPENSE_IMPORT_MAX_REQUEST_SIZE=5MB
EXPENSE_IMPORT_MAX_ROWS=1000

MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,info,metrics,prometheus
MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS=when_authorized
SPRINGDOC_SWAGGER_UI_ENABLED=false
SPRINGDOC_API_DOCS_ENABLED=false

POSTGRES_DB=easy_finance
POSTGRES_USER=easy_finance
POSTGRES_PASSWORD=<database-password>
POSTGRES_PORT=5432
APP_PORT=8080
```

## PostgreSQL

Recommended baseline:

- PostgreSQL 17, matching local `docker-compose.yml`.
- Dedicated database and user.
- TLS enabled when crossing network boundaries.
- Regular backups enabled before deploying the release candidate.
- Restore procedure tested outside production.
- Application user should not be a PostgreSQL superuser.

Flyway is enabled at application startup and uses `classpath:db/migration`.

## JWT

- `JWT_SECRET` is mandatory in staging/prod and must not use local defaults.
- Use a strong random secret with at least 32 bytes of entropy.
- Keep `JWT_EXPIRATION` short enough for financial operations. Current recommendation: `PT1H`.
- Refresh tokens are not implemented in the MVP.

## Profile

Use:

```bash
SPRING_PROFILES_ACTIVE=prod
```

The application defaults to `local` only when no profile is set. Staging should behave like prod unless a dedicated `staging` profile is introduced later.

## Docker Compose

Local staging-like run:

```bash
docker compose up --build
```

API:

```text
http://localhost:8080
```

Health:

```text
GET /actuator/health
```

Expected result:

- PostgreSQL container is healthy.
- API container starts.
- Flyway migrations apply.
- `/actuator/health` returns `UP`.

## Running The JAR

Alternative command:

```bash
mvn clean package
java -jar target/easy-finance-backend-0.1.0-SNAPSHOT.jar
```

Provide all required environment variables before running.

## Health Checks

Recommended external checks:

- `GET /actuator/health`
- `GET /actuator/health/liveness` if exposed by deployment platform.
- `GET /actuator/health/readiness` if exposed by deployment platform.

Do not expose sensitive actuator details publicly.

## Flyway Validation

Before go/no-go:

1. Start with an empty staging database.
2. Start the app and let Flyway apply migrations.
3. Confirm the app reaches healthy state.
4. Execute smoke tests.
5. Preserve migration logs for the release record.

## OpenAPI, Actuator, And CORS Hardening

- Swagger/OpenAPI is useful for local and staging QA.
- In production, protect it behind authentication/network controls or disable access at the ingress/reverse proxy.
- Expose only required actuator endpoints externally. Recommended public endpoint: health only.
- Keep metrics and Prometheus behind internal network access.
- Configure CORS at the edge or application level for the approved frontend origin only.

## Basic Rollback

Application rollback:

1. Stop the new container or deployment.
2. Redeploy the previous known-good image.
3. Verify `/actuator/health`.
4. Run smoke tests for read and auth flows.

Database rollback:

- Flyway migrations in the MVP are forward-only.
- Take a database snapshot/backup before applying migrations to staging/prod.
- If rollback requires schema/data reversal, restore the pre-release backup.
- Do not manually edit Flyway history in staging or production.

## Staging Go/No-Go

Go if:

- `mvn verify -Pci` passed in CI.
- Staging deployment is healthy.
- Flyway migrated successfully.
- Smoke tests passed.
- No blocker from `qa-checklist.md` remains open.

No-go if:

- Any critical financial write fails or duplicates data.
- Any cross-account leak is observed.
- Auth status revalidation fails.
- Import confirmation duplicates rows.
- Debt payment overpayment is possible.

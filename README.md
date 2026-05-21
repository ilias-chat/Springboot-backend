# DWSC Backend

Spring Boot API with **PostgreSQL** (JPA) and **user routes** aligned with **TRWM-backend** (`routes/userRoutes.js`). `server.port` follows **`PORT`** (default `8080`) for Cloud Run.

## Run locally

```bash
mvn spring-boot:run
```

- **PostgreSQL:** [docs/postgresql-env.md](docs/postgresql-env.md) — JDBC + optional `application-local.properties`.
- **Firebase (for `/api/users/*`):** set env var **`FIREBASE_SERVICE_ACCOUNT_JSON`** (same JSON as the Node backend). To skip Firebase (e.g. smoke tests), set **`dwsc.security.firebase.enabled=false`**.

Endpoints:

- `http://localhost:8080/` → `{"message":"Hello World"}`
- `http://localhost:8080/health` → `ok`
- **`/api/users/*`** — Bearer Firebase ID token; same paths and JSON shapes as TRWM-backend.
- **`POST /api/players`** — include **`lat`** and **`lng`** (device GPS) when stadium coordinates from API-Football are unavailable; the backend stores them as the player location.
- **`/api/docs`** — Swagger UI (same path as TRWM-backend).
- **`/api/docs.json`** — OpenAPI 3 JSON spec.

On **Cloud Run**, `server.forward-headers-strategy=framework` ensures Swagger **Servers** uses `https://` (from `X-Forwarded-Proto`), so **Try it out** works when the UI is opened over HTTPS.

## Environment variables

| Variable | Purpose |
|----------|--------|
| `SPRING_DATASOURCE_URL` | JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | DB password |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | Firebase Admin service account JSON (user API) |
| `DDL_AUTO` | optional; use `validate` in production |
| `DB_POOL_MAX` | optional Hikari pool size |

## Docker

```bash
docker build -t dwsc-backend:local .
docker run --rm -p 8080:8080 -e PORT=8080 \
  -e SPRING_DATASOURCE_URL=... \
  -e SPRING_DATASOURCE_USERNAME=... \
  -e SPRING_DATASOURCE_PASSWORD=... \
  -e FIREBASE_SERVICE_ACCOUNT_JSON=... \
  dwsc-backend:local
```

## CI/CD

| Workflow | File | When |
|----------|------|------|
| **CI** | [`.github/workflows/ci.yml`](.github/workflows/ci.yml) | PRs to `main`/`master`; pushes to other branches |
| **CD** | [`.github/workflows/cd.yml`](.github/workflows/cd.yml) | Push to `main` or `master`: tests, Docker Hub, Cloud Run |

**Secrets / variables:** [docs/google-cloud-run.md](docs/google-cloud-run.md)

## Tests

`mvn test` uses profile **`test`** (H2 in-memory, Firebase disabled). CI/CD runs `mvn verify -Dspring.profiles.active=test`.

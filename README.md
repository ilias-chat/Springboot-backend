# DWSC Backend

Spring Boot API with **PostgreSQL** (JPA). `server.port` follows **`PORT`** (default `8080`) for Cloud Run.

## Run locally

1. Create a Postgres database named **`dwsc`** (or any name — match it in the JDBC URL).
2. Either set env vars (see below) or copy `src/main/resources/application-local.properties.example` to **`application-local.properties`** and fill in credentials (that file is gitignored).
3. Run:

```bash
mvn spring-boot:run
```

- `http://localhost:8080/` → `{"message":"Hello World"}`
- `http://localhost:8080/health` → `ok`

**Schema:** Hibernate `ddl-auto` defaults to **`update`** (creates/updates `users`, `players`, `player_comments` to match TRWM Node models). See [docs/postgresql-env.md](docs/postgresql-env.md).

## Environment variables (local or Cloud Run)

| Variable | Example |
|----------|--------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://HOST:5432/dwsc?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | app user |
| `SPRING_DATASOURCE_PASSWORD` | app password |
| `DDL_AUTO` | optional; use `validate` in production |
| `DB_POOL_MAX` | optional Hikari pool size (default `10`) |

Do **not** commit passwords. Use Cloud Run **Secrets** or **Secret Manager** for production.

## Docker

```bash
docker build -t dwsc-backend:local .
docker run --rm -p 8080:8080 -e PORT=8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://... \
  -e SPRING_DATASOURCE_USERNAME=... \
  -e SPRING_DATASOURCE_PASSWORD=... \
  dwsc-backend:local
```

## CI/CD (GitHub Actions → Cloud Run)

| Workflow | File | When |
|----------|------|------|
| **CI** | [`.github/workflows/ci.yml`](.github/workflows/ci.yml) | PRs to `main`/`master`; pushes to other branches |
| **CD** | [`.github/workflows/cd.yml`](.github/workflows/cd.yml) | Push to `main` or `master`: tests, Docker Hub, Cloud Run |

**GitHub secrets/variables:** [docs/google-cloud-run.md](docs/google-cloud-run.md)  
**Postgres + tables:** [docs/postgresql-env.md](docs/postgresql-env.md)

## Tests

`mvn test` runs **without** a database (JPA auto-config is excluded in the smoke test).

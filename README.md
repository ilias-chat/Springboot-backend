# DWSC Backend (Microservices)

Spring Cloud microservices split into 4 Spring Boot applications:

- `discovery-server` (Eureka) — port `8761`
- `config-server` (Spring Cloud Config) — port `8888`
- `player-service` — port `8081` (players, users, admin/API-Football, geolocation; composes player profiles with comments via Feign)
- `comment-service` — port `8082` (comments + star ratings)

Configuration is served by `config-server` from the local filesystem repo: `config-repo/`.

## Run locally

```bash
mvn -pl discovery-server spring-boot:run
mvn -pl config-server spring-boot:run
mvn -pl comment-service spring-boot:run
mvn -pl player-service spring-boot:run
```

Recommended startup order:

1. Discovery Server (`8761`)
2. Config Server (`8888`)
3. Comment Service (`8082`)
4. Player Service (`8081`)

### Databases (PostgreSQL)

This split uses **two separate databases** on the same Postgres server:

- **Player DB**: `dwsc_players`
- **Comment DB**: `dwsc_comments`

Each service reads its datasource settings from Config Server:

- `config-repo/dwsc-player.yml`
- `config-repo/dwsc-comment.yml`

### Firebase

Both `player-service` and `comment-service` validate Firebase Bearer tokens on protected routes.

Set env var **`FIREBASE_SERVICE_ACCOUNT_JSON`** (same JSON as the Node backend). To skip Firebase (e.g. tests), set `dwsc.security.firebase.enabled=false`.

Endpoints:

- **Player Service**: `http://localhost:8081/`
  - `GET /health`
  - `/api/users/*` (Firebase)
  - `/api/admin/*` (Firebase)
  - `/api/players/*` (public GETs; protected writes)
  - `GET /api/players/{id}` returns a player profile **including comments** (fetched from comment-service via Feign + Eureka)
  - Swagger: `/api/docs` and `/api/docs.json`
- **Comment Service**: `http://localhost:8082/`
  - `GET /api/players/{playerId}/comments`
  - `POST /api/players/{playerId}/comments` (Firebase)
  - `DELETE /api/players/{playerId}/comments/{commentId}` (Firebase)
  - Swagger: `/api/docs` and `/api/docs.json`
- **Eureka**: `http://localhost:8761/`
- **Config Server**: `http://localhost:8888/`

## Environment variables

- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` (per-service, usually set in Config Server config files)
- `FIREBASE_SERVICE_ACCOUNT_JSON`
- `DDL_AUTO`
- `DB_POOL_MAX`

## CI/CD

| Workflow | File | When |
|----------|------|------|
| **CI** | [`.github/workflows/ci.yml`](.github/workflows/ci.yml) | PRs to `main`/`master`; pushes to other branches |
| **CD** | [`.github/workflows/cd.yml`](.github/workflows/cd.yml) | Push to `main` or `master`: tests, **4 Docker images**, **4 Cloud Run services** |

CD deploys (in order): `dwsc-discovery` → `dwsc-config` → `dwsc-comment` → `dwsc-player`.

**Secrets / variables:** [docs/google-cloud-run.md](docs/google-cloud-run.md) — includes `SPRING_DATASOURCE_URL_PLAYER`, `SPRING_DATASOURCE_URL_COMMENT`, and per-service Cloud Run / Docker Hub names.

## Tests

`mvn test` uses profile **`test`** (H2 in-memory, Firebase disabled). CI/CD runs `mvn verify -Dspring.profiles.active=test`.

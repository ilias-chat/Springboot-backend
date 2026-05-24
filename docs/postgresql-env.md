# PostgreSQL (Cloud SQL public IP) — database, tables, and env vars

User HTTP routes under **`/api/users/*`** match **TRWM-backend** and require **`FIREBASE_SERVICE_ACCOUNT_JSON`** (see [google-cloud-run.md](google-cloud-run.md) for Cloud Run).

## 1. Create the database in Cloud SQL (once)

Hibernate **does not** create the PostgreSQL *database* (catalog) for you — only **tables** inside an existing database.

1. In [Google Cloud Console](https://console.cloud.google.com/) → **SQL** → your PostgreSQL instance.
2. Open **Databases** → **Create database**.
3. Name it e.g. **`dwsc`** (must match the name in your JDBC URL after the last `/`).

Also create an **application user** (recommended) under **Users**, with a strong password, and use that user in `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`.

## 2. Tables (automatic)

With default **`DDL_AUTO=update`**, Hibernate creates/updates these tables on startup from the JPA entities (aligned with TRWM Node models):

| Table | Maps to |
|--------|---------|
| `users` | `models/User.js` |
| `players` | `models/Player.js` |

If **`GET /api/players`** returns 500:

- `function lower(bytea) does not exist` — text columns were created as **bytea** (legacy import). Run [scripts/postgresql-fix-player-text-columns.sql](../scripts/postgresql-fix-player-text-columns.sql) on the database.
- `could not determine data type of parameter` (SQLState **42P18**) — fixed in app code by casting optional filter bind parameters; redeploy Spring after pulling latest `main`.

## API-Football (`/api/admin/leagues`, etc.)

GitHub Actions secret **`API_FOOTBALL_KEY`** must be **only the key** (e.g. `cbb851f8…`), not `API_FOOTBALL_KEY=cbb851f8…`. A bad value makes API-Football return HTTP 401/403; the app maps that to a clear error instead of a generic 500.
| `player_comments` | embedded `comments[]` in Mongo → relational rows |

**Production:** set `DDL_AUTO=validate` (or `none`) and manage schema with **Flyway** / **Liquibase** instead of `update`.

## 3. Where to put connection settings

### Local development

**Option A — Environment variables** (same names as Cloud Run):

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://CLOUD_SQL_PUBLIC_IP:5432/dwsc?sslmode=require
SPRING_DATASOURCE_USERNAME=your_app_user
SPRING_DATASOURCE_PASSWORD=your_password
```

**Option B — `application-local.properties`** (recommended; file is **gitignored**)

1. Copy `src/main/resources/application-local.properties.example` to  
   `src/main/resources/application-local.properties`
2. Uncomment and set `spring.datasource.url`, `username`, `password`.

`application.properties` already contains:

```properties
spring.config.import=optional:classpath:application-local.properties
```

so the local file is picked up when present.

### Google Cloud Run

In the service → **Edit & deploy new revision** → **Variables & secrets**:

| Name | Value |
|------|--------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://INSTANCE_PUBLIC_IP:5432/dwsc?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | DB password (prefer **Secret Manager** as a secret reference) |

Spring Boot maps these env vars to `spring.datasource.*` automatically.

### GitHub Actions CD

If you use **`.github/workflows/cd.yml`**, add repository **Secrets** named `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`. Each deploy builds `cloudrun-env.json` and **merges** them into the Cloud Run service (same pattern as TRWM-backend’s `write-cloudrun-env.js` + `env_vars_update_strategy: merge`). You can still manage extra variables in the Cloud Run console.

Optional:

| Name | Example |
|------|--------|
| `DDL_AUTO` | `validate` in production |
| `DB_POOL_MAX` | `10` (Hikari max pool size) |

## 4. Networking (public IP)

Under Cloud SQL → **Connections** → **Networking**:

- Add your **Cloud Run egress** or a stable path, or for early testing your **home IP** under **Authorized networks**.
- Avoid `0.0.0.0/0` in production.

## 5. Do **not** paste secrets in chat

Keep passwords and JDBC URLs with passwords in **Cloud Run / Secret Manager / local env / `application-local.properties` only**, not in the Git repo.

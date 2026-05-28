# Google Cloud Run — CI/CD from GitHub Actions

This repo deploys **four Cloud Run services** (one per Spring Boot module):

| Service | Default Cloud Run name | Default Docker Hub repo | API |
|---------|------------------------|-------------------------|-----|
| discovery-server | `dwsc-discovery` | `dwsc-discovery` | Eureka UI |
| config-server | `dwsc-config` | `dwsc-config` | Spring Cloud Config |
| comment-service | `dwsc-comment` | `dwsc-comment` | `/api/players/{id}/comments` |
| player-service | `dwsc-player` | `dwsc-player` | `/api/players`, `/api/users`, `/health` |

Workflows:

1. **`.github/workflows/ci.yml`** — `mvn verify` on PRs and non-main branches.
2. **`.github/workflows/cd.yml`** — on push to **`main`/`master`**: tests, builds **four images**, deploys in order (discovery → config → comment → player), patches Eureka hostnames, smoke tests.

Each container listens on the **`PORT`** env var injected by Cloud Run (typically `8080`); Spring uses `server.port=${PORT:…}` in config. Do not set `PORT` in deploy env files — it is reserved. Local dev still uses 8761 / 8888 / 8081 / 8082.

---

## 1. GitHub repository configuration

### 1.1 Secrets (Settings → Secrets and variables → Actions → **Secrets**)

| Name | Required | Value |
|------|----------|--------|
| `DOCKERHUB_USERNAME` | Yes | Docker Hub user or org |
| `DOCKERHUB_TOKEN` | Yes | Docker Hub access token (read & write) |
| `GCP_SA_KEY` | Yes | Full JSON key for a GCP service account that can deploy Cloud Run |
| `SPRING_DATASOURCE_URL_PLAYER` | Yes | JDBC URL for `dwsc_players` (e.g. `jdbc:postgresql://HOST:5432/dwsc_players?sslmode=require`) |
| `SPRING_DATASOURCE_URL_COMMENT` | Yes | JDBC URL for `dwsc_comments` |
| `SPRING_DATASOURCE_USERNAME` | Yes | PostgreSQL user |
| `SPRING_DATASOURCE_PASSWORD` | Yes | PostgreSQL password |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | Strongly recommended | Full Firebase service account JSON |
| `API_FOOTBALL_KEY` | Strongly recommended | API-Football key (player-service admin routes) |

| Name | Required | Notes |
|------|----------|--------|
| `SPRING_DATASOURCE_URL` | No | Legacy fallback for **player** DB only if `SPRING_DATASOURCE_URL_PLAYER` is unset |

### 1.2 Secrets or Variables (Secrets take precedence, then Variables)

| Name | Required | Example |
|------|----------|---------|
| `GCP_PROJECT_ID` | Yes | `my-gcp-project` |
| `GCP_REGION` | Yes | `europe-west1` |

### 1.3 Optional — override Cloud Run / Docker Hub names

| Name | Default |
|------|---------|
| `CLOUD_RUN_SERVICE_DISCOVERY` | `dwsc-discovery` |
| `CLOUD_RUN_SERVICE_CONFIG` | `dwsc-config` |
| `CLOUD_RUN_SERVICE_COMMENT` | `dwsc-comment` |
| `CLOUD_RUN_SERVICE_PLAYER` | `dwsc-player` |
| `DOCKERHUB_REPO_DISCOVERY` | `dwsc-discovery` |
| `DOCKERHUB_REPO_CONFIG` | `dwsc-config` |
| `DOCKERHUB_REPO_COMMENT` | `dwsc-comment` |
| `DOCKERHUB_REPO_PLAYER` | `dwsc-player` |

The old single-service names `CLOUD_RUN_SERVICE` / `DOCKERHUB_REPO` (`dwsc-backend`) are **no longer used** by CD.

---

## 2. One-time Google Cloud setup

1. Create (or reuse) a service account with at least:
   - **`roles/run.admin`**
   - **`roles/iam.serviceAccountUser`**
2. Paste the JSON key into GitHub Secret **`GCP_SA_KEY`**.
3. Enable the **Cloud Run API** for the project.
4. Ensure PostgreSQL is reachable from Cloud Run (public IP + authorized networks, or Cloud SQL connector).

---

## 3. What CD does on each deploy

1. `mvn verify` (test profile, H2).
2. Build and push four images from `discovery-server/Dockerfile`, `config-server/Dockerfile`, etc.
3. Deploy **discovery** → record Eureka URL.
4. Deploy **config** with Eureka + bundled `config-repo`.
5. Deploy **comment** with Eureka, Config URI, comment DB secrets, Firebase.
6. Deploy **player** with Eureka, Config URI, player DB secrets, Firebase, API-Football, and **`COMMENT_SERVICE_URL`** (comment-service public base URL — used by OpenFeign instead of Eureka).
7. **`gcloud run services update`** on config, comment, and player to set `EUREKA_INSTANCE_HOSTNAME` from each service’s public URL.
8. Smoke test: config endpoint, player `/health`, and `GET /api/players/{id}` (aggregated comments via Feign).

---

## 4. Frontend / client URLs

Point the UI at the **player** and **comment** Cloud Run URLs (from the CD log or Cloud Console), not Eureka or Config Server.

Example:

```env
VITE_PLAYER_API=https://dwsc-player-xxxxx-ew.a.run.app
VITE_COMMENT_API=https://dwsc-comment-xxxxx-ew.a.run.app
```

---

## 5. Local Docker (optional)

```bash
docker build -f player-service/Dockerfile -t dwsc-player:local .
docker run --rm -p 8080:8080 -e PORT=8080 dwsc-player:local
```

---

## 6. Troubleshooting

| Symptom | What to check |
|--------|----------------|
| Docker Hub login fails | `DOCKERHUB_USERNAME` / `DOCKERHUB_TOKEN` |
| GCP auth fails | `GCP_SA_KEY` JSON and IAM roles |
| Deploy fails early | `GCP_PROJECT_ID`, `GCP_REGION` |
| Comment/player env script fails | `SPRING_DATASOURCE_URL_COMMENT` / `_PLAYER`, username, password |
| Player starts but Feign fails | Re-run CD (hostname patch); check Eureka at discovery URL |
| 502 / container exits | Cloud Run logs; DB reachable; `PORT=8080` |

Official references: [Cloud Run](https://cloud.google.com/run/docs), [deploy-cloudrun action](https://github.com/google-github-actions/deploy-cloudrun).

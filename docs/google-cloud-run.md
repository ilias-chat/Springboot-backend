# Google Cloud Run — CI/CD from GitHub Actions

This repo ships a **Dockerfile** and two workflows:

1. **`.github/workflows/ci.yml`** — runs **`mvn verify`** on pull requests to **`main`** / **`master`**, and on pushes to **other** branches.
2. **`.github/workflows/cd.yml`** — on **push to `main` or `master`** (or **manual “Run workflow”**), runs **`mvn verify`**, builds the image, pushes to **Docker Hub**, and deploys to **Cloud Run**.

The CD workflow uses the **same GitHub Actions secret and variable names** as **`TRWM-backend`** (Node): see comments at the top of `TRWM-backend/.github/workflows/cd.yml` and the tables below.

Your app listens on **`PORT`** (Cloud Run sets this automatically). `application.properties` uses `server.port=${PORT:8080}`. The deploy step uses **`--port=8080`** (Node backend uses `3000`).

---

## 1. GitHub repository configuration (same names as Node)

### 1.1 Secrets (Settings → Secrets and variables → Actions → **Secrets**)

| Name | Required for DWSC CD | Value |
|------|----------------------|--------|
| `DOCKERHUB_USERNAME` | Yes | Docker Hub user or org |
| `DOCKERHUB_TOKEN` | Yes | Docker Hub access token (read & write) |
| `GCP_SA_KEY` | Yes | Full JSON key for a GCP service account that can deploy Cloud Run |
| `MONGO_URI` | No* | Same name as Node; **not** consumed by the current Spring CD workflow (reserved / future) |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | No* | Same name as Node; **not** consumed by the current Spring CD workflow (reserved / future) |
| `API_FOOTBALL_KEY` | No* | Same name as Node; **not** consumed by the current Spring CD workflow (reserved / future) |

\*You can define these in the same repo or org for consistency with the Node project; add a `write-cloudrun-env`–style step later if you want them merged into Cloud Run on each deploy (Node does this via `TRWM-backend/scripts/write-cloudrun-env.js`).

### 1.2 Secrets or Variables (Secrets take precedence, then Variables — same as Node)

| Name | Required | Example |
|------|----------|---------|
| `GCP_PROJECT_ID` | Yes | `my-gcp-project` |
| `GCP_REGION` | Yes | `europe-west1` |
| `CLOUD_RUN_SERVICE` | Yes | `dwsc-backend` |
| `DOCKERHUB_REPO` | No | Defaults to **`dwsc-backend`** in this workflow (Node defaults to `ic844-node-backend`) |

---

## 2. One-time Google Cloud setup (service account for `GCP_SA_KEY`)

1. Create (or reuse) a service account with at least:
   - **`roles/run.admin`** — deploy and update Cloud Run services  
   - **`roles/iam.serviceAccountUser`** — act as the Cloud Run runtime service account when needed  
2. Create a **JSON key** for that account and paste the entire JSON into GitHub Secret **`GCP_SA_KEY`** (same pattern as Node).

Enable the **Cloud Run API** for the project if it is not already enabled.

---

## 3. Runtime configuration on Cloud Run (environment variables)

The CD workflow does **not** pass an `env_vars_file` today (unlike Node, which merges `cloudrun-env.json`). Configure the service in the **Cloud Console** (or add a deploy step later), for example when you add PostgreSQL:

| Variable | Notes |
|----------|--------|
| `SPRING_DATASOURCE_URL` | JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | Prefer Secret Manager |
| `SPRING_PROFILES_ACTIVE` | e.g. `prod` |

---

## 4. First deploy and smoke test

1. Push to **`main`** or **`master`**, or run **Actions → CD → Run workflow**.
2. In **Cloud Run** → your service → copy the **URL** and open **`/health`** or **`/`**.

---

## 5. Local Docker (optional)

```bash
docker build -t dwsc-backend:local .
docker run --rm -p 8080:8080 -e PORT=8080 dwsc-backend:local
```

---

## 6. Troubleshooting

| Symptom | What to check |
|--------|----------------|
| Docker Hub login fails | `DOCKERHUB_USERNAME` / `DOCKERHUB_TOKEN` |
| GCP auth fails | `GCP_SA_KEY` is valid JSON for a SA with deploy permissions |
| Deploy fails | `GCP_PROJECT_ID`, `GCP_REGION`, `CLOUD_RUN_SERVICE`; API enabled |
| 502 / container exits | Cloud Run logs; app must listen on `PORT` |

Official references: [Cloud Run](https://cloud.google.com/run/docs), [deploy-cloudrun action](https://github.com/google-github-actions/deploy-cloudrun).

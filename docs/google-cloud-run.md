# Google Cloud Run — CI/CD from GitHub Actions

This repo ships a **Dockerfile** and two workflows:

1. **`.github/workflows/ci.yml`** — runs **`mvn verify`** on pull requests to `main` and on pushes to branches **other than** `main`.
2. **`.github/workflows/deploy-cloud-run.yml`** — on **push to `main`** (or **manual “Run workflow”** when the branch is `main`), runs **`mvn verify`**, then builds the image, pushes it to **Artifact Registry**, and deploys it to **Cloud Run**.

Your app listens on **`PORT`** (Cloud Run sets this automatically). `application.properties` uses `server.port=${PORT:8080}`.

---

## 1. One-time Google Cloud setup

Replace placeholders (`PROJECT_ID`, `REGION`, `GITHUB_ORG`, `GITHUB_REPO`) with your values.

### 1.1 APIs

```bash
gcloud config set project PROJECT_ID

gcloud services enable run.googleapis.com artifactregistry.googleapis.com iamcredentials.googleapis.com
```

### 1.2 Artifact Registry (Docker repository)

```bash
gcloud artifacts repositories create dwsc \
  --repository-format=docker \
  --location=REGION \
  --description="DWSC backend images"
```

If you use a different repository id, set GitHub Variable **`ARTIFACT_REGISTRY_REPOSITORY`** to that id (default in the workflow is `dwsc`).

### 1.3 Workload Identity Federation (recommended for GitHub → GCP)

This lets GitHub Actions call GCP **without** storing a JSON key in GitHub.

Follow Google’s guide: [Authenticate to Google Cloud from GitHub Actions](https://cloud.google.com/iam/docs/workload-identity-federation-with-deployment-pipelines).

Summary of what you need at the end:

- **Workload Identity Provider** resource name (full string), e.g.  
  `projects/123456789/locations/global/workloadIdentityPools/github-actions/providers/github`
- **Service account email** used by GitHub (e.g. `github-deployer@PROJECT_ID.iam.gserviceaccount.com`)

Grant that service account:

```bash
DEPLOYER_SA=github-deployer@PROJECT_ID.iam.gserviceaccount.com

gcloud projects add-iam-policy-binding PROJECT_ID \
  --member="serviceAccount:${DEPLOYER_SA}" \
  --role="roles/artifactregistry.writer"

gcloud projects add-iam-policy-binding PROJECT_ID \
  --member="serviceAccount:${DEPLOYER_SA}" \
  --role="roles/run.admin"

gcloud projects add-iam-policy-binding PROJECT_ID \
  --member="serviceAccount:${DEPLOYER_SA}" \
  --role="roles/iam.serviceAccountUser"
```

`roles/run.admin` is enough for many setups; tighten further with custom roles if your org requires it.

---

## 2. GitHub repository configuration

### 2.1 Secrets (Settings → Secrets and variables → Actions → **Secrets**)

| Name | Value |
|------|--------|
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | Full provider resource name from WIF setup |
| `GCP_SERVICE_ACCOUNT` | Deployer service account email |

*(Alternative: use a JSON key with `google-github-actions/auth` and `credentials_json` — possible but not recommended.)*

### 2.2 Variables (Settings → Secrets and variables → Actions → **Variables**)

| Name | Required | Example / default |
|------|----------|-------------------|
| `GCP_PROJECT_ID` | **Yes** | `my-gcp-project` |
| `GCP_REGION` | No | `europe-west1` (workflow default) |
| `ARTIFACT_REGISTRY_REPOSITORY` | No | `dwsc` (must match the Artifact Registry repo id) |
| `CLOUD_RUN_SERVICE` | No | `dwsc-backend` |

---

## 3. Runtime configuration on Cloud Run (environment variables)

The image does **not** embed database passwords. When you add PostgreSQL (or other config), set variables on the service.

### Option A — Google Cloud Console

1. **Cloud Run** → your service → **Edit & deploy new revision** → **Variables & secrets**.
2. Add environment variables, for example:

| Variable | Notes |
|----------|--------|
| `SPRING_DATASOURCE_URL` | JDBC URL to your DB (Cloud SQL, Neon, etc.) |
| `SPRING_DATASOURCE_USERNAME` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | Prefer **Secret Manager** reference, not plain text in the UI in shared environments |
| `DDL_AUTO` | e.g. `validate` in production (avoid `update` if you use migrations) |
| `SPRING_PROFILES_ACTIVE` | e.g. `prod` |

### Option B — Secret Manager + workflow (recommended for passwords)

1. Create a secret, e.g. `dwsc-db-password`, and grant the **Cloud Run runtime** service account access (`roles/secretmanager.secretAccessor`).
2. In Cloud Run, mount or reference the secret as env `SPRING_DATASOURCE_PASSWORD` (Console: “Secrets” tab).
3. Optionally extend **`.github/workflows/deploy-cloud-run.yml`** with a `secrets:` block on `google-github-actions/deploy-cloudrun@v2` (see comments in that file). Use the full secret resource path, e.g.  
   `projects/PROJECT_ID/secrets/dwsc-db-password/versions/latest`.

### Cloud SQL (optional)

If the database is **Cloud SQL for PostgreSQL**, use the [Cloud SQL Java connector](https://github.com/GoogleCloudPlatform/cloud-sql-jdbc-socket-factory) and deploy with a **Cloud SQL instance** attachment (`--add-cloudsql-instances=...`). That is separate from this baseline Dockerfile; add the dependency and JDBC URL format when you connect.

---

## 4. First deploy and smoke test

1. Push to `main` or run **Actions → Deploy (Cloud Run) → Run workflow** on `main`.
2. In **Cloud Run** → service → copy the **URL** and call your health endpoint, e.g. `https://SERVICE-URL/health`.

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
| Auth fails in GitHub | WIF provider id, service account email, IAM bindings on the deployer SA |
| Push denied | `roles/artifactregistry.writer`, repository name/region match **Variables** |
| Deploy fails | `roles/run.admin`, `roles/iam.serviceAccountUser` |
| Container exits / 502 | Logs in Cloud Run; DB URL/password; listen on `PORT` |
| Wrong image | Confirm workflow pushes to the same `REGION-docker.pkg.dev/PROJECT/REPO/SERVICE:tag` you expect |

Official references: [Cloud Run](https://cloud.google.com/run/docs), [deploy-cloudrun action](https://github.com/google-github-actions/deploy-cloudrun).

# DWSC Backend

Minimal Spring Boot web app. `server.port` follows the `PORT` environment variable (default `8080`) so the same container runs locally and on **Google Cloud Run**.

## Run locally

```bash
mvn spring-boot:run
```

- `http://localhost:8080/` → JSON `{"message":"Hello World"}`
- `http://localhost:8080/health` → plain text `ok`

## Docker

```bash
docker build -t dwsc-backend:local .
docker run --rm -p 8080:8080 -e PORT=8080 dwsc-backend:local
```

## CI/CD (GitHub Actions → Cloud Run)

| Workflow | File | When |
|----------|------|------|
| **CI** | [`.github/workflows/ci.yml`](.github/workflows/ci.yml) | Pull requests to `main`, and pushes to branches other than `main` |
| **Deploy** | [`.github/workflows/deploy-cloud-run.yml`](.github/workflows/deploy-cloud-run.yml) | Push to `main` (tests then Docker → Artifact Registry → Cloud Run), or manual **Run workflow** on `main` |

**Setup (GCP project, Artifact Registry, Workload Identity, GitHub secrets/variables):** see [docs/google-cloud-run.md](docs/google-cloud-run.md).

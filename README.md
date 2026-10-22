# 24eg107d47_tt

Full Stack Development Lab Exam starter: **React (Vite)** frontend + **Java 17 / Spring Boot** REST API + **MySQL on Aiven**, deployable to **Netlify** (frontend) and **Render** (backend, Docker).

The app is only a connectivity starter. It shows frontend / backend / database status and proves every layer works. The exam question is added on top of it.

---

## Project structure

```
.
├── backend/                     Spring Boot REST API (Java 17)
│   ├── src/main/java/com/tt/labexam/
│   │   ├── LabExamApplication.java
│   │   ├── controller/HealthController.java
│   │   ├── service/HealthService.java
│   │   ├── service/DatabaseHealthService.java
│   │   ├── dto/                 ApiResponse, HealthResponse, DbHealthResponse
│   │   ├── config/CorsConfig.java
│   │   └── exception/GlobalExceptionHandler.java
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   ├── application-verified-tls.properties
│   │   └── tls/aiven-ca.pem     Public Aiven project CA (no private key)
│   ├── src/test/java/...        HealthControllerTest (3 tests)
│   ├── scripts/                 setup-aiven-tls.ps1 / .sh
│   ├── Dockerfile               Multi-stage build (Maven -> JRE 17)
│   ├── .env.example             Local secrets template
│   └── pom.xml
├── frontend/                    React + Vite
│   ├── src/App.jsx              Status page
│   ├── src/api/client.js        Axios instance + VITE_API_BASE_URL
│   ├── src/api/health.js        API calls
│   ├── .env.example
│   ├── netlify.toml
│   └── package.json
├── render.yaml                  Optional Render blueprint
├── docker-compose.yml           Local MySQL + backend + frontend
├── .env.example                 Docker Compose variables
├── .gitignore
└── README.md
```

When the exam question needs persistence, add `entity/` and `repository/` packages under `com.tt.labexam`, add `spring-boot-starter-data-jpa` to `pom.xml`, and keep the same controller/service/dto layout.

---

## API

| Method | Path             | Purpose                                       |
| ------ | ---------------- | --------------------------------------------- |
| GET    | `/api/health`    | Backend liveness                              |
| GET    | `/api/health/db` | Runs `SELECT 1` / `SELECT VERSION()` on MySQL |

Every response uses one envelope:

```json
{
  "success": true,
  "message": "Database check completed",
  "data": { "connected": true, "product": "MySQL", "version": "8.4.11", "message": "Database connection successful", "timestamp": 1792688053843 },
  "timestamp": 1792688053843
}
```

Errors never include connection strings, hosts, usernames or passwords.

---

## Environment variables

### Backend

| Variable                    | Required | Example / default                                              |
| --------------------------- | -------- | -------------------------------------------------------------- |
| `SPRING_DATASOURCE_URL`     | yes      | `jdbc:mysql://HOST:PORT/defaultdb?sslMode=REQUIRED`            |
| `SPRING_DATASOURCE_USERNAME`| yes      | the Aiven user (for example `avnadmin`)                        |
| `SPRING_DATASOURCE_PASSWORD`| yes      | the Aiven password                                             |
| `APP_CORS_ALLOWED_ORIGINS`  | yes      | `https://your-site.netlify.app,http://localhost:5173`          |
| `PORT`                      | no       | injected by Render; `8080` locally                             |
| `SPRING_PROFILES_ACTIVE`    | no       | `verified-tls` to verify the Aiven server certificate          |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | no   | `update` (only matters once JPA entities are added)            |

`CORS_ALLOWED_ORIGINS` is accepted as a fallback name for `APP_CORS_ALLOWED_ORIGINS`.

### Frontend

| Variable             | Required | Example                                  |
| -------------------- | -------- | ---------------------------------------- |
| `VITE_API_BASE_URL`  | yes      | `http://localhost:8080/api` (local)      |

`VITE_*` values are **public**: they are baked into the browser bundle. Never put database credentials or secrets there.

---

## Aiven MySQL

Aiven's "Service URI" looks like this:

```
mysql://USER:PASSWORD@HOST:PORT/DATABASE?sslmode=require
```

Spring needs the JDBC form, so translate it:

```
jdbc:mysql://HOST:PORT/DATABASE?sslMode=REQUIRED
```

- Change `sslmode=require` to `sslMode=REQUIRED` (Connector/J spelling).
- Drop `mysql://` and move the username/password into the two separate variables.
- `defaultdb` is Aiven's default database; use another one if you created it.

### Two TLS modes

| Mode | URL | Truststore | Meaning |
| ---- | --- | ---------- | ------- |
| Encrypted | `?sslMode=REQUIRED` | none | Traffic is encrypted, server certificate is not verified. Simplest and works out of the box. |
| Verified | `?sslMode=VERIFY_IDENTITY` (+ `SPRING_PROFILES_ACTIVE=verified-tls` on Render) | `/app/certs/aiven-truststore.p12` | Traffic is encrypted **and** the server certificate is checked against the Aiven CA. |

The Aiven CA certificate is committed at `backend/src/main/resources/tls/aiven-ca.pem`
(it is a public trust anchor, not a private key). `backend/Dockerfile` converts it into
`/app/certs/aiven-truststore.p12` during the image build, so the `verified-tls` profile
needs no extra file on Render.

> Do **not** set `trustCertificateKeyStoreUrl` to an empty value. That makes Connector/J
> validate against the JRE default truststore and fail with
> `CertPathValidatorException: Path does not chain with any of the trust anchors`.
> The base `application.properties` therefore defaults it to
> `file:/app/certs/aiven-truststore.p12` (ignored while `sslMode=REQUIRED`, so a plain
> local run still works).

To use verified TLS locally, build the truststore once — `.env` points at it and no
Spring profile is needed (profiles cannot be activated from an imported `.env`):

```powershell
cd backend
powershell -ExecutionPolicy Bypass -File scripts/setup-aiven-tls.ps1
# the script prints the line to put in backend/.env:
# DB_TRUST_STORE_URL=file:///C:/.../backend/certs/aiven-truststore.p12
# DB_TRUST_STORE_PASSWORD=changeit
# DB_TRUST_STORE_TYPE=PKCS12
# and set sslMode=VERIFY_IDENTITY in SPRING_DATASOURCE_URL
```

---

## Run locally

### Backend

```powershell
cd backend
Copy-Item .env.example .env     # fill in the real Aiven values once
mvn spring-boot:run
```

`application.properties` imports `backend/.env` via
`spring.config.import=optional:file:.env[.properties]`, so ordinary environment
variables still win. `.env` is git-ignored.

Build and test:

```powershell
cd backend
mvn clean package               # compiles and runs the 3 controller tests
java -jar target/lab-exam-backend-0.0.1-SNAPSHOT.jar
```

### Frontend

```powershell
cd frontend
npm install
npm run dev                     # http://localhost:5173
```

Production build:

```powershell
cd frontend
npm run build                   # output in frontend/dist
npm run lint
```

### Quick manual check

```powershell
curl http://localhost:8080/api/health
curl http://localhost:8080/api/health/db
```

---

## Docker

The backend image is built from the **repository root** context:

```powershell
docker build -f backend/Dockerfile -t tt-exam-backend .
docker run --rm -p 8080:8080 `
  -e SPRING_DATASOURCE_URL="jdbc:mysql://HOST:PORT/defaultdb?sslMode=REQUIRED" `
  -e SPRING_DATASOURCE_USERNAME="USER" `
  -e SPRING_DATASOURCE_PASSWORD="PASSWORD" `
  tt-exam-backend
```

Render-style port test:

```powershell
docker run --rm -e PORT=10000 -p 10000:10000 ... tt-exam-backend
```

Full local stack (MySQL container + backend + nginx frontend):

```powershell
docker compose up --build
# frontend http://localhost:5173, backend http://localhost:8080/api
docker compose down
docker compose down -v          # reset the local database volume
```

---

## Deploy the backend to Render

Service type: **Web Service** (or apply `render.yaml` as a Blueprint).

| Setting | Value |
| ------- | ----- |
| Repository | `poojapailla/24eg107d47_tt` |
| Branch | `main` |
| Runtime | **Docker** |
| Dockerfile path | `backend/Dockerfile` |
| Docker build context | repository root (`.`) |
| Health check path | `/api/health` |
| Auto deploy | on (every push to `main`) |

Environment variables:

```
SPRING_DATASOURCE_URL=jdbc:mysql://HOST:PORT/defaultdb?sslMode=REQUIRED
SPRING_DATASOURCE_USERNAME=avnadmin
SPRING_DATASOURCE_PASSWORD=<Aiven password>
APP_CORS_ALLOWED_ORIGINS=https://<your-netlify-site>.netlify.app,http://localhost:5173
```

Optional (recommended): verified TLS

```
SPRING_PROFILES_ACTIVE=verified-tls
SPRING_DATASOURCE_URL=jdbc:mysql://HOST:PORT/defaultdb?sslMode=VERIFY_IDENTITY
```

Do not add `PORT`; Render sets it and Spring reads it through `server.port=${PORT:8080}`.

After the first deploy, open `https://<service>.onrender.com/api/health` and
`https://<service>.onrender.com/api/health/db`. The free plan sleeps after inactivity,
so the first request can take ~30–60 seconds.

---

## Deploy the frontend to Netlify

Import the same repository and configure:

| Setting | Value |
| ------- | ----- |
| Base directory | `frontend` |
| Build command | `npm run build` |
| Publish directory | `dist` |
| Node version | `22` |

Environment variable (Netlify UI) **or** the value in `frontend/netlify.toml`:

```
VITE_API_BASE_URL=https://<your-render-service>.onrender.com/api
```

The `/api` suffix is required because the controllers live under `/api`.

> `frontend/netlify.toml` currently contains `https://tt-exam-backend.onrender.com/api`
> and `render.yaml` contains `https://24eg107d47tt.netlify.app`.
> **Replace both with your real Render and Netlify URLs** if they differ, and update
> `APP_CORS_ALLOWED_ORIGINS` on Render to the exact Netlify origin (scheme + host, no
> trailing slash).

CORS has no wildcard: only the origins listed in `APP_CORS_ALLOWED_ORIGINS` are allowed.

---

## Git

`.gitignore` excludes `.env` files, `node_modules/`, `target/`, `dist/`, keystores and
IDE/OS files. `backend/.env` (real Aiven password) must never be committed.

```powershell
cd 24eg107d47_tt
git status                      # confirm no .env / node_modules / target entries
git add .
git commit -m "Add full-stack lab exam starter (React + Spring Boot + Aiven MySQL)"
git push origin main
```

Render and Netlify redeploy automatically from `main`.

---

## Post-deploy checklist

1. `GET https://<render>/api/health` → `success: true`, `status: "UP"`.
2. `GET https://<render>/api/health/db` → `connected: true` with a MySQL version.
3. Render logs show no password / JDBC URL output.
4. Open the Netlify site → all three cards show **ONLINE** and "All checks passed".
5. Press **Test Backend Connection** → no CORS error in the browser console.
6. The displayed API URL is the Render URL ending in `/api`.
7. Push a trivial commit → both services redeploy automatically.

---

## Security notes

- No credentials are committed. Only `*.env.example` templates are tracked.
- The Aiven CA certificate is public (`CN=... Project CA`), contains no private key, and is intentionally committed for TLS verification.
- The backend logs only exception class names, never connection strings.
- Frontend environment variables are public; they only hold the API URL.
- The Aiven service itself is not created or modified by this project.

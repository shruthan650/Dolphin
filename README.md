# Dolphin

Dolphin is a student progress tracking system for colleges. Administrators create teacher accounts, teachers create
classes and share a class code, and students join with that code. Students then track their **projects** and
**LeetCode problem-solving**, and teachers follow the progress of every student in their classes.

> **Storage:** all users, classes, projects and LeetCode entries are stored in **Cloudflare D1** (free tier).
> The backend needs Cloudflare credentials to start — see [Cloudflare D1 storage](#cloudflare-d1-storage).

---

## Architecture

```text
React (Vite) frontend
        │  REST + JWT (Authorization: Bearer <token>)
        ▼
Spring Boot controllers      ← thin, @PreAuthorize role checks
        ▼
Services                     ← business rules + ownership checks
        ▼
Repository interfaces        ← UserRepository, ClassRepository, ProjectRepository, LeetCodeRepository
        ▼
D1*Repository                ← Cloudflare D1 implementations
        ▼
Cloudflare D1 (SQLite)       ← via the Cloudflare REST API
```

It is a single modular monolith: auth, admin, teacher, student, class, project and LeetCode modules live in one
Spring Boot application. Data is persisted in Cloudflare D1; no local database server is needed.

### Backend layout (`backend/src/main/java/com/dolphin`)

| Package | Contents |
|---|---|
| `controller` | REST endpoints (`Auth`, `Admin`, `Teacher`, `Student`, `Class`, `Project`, `LeetCode`) |
| `service` | Business logic, ownership checks, dashboard calculations, class-code generation |
| `repository` | Persistence interfaces |
| `repository/memory` | `InMemory*Repository` implementations, used only by the automated tests |
| `repository/d1` | Cloudflare D1 client and `D1*Repository` implementations; schema in `resources/d1/schema.sql` |
| `model` | `User`, `ClassEntity`, `Project`, `LeetCodeEntry`, enums |
| `dto` | Request / response records (never expose `passwordHash`) |
| `mapper` | Model ↔ DTO conversion |
| `security` | `JwtService`, `JwtAuthenticationFilter`, JSON 401/403 handlers |
| `exception` | Custom exceptions, `ErrorResponse`, `GlobalExceptionHandler` |
| `config` | `SecurityConfig` (JWT, CORS), BCrypt, development admin seeder |

### Frontend layout (`frontend/src`)

`services/` (Axios instance + one service per domain), `context/AuthContext.jsx`, `hooks/useAuth.js`,
`routes/ProtectedRoute.jsx` + `RoleRoute.jsx`, reusable `components/` (buttons, inputs, modals, tables, cards,
empty/loading/error states, toasts) and role-specific `pages/`.

---

## Roles

| Role | Can | Cannot |
|---|---|---|
| **ADMIN** | Sign in, view system dashboard, create teachers, list teachers / students / users, activate, deactivate or delete teachers | Create classes, join classes, create projects or LeetCode entries |
| **TEACHER** | Create / edit / delete **own** classes, see class codes, view students enrolled in **own** classes and their projects & LeetCode progress, remove a student from own class, delete a student enrolled in an own class, give advice on their students' projects and LeetCode problems | Create teachers, access admin APIs, touch another teacher's classes, see students outside own classes, modify student data |
| **STUDENT** | Self-register, join classes by code, create / update / delete **own** projects and LeetCode entries, view own dashboard and profile | Create teachers or classes, access admin or teacher APIs, view or modify another student's data |

Security is enforced on the backend:

* **Role checks** — URL rules in `SecurityConfig` plus `@PreAuthorize` on every controller.
* **Ownership checks** — in services (`project.ownerId == authenticated user`, `class.teacherId == authenticated
  teacher`, teacher may only read students enrolled in one of their classes). Violations return **403**.
* Owner IDs (`ownerId`, `studentId`, `teacherId`) are **never** accepted from the client; they come from the JWT.
* Passwords are hashed with **BCrypt**; deactivated accounts are rejected on every request.

Frontend route guards only improve UX; they are not the security boundary.

---

## Setup

### Prerequisites

* **Java 17+** (tested with Temurin 21)
* **Node.js 18+** and npm
* Maven is **not** required — the project ships a Maven wrapper (`mvnw` / `mvnw.cmd`) that downloads it.

### 1. Backend

First complete the one-time [Cloudflare D1 setup](#cloudflare-d1-storage), which creates `backend/d1.env` with
the Cloudflare credentials and `JWT_SECRET` (at least 32 characters). The backend reads that file automatically and
refuses to start without it.

```powershell
cd backend
.\mvnw.cmd spring-boot:run      # macOS / Linux: ./mvnw spring-boot:run
```

The API runs at `http://localhost:8080/api`.

Optional environment variables:

| Variable | Default | Purpose |
|---|---|---|
| `JWT_SECRET` | — (required) | HMAC key for signing tokens |
| `JWT_EXPIRATION` | `86400000` | Token lifetime in ms (24 h) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated allowed frontend origins |
| `ADMIN_EMAIL` | `admin@dolphin.com` | Development admin email |
| `ADMIN_PASSWORD` | `Admin@12345` | Development admin password |

Run the tests:

```bash
cd backend
./mvnw test        # Windows: .\mvnw.cmd test
```

### 2. Frontend

```bash
cd frontend
cp .env.example .env     # Windows: copy .env.example .env
npm install
npm run dev
```

Open `http://localhost:5173`. `VITE_API_URL` in `.env` points at the backend
(default `http://localhost:8080/api`). Production build: `npm run build`.

---

## Deployment (Render + Vercel)

The backend runs on **Render** (Docker, free plan) and the frontend on **Vercel**; data stays in Cloudflare D1.
Both platforms deploy from a GitHub repository. `backend/d1.env` and `.env` files are git-ignored and must never be
committed — on Render the same values are entered as environment variables.

### 1. Backend on Render

1. [dashboard.render.com](https://dashboard.render.com) → **New → Blueprint** → connect the GitHub repository.
   Render reads `render.yaml` and creates the `dolphin-backend` web service from `backend/Dockerfile`.
2. When asked, fill in `CLOUDFLARE_ACCOUNT_ID`, `CLOUDFLARE_D1_DATABASE_ID`, `CLOUDFLARE_API_TOKEN` and
   `ADMIN_PASSWORD` (same values as in `backend/d1.env`), and for now `CORS_ALLOWED_ORIGINS=http://localhost:5173`.
   `JWT_SECRET` is generated by Render.
3. Wait for the deploy, then open `https://<service>.onrender.com/api/health` → `{"status":"UP"}`.

### 2. Frontend on Vercel

1. [vercel.com/new](https://vercel.com/new) → import the same repository.
2. **Root Directory:** `frontend` (framework Vite is detected; `frontend/vercel.json` adds the SPA rewrite).
3. **Environment Variable:** `VITE_API_URL=https://<service>.onrender.com/api`, then **Deploy**.

### 3. Connect them

On Render, set `CORS_ALLOWED_ORIGINS` to your Vercel URL(s), e.g.
`https://dolphin.vercel.app,https://dolphin-*.vercel.app` (the wildcard covers preview deployments), and save —
Render redeploys automatically.

### Notes

* **Free Render services sleep after 15 minutes without traffic**; the first request afterwards takes about a minute
  while the backend starts.
* `ADMIN_PASSWORD` is applied on every start. Local runs and Render share the D1 database, so keep the value in
  `backend/d1.env` identical to Render's, otherwise each start resets the other's admin password.
* Changing `VITE_API_URL` on Vercel requires a redeploy (it is baked in at build time).

## Development admin

On startup the backend creates the admin account if it does not exist yet, and sets its password to
`ADMIN_PASSWORD` (changing the variable and restarting changes the password):

| Email | Password |
|---|---|
| `ADMIN_EMAIL` (default `admin@dolphin.com`) | `ADMIN_PASSWORD` (default `Admin@12345`, **development only**) |

Always set a strong `ADMIN_PASSWORD` (in `backend/d1.env` and on Render) for any shared database.
No teachers, classes, projects or students are seeded.

### Try the full flow

1. Sign in as the admin → **Teachers → Create teacher**.
2. Log out, sign in as the teacher → **My Classes → Create class** → copy the class code.
3. Log out → **Student sign up** on the login page (students self-register) → enter the class code to join.
4. As the student: **Projects → Create project**, **LeetCode → Add problem**.
5. Log out, sign in as the teacher → open the class → click the student to see their projects and LeetCode progress.

---

## API reference

All endpoints are under `/api`. Protected endpoints require `Authorization: Bearer <token>`.

### Auth

| Method | Path | Access | Description |
|---|---|---|---|
| POST | `/auth/login` | Public | `{email, password}` → `{token, userId, name, email, role, githubUrl, leetCodeUrl, expiresIn}` |
| POST | `/auth/register` | Public | Student self-registration `{name, email, password, confirmPassword, githubUrl, leetCodeUrl}` → same as login (201). Both profile URLs are required (`https://github.com/<user>`, `https://leetcode.com/u/<user>`) |
| GET | `/auth/me` | Any signed-in user | Current user profile |

### Admin (`ADMIN`)

| Method | Path | Description |
|---|---|---|
| GET | `/admin/dashboard` | Totals for users, teachers, students, classes, projects, LeetCode entries + recent users |
| POST | `/admin/teachers` | Create teacher `{name, email, password, confirmPassword}` (201, 409 on duplicate email) |
| GET | `/admin/teachers` | List teachers |
| PATCH | `/admin/teachers/{id}/status` | `{active: true/false}` activate / deactivate |
| DELETE | `/admin/teachers/{id}` | Delete a teacher and their classes; enrolled students keep their accounts (204) |
| GET | `/admin/students` | List students |
| GET | `/admin/users` | List all users |

### Classes

| Method | Path | Access | Description |
|---|---|---|---|
| POST | `/classes` | TEACHER | Create `{className, semester, branch, section}` (201, code generated) |
| GET | `/classes/mine` | TEACHER | Own classes |
| GET | `/classes/{id}` | TEACHER (owner) | Class + enrolled students with progress summary |
| PUT | `/classes/{id}` | TEACHER (owner) | Update class |
| DELETE | `/classes/{id}` | TEACHER (owner) | Delete class (204) |
| DELETE | `/classes/{id}/students/{studentId}` | TEACHER (owner) | Remove student (204) |
| POST | `/classes/join/{classCode}` | STUDENT | Join (404 unknown code, 409 already joined) |

### Teacher (`TEACHER`)

| Method | Path | Description |
|---|---|---|
| GET | `/teacher/dashboard` | Classes, students, projects, LeetCode entries, problems solved, recent activity |
| GET | `/teacher/students` | Students in own classes |
| GET | `/teacher/students/{studentId}` | Student profile, stats, projects, LeetCode (403 if not in own class) |
| DELETE | `/teacher/students/{studentId}` | Delete a student in an own class, with their projects, LeetCode entries and all enrolments (204; 403 if not in own class) |
| GET | `/teacher/students/{studentId}/projects` | Student's projects |
| GET | `/teacher/students/{studentId}/leetcode` | Student's LeetCode entries |
| GET | `/teacher/projects` | All projects from own students |
| GET | `/teacher/leetcode` | All LeetCode entries from own students |

### Student (`STUDENT`)

| Method | Path | Description |
|---|---|---|
| GET | `/student/dashboard` | Joined classes, project count, LeetCode stats, recent projects & problems |
| GET | `/student/classes` | Joined classes |
| PUT | `/student/profile-links` | Add/update `{githubUrl, leetCodeUrl}` → updated user profile |
| POST | `/projects` | Create `{title, description, githubUrl, liveUrl, technologies[]}` (201) |
| GET | `/projects/my` | Own projects |
| GET | `/projects/{id}` | Own project |
| PUT | `/projects/{id}` | Update own project (403 if not owner) |
| DELETE | `/projects/{id}` | Delete own project (204) |
| POST | `/leetcode` | Create `{problemName, problemUrl, difficulty, status, topic, solvedAt}` (201) |
| GET | `/leetcode/my` | Own entries |
| PUT | `/leetcode/{id}` | Update own entry (403 if not owner) |
| DELETE | `/leetcode/{id}` | Delete own entry (204) |

`difficulty`: `EASY | MEDIUM | HARD` · `status`: `SOLVED | ATTEMPTED | IN_PROGRESS`.
A `SOLVED` entry without `solvedAt` defaults to today.

### Advice

| Method | Path | Access | Description |
|---|---|---|---|
| POST | `/advice` | TEACHER | `{targetType: PROJECT or LEETCODE, targetId, message}`: advice on a project / LeetCode entry of a student in an own class (201, 403 otherwise) |
| DELETE | `/advice/{id}` | TEACHER (author) | Delete own advice (204) |
| GET | `/advice/my` | STUDENT | All advice given to me, newest first, with teacher name and project/problem title |

Teachers also get the student's advice in `GET /teacher/students/{studentId}` (`advice` field). Deleting a project,
LeetCode entry, student or teacher removes the related advice.

### Errors

Every error uses the same shape (`errors` only appears for validation failures):

```json
{
  "status": 403,
  "message": "You are not allowed to modify this project",
  "timestamp": "2026-10-06T10:00:00",
  "path": "/api/projects/123",
  "errors": { "title": "Title is required" }
}
```

Status codes used: `200, 201, 204, 400, 401, 403, 404, 409, 500`.

---

## Cloudflare D1 storage

All users, classes, projects and LeetCode entries are stored in a
[Cloudflare D1](https://developers.cloudflare.com/d1/) database (serverless SQLite). The backend keeps running
wherever you run it today and talks to D1 over Cloudflare's REST API; nothing has to be deployed to Cloudflare.

### One-time setup (Cloudflare dashboard)

1. Sign up / log in at [dash.cloudflare.com](https://dash.cloudflare.com) (the free plan is enough).
2. **Storage & Databases → D1 SQL Database → Create**, name it `dolphin`. Copy its **Database ID**.
3. Copy your **Account ID** (shown on the account home page, or in the dashboard URL after `dash.cloudflare.com/`).
4. **My Profile → API Tokens → Create Token → Create Custom Token**: permission **Account · D1 · Edit**, limited
   to your account. Copy the token — it is shown only once.

You do not need to create tables: on startup the backend applies `backend/src/main/resources/d1/schema.sql`
(idempotent `CREATE TABLE IF NOT EXISTS …`) and seeds the admin account if it does not exist yet.

### Configure the backend

Create `backend/d1.env` (ignored by git — never commit it):

```properties
CLOUDFLARE_ACCOUNT_ID=<account id>
CLOUDFLARE_D1_DATABASE_ID=<database id>
CLOUDFLARE_API_TOKEN=<api token>
JWT_SECRET=<long random string, at least 32 characters>
```

Then start the backend normally (`.\mvnw.cmd spring-boot:run`). Environment variables with the same names
override the file. Look for `Cloudflare D1 schema is up to date` in the log.

| Variable | Default | Purpose |
|---|---|---|
| `CLOUDFLARE_ACCOUNT_ID` | — | Required |
| `CLOUDFLARE_D1_DATABASE_ID` | — | Required |
| `CLOUDFLARE_API_TOKEN` | — | Required; needs **D1 Edit** |
| `CLOUDFLARE_D1_TIMEOUT` | `15s` | Per-request timeout |

### Free-tier limits and trade-offs

* D1 free tier: **5 million rows read / day, 100,000 rows written / day, 5 GB storage** (resets 00:00 UTC). When a
  limit is hit, requests fail with a 500 until the reset.
* Every database call is an HTTPS round-trip to Cloudflare, so pages are slower than with a local database, and
  the Cloudflare API's general rate limit (about 1,200 requests per 5 minutes per account) applies. Each API
  request to Dolphin makes several D1 calls (authentication alone loads the user), so this suits a class-sized
  deployment, not heavy traffic. For more, run the queries from a Cloudflare Worker with a D1 binding instead —
  only `CloudflareD1Client` would change.
* Multi-statement writes (saving a class with its students, deleting a class) are sent as one D1 batch, which
  runs as a single transaction.

### Tests

`mvnw test` runs the whole suite twice: once on in-memory storage and once on the D1 repositories, using a local
SQLite engine in place of Cloudflare (D1 is SQLite). `CloudflareD1ClientTest` checks the REST request/response
format against a mock server. No Cloudflare account is needed to run the tests.

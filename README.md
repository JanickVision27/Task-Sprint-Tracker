# Team Task & Sprint Tracker

A full-stack project and task tracker I built to learn how **Spring Boot**, **React**, **PostgreSQL**, and **real-time WebSockets** work together in a real web application.

🔗 **Live Demo:** [https://task-sprint-tracker.vercel.app](https://task-sprint-tracker.vercel.app)

---

## Why I Built This Project

When I started learning backend and frontend development, most beginner projects I saw were simple single-table To-Do lists where you had to refresh the page every time something changed.

I wanted to challenge myself as a student to build something closer to what real teams use every day (like a simplified Jira or Trello):
- Teams can organize their work step by step: **Projects → Sprints → Tasks → Comments**.
- Users can drag and drop tasks across **TODO**, **IN PROGRESS**, and **DONE** columns on a Kanban board.
- When one teammate moves or creates a task, **everyone else looking at that board sees it update live without refreshing the page**.

---

## Tech Stack (Kept Simple & Practical)

| Part of the App | What I Used | Why I Used It |
| :--- | :--- | :--- |
| **Backend** | Java 21, Spring Boot 3, Spring Security, Spring Data JPA | To build REST APIs, talk to the database, and handle security |
| **Real-Time Updates** | Spring WebSocket (STOMP + SockJS) | To push live board changes to connected browsers instantly |
| **Database** | PostgreSQL (Hosted on Neon) + H2 (for tests) | Cloud Postgres so I didn't overload my 8GB RAM laptop locally |
| **Authentication** | JWT (JSON Web Tokens) + BCrypt | To hash passwords safely and keep users logged in across requests |
| **Transactional Email** | Brevo (v3 HTTPS API) | Welcome email on sign-up and secure "forgot password" links |
| **Frontend** | React (Vite), Tailwind CSS, React Query, Axios, `@dnd-kit` | Fast UI with drag-and-drop cards and automatic data fetching |
| **Testing & CI** | JUnit 5 + Mockito, GitHub Actions, CodeQL, Trivy, Dependabot | Runs backend tests and security scans for pull requests to `main` and pushes to `main`; Dependabot checks updates weekly |
| **Deployment** | Render (Backend Docker) · Vercel (Frontend) · Neon (DB) | Free cloud hosting so anyone can try the app live |

---

## System Architecture

This diagram follows a task update from the browser to the database, then shows how other people on the board receive it live without refreshing.

```mermaid
flowchart LR
    subgraph Browser["User's browser"]
        User["Team member"] --> UI["React + Vite<br/>Kanban board"]
        UI --> Axios["Axios<br/>REST client + JWT"]
        UI --> WSClient["SockJS + STOMP<br/>live-update client"]
        WSClient -->|"Update board without reload"| UI
    end

    Vercel["Vercel<br/>serves the frontend build"] -->|"Loads the React app"| UI

    subgraph Backend["Render · Spring Boot Docker backend"]
        Security["Spring Security<br/>JWT and role checks"]
        Controllers["REST controllers<br/>/api/..."]
        Services["Service layer<br/>business rules"]
        Repositories["JPA repositories"]
        Broker["STOMP message broker<br/>/topic/sprints/..."]
        Security --> Controllers --> Services --> Repositories
        Services -.->|"Publish task or sprint update"| Broker
    end

    Neon[("Neon PostgreSQL")]
    Axios <-->|"1 · HTTPS /api · Bearer JWT"| Security
    Repositories <-->|"2 · SQL via JDBC"| Neon
    WSClient <-->|"3 · WebSocket /ws · STOMP"| Broker

    classDef browserNode fill:#eff6ff,stroke:#2563eb,stroke-width:1px,color:#0f172a
    classDef platformNode fill:#fff7ed,stroke:#ea580c,stroke-width:1px,color:#431407
    classDef backendNode fill:#f5f3ff,stroke:#7c3aed,stroke-width:1px,color:#1e1b4b
    classDef databaseNode fill:#ecfdf5,stroke:#059669,stroke-width:1px,color:#064e3b
    class User,UI,Axios,WSClient browserNode
    class Vercel platformNode
    class Security,Controllers,Services,Repositories,Broker backendNode
    class Neon databaseNode
    style Browser fill:#f8fbff,stroke:#93c5fd
    style Backend fill:#fcfaff,stroke:#c4b5fd
```

**How to read it:** Axios sends authenticated API requests. Spring checks the token and role, applies the business rules, and saves changes in PostgreSQL. The WebSocket broker then pushes an update to connected browsers.

---

## Database Relationships

Instead of putting everything in one table, I connected 5 tables so the data stays organized:

```mermaid
flowchart TD
    U["User\n(id, name, email, password, role)"]
    P["Project\n(id, name, description)"]
    S["Sprint\n(id, name, start_date, end_date, project_id)"]
    T["Task\n(id, title, description, status, priority, sprint_id, assignee_id)"]
    C["Comment\n(id, text, task_id, author_id)"]

    U -->|"Creates / Assigned to"| P
    P -->|"Has many"| S
    S -->|"Has many"| T
    T -->|"Has many"| C
```

---

## Core Features & Rules Implemented

### 1. Login, Registration & Roles
- Passwords are encrypted using **BCrypt** (plain-text passwords are never stored).
- Logging in returns a **JWT token** that the frontend saves and sends in the `Authorization: Bearer <token>` header.
- Three roles are supported:
  - **ADMIN:** Full access to everything.
  - **MANAGER:** Can create and delete Projects and Sprints, and manage all tasks.
  - **MEMBER:** Can view boards, create tasks, claim unassigned tasks, and move their own assigned tasks.

### 2. Practical Business Rules
- **No "DONE" Without an Assignee:** A task cannot be moved to the `DONE` column unless someone is assigned to it (`Cannot move task to DONE without an assignee`).
- **Logical Sprint Dates:** A sprint's end date cannot be earlier than its start date.
- **Clean Error Messages:** A `GlobalExceptionHandler` catches validation and business-rule errors and sends clear messages back to the React UI instead of crashing.

### 3. Emails & Password Reset (Brevo)

Both emails are sent through **Brevo's transactional HTTPS API** (not SMTP — free container hosts block outbound SMTP ports). The HTML is a branded, table-based template that mirrors the app UI, with a plain-text fallback for every message.

- **Welcome / confirmation email:** fired right after `POST /api/auth/register`. It greets the user by name, confirms the address and the role they picked, explains Projects → Sprints → Tasks, and links to their dashboard. The registration response does **not** wait for Brevo (the send happens on a background thread), so a mail provider outage never breaks sign-up.
- **Forgot password:** the user clicks *Forgot password?* on the login screen, the backend creates a **single-use, 30-minute token** and emails a link to `https://<frontend>/reset-password?token=...`. The page verifies the link first (so expired links get a friendly "request a new one" screen), then the new password is BCrypt-hashed and the token is destroyed.

Security details worth calling out:
- Only the **SHA-256 hash** of the reset token is stored, so a database leak cannot be replayed to take over accounts.
- `POST /api/auth/forgot-password` always returns the same generic message, so it cannot be used to discover which emails are registered.
- Requesting a new link revokes the older ones; using a link marks it used and deletes every other outstanding link for that account.
- User-supplied values (name, email) are HTML-escaped before they land in an email.

A rendered preview of both emails is committed at [`Backend/docs/email-previews.html`](Backend/docs/email-previews.html) — open it in a browser to review the design without sending anything.

---

## Main API Endpoints

| Method | Endpoint | What It Does |
| :--- | :--- | :--- |
| `GET` | `/api/health` | Public health check used by Render (`{"status": "UP"}`) |
| `POST` | `/api/auth/register` | Register a new user account |
| `POST` | `/api/auth/login` | Log in and receive a JWT token |
| `POST` | `/api/auth/forgot-password` | Email a single-use password reset link via Brevo (generic response, no user enumeration) |
| `GET` | `/api/auth/reset-password/validate?token=...` | Check whether a reset link is still valid before showing the form |
| `POST` | `/api/auth/reset-password` | Set a new password with a valid token (BCrypt-hashed, token burned) |
| `GET / POST` | `/api/projects` | View all projects or create a new project |
| `DELETE` | `/api/projects/{id}` | Delete a project and all its sprints, tasks, and comments |
| `GET` | `/api/sprints/project/{projectId}` | Get all sprints inside a project |
| `POST / DELETE` | `/api/sprints` | Create or delete a sprint (Managers & Admins) |
| `GET` | `/api/tasks/sprint/{sprintId}` | Get all tasks for a sprint's Kanban board |
| `POST / PUT / DELETE` | `/api/tasks` | Create, move/update, or delete a task (triggers WebSocket update) |
| `GET / POST` | `/api/comments` | View or add comments on a task |

---

## Challenges I Faced & What I Learned (My Honest Journey)

Building this project was a huge learning curve for me. Coming in as a beginner to Spring Boot and full-stack deployment, there were moments where I felt overwhelmed by all the annotations, security filters, and deployment errors—and I had to debug them step by step:

1. **"It worked on my Windows laptop, why did it fail on Linux/Render?"**
   - Locally on Windows, my code compiled fine even though my file was named `JWTService.java` while the class inside was `public class JwtService` (and my Docker file was named `DockerFile`). When I pushed to GitHub Actions and Render (which run on Linux), the build failed immediately because Linux is strictly case-sensitive. Fixing that taught me how important file naming and CI pipelines are in real teams.

2. **Debugging the Cloud Database Connection (JDBC URL Format)**
   - My first Render deployment built the Docker image successfully but crashed on startup with `Unable to determine Dialect without JDBC metadata`. I learned that Java's JDBC driver cannot read `username:password@host` inside the URL the way Node.js or Python does—it needs a clean `jdbc:postgresql://host/dbname` URL with the username and password passed as separate environment variables.

3. **Connecting Vercel to Render (Proxies & CORS)**
   - During local development, Vite's proxy forwarded `/api` and `/ws` to `localhost:8080`, so everything worked without CORS issues. Once deployed to Vercel and Render on two different domains, I had to configure `VITE_API_URL` on the frontend and enable CORS in both `SecurityConfig` and `WebSocketConfig` on the backend.

4. **Getting Comfortable with Spring Boot Annotations**
   - At first, seeing `@RestController`, `@Service`, `@PreAuthorize`, and `@RestControllerAdvice` felt like magic. Breaking the backend into simple layers—**Controllers** (receive the request), **Services** (check plain Java `if` rules), **Repositories** (talk to the database), and **GlobalExceptionHandler** (a global `try-catch`)—made everything click for me.

---

## What I Plan to Improve Next

This project is a strong working MVP, and I am continuing to learn and improve it:
- [ ] **Task Comments UI Modal:** The backend CRUD APIs for comments (`/api/comments`) are already built and tested; next, I want to add a slide-over panel on each task card so teammates can chat inside a task on the frontend.
- [ ] **Deeper JPA Entity Relationships:** Upgrade `assigneeId` and `authorId` from simple ID numbers to full `@ManyToOne` `User` relationships so task cards display the assignee's full name and avatar directly.
- [ ] **Team Member Invite & Filter Bar:** Allow filtering the Kanban board by priority (`HIGH`, `MEDIUM`, `LOW`) or by assigned teammate.

---

## How to Run Locally

### 0. Prerequisites
- **Java 21** — the included `./mvnw` downloads Maven for you
- **Node 20+**
- **A PostgreSQL database**, pick whichever is easiest:
  - **Docker** (recommended): `docker compose up -d` starts Postgres 16 on `localhost:5432`
  - an existing local PostgreSQL install
  - a free cloud database such as [Neon](https://neon.tech)

### 1. Backend (`Spring Boot`)

**a. Create your local config file.** It is git-ignored, so your password never reaches GitHub:

```bash
# macOS / Linux
cp Backend/application-local.properties.example Backend/application-local.properties

# Windows PowerShell
Copy-Item Backend/application-local.properties.example Backend/application-local.properties
```

The template defaults already match `docker-compose.yml`. Open `Backend/application-local.properties` and check:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/sprint_tracker
spring.datasource.username=sprint_tracker
spring.datasource.password=local_dev_password
app.jwt.secret=<paste any random string of 32+ characters>
```

Using Neon or your own Postgres instead? Paste those values — a raw
`postgresql://user:password@host/db?sslmode=require` string works too and is converted to JDBC automatically.

> The file is picked up from `Backend/application-local.properties`, `Backend/config/application-local.properties`, or
> `Backend/src/main/resources/application-local.properties` — any one of them is fine, because
> `spring.profiles.active` defaults to `local`.

**b. Start the database (Docker only), then the backend:**

```bash
docker compose up -d && docker compose ps      # wait until STATUS says "healthy"
cd Backend
./mvnw spring-boot:run
```

A healthy start prints the resolved target and a real database round-trip:

```
Database target: jdbc:postgresql://localhost:5432/sprint_tracker (user: sprint_tracker, profiles: local)
Database connection OK — PostgreSQL 16.9 (read-write check passed)
```

**c. Confirm it from another terminal:**

```bash
curl http://localhost:8080/api/health
# {"status":"UP","service":"Task & Sprint Tracker Backend","database":"UP"}
```

If the backend refuses to start, it now tells you exactly what is missing instead of failing inside Hibernate —
see [Troubleshooting the Database](#troubleshooting-the-database).

Run the automated tests (in-memory H2, no Postgres or Docker needed):

```bash
cd Backend
./mvnw clean test
```

### 2. Frontend (`React + Vite`)
```bash
cd Frontend
npm install
npm run dev
```
Open `http://localhost:5173`. In dev, Vite proxies `/api` and `/ws` to `http://localhost:8080`, so no CORS setup is needed.

### 3. Email locally (optional)

Create a free [Brevo](https://brevo.com) account, grab a **v3 API key** from **SMTP & API → API Keys**, and verify a
sender under **Senders & IP → Senders** (otherwise Brevo answers *"sender not valid"*). Then either put these in
`application-local.properties` or export them as environment variables:

```properties
app.brevo.enabled=true
app.brevo.api-key=xkeysib-your-key
app.brevo.sender-email=no-reply@yourdomain.com
app.brevo.sender-name=Task & Sprint Tracker
app.frontend-url=http://localhost:5173
```

No key yet? Leave `app.brevo.enabled=false`: sign-up and password reset still work, emails are skipped, and the
password-reset link is printed to the backend console so you can finish the flow offline.

---

## Troubleshooting the Database

### Local setup checklist

Run through these five lines — they catch almost every local database problem:

```bash
docker compose ps                                   # 1. is Postgres running and "healthy"?
ls Backend/application-local.properties             # 2. does the git-ignored config exist?
curl http://localhost:8080/api/health               # 3. does it say "database":"UP"?
# 4. read the backend console: "Database target: ..." shows the URL Spring actually used
# 5. no Docker? point spring.datasource.url at your own Postgres or at Neon instead
```

The app accepts a connection string in **three** places, and it normalises the awkward ones for you:

| What you provide | Example | Handled? |
| :--- | :--- | :--- |
| Node-style URL (what Neon/Render/Heroku give you) | `DATABASE_URL=postgresql://user:pass@ep-xxx.neon.tech/neondb?sslmode=require` | ✅ converted automatically to `jdbc:postgresql://ep-xxx.neon.tech/neondb?sslmode=require` |
| JDBC URL | `SPRING_DATASOURCE_URL=jdbc:postgresql://ep-xxx.neon.tech/neondb?sslmode=require` | ✅ used as-is |
| Local file | `Backend/src/main/resources/application-local.properties` | ✅ wins when no env var is set |

`DatabaseEnvironmentPostProcessor` does the conversion at startup (before Hibernate builds the dialect), so the old
`Unable to determine Dialect without JDBC metadata` crash from a Node-style URL is gone. Credentials embedded in the
URL are split into `spring.datasource.username/password` automatically; if you also set `DATABASE_USERNAME` /
`DATABASE_PASSWORD`, those win.

**Check the database from a single URL** — open `https://<your-backend>/api/health`:

```json
{ "status": "UP", "service": "Task & Sprint Tracker Backend", "database": "UP" }
```

If the database is unreachable you get `"database": "DOWN"` plus a short `databaseError` (password-free), e.g.
`FATAL: password authentication failed for user "app_user"`. The HTTP status stays `200` so Render does not restart a
healthy container over a temporary database hiccup; `/actuator/health` still reports `DOWN` for probes.

Startup logs also print the sanitized target and the real `SELECT 1` result:

```
Database target: jdbc:postgresql://ep-xxx.neon.tech/neondb?sslmode=require (user: app_user, profiles: prod)
Database connection OK — PostgreSQL 17.2 (read-write check passed)
```

If it fails, the log block lists the usual suspects. The short version:

| Symptom | Cause | Fix |
| :--- | :--- | :--- |
| `Failed to configure a DataSource` / `No database is configured` | No env var and no `application-local.properties` | `docker compose up -d` then copy `application-local.properties.example`, or set `DATABASE_URL` |
| `Unable to determine Dialect without JDBC metadata` | A Node-style URL reached the JDBC driver (older builds) | Update to current `main`; or convert to `jdbc:postgresql://…` yourself |
| `password authentication failed` | Password copied wrong, or URL-encoding needed | Percent-encode `@ # : / ?` in the password (`p@ss` → `p%40ss`) |
| `Connection refused` on port 5432 locally | Nothing listening — Docker not started, or Postgres not installed | `docker compose up -d`, or install/start Postgres |
| `port is already allocated` | Another Postgres already uses 5432 | Change the host port in `docker-compose.yml` (`"5433:5432"`) and update the URL |
| Data disappears after `docker compose down -v` | `-v` deletes the named volume | Use `docker compose down` (no `-v`) to keep data |
| `The connection attempt failed` / timeout | Neon project suspended, wrong host, or cluster cannot egress port 5432 | Wake the Neon project, check the host, review the NetworkPolicy |
| `SSL required` / `no pg_hba.conf entry` | Missing TLS mode | Keep `?sslmode=require` on the URL |
| Works locally, empty data on Render | Two different databases (local file vs. Render env vars) | Compare the host in `application-local.properties` with Render's `SPRING_DATASOURCE_URL` |
| Tables missing after deploy | `ddl-auto` overridden | Leave `SPRING_JPA_HIBERNATE_DDL_AUTO` unset (defaults to `update`) |

> **Render specifics:** `render.yaml` marks the database keys `sync: false`, so they are **not** in the file — you must
> paste them in Render → your service → **Environment**. A missing value there means Spring sees an empty datasource URL.

---

## Deploying to Vercel + Render

The frontend and backend deploy separately, and **they must be in sync**. The screens you see are built from the
frontend commit while every piece of data comes from the backend — so "the site did not update" is usually one of the
three situations below.

### Checklist

| # | Where | Setting | Why it matters |
| :--- | :--- | :--- | :--- |
| 1 | Vercel → Settings → **General → Root Directory** | `Frontend` | Without it the build runs at the repo root, finds no `package.json`, fails, and the site keeps serving the last successful deployment |
| 2 | Vercel → Settings → **Environment Variables** | `VITE_API_URL=https://<backend-host>` (no trailing slash, no `/api`) | Without it the bundle calls `/api` on the Vercel domain, gets `index.html` back, and every screen looks broken |
| 3 | Vercel → **Deployments → Redeploy** | after changing #2 | **`VITE_*` values are baked into the bundle at build time.** Editing the variable does not touch an existing deployment |
| 4 | Render → your service | redeploy the same commit | A new frontend calling new endpoints (like `/api/auth/forgot-password`) returns 404 against an old backend |
| 5 | Render → Environment | `DATABASE_URL`, `BREVO_API_KEY`, `BREVO_SENDER_EMAIL`, `APP_FRONTEND_URL` | See [Troubleshooting the Database](#troubleshooting-the-database) and the Brevo notes above |

### Verify a deployment in three commands

```bash
# 1. Is the backend awake and is its database reachable?
curl -s https://<backend-host>/api/health
#    -> {"status":"UP","service":"...","database":"UP"}

# 2. Which backend URL is baked into the bundle Vercel is serving right now?
ASSET=$(curl -s https://<vercel-host>/ | grep -o '/assets/index-[A-Za-z0-9_-]*\.js' | head -1)
curl -s "https://<vercel-host>${ASSET}" | grep -o 'https://[a-z0-9.-]*\.onrender\.com' | head -1
#    -> prints the API host from the live bundle, or nothing if VITE_API_URL was missing

# 3. Is an API route served by the API rather than the SPA?
curl -s -o /dev/null -w '%{http_code} %{content_type}\n' https://<vercel-host>/api/health
#    -> 404 text/plain is expected (the SPA no longer swallows /api);
#       text/html would mean the request was answered by index.html
```

### Why `/api` used to return HTML

`vercel.json` used to rewrite **every** path to `index.html`. A request to `/api/health` therefore came back as
`200 text/html`, so axios received a web page instead of JSON and the UI showed an endless spinner, "Invalid email or
password", or stale/empty data. The rewrite is now scoped to non-API paths (`/((?!api/|ws/).*)`), and `client.js`
detects an HTML response and surfaces a clear message. A deployment without `VITE_API_URL` also shows an amber
**"Backend not configured"** banner on every auth screen instead of failing silently.

### What CI now checks

`.github/workflows/ci.yml` gained a **Frontend lint and build** job (`npm ci`, `npm run lint`, `npm run build`) next to
the Maven job, so a broken frontend build — which previously only appeared inside Vercel — fails the commit check.

---

## DevOps & Deployment

- **Automated checks:** GitHub Actions builds and tests the Java 21 backend, then runs CodeQL and Trivy in parallel. Trivy reports findings but currently does not fail the pipeline for them. Jenkins and Azure Pipelines examples also run Trivy; Dependabot checks for updates weekly.
- **Container publishing:** The multi-stage Docker image runs as a non-root user. When `JFROG_URL` is configured, GitHub Actions publishes the scanned image to JFrog only after a successful push to `main` and passing scan jobs; otherwise, the publish job skips. The pipeline does not deploy to Kubernetes.
- **Optional Kubernetes setup:** `k8s/` contains backend-only manifests for manual deployment. Create real app and JFrog image-pull Secrets outside Git, render `${JFROG_URL}` before applying, and do not use the committed placeholder Secret values. The current NetworkPolicy allows TCP/5432 to any IP, so narrow that rule before production.

---

## License
MIT License © 2026 Venkata Vikranth Jannatha

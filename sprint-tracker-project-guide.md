# Team Task & Sprint Tracker — Full Project Build Guide

A real-time, full-stack task management app (Spring Boot + React + WebSockets), built and documented step by step so you understand *why* each piece exists, not just *how* to copy it.

---

## 1. Project Overview

**What you're building:** A multi-user project management tool where teams organize work into Projects → Sprints → Tasks, move tasks across a Kanban board, and see updates from teammates live (no page refresh) via WebSockets.

**What it proves to an employer:**
- You can design a relational data model with real relationships (not a single flat table)
- You can secure an API properly (JWT + role-based access)
- You can handle real-time data, which most junior portfolios skip entirely
- You can ship something — live URL, CI/CD pipeline, tests

**Tech stack**
| Layer | Tech | Why |
|---|---|---|
| Backend | Java 17, Spring Boot 3, Spring Security, Spring Data JPA, Spring WebSocket (STOMP) | Matches your TalentSprint curriculum + existing Spring Boot project |
| Database | PostgreSQL (hosted free on Neon or Supabase) | Real production DB, zero local RAM cost |
| Frontend | React (Vite), Tailwind CSS, React Query, @stomp/stompjs | Lightweight, fast dev server |
| Auth | JWT (access token) | Same pattern as your banking system project — reuse that code |
| CI/CD | CircleCI | Runs tests + builds on every push |
| Hosting | Backend → Render (free tier) · Frontend → Vercel · DB → Neon | $0 cost, all public URLs |
| Testing | JUnit 5 + Mockito (backend) | Matches your existing test style (15 unit / 14 integration on last project) |

---

## 2. Data Model (design this on paper first)

```
User (id, name, email, password_hash, role: ADMIN/MANAGER/MEMBER)
  |
  | 1---many
Project (id, name, description, owner_id -> User)
  |
  | 1---many
Sprint (id, name, project_id -> Project, start_date, end_date)
  |
  | 1---many
Task (id, title, description, status: TODO/IN_PROGRESS/DONE,
      sprint_id -> Sprint, assignee_id -> User, priority)
  |
  | 1---many
Comment (id, text, task_id -> Task, author_id -> User, created_at)
```

**Why this order matters:** Get the entity relationships right *before* writing a single line of code. This is the #1 thing that separates a junior CRUD app from something that looks like real software — draw it out (pen and paper or draw.io) and check every foreign key makes sense before you touch Spring.

---

## 3. Build Phases (do them in this order — don't skip ahead)

### Phase 0 — Setup (Day 1)
- Create GitHub repo, add `.gitignore` for Java + Node
- Set up Neon/Supabase Postgres instance, save connection string somewhere safe (you'll need it for `application.properties` and later for Render env vars — never commit it to GitHub)
- Spring Initializr: generate project with Web, Security, JPA, PostgreSQL Driver, Validation, WebSocket dependencies
- Confirm the Spring Boot app connects to your hosted DB and starts cleanly before writing any business logic

### Phase 1 — Core Entities & Plain CRUD (Days 2–4)
- Write the 5 entity classes above with JPA annotations (`@Entity`, `@ManyToOne`, `@OneToMany`)
- Write Repositories (`extends JpaRepository`)
- Write Services + Controllers for **Project** and **Sprint** only first — get one full vertical slice working (Controller → Service → Repository → DB) before building the rest. This is a habit that will make you fast in real jobs: prove one thin slice end-to-end before scaling out.
- Test every endpoint in Postman before moving on

### Phase 2 — Task Board + Business Rules (Days 5–7)
- Add Task entity/endpoints
- Add validation: a task can't move to DONE without an assignee, sprint dates must be logical, etc. (this is where you demonstrate you understand business logic, not just data plumbing)
- Add a `GlobalExceptionHandler` (you already have this pattern from your Vacation Request API — reuse it)

### Phase 3 — Auth (Days 8–9)
- JWT filter chain, login/register endpoints, password hashing (BCrypt)
- Role-based endpoint restrictions: MEMBER can move their own tasks; MANAGER can create sprints; ADMIN can do everything
- Reuse your banking project's JWT setup as a base — don't rebuild this from scratch

### Phase 4 — WebSockets (Days 10–12, the differentiator)
- Add a `/ws` STOMP endpoint in Spring config
- When a task's status changes, broadcast the update to a topic like `/topic/project/{projectId}`
- Frontend subscribes to that topic and updates the board instantly for every connected user
- This is genuinely the part that will make your project stand out in interviews — be ready to explain *how* STOMP/WebSocket works, not just that you used it

### Phase 5 — React Frontend (Days 13–18)
- Login/register pages
- Project list → Sprint view → Kanban board (use `@dnd-kit` or `react-beautiful-dnd` for drag-and-drop)
- React Query for fetching/caching REST data, STOMP client for live updates layered on top
- Keep components small — one component per board column, one per card, one per modal

### Phase 6 — Tests (Days 19–20)
- Backend: unit test services (mock repositories), integration test controllers with `@SpringBootTest` + H2 in-memory DB
- Aim for the same rigor as your last project (you hit 29 test cases there — match or beat it)

### Phase 7 — CI/CD with CircleCI (Day 21)
- `.circleci/config.yml` that: checks out code → runs `mvn test` → builds a JAR → (optionally) builds a Docker image → triggers deploy
- Connect your GitHub repo to CircleCI, get the pipeline green before touching deployment

### Phase 8 — Deployment (Days 22–23)
- Backend: push Docker image or connect repo directly to Render, set env vars (DB URL, JWT secret) in Render's dashboard — never hardcode secrets
- Frontend: connect repo to Vercel, set `VITE_API_URL` env var pointing to your Render backend URL
- Test the live app end-to-end from two different browser tabs to confirm real-time sync actually works publicly

### Phase 9 — Documentation (Day 24)
- Write a proper `README.md`: problem statement, architecture diagram, screenshots/GIF of the live board updating, setup instructions, link to live demo
- This README is often the *first* thing a recruiter or interviewer looks at — treat it as part of the deliverable, not an afterthought

---

## 4. Laptop-Specific Workflow Notes (8GB RAM / ~5.9GB usable)

- Use the hosted Postgres DB from Phase 0 onward — never run Postgres in Docker locally
- Use VS Code, not IntelliJ Ultimate, for daily work
- Run backend and frontend dev servers one at a time when things feel sluggish
- If your machine struggles once WebSockets + React + Spring Boot are all running together, move development into a free GitHub Codespace for Phases 4 onward

---

## 5. How to Talk About This Project in Interviews

Be ready to explain, in your own words:
- Why you chose WebSockets over polling for live updates
- How JWT auth actually works (token issued → stored client-side → sent in Authorization header → validated by a filter)
- One bug you hit and how you debugged it (interviewers care more about this than a clean success story)

---

## 6. Suggested Weekly Pace

| Week | Focus |
|---|---|
| 1 | Phases 0–3 (setup, CRUD, auth) |
| 2 | Phases 4–5 (WebSockets, frontend) |
| 3 | Phases 6–9 (tests, CI/CD, deploy, docs) |

Roughly 3–4 weeks at a steady pace alongside your DSA prep and TalentSprint coursework.

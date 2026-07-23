# Princess Project — Progress Notes

## Repo layout

- `backend/` — Spring Boot (this directory)
- `frontend/` — React + Vite + TS
- `docs/` — `API_SPEC.md` (Notion-friendly tables), `ERD.md` + `schema.dbml` (dbdiagram.io), `DEPLOYMENT.md` (Vercel/Railway)

## Data model (current, fully implemented)

```
User → UserProject (goalHuman/goalEnding/status; one auto-created "active" project per user for now)
     → UserGoal    (habitus/자본 selection: which of the 7 fixed goal_types + weight%)
     → UserStat    (behavior-category selection under a chosen habitus, e.g. 신체→운동/식단,
                     OR a custom "나만의 미션" pseudo-stat — stat_type_id is nullable,
                     sibling-level to catalog stats, not nested inside one)
     → UserMission (from the mission_definitions catalog, or fully custom; missionType
                     DAILY or WEEKLY per mission)
     → DailyRecord (per-mission-per-day input, snapshotted target/points, computed
                    achievement_rate/earned_score)
```

Catalog (read-only reference tree, seeded on first boot by `CatalogSeeder`): `GoalType`(7 fixed
habitus) → `StatType`(behavior categories) → `MissionDefinition`(missions), exposed via
`GET /api/catalog`.

Schema is **hand-authored SQL, not Hibernate-managed**: `backend/src/main/resources/db/schema.sql`
is canonical; `spring.jpa.hibernate.ddl-auto=validate` (Hibernate checks entities match at boot,
never mutates). Any schema change = edit `schema.sql` + `ALTER TABLE` by hand on the real DB, then
update the matching `@Entity`/`@Column` annotations to match (see "MySQL CHECK constraint gotcha"
below — hit this twice).

## Done

- Full feature-based backend package structure: `auth/`, `user/`, `goal/`(unused now, superseded
  by `project/`), `catalog/`, `project/`, `record/`, `aifeedback/`, `vision/`, `upload/`, `common/`.
- **Auth**: JWT + password (`POST /api/auth/login {nickname, password}`) — first login for a
  nickname creates the account with that password (BCrypt-hashed), later logins must match
  (401 on mismatch via `BadCredentialsException` → `@ExceptionHandler` in `AuthController`).
- **Profile photo (optional, at signup)**: `users.profile_image_url` (nullable). Login itself
  stays JSON-only (unchanged, so existing tests didn't need touching) — the frontend calls
  `login()` first, and if a photo file was picked on the login/signup form, immediately follows
  up with `PUT /api/users/{id}/profile-image` (multipart, `@PreAuthorize("#id ==
  authentication.principal")`, reuses `FileStorageClient.store` directly — same bucket as record
  photos) using the token just issued. Works for both new and existing accounts (re-uploading
  changes the photo). Verified live: uploaded a test PNG, got back
  `/api/uploads/<uuid>.png`, confirmed `GET` on that URL returns `200 image/png`.
- **Catalog**: 7 habitus × ~2-3 behavior categories × ~1-2 missions each, seeded by
  `catalog/CatalogSeeder` (idempotent, skips if `goal_types` non-empty).
- **Project selections**: `GET /api/projects/active` (auto-creates), `PUT
  /api/projects/active/selections` (bulk clear-and-rebuild of the whole goals→stats→missions
  tree in one call — mirrors the old flat stat-focus endpoint's replace semantics).
  - Custom missions supported (`missionDefinitionId` null + `customName`) — presented in the
    frontend wizard as a **sibling checklist row** ("나만의 미션") next to the catalog stats
    within a habitus, not nested inside one specific stat (user explicitly asked for this after
    an initial version nested it under each stat).
  - Custom **stats** also supported the same way (`statTypeId` null + `customStatName`) — needed
    a schema change: `user_stats.stat_type_id` made nullable + a CHECK constraint mirroring the
    `user_missions` pattern.
- **Scoring, branched by mission type** (`DailyRecordService`):
  - `DAILY` missions: that day's own record vs. its own target.
  - `WEEKLY` missions: week-to-date (Mon..date) sum of inputs vs. the weekly target — fills in
    gradually across the week rather than expecting the full weekly amount in one day's record.
  - `WeeklyReportService` uses a dedicated `getWeekTotalProgress` (NOT a naive sum of 7 daily
    `getMissionProgress` calls) so a WEEKLY mission's score counts exactly once toward the week
    total instead of being multiplied ~7x (its week-to-date score grows every day it's queried).
  - Verified end-to-end via curl against real MySQL: a "3x/week" mission recorded twice showed
    1/3 → 2/3 cumulative on successive days, and the weekly report totalScore was exactly the
    2-occurrence sum, not 7x that.
- **File storage**: renamed away from AWS-branded naming since we only ever target Railway
  Buckets (S3-*protocol*-compatible, not actually AWS) — `S3FileStorageClient` →
  `BucketFileStorageClient`, `aws.s3.*` properties → `storage.bucket.*`, env vars `AWS_S3_*` →
  `BUCKET_*`. Credentials are read explicitly via our own properties and wired into a
  `StaticCredentialsProvider`, not via the AWS SDK's implicit `AWS_ACCESS_KEY_ID` env chain.
  Also added `storage.bucket.path-style-access` (default true) since Railway Buckets
  (Tigris-backed) use **virtual-host style** URLs, not path-style like MinIO — this must be
  `false` for Railway Buckets specifically (confirmed via `railway bucket credentials`, which
  reports `"urlStyle": "virtual-host"`).
- Frontend fully rewired to match: onboarding wizard (habitus % → behavior categories → missions,
  DAILY/WEEKLY picker per mission, custom mission/stat add), record page (keyed off
  `userMissionId`), dashboard (project-based goal display, stat accumulation).
- Backend: 16/17 tests passing (pure unit + `RestTestClient` integration tests against H2),
  rewritten for the new schema shapes throughout this migration.

## MySQL CHECK constraint gotcha (hit twice, will hit again if more custom-FK columns are added)

MySQL 8.0.16+/9.x rejects a `CHECK` constraint on a column that's also part of a FK with a
`CASCADE`/`SET NULL` referential action on that same column — error 3823. Fix each time: change
that FK's `ON UPDATE`/`ON DELETE` to `RESTRICT` before adding the CHECK. Hit this for
`user_missions.mission_definition_id` (Phase 0) and `user_stats.stat_type_id` (custom-stat work).

## Deployment — LIVE (Railway + Vercel)

Goal: live deployed demo before a 5-week deadline (user is solo, no other devs). Decided to
deploy early (week 1 of 5) rather than leave it to the end, precisely because of the kind of
infra surprises documented below.

**Live URLs**:
- Frontend: https://princess-project-frontend.vercel.app
- Backend: https://backend-production-e551.up.railway.app

**Railway project**: `princess-project` (workspace `gpdbs9409's Projects`), CLI installed
(`@railway/cli`) and logged in as `gpdbs9409@naver.com`. Both `railway` and `vercel` CLIs are
authenticated on this machine — `railway <cmd>` / `vercel <cmd>` work directly, no need to
re-login.

- ✅ **MySQL** — Railway plugin added, running fine (`railway add --database mysql`). Connection
  vars: `MYSQLHOST=mysql.railway.internal` (private), public proxy
  `sakura.proxy.rlwy.net:41558` for external tools (e.g. MySQL Workbench) — user is using
  Workbench directly against this, same as they did locally.
- ✅ **Bucket** — created via `railway bucket create uploads --region sin` (Singapore, closest
  to Korea). Credentials via `railway bucket credentials --bucket uploads`. **Not yet wired
  into the backend service's env vars** — still TODO once backend is actually running (need
  `BUCKET_NAME`, `BUCKET_ENDPOINT`, `BUCKET_REGION=auto`, `BUCKET_PATH_STYLE_ACCESS=false`,
  `BUCKET_ACCESS_KEY_ID`, `BUCKET_SECRET_ACCESS_KEY`).
- ✅ **Backend service** — LIVE at `https://backend-production-e551.up.railway.app`. Verified via curl:
  login (creates account on first use) → 200 + JWT, `GET /api/catalog` → 200, `GET
  /api/projects/active` → 200. `BUCKET_*` env vars confirmed already wired on the service.
  `CORS_ALLOWED_ORIGINS` still the `localhost:5173` placeholder — update once frontend has a
  real URL. Troubleshooting timeline that got it here:
  1. Created empty service (`railway add --service backend`), set `DB_*` vars referencing
     `${{MySQL.MYSQLHOST}}` etc, `JWT_SECRET` (random, generated via `openssl rand -base64 48`),
     `CORS_ALLOWED_ORIGINS` (placeholder `http://localhost:5173`, needs updating once frontend
     is deployed).
  2. `railway up` (local directory upload of `backend/`) failed **repeatedly**, every time,
     instantly, with zero build-log output beyond `"scheduling build on Metal builder ..."`.
     Ruled out as the cause: file size (upload was only 129KB), a malformed `railway.json`
     (removed it, still failed), and a platform-wide outage (a throwaway `nginx:latest` image
     deploy to a test service succeeded fine, and the Dockerfile builds perfectly with plain
     local `docker build` — proved both Railway's deploy pipeline and our Dockerfile are fine
     in isolation). Root cause of the *local-upload* failures specifically was never
     conclusively identified — abandoned in favor of GitHub-connected deploys instead.
  3. Connected the service to GitHub (`railway service source connect --repo
     gpdbs9409/Princess_Project --branch develop --service backend`) — needed the user to
     authorize Railway's GitHub App on the repo first (a browser action only they could do;
     initially failed with "User does not have access to the repo" until they did this).
  4. Set the service's root directory to `backend` via a direct GraphQL mutation (no CLI flag
     for this exists): `serviceInstanceUpdate(serviceId, environmentId, input:
     {rootDirectory: "backend"})`.
  5. First GitHub-connected redeploy got further (status `BUILDING` then `CRASHED`, vs. instant
     `FAILED` before) but was still not using our Dockerfile — logs showed `ls: cannot access
     '*/build/libs/*jar'`, which doesn't match our exact `*-SNAPSHOT.jar` glob, proving Railway's
     **Railpack** auto-builder was generating its own Java/Gradle build plan instead of reading
     `backend/Dockerfile`. Confirmed via `serviceInstance.builder` (GraphQL) = `RAILPACK` even
     with `backend/railway.json` (`{"build":{"builder":"DOCKERFILE","dockerfilePath":"Dockerfile"}}`)
     committed in the repo — **that config-as-code file is not being picked up at all** in this
     setup, for reasons not yet understood.
  6. Fix applied: `Builder` enum only has `HEROKU|NIXPACKS|PAKETO|RAILPACK` — there's no separate
     `DOCKERFILE` builder value. Instead, explicitly set `dockerfilePath: "Dockerfile"` via the
     same `serviceInstanceUpdate` mutation (keeping `builder: RAILPACK`) — Railpack uses a
     Dockerfile when told where one is, it just wasn't auto-detecting ours from the repo file.
  7. Redeploy with the explicit `dockerfilePath` succeeded (`BUILDING`→`DEPLOYING`→`SUCCESS`),
     confirming our Dockerfile was actually used this time.
  8. Even with a `SUCCESS` deploy, the public URL returned `502 Application failed to respond`.
     `railway logs --service backend --latest -n 200` showed the real cause: Hibernate
     `Schema validation: missing table [ai_feedbacks]` (and by extension the other tables) —
     `schema.sql` had never actually been applied to Railway's MySQL.
  9. First fix attempt ran the **full** `schema.sql` (including its own
     `DROP DATABASE IF EXISTS princess_project; CREATE DATABASE princess_project; USE
     princess_project;` header) against the MySQL public proxy. This silently created the tables
     in a brand-new `princess_project` database — **not** the database the app actually connects
     to. Railway's MySQL plugin always names its database `railway` (see the `MYSQLDATABASE` env
     var / `DB_NAME=railway`), regardless of what a local script's own `CREATE DATABASE` header
     says. Caught via `SHOW TABLES` against `railway` coming back empty.
  10. Real fix: stripped the DROP/CREATE DATABASE/USE header (`tail -n +14 schema.sql`) and
      re-ran just the `CREATE TABLE` statements directly against the `railway` database. Verified
      all 10 tables now exist there, dropped the stray `princess_project` database, and ran
      `railway restart --service backend --yes`.
  11. **Confirmed live and fully working** via curl against
      `https://backend-production-e551.up.railway.app`: login → 200 + JWT (creates account on
      first use), `GET /api/catalog` → 200, `GET /api/projects/active` → 200. The 502/missing-table
      issue is resolved. (Test user created during this verification was deleted afterward to
      keep the production DB clean.)
- ✅ **Frontend** — deployed to Vercel via CLI (`vercel link` then `vercel --prod`), project
  `gpdbs9409s-projects/princess-project-frontend`, live at
  `https://princess-project-frontend.vercel.app`. `VITE_API_BASE_URL` set as a production env
  var (`vercel env add ... production`) pointing at the Railway backend URL — Vite bakes this in
  at build time, so it's baked into that deploy's bundle already.
- ✅ Backend's `CORS_ALLOWED_ORIGINS` updated to `https://princess-project-frontend.vercel.app`
  via `railway variables --service backend --set` — this alone triggers an automatic redeploy
  (no separate `railway redeploy` needed). Confirmed working with a real `OPTIONS` preflight
  curl from that origin → `access-control-allow-origin` echoes back correctly.
- ✅ Full flow re-verified end-to-end against the **live production URLs** after both pieces
  were wired together: login → `PUT /api/projects/active/selections` (goal+stat+mission pick) →
  `POST /api/records` → `GET /api/projects/active/daily` (score reflects the record) → `GET
  /api/projects/active/weekly-report` (correct day bucketing). Test account's data was deleted
  from the live DB afterward.
- ⬜ **Not verified in an actual browser** — no browser-automation tool was available in this
  environment, so the above was confirmed via direct API calls matching exactly what the
  frontend sends (checked field names against `ProjectSelectionsRequest.java`/`RecordRequest`),
  not by clicking through the deployed site itself. Worth a manual click-through pass (signup →
  wizard → record → dashboard → weekly report in an actual browser) before the demo, especially
  for anything CSS/layout/interaction-related that a curl check can't catch.
- ⬜ `JWT_SECRET` was already set to a real random value at service creation (not the dev
  default) — nothing further needed there.

**Useful commands discovered this session** (Railway CLI is quite complete — `railway --help`
covers add/service/variable/domain/bucket/logs, `railway api` gives raw GraphQL access with
`search`/`describe` for schema introspection when the CLI itself has no flag for something,
e.g. `rootDirectory` and `dockerfilePath` had no dedicated CLI flags, only reachable via
`railway api 'mutation {...}' --variables '{...}'`).

## Local dev notes

- MySQL (Homebrew) root password reset to empty to match `application.properties` defaults.
- `princess_project` DB has all 10 tables + catalog seed data; user/project/record tables get
  truncated after each manual test pass to keep local dev DB clean.
- Local `bootRun` + `npm run dev` (frontend :5173) both working and were the state used for all
  the scoring-branch/custom-mission verification above.
- Docker Desktop needed to be started (`open -a Docker`) to test-build the backend Dockerfile
  locally — wasn't running by default in this environment.

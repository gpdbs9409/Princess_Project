# Princess Project — Progress Notes

## Repo layout

- `backend/` — Spring Boot (this directory)
- `frontend/` — React + Vite + TS
- `docs/` — `API_SPEC.md` (Notion-friendly tables), `ERD.md` + `schema.dbml` (dbdiagram.io), `DEPLOYMENT.md` (Vercel + Railway)

## Done (previous pass)

- Split the original single-folder Spring Boot project into `backend/` + `frontend/`, removed the old static HTML/JS prototype in favor of a real React app (login/onboarding → today's record input → dashboard).
- Backend reorganized from layer-based (`domain/repository/service/web`) to **feature-based** packages: `auth/`, `user/`, `goal/`, `mission/`, `record/`, `aifeedback/`, `vision/`, `upload/`, `common/`, each with its own `model/repository/service/dto/controller`.
- Split `User`↔`UserGoal` into a proper 1:1 (was embedded fields on `User`).
- CORS wired for a separate frontend origin; `S3FileStorageClient` supports a custom endpoint + proxies reads through `GET /api/uploads/{key}` (needed for Railway Buckets, which are private by default, unlike public AWS S3 URLs).
- Deployment scaffolding: `backend/Dockerfile` (JDK 21), env-driven `server.port`/DB host-port-name for Railway, `frontend/vercel.json` SPA rewrites.
- Fixed a real pre-existing bug found during verification: `VisionAnalysisService`'s constructor couldn't be autowired (plain `String` params, no `@Value`), which crashed the **entire app** at startup, not just the vision feature.

## In progress — hierarchical schema migration (NOT runnable right now)

The user hand-designed a much richer MySQL schema directly (10 tables) to replace the flat MVP model:

```
User → UserProject (goalHuman/goalEnding/status; one auto-created "active" project per user for now)
     → UserGoal   (habitus/자본 selection: which of the 7 fixed goal_types + weight%)
     → UserStat   (behavior-category selection under a chosen habitus, e.g. 신체→운동/식단)
     → UserMission (from the mission_definitions catalog, or custom)
     → DailyRecord (per-mission-per-day input, with snapshotted target/points + computed
                    achievement_rate/earned_score at record time — no more materialized
                    DailyScore/DailyStatScore aggregate tables; aggregation is computed live)
```

Full plan: `/Users/kimhyeyoon/.claude/plans/eventual-wibbling-river.md`

**Status**: only Phase 0 is done —
- `backend/src/main/resources/db/schema.sql` holds the canonical DDL for all 10 tables (applied
  to the local `princess_project` MySQL DB). One fix was needed vs. the original paste: MySQL
  8.0.16+/9.x rejects `ON UPDATE CASCADE` on a FK column that's also referenced by a `CHECK`
  constraint (`fk_user_missions_definition` → changed to `ON UPDATE RESTRICT`).
- `application.properties`: `spring.jpa.hibernate.ddl-auto` switched from `update` to `validate`
  — **this SQL file is now the schema's source of truth**; Hibernate no longer auto-migrates the
  real MySQL DB (the H2 test profile is untouched, still `create-drop`).

**Not done yet** (Phases 1–4 of the plan) — because these aren't done, the app **will not boot**
against the current DB: the Java entities (`User`, old `goal`/`mission` packages, `DailyRecord`,
`DailyScore`, `DailyStatScore`, `AiFeedback`) still reflect the *old* flat schema, which no longer
exists (the old tables were dropped when `schema.sql` recreated the database). Still to build:
- `catalog/` package (replaces `mission/`): `GoalType`/`StatType`/`MissionDefinition` entities +
  `CatalogSeeder` (7 habitus × behavior categories × missions — content drafted in the plan file)
  + `GET /api/catalog` (nested tree).
- `project/` package (replaces `goal/`, absorbs old `user/` stat-focus): `UserProject`/`UserGoal`/
  `UserStat`/`UserMission` + `GET /api/projects/active` + `PUT /api/projects/active/selections`
  (bulk replace of the whole goals→stats→missions tree in one call).
- `record/` rewrite: `DailyRecord` gains snapshot fields (`BigDecimal`, matching the DDL's
  `DECIMAL` columns — not `Double`), `DailyScore`/`DailyStatScore` entities deleted, daily/weekly
  aggregation computed live from `daily_records` instead of read from a materialized table.
  `RecordRequest` takes `userMissionId`, not a flat `missionId`.
- `aifeedback/` update: add `project` FK + `feedbackType` enum, drop the `prompt` column (not in
  the new DDL).
- Delete old `goal/` and `mission/` packages entirely; trim `user/` (no more embedded
  `goal`/`statFocus`).
- Frontend rewire: onboarding wizard (habitus % → behavior categories → missions) replacing the
  old flat stat-focus screen; record screen keys off `userMissionId`; `types.ts`/`endpoints.ts`
  updated for the new shapes.

## Local dev notes

- MySQL (Homebrew) root password was reset to empty to match `application.properties` defaults
  (`DB_USERNAME=root`, `DB_PASSWORD=` empty) — standard `--skip-grant-tables` procedure, done
  because the previous root password was unknown/forgotten.
- `princess_project` DB currently has all 10 tables, all empty (no seed/user data yet).

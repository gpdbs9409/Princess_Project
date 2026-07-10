# Princess Project — Progress Notes

Full task plan lives at: `/Users/kimhyeyoon/.claude/plans/compressed-swimming-patterson.md`

**Renamed**: project directory moved from `/Users/kimhyeyoon/demo` to
`/Users/kimhyeyoon/Princess_Project`; Java package renamed
`com.example.demo` → `com.example.princessproject`; main class
`DemoApplication` → `PrincessProjectApplication`; Gradle `rootProject.name`
and `spring.application.name` updated to match. `group = 'com.example'` in
`build.gradle` was left as-is (unrelated to the "demo" name).

## Status: MVP backend + JWT + S3 upload + Weekly report — ALL DONE and verified

Everything below is built, compiles, and passes tests (`./gradlew build` — 16 tests, 0 failures):

### MVP backend
- Spring Boot 4.1.0, Java 21 toolchain (JDK 17 is **not installed** on this machine — `build.gradle`'s
  toolchain uses 21, a JDK that is installed and fully compatible).
- MySQL as the real datasource (`application.properties`), H2 in-memory for the `test` Spring
  profile (`src/test/resources/application-test.properties`) so `./gradlew test` needs no real DB.
- Domain: `User`, `UserStatFocus`, `MissionDefinition`, `DailyRecord`, `DailyStatScore`,
  `DailyScore`, `AiFeedback` (package `com.example.princessproject.domain`).
- `ScoringService` (pure, unit-tested) computes mission/stat/total scores; backend always
  computes scores, AI only narrates them (`service/ai/AiFeedbackContext` is the only data AI
  ever sees). 5 fixed MVP missions seeded on startup via `config/MissionSeeder`.
- AI feedback behind an interface (`service/ai/AiFeedbackClient`): `MockAiFeedbackClient`
  active when `openai.api.key` is empty (default), `OpenAiFeedbackClient` (GPT-4o mini via
  `RestClient`) activates once `OPENAI_API_KEY` is set.

### JWT login (Phase 1)
- `POST /api/auth/login {nickname}` → no password, issues an HS256 JWT
  (`service/auth/JwtService`) whose subject is the userId. `service/auth/JwtAuthenticationFilter`
  reads `Authorization: Bearer <token>` and sets the userId as the authenticated principal.
- `config/SecurityConfig`: stateless, CSRF disabled, static resources + `/api/auth/login`
  public, everything else under `/api/**` requires a valid token. Custom `HttpStatusEntryPoint`
  ensures **no token → 401**, **wrong user's data → 403** (Spring Security's default would
  return 403 for both, which was fixed here).
- `@PreAuthorize("#userId == authentication.principal")` (or `#request.userId()` where userId
  is in the body) on every user-scoped endpoint: `UserController`, `DailyRecordController`,
  `WeeklyReportController`.
- The old unauthenticated `POST /api/users` was removed — `/api/auth/login` replaces it.
- Frontend (`static/app.js`, `index.html`) stores the token in `localStorage` and attaches it
  as a bearer header on every API call.

### S3 photo upload (Phase 2)
- `service/storage/FileStorageClient` interface, mirroring the AI mock/real split:
  `LocalFileStorageClient` (default, saves to `./uploads/`, served at `/uploads/**` via
  `config/WebMvcConfig`) and `S3FileStorageClient` (activates once `aws.s3.bucket` is
  non-empty; credentials come from the default AWS provider chain, never hardcoded).
- `POST /api/uploads` (multipart, field `file`) → `{url}`. `today.html`'s photo field is now
  a real `<input type="file">`; on save it uploads first, then includes the returned URL in
  the `/api/records` call.
- **Note**: both the app and the test suite resolve `./uploads` relative to the process's
  working directory. Running `./gradlew test`/`bootRun` from the project root leaves stray
  files there — `uploads/` is gitignored, but you may want to `rm -rf uploads/` after a test
  run in this environment (no CI to isolate it).

### Weekly report (Phase 3)
- `GET /api/users/{userId}/weekly-report?weekStart=yyyy-MM-dd` aggregates a 7-day window into
  total score, average progress (missing days count as 0 progress, denominator is always 7),
  per-stat totals, per-mission completion counts, and a day-by-day breakdown.
- `service/WeeklyReportService.aggregate(...)` is a pure method (like `ScoringService`) that
  takes plain entity lists, not repositories — `WeeklyReportServiceTest` unit-tests it with
  hand-built entities and no DB. `getWeeklyReport(...)` does the repository lookups and calls
  `aggregate(...)`. `WeeklyReportFlowIT` verifies the actual date-range repository queries
  over real HTTP.
- No frontend page for this yet (user chose backend-only for this feature in this pass).

## Gotchas discovered while building (Spring Boot 4 / Spring Framework 7 are new-ish)
- Jackson moved to Jackson 3: use `tools.jackson.databind.ObjectMapper` /
  `tools.jackson.databind.JsonNode`, **not** `com.fasterxml.jackson.databind.*`.
  `JsonNode.asText()` is deprecated — use `asString()`.
- `TestRestTemplate` is **gone** in Spring Boot 4. Use
  `org.springframework.test.web.servlet.client.RestTestClient` instead:
  `RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build()`, then
  `.get()/.post()/.put()...uri(...).header(...)... .body(...).exchange().expectStatus().isOk().expectBody(Class)`.
  For multipart, `.body(MultiValueMap<String,Object>)` with a `ByteArrayResource` part and
  `.contentType(MediaType.MULTIPART_FORM_DATA)` works the same way it does with `RestTemplate`.
- Spring Security's default `AuthenticationEntryPoint` (when none is configured) returns 403
  for *any* denial, including "no credentials at all" — register an explicit
  `HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)` if you want conventional 401-vs-403 semantics.

## Not started / possible next steps (not requested yet)
- No frontend page for the weekly report.
- No Next.js/MariaDB migration (user explicitly deferred this — current stack stays Spring
  Boot + MySQL + static HTML/JS frontend).
- No password-based auth (JWT is nickname-only by explicit user choice).

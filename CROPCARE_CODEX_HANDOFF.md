# CropCare Android — Codex Handoff Brief

**Prepared:** 2026-11-04 (Asia/Manila)
**Purpose:** Enable a local Codex agent to inspect and repair the Android farmer app based on the team's decisions. This is a specification, not evidence that any planned changes have been applied or tested.

## Repositories and source of truth

- **Mobile:** `https://github.com/Stigma424/CropCare`, branch `functionality`. Code root: `app/src/main/java/com/example/cropcare/`, layouts `app/src/main/res/layout/`.
- **API:** `https://github.com/Stigma424/CropCare.Api`, branch `master`, ASP.NET Core 8, Firebase Admin SDK, Firestore.
- **Web:** `https://github.com/Stigma424/CropCare.Web`, branch `master`, ASP.NET Core MVC/Razor (not React). Use existing web behavior as reference, but **do not change web code in this mobile task**.
- **Paper:** `CropCare: IoT-Based Fertilization Monitoring System`, corn farmers, mobile and web access, zone/sensor monitoring, 7-in-1 soil data, notifications, sustainability scorecard, fertilizer advisory. User updated paper: **Android 8.0+**, **removed React references**.
- **IMPORTANT:** User did **not** apply earlier proposed R1 replacement code; local files / git working tree override any descriptions here. First inspect code and `git status`.

## User decisions / hard constraints

1. Farmer-facing mobile functionality ONLY. No admin dashboard, admin workflow, or administrator UI.
2. **Preserve username/password login** on mobile. Website will be migrated to username/password separately. Never silently switch mobile login to email-only. Firebase Auth uses email internally; currently Android resolves username to email via an insecure direct Firestore query. Preserve existing UX now; later design safer API-backed resolution/auth without exposing other users' emails. Treat security gap as known and track it.
3. Firebase Authentication stores passwords; **never write plaintext passwords to Firestore**, logs, source, or settings. Existing plaintext fields require a cautious cleanup strategy after backup and rules audit.
4. Keep Android Kotlin Activities/XML layouts and existing package `com.example.cropcare`; avoid Compose/unnecessary new architecture or wholesale UI redesign. Reuse existing IDs and navigation. No fake/live sensor readings; a newly added sensor is not necessarily online.
5. **Leave recommendation/fertilizer engine development to another chat.** Do not edit `CornRecommendationEngine.kt`, recommendation algorithms, agronomic rules, or backend assessment calculations unless the user later explicitly coordinates changes. Avoid showing unvalidated fertilizer rates as proven field recommendations.
6. QR generation and real ESP32 tests are **on hold** with teammate. Existing QR screen and pairing API must be preserved. Same QR should be reusable after **authorized unpairing**; current API already supports this in `DELETE /api/sensors/{sensorId}` for QR-paired sensors. Preserve safe operation and history.
7. Subscription: farmer submits GCash receipt reference for manual review; request does NOT activate Pro. Do not invent GCash payee details, PIN, OTP, price, or auto-approval. No dedicated admin page in scope.
8. Work locally. API port `5056`; Android emulator host `http://10.0.2.2:5056`; physical Android phone with `adb reverse tcp:5056 tcp:5056` uses `http://127.0.0.1:5056`. No production HTTP exposure; future IoT ingestion needs trusted HTTPS reachable to ESP32.
9. Preserve cross-platform Firestore schema, API JSON contracts, existing working screens, and existing user data. No destructive cleanup, no force pushes, no deletion of Auth users, no production Firebase rule change without an explicit migration and tests.
10. **No unverified “perfect” claims.** Make actual code edits, run available build/test commands, repair compile errors, report anything not testable (including API, SMTP, hardware).
11. No unrequested commits, pushes, or unrelated refactors. Respect existing uncommitted changes. Confirm repo and branch before editing.

## Current mobile baseline (as last audited, verify locally)

- Android AGP 8.3.2 / Kotlin 2.0.21, app `compileSdk 34`, `targetSdk 34`, `minSdk 24` (updated paper targets Android 8+, eventually `minSdk 26`). Existing Firebase Auth, Firestore, Messaging, OkHttp, Google Play Code Scanner dependencies.
- `RegisterActivity.kt` creates Firebase Auth user, directly sets Firestore `users/{uid}`, including unsafe `"password"` field.
- `LoginActivity.kt` accepts username/email, looks up `users` by `username` to resolve email, then signs in using Firebase Auth; preserves FCM registration. Must **not switch to email-only**.
- `CropCareApiClient.kt` handles profile, zones, sensor pairing/management, history. `SensorMonitoringApi.kt` handles monitoring.
- `ForgotPasswordActivity.kt`, `EmailVerificationActivity.kt`, `ResetPasswordActivity.kt` have intentionally disabled functionality while API recovery was built.
- `SubscriptionActivity.kt` only checks Firestore `isSubscribed`; payments were disabled to prevent self-activation. `activity_subscription.xml` contains a **fabricated placeholder GCash account/QR** and unsafe feature promises; remove those.
- `NotificationsActivity.kt` uses direct Firestore user notification listener and batch Clear All; API already has notification CRUD and mark-read.
- `SensorStatusActivity.kt` still directly lists user sensors from Firestore, queries API per sensor, and disables Delete All. `SensorDetailActivity.kt` blocks QR unpair. API has transactional QR release in DELETE.
- `ZoneAdapter.kt` and `ZoneManagementActivity.kt` still query Firestore sensors / `soil_data` and compute separate provisional scores. Some empty zones show `0% Poor` instead of no data. `ZoneManagementActivity` has harvest/replant state fix.
- `SensorHistoryActivity.kt` + `SensorHistoryAdapter.kt` show seven measures together in vertical cards with pagination, newest first. **Preserve**; web has graphs and filters that mobile lacks.
- `MyFirebaseMessagingService.kt`, `CropCareFcmRegistration.kt`, `NotificationHelper.kt` exist; check no duplicate client/server notifications.

## Working API endpoints to reuse (verify signatures / schema in local API source before editing)

- `POST /api/onboarding/profile` (requires Firebase ID token; body `{firstName,lastName,address,username}`; reserves username in `username_registry`, creates `users/{uid}`; expected 201). Do not have the app directly create profile or password field.
- `GET /api/account/me`, `PATCH /api/account/me` (authenticated).
- `POST /api/recovery/request` (body `{email}`; generic 202 response; SMTP, rate limits), `POST /api/recovery/verify` (`{email,code}`; returns `{resetTicket}`), `POST /api/recovery/reset` (`{email,resetTicket,newPassword}`; password >=8, ticket is one-time). Backend requires `Recovery:HashKey` and SMTP settings in secrets, not git.
- `GET /api/subscriptions/me` (returns `status` free/pending/rejected/pro, `isSubscribed`, `referenceLastFour`, `submittedAtMs`, `message`), `POST /api/subscriptions/request` (`{referenceNumber}` exactly 8–20 digits, 202 pending; never activate Pro from client).
- `GET /api/notifications?limit=100`, `POST /api/notifications/{id}/read`, `DELETE /api/notifications/{id}`, `DELETE /api/notifications`.
- `GET /api/zones`, `GET /api/zones/{id}`, `POST /api/zones`, `PATCH /api/zones/{id}`, `DELETE /api/zones/{id}`.
- `GET /api/sensors?zoneId=`, `GET /api/sensors/{id}`, `POST /api/sensors/pair`, `PATCH /api/sensors/{id}`, `DELETE /api/sensors/{id}`; old manual POST `/api/sensors` disabled. Pair QR payload: `CROPCARE1|DEVICE_ID|32_UPPERCASE_HEX_CLAIM_CODE`, validated against hashed code in `device_registry`. API transactional delete releases QR claim allowing same QR later; do not bypass this via direct Firestore deletes.
- `GET /api/sensors/{id}/monitoring` returns status `awaiting_first_reading|unknown_time|recent|stale` plus seven-parameter latest reading. `GET /api/sensors/{id}/readings?limit=20&cursor=...` returns full readings page and next cursor.
- `GET /api/zones/{zoneId}/assessment` (assessment owned by server; **do not overwrite shared recommendation engine**).
- `POST /api/ingestion/soil` for future ESP32 dedicated authenticated account; on hold for real hardware. Firestore `soil_data` uses one record per 7-parameter reading (N,P,K,pH, EC, moisture, temperature); `sensor_latest` for API-ingested devices.

## Phased implementation / milestones

### Phase 0 — Read-only audit and plan BEFORE edits

- Inspect actual working tree, active branch, Android activities/XML resources, `AndroidManifest.xml`, Gradle, existing tests, API client and web/API contracts (if repos available). Flag any existing uncommitted work.
- Produce an implementation matrix: existing behavior, gap, exact file changes, API endpoint and JSON contract, acceptance test, dependencies.
- Propose and document `docs/CROPCARE_MOBILE_REPAIR_PLAN.md` only after user approves writing files. **Do not begin implementation until user reviews the initial plan.**

### Phase R1 — Accounts, recovery, subscription (NO LOGIN UX CHANGE)

- Replace registration flow: validate 1..80 first/last, 1..250 address, username `[A-Za-z0-9._-]{3,30}`, password >=8 with confirmation; create Auth account, then authenticated API onboarding. Avoid raw Firestore profile writes and password storage. Handle partially created Auth accounts gracefully; do not delete Auth users.
- **Preserve username/password sign-in UI and behavior.** Existing `LoginActivity.kt` is not to be replaced just for style. If code changes are necessary, avoid switching to email-only.
- Enable Forgot Password email code -> verify -> reset using shared API. Keep one-time resetTicket in memory/activity navigation; do not persist in prefs or log. Enforce 6-digit code, valid email, >=8 password.
- Restore Free/Pending/Rejected/Pro subscription through API; submit only GCash reference; no self-activation and no invented payment receiver. Edit `activity_subscription.xml` to accurate feature text.
- Favor one additional `CropCareFarmerApi.kt` for R1 if it avoids altering stable existing `CropCareApiClient.kt`; reuse existing OkHttp dependencies.

### Phase R2 — Notification parity

- Use API for notification history (per-user), mark read, delete one, clear all; preserve compatible FCM reception/token registration and notification permission. Avoid duplicate notifications and avoid creating client-generated fabricated fertilizer alerts. Handle timestamps, error and empty states.

### Phase R3 — Zones, monitoring, and histories (NO RECOMMENDATION ENGINE EDITS)

- Complete zone edit (name, area) UI if missing; harvest/replant persisted in API; preserve crop age/date.
- Move repeated direct Firestore zone/sensor listing queries to API where endpoints support it; handle no sensors and no readings honestly (never show 0% Poor for absent readings).
- Preserve unified 7-in-1 scrollable history card page and pagination; add per-metric graph and date filtering analogous to web, but phone-friendly.
- Fix stale/recent/offline semantics; historical timestamp alone is not proof of online device; check Firestore composite index requirement for `soil_data(deviceId ASC, timestamp DESC)`.
- For score/assessment parity, coordinate with user before modifying overlapping recommendation modules.

### Phase R4 — QR and unpair lifecycle (UI framework only; real test on hold)

- Current Android scanner and `POST /api/sensors/pair` are already present. Leave hardware testing pending. API already supports transactional reuse after unpair. Later wire Android delete confirmation to DELETE API and remove UI block, preserving user ownership and history; avoid direct Firestore delete-all that orphans claims.

### Phase R5 — Security migration / cleanup

- Design safe **username/password** backend login flow that does not anonymously return other users' emails or require public Firestore user reads. This likely involves backend/API coordination; do not implement an unsafe username-to-email public endpoint merely to remove direct Firestore lookup. Do not silently switch to email sign-in.
- After all mobile reads/writes are migrated, tighten Firestore rules. Plan controlled removal of old `users/{uid}.password` values, backups and test accounts, and require password reset where necessary. Protect `fcmToken`, subscription status, QR claims, owner IDs, and historical readings.

### Phase R6 — Verification and paper consistency

- Keep Kotlin/Android app compilable; prefer small scoped diffs, verify IDs and types in XML before editing Activity `findViewById` casts.
- Use Windows VS Code terminal: `./gradlew.bat assembleDebug` (PowerShell `./gradlew.bat` or `.\gradlew.bat`); test `./gradlew.bat testDebugUnitTest`; Android debug install `./gradlew.bat installDebug` with ADB enabled.
- API local on port 5056. Physical phone: `adb reverse tcp:5056 tcp:5056`. Emulator: `10.0.2.2:5056`.
- Android 8.0+ target in paper. Existing `minSdk=24` should be evaluated for raising to `26` (Android 8), explaining any compatibility impact.
- Do not report live ESP32 / QR / SMTP / notification delivery success without real tests. Provide detailed later hardware test checklist.

## Success criteria

- New registration stores **no password** in Firestore; creates user and username registry using the API; username/password login remains functional.
- Recovery verifies email code and resets Auth password without insecure local bypass when SMTP configured.
- Subscription request remains Pending until trusted out-of-band verification; client cannot activate Pro.
- Notifications, zones, sensor management and seven-parameter history work via expected server contracts and show truthful empty/error states.
- Code builds. Unverified workflows documented distinctly from tested workflows.
- Each milestone produces a changed file list, validations run, issues remaining and a concise acceptance checklist.

## Extra engineering rules

- Treat GitHub as baseline, **local working tree as final source of truth**. Respect work in progress. Do not reset/force-push/overwrite other work.
- Prefer complete, production-intent Kotlin and XML files consistent with existing project; no fake stubs, silent exceptions, sample Firebase values or TODOs disguised as working features.
- API and web repositories may not be present in workspace; inspect them read-only if reachable, otherwise state exact blocked dependency and ask for checkout rather than inventing DTO contracts.
- Implement in small batches after user approves plan; run build after each batch. Do not claim tests passed unless output proves it.

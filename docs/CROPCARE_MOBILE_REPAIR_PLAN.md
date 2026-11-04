# CropCare Android repair implementation

Implemented in local branch functionality; no commit or push. The original untracked root handoff is preserved. API and web repositories are reference-only; their existing work is untouched.

## Batch summary

| Batch | Changes |
|---|---|
| R6-A | MainActivity redirects to farmer login/dashboard; graph uses existing Activity destinations |
| R1 | Shared cancellable object/array HTTP transport, API onboarding with partial-account recovery, email-code recovery, four-state manual subscription submission, password confirmation/validation |
| R2 | API notification history/read/delete/clear, visible refresh, message-ID delivery deduplication, conditional FCM token detachment at logout |
| R3 | Bounded API monitoring repository, pure-rendering zone cards, nullable readings and true zero scores, zone name/area editing, selectable replant date, seven-in-one history charts/filtering over loaded readings |
| R4 | Confirmed single-sensor API unpair, accurate qrReusable messaging, retained QR scanner and disabled bulk removal |
| R5 | Settings profile API read, debug-only local HTTP, release endpoint guard, separate backend/rules/password migration design |
| R6-B | JVM wire/state/data tests, instrumentation resource/navigation checks, verification checklist |

Minimum SDK remains 24; compile/target 34. Existing recommendation engine and SoilUtils thresholds are untouched. Existing zone formulas and recommendation call remain; missing measurements are not fabricated for calculation.

## Interfaces

CropCareHttpClient returns FarmerApiResult containing an object, array or empty body and an ApiRequestHandle. It uses Firebase ID-token bearer authentication except for recovery, posts callbacks to the main thread and suppresses delivery after cancellation. Existing CropCareApiClient methods delegate to this transport to preserve callers.

CropCareFarmerApi wraps existing onboarding, recovery, subscription and notification contracts. ZoneMonitoringRepository lists zones/sensors once per dashboard refresh, limits sensor monitoring concurrency to four and supplies nullable snapshot data to rendering code. MonitoringRequest cancels the group. Sensor history retains its API cursor and 20-record pages.

RecoverySession contains email and reset ticket only in process memory with a monotonic ten-minute local expiry. The server remains authoritative. Process death requires requesting another code. Password/code/reference inputs are not saved in Activity state.

Charts and date filters cover loaded readings only, use Asia/Manila calendar days and leave Load More available. The chart/filter panel can collapse to preserve space for cards. Missing measurements create chart gaps. Per-metric detail buttons open the same unified history screen.

## Validation status

Each implementation batch attempted assembleDebug. All attempts stopped before compilation because JAVA_HOME is unset and java is not available on PATH. JDK/Android SDK/ADB were not found in the inspected standard/user locations. No Gradle test, lint, APK or device success is claimed. See the test checklist and final source-check report for further results.

New tests are intended to cover response shape/error parsing, HTTP method/auth/body preservation, onboarding ownership decisions, input boundaries, ticket expiry, subscription submission state, delivery deduplication, missing versus zero data, many-sensor snapshots, Manila date boundaries and QR-release response handling. The instrumented test resolves Activities and inflates key layouts; it is not a substitute for end-to-end farmer workflow tests.

## Deferred dependencies

Secure username credential exchange, token API endpoints, deployed rules/index audit, legacy password removal and cross-owner history policy require separate backend/security approval. SMTP, FCM delivery, payment review, actual QR reuse and ESP32 testing require configured services/devices. Release API calls deliberately fail closed until a trusted HTTPS endpoint is configured.

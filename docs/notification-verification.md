# Sensor notification verification — November 12, 2026

The current ESP32 test writes directly to Firestore (confirmed by the user). Those writes previously bypassed API notification generation. The Android `NotificationHelper` offline method is intentionally empty; it cannot provide background alerts by itself. The API's push setting also defaulted to false.

The API now watches both API ingestion and existing direct Firestore uploads. It polls each minute. A timestamp within two minutes is recent, matching the monitoring screen; an offline notification normally appears two to three minutes after the last valid reading. Another reading generates an online event. Repeated unchanged polls do not create repeated alerts. Starting the watcher with old legacy readings establishes a silent offline baseline, rather than notifying everyone about old history.

For legacy uploads, the watcher verifies the current sensor assignment transactionally and queries date, epoch-second, and epoch-millisecond timestamps separately. It stores transition state in `sensor_presence_state/{sensorId}`. It does not create measurements, change thresholds for fertilizer advice, or treat the notification as proof of sensor calibration.

## Required for a live test

1. Run the updated ASP.NET API continuously using the existing Firebase Admin credentials. Keeping only Firebase and the mobile app running does not run this watcher. Hosting on a sleeping laptop will interrupt monitoring.
2. Ensure configuration has `Notifications:EnablePush=true` (or environment variable `Notifications__EnablePush=true`). The source default is now true. An environment override can still disable it.
3. Firestore needs the existing composite index on `soil_data`: `deviceId ASC`, `timestamp DESC`, collection scope. If missing, the API logs a failed presence check and Firestore's index creation link. The watcher continues checking other sensors.
4. The sensor document must have its real `deviceId` and the correct owner's `userId`. Readings must have that same `deviceId` and a valid timestamp. Future/unknown timestamps do not establish online status. API-paired sensors must retain a valid enabled device registry assignment.
5. Configure the Android build to reach the running API. This checkout defaults to `http://10.0.2.2:5056`, which is for the emulator. A USB-connected phone can use `http://127.0.0.1:5056` with `adb reverse tcp:5056 tcp:5056`; a remote phone requires a reachable trusted HTTPS API URL. Set `cropcare.api.baseUrl` at build time. Do not expose the development HTTP server publicly.
6. Sign in as the affected user and allow Android notifications. Refresh the notification screen and check that the push token reports registered and phone alerts are allowed. A saved old FCM token is not proof of present delivery.

## End-to-end acceptance test (not performed in this session)

- With the API running and phone online, power the sensor on and observe a fresh Firestore reading. After a poll, check `users/{uid}/notifications` for `Sensor online` and `notification_deliveries` for delivery state.
- Check both the in-app history and phone notification tray, with the app foregrounded and backgrounded. `accepted_by_fcm` confirms FCM accepted the request; it does not prove the phone displayed it.
- Power off the sensor. Wait at least three minutes. Expect one offline history record and one phone alert.
- Leave it off for another three minutes. Expect no repeated offline alert.
- Restore power. After a fresh reading and poll, expect one online alert.
- Repeat with two API host instances if deployed with multiple replicas; transactional state should prevent duplicate transition history.
- If token delivery fails, inspect states such as `invalid_token`, `no_registered_token`, `failed`, or `retry_exhausted`; sign in/refresh to register a current token before retesting.

## UI scope

The dashboard and zone cards use the supplied Figma mobile dashboard, local Figma assets, and Montserrat. Existing API data and truthful missing-data/assessment states are preserved. Figma Starter-plan MCP limits prevented fetching the remaining screens' full design context, so the remaining screens have not been claimed as redesigned or visually verified. No Android device was connected during the initial checks.

## Verification

- API: 295 tests passed after adding direct-upload presence checks and aligning the timeout.
- Fixture startup failure corrected by restoring canonical LF bytes; all three existing SHA-256 values match. `.gitattributes` preserves LF on future checkouts. Recommendation content and checksums were not changed.
- Android debug APK builds. The original unit-test run exposed an outdated assertion expecting a locally computed soil score; the test now verifies that raw zero readings are retained and that only a server assessment supplies a soil score.
- No test notification was sent to the user's friend. Live Firestore data was read only; no production rules or records were edited.

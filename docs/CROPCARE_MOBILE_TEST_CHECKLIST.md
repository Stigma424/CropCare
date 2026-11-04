# CropCare verification checklist

## Windows terminal

From the actual Android repository, configure an installed JDK 17 in JAVA_HOME and put its bin directory on PATH for that terminal. Configure an Android SDK with platform 34/build tools and a local, ignored local.properties sdk.dir as needed. Do not commit machine paths or credentials. No JDK/SDK was installed by this repair.

```powershell
java -version
.\gradlew.bat --version
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleRelease
adb devices
.\gradlew.bat connectedDebugAndroidTest
.\gradlew.bat installDebug
adb reverse tcp:5056 tcp:5056
```

Debug emulator host: http://10.0.2.2:5056. Physical phone with ADB reverse: http://127.0.0.1:5056. Release is blocked pending a trusted HTTPS API configuration.

## Automated suite, not yet executed

- [ ] Wire response objects/arrays/empty/malformed bodies and API/ProblemDetails errors.
- [ ] Bearer headers, request method, leading-zero reference JSON, cancellation and timeout.
- [ ] Partial onboarding owner/missing-profile/unavailable decisions and credential validation.
- [ ] Leading-zero recovery code, local ticket expiry, clearing/restarting recovery.
- [ ] Free/Rejected can submit; Pending/Pro/unknown cannot submit.
- [ ] Message-ID duplicates versus distinct alerts and bounded cache eviction.
- [ ] Missing metrics stay null; real zero score stays valid; 35-sensor aggregation.
- [ ] Manila midnight filtering and missing chart values.
- [ ] QR reuse true/false/conflict response interpretation.
- [ ] Registered farmer Activities and key layout inflation on device/emulator.

## End-to-end service/device tests, pending

- [ ] Existing username/password login, optional email compatibility and legacy short passwords.
- [ ] New registration has no Firestore password field; profile/registry are created through API.
- [ ] Auth succeeds/onboarding fails: same-email/password retry, username conflict, lost response and process restart; no Auth user deletion.
- [ ] Recovery SMTP delivery/spam, generic unknown-email response, resend, wrong/expired six-digit code, ticket reuse/expiry, password mismatch and new-password login.
- [ ] Subscription Free → Pending; rejected receipt resubmission; duplicate reference rejection; trusted manual Pro approval; no client activation.
- [ ] Notification history, unread/read, individual delete, Clear All above server cap, permission denial and API/network failure.
- [ ] FCM foreground/background/terminated delivery, duplicate delivery, token rotation, conditional logout detach failure and account switching.
- [ ] Android 7/8 and Android 13+ permissions; small screens, landscape, rotation, process death, back navigation and repeated refreshes.
- [ ] Zone creation/edit/harvest/replant persists; ownership rejection; deletion with sensors returns conflict; empty/no/partial/stale/unknown-time monitoring.
- [ ] More than 30 sensors, partial API failure and slow responses; old callbacks never replace refreshed data.
- [ ] Seven-in-one cards/pagination, chart metric/date, null gaps/single point, loaded-only coverage and Load More after no date matches.
- [ ] QR scanner cancel/malformed/unavailable Play services; pair/unpair/reuse original QR without deleting registry directly.
- [ ] ESP32 first reading, seven parameters, units/calibration, disconnection/staleness and history preservation.
- [ ] Resolve backend historical ownership policy before cross-farmer device-transfer tests.

The API uses a soil_data composite index on deviceId ascending and timestamp descending; verify deployed index availability without changing security rules. Do not claim hardware, SMTP, FCM or payment acceptance until exercised with authorized test accounts and real service/device evidence.

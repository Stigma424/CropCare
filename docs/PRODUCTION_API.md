# Production API integration

Shared API: https://cropcareapi-production.up.railway.app. Source branch: Production.

The existing Activities/XML UI, email sign-in, Firebase Authentication and FCM remain. Username sign-in now calls POST /api/auth/login and exchanges the returned customToken using FirebaseAuth. The API requires a reviewed username registry; if not ready it fails closed rather than publicly querying user emails.

Build with JDK 17: `gradlew.bat :app:testDebugUnitTest :app:assembleDebug`. Gradle property `cropcare.api.baseUrl` configures the endpoint. Override for emulator testing with `-Pcropcare.api.baseUrl=http://10.0.2.2:5056`. Release requests require trusted HTTPS. Production endpoint availability is recorded in the API repository docs/VERIFICATION.md.

Real-device acceptance remains required: username/email login, token expiry, account switching, QR pair/unpair, seven-measure history, disconnected probe, notification permission, FCM token rotation, foreground/background/killed-app delivery. Never embed Firebase Admin credentials in Android. Existing per-user single-token registration remains compatible; latest registered handset wins.

Recovery supports deliveryMode=firebase_link. The app shows a check-email dialog and returns to sign-in after the user opens Firebase's hosted reset link. No six-digit code is required in link mode, and account ownership is still verified through the inbox. Rebuild/install this APK before testing the new flow; old installed APKs may still enter the code screen.

The recovery APK was installed successfully on the connected phone using adb install -r. Full reset/re-login remains a user/device acceptance step. The requested Figma UI at node 130:121 remains pending: detailed design context was unavailable and Figma Starter MCP calls were exhausted. Existing UI is preserved.

Final product requirement: use the existing six-digit-code verification and new-password Activities. The API defaults to code mode through HTTPS Resend; link dialog is optional explicit link mode only. Resend API key is required to test real code delivery.

Live recommendation UI: RecommendationView shows the main action first and collapses secondary checks, sensor quality, reference material and evidence. The action button remains disabled until an assessment is ready. An opt-in LiveRecommendationTest accepts demo credentials only through instrumentation arguments and tests actual API/Firebase authentication and zone advice on a connected device. No credentials are hardcoded. Direct instrumentation was used for final acceptance so the regular APK remains installed.

Zone nutrient, moisture, temperature and EC averages retain one decimal place in the mobile presentation; small fractional readings are not rounded up to an integer. Backend scores and raw sensor records are unchanged.

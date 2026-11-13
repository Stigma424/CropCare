# Production API integration

Shared API: https://cropcareapi-production.up.railway.app. Source branch: Production.

The existing Activities/XML UI, email sign-in, Firebase Authentication and FCM remain. Username sign-in now calls POST /api/auth/login and exchanges the returned customToken using FirebaseAuth. The API requires a reviewed username registry; if not ready it fails closed rather than publicly querying user emails.

Build with JDK 17: `gradlew.bat :app:testDebugUnitTest :app:assembleDebug`. Gradle property `cropcare.api.baseUrl` configures the endpoint. Override for emulator testing with `-Pcropcare.api.baseUrl=http://10.0.2.2:5056`. Release requests require trusted HTTPS. Production endpoint availability is recorded in the API repository docs/VERIFICATION.md.

Real-device acceptance remains required: username/email login, token expiry, account switching, QR pair/unpair, seven-measure history, disconnected probe, notification permission, FCM token rotation, foreground/background/killed-app delivery. Never embed Firebase Admin credentials in Android. Existing per-user single-token registration remains compatible; latest registered handset wins.

Recovery supports deliveryMode=firebase_link. The app shows a check-email dialog and returns to sign-in after the user opens Firebase's hosted reset link. No six-digit code is required in link mode, and account ownership is still verified through the inbox. Rebuild/install this APK before testing the new flow; old installed APKs may still enter the code screen.

The recovery APK was installed successfully on the connected phone using adb install -r. Full reset/re-login remains a user/device acceptance step. The requested Figma UI at node 130:121 remains pending: detailed design context was unavailable and Figma Starter MCP calls were exhausted. Existing UI is preserved.

# CropCare mobile UI — 2026-11-13

Branch: `Production`. Design reference: https://www.figma.com/design/n4P2AUS6E4witAV4soKibE/CropCare?node-id=130-121

## Implemented

Applied the green/white visual system to the existing Kotlin/XML app, including the previously saved screenshot-guided changes. Login, registration, dashboard, zone overview, settings, and sensor cards received layout work. Other forms use the shared input, button, card, and type styles. Existing view IDs were preserved across all changed layouts.

The current Figma file was accessible during this pass. Inspected high-fidelity design context for Login, Register, Dashboard, Profile, Zone Management, and Sensor Status. Downloaded the reference password-eye icon for local Android use; existing local Figma navigation assets were reused. No temporary Figma asset URLs are used at runtime.

The bundled variable Montserrat font defaulted to Thin (weight 100) on the phone. Generated static 400/700 weights from that existing font and added a native Android font family. License: `docs/fonts/Montserrat-OFL.txt`.

## Functional behavior

Firebase authentication, API communication, ownership checks, sensor processing, and recommendation rules remain in their existing implementations. The password-eye controls now work. The dashboard ring displays the available server score; missing assessments display a dash. Demo names, dates, readings, notification counts, and portrait photos from Figma are not presented as real account data. Profile initials/icon and current account data are used instead.

Recommendation details remain expandable. Existing profile editing, subscription, sensor management, and planting-cycle actions are retained, including actions beyond those shown in the mockup. Touch targets remain at least 48dp where edited. Screens remain scrollable rather than copying Figma's fixed canvas dimensions.

## Validation

- Debug APK and instrumentation APK build successfully with JBR 17.
- All 18 existing local unit tests pass.
- Screen-loading instrumentation passes on the connected Samsung phone; expanded to include dashboard, settings, sensor status, and reusable sensor/zone cards.
- Live recommendation test passes using the user-authorized demo account and deployed API: login, recommendation loading, other-check expansion, sensor-quality detail expansion, dashboard zone loading, and settings navigation.
- Login, dashboard, zone, and recommendation screenshots were inspected on the phone; this found and resolved the Thin-font issue. The settings capture revealed excess padding on the navigation label, which was corrected. Screenshot capture now waits for UI animations to settle.
- A first live test timed out while the phone was locked. The rerun after the user unlocked the phone passed.

## Limits and continuation

This is a native adaptation, not a claim that every screen is pixel-identical to Figma. The Figma dashboard contains an empty summary card; the app retains its useful server-driven score overview. Existing recommendation details and functional additions are intentionally retained. Full large-font, small-screen, tablet, and every-form visual QA remain outstanding. This pass does not verify new ESP32 readings or firmware.

Canonical working clone: `C:/Users/JD/Documents/ChatGPT/CropCare/production-mobile`. User Android Studio checkout: `D:/Android Projects/CropCare`; preserve its pre-existing `.idea/misc.xml` edit when synchronizing. No demo credentials, Firebase keys, or service-account material belong in source control.

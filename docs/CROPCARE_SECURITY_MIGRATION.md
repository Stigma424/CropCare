# Security migration — Android repair

No API/web code, deployed rules, accounts or Firestore records were migrated by this change.

## Remaining direct access

| Caller | Access | Required follow-up |
|---|---|---|
| LoginActivity | Pre-authentication users query by username, reading email | Replace with reviewed backend credential exchange before restricting public users queries |
| CropCareFcmRegistration | Own users/{uid}.fcmToken update and matching-token transactional removal | Add authenticated registration/removal API; preserve conditional removal |

Registration, subscriptions, notification history/mutations, zone/sensor lists, profile display and latest monitoring now use API endpoints. Firebase Authentication and Messaging remain client integrations. EmailHelper is legacy unused code; do not reconnect it. NotificationHelper remains disabled.

## Proposed backend credential exchange (separate approval)

Add POST /api/auth/username accepting username/password over trusted HTTPS. Resolve the account email internally, verify credentials through Firebase password authentication, and return a Firebase custom token for Android signInWithCustomToken. Do not return email, password, a public lookup response or raw Firebase error details. Do not log bodies or tokens. Apply per-IP and per-account throttling with generic failures; review account enumeration timing and abuse controls. Preserve existing email login as optional compatibility.

Before implementation inventory username casing, duplicates and legacy registry gaps using restricted tooling. Resolve ambiguous accounts through an approved support process; do not silently pick an arbitrary normalized match. Verify the existing Firebase session and API ownership contracts still hold for custom-token sign-in. Existing passwords are checked by Firebase; never compare against Firestore password fields.

## FCM API migration (separate approval)

Add authenticated token registration/removal scoped to the ID-token owner. Removal must be conditional on the supplied token matching the stored token. Review multi-device storage and a stable server event/history ID separately. Current Android delivery deduplication uses a bounded process-memory message-ID cache; it does not merge distinct backend alerts or guarantee suppression after process death. Failed logout token detachment is reported to the farmer.

## Legacy passwords and rules

Deployed rules, index configuration, credentials and production data were not accessible through this source audit. No live password inventory or cleanup was performed. Use an authorized restricted admin environment to count documents containing users.password without printing values. Produce a dry-run UID inventory in restricted storage; never commit it.

Before cleanup obtain a restricted encrypted backup under the project's retention policy, audit every mobile/web/device direct operation, and exercise rules in an emulator. Define a password-reset policy for accounts affected by historical exposure. Remove only the password field in bounded resumable batches after explicit approval; never delete users, username reservations, Auth users, device claims or historical soil records. Verify counts and audit records without logging values. Do not restore exposed passwords as a rollback strategy.

Rules changes must protect subscription status, owner identifiers, claim registry, token ownership and measurements, and follow migration of legacy login/token calls. No production rules deployment belongs to this Android change.

## Historical ownership

The current history API authorizes the sensor registration then reads soil_data by deviceId. After device transfer, prior-owner history may be returned. Establish the intended ownership/time-boundary policy and implement it server-side before cross-farmer transfer acceptance testing. Android cannot enforce security by hiding returned records.

## Build variants

Debug permits local HTTP for port 5056 and ADB reverse. Main/release denies cleartext, and the shared client refuses to use the local endpoint in release. No production HTTPS URL was invented. Configure and review a trusted production endpoint before releasing a functioning production build.

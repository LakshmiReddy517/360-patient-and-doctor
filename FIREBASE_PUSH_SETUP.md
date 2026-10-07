# Firebase Push Notifications — Activation Guide

The entire FCM push pipeline is already built and wired. To go live, you only need to
create a Firebase project and drop in **three** files. Until then, everything runs fine —
notifications are still recorded and shown in‑app; only device push is dormant.

## What's already done (no action needed)
- **Backend** sends an FCM push on **every case event** (via `NotificationService`) and on **every
  new agent job** (via `AssignmentService`). It stays gracefully disabled until credentials exist.
- **Both apps** include the Firebase SDK, a shared `PcareMessagingService`, register the device's
  FCM token on login (`POST /api/v1/device-tokens`), ask for the notification permission, and show
  notifications when a push arrives.

## Activate push (one‑time, ~10 min)

1. **Create a Firebase project** at https://console.firebase.google.com → *Add project*.

2. **Add two Android apps** in the project (Project settings → *Your apps* → Add app → Android):
   - Package name **`com.pcare.patient`** → download its `google-services.json`
   - Package name **`com.pcare.agent`** → download its `google-services.json`

3. **Replace the placeholders** with the real files:
   - `android/patient/google-services.json`  ← patient app's file
   - `android/agent/google-services.json`    ← agent app's file
   Then rebuild/reinstall the apps.

4. **Backend service account** (Project settings → *Service accounts* → *Generate new private key*)
   → download the JSON, then point the backend at it (either way works):
   - Env var: `FIREBASE_CREDENTIALS=C:\path\to\service-account.json`, **or**
   - `application.yml`: `app.firebase.credentials-path: C:/path/to/service-account.json`
   Restart the backend. You'll see `Firebase push ENABLED` in the log instead of `DISABLED`.

That's it. After step 4, logging into either app registers the device, and every case event /
new job delivers a real push — even when the app is closed.

## Notes
- Keep the service‑account JSON **out of git** (it's a secret). The placeholder
  `google-services.json` files are safe to commit; replace them locally with the real ones.
- Push respects the patient's **notification preferences** (the Profile → Notification preferences
  screen) and the EMERGENCY‑always rule, same as the in‑app notification engine.

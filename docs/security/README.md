# Security

SmritiSaathi processes identity data, health context, voice recordings,
memories, family information, reminder schedules, and cognitive performance.
This document describes the controls currently implemented and the work needed
before a production deployment.

## Current controls

### Caregiver authentication

Caregivers authenticate with Firebase Authentication using email/password or
Google Sign-In. Firestore patient documents require an authenticated account
whose UID matches the document's `caregiverId` for normal read, update, and
delete operations.

### Patient authentication

Patient passwords are never stored as plain text. A random 16-byte salt and a
PBKDF2-HMAC-SHA256 verifier with 120,000 iterations are stored in Room. Password
comparison uses `MessageDigest.isEqual`.

Patient authentication is local rather than Firebase-backed. A patient can
only sign in on a device that already contains both the patient profile and its
credential record.

### Local encryption and backup

Room uses SQLCipher. Android cloud backup rules exclude the Room database and
shared preferences. The app also stores voice recordings and selected photos
in app-managed or provider-managed locations; these require a separate privacy
and retention review.

### Firestore access

Patient documents and username reservations enforce caregiver ownership.
Social profiles expose only a matching-safe projection containing patient ID,
display name, profile image URL, and interests.

## Known risks

### Hardcoded database key

The SQLCipher passphrase is the constant `super_secret_key_123`. Anyone who can
inspect the APK can recover it, which substantially weakens encryption at rest.
Generate a random key per installation and protect it with Android Keystore.
Migration must preserve access to existing encrypted databases.

### Demo caregiver PIN

The caregiver PIN is hardcoded and displayed as `1234`. The normal caregiver
flow can also navigate directly to the dashboard without passing through this
screen. The PIN currently provides no meaningful access control. Replace it
with Firebase reauthentication, device credentials, or a securely stored,
rate-limited caregiver secret.

### Over-broad game-session rules

The current Firestore rule allows every authenticated user to read and write
every document in `game_sessions`. Session documents must be restricted to the
caregiver who owns the referenced patient. Validate allowed fields and prevent
clients from changing ownership identifiers.

### Public social profiles

`social_profiles` permits unauthenticated reads. Although the projection is
limited, names, photos, interests, and stable IDs are still personal data.
Require authentication or use a mediated matching service with consent,
blocking, deletion, and visibility controls.

### Client-side service credentials

Gemini and Bhashini credentials are compiled into `BuildConfig`. Secrets in a
mobile binary can be extracted. Production traffic should go through a backend
that authenticates the caller, restricts usage, applies quotas, and keeps
provider credentials server-side.

### Release configuration

The release build is signed with the Android debug signing key, and code/resource
shrinking is disabled. Configure a protected production keystore, remove debug
signing from the release variant, and enable appropriate R8 rules before public
distribution.

### Destructive database fallback

Room is configured with `fallbackToDestructiveMigration()`. An unsupported
schema upgrade can erase patient data and locally stored credentials. Add and
test an explicit migration for every released schema change.

### Session handling

The patient role and ID are stored in DataStore and restored without asking for
the password again. Define an explicit session timeout or caregiver-controlled
lock policy for shared devices. DataStore preferences are excluded from backup,
but they are not separately encrypted.

### Logging and external processing

Several failure paths log exception details. Ensure production logs never
contain transcripts, patient fields, provider responses, credentials, or
recording paths. Establish consent and retention rules for information sent to
Gemini, Bhashini, Firebase, and Android speech providers.

## Firestore hardening checklist

- Bind each game session to a patient owned by `request.auth.uid`.
- Validate document schemas with `keys().hasOnly(...)` and field types.
- Require authentication for matching data unless public discovery is an
  explicit product decision.
- Test rules with the Firebase Emulator Suite for multiple caregivers.
- Add App Check as a defense-in-depth control.
- Review delete behavior for patients, sessions, usernames, and social profiles.

## Mobile release checklist

- Move service calls that need secrets behind an authenticated backend.
- Generate the SQLCipher key through Android Keystore.
- Replace or remove the demo PIN.
- Configure a release keystore outside source control.
- Enable shrinking and verify required reflection-based libraries.
- Add dependency, static-analysis, and secret-scanning checks in CI.
- Create a privacy notice, consent flow, retention schedule, and account/data
  deletion path appropriate for health-related and elder-care data.
- Complete a threat model covering a lost device, malicious caregiver account,
  network interception, prompt injection, and unauthorized patient matching.

SmritiSaathi's cognitive scores and recommendations are assistive signals, not
medical diagnoses. The UI and exported summaries should state that boundary
clearly.


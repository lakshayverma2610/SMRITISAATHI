# Offline support

SmritiSaathi is designed to keep its core patient experience usable when the
device has an intermittent or unavailable network connection. Room is the main
runtime data source, and network-backed operations degrade independently.

## Offline capability matrix

| Capability | Offline behavior |
| --- | --- |
| Open an existing patient session | Works while the local session and patient record exist |
| Patient sign-in | Works only for credentials already stored on the device |
| Caregiver sign-in/account creation | Requires Firebase connectivity unless Firebase retains a valid session |
| Patient profile and home screen | Reads from Room |
| Dedicated cognitive games | Work locally and save sessions to Room |
| Generic games | Built-in questions work; personalization uses local profile and memories |
| Game recommendations and scoring | Computed locally from Room sessions |
| Reminders and notifications | Scheduled locally with AlarmManager |
| Reminder voice playback | Attempts Bhashini, then can use device TTS fallback |
| Memory gallery and family circle | Local Room data, photos, recordings, and device TTS |
| Life-story capture | Answers are saved locally; question quality depends on available AI engine |
| Voice companion | Full local generation requires the separately packaged Gemma model |
| Patient matching | Falls back to other patient profiles already stored on the same device |
| Cloud synchronization | Deferred until connectivity returns |

## Local-first data flow

Game sessions are written to Room immediately. Each new session starts as
unsynchronized. `SyncWorker` runs periodically under a connected-network
constraint and uploads unsynchronized sessions to Firestore. Successful uploads
mark their local records as synchronized.

Patient changes are also saved locally, but profile synchronization is attempted
inline rather than through a durable pending-operation queue. A failed profile
sync is logged and is not guaranteed to retry automatically.

Reminders, life-story memories, family members, voice recordings, and patient
credentials have no cloud synchronization in the current implementation.

## Firestore offline cache

Firestore persistent local caching is enabled with a 100 MB cache. This helps
Firebase serve recently accessed documents during temporary outages. It does
not replace Room and cannot make documents available if they were never fetched
on that installation.

## Background synchronization

`SyncWorker` is registered as unique periodic work every 30 minutes with a
connected-network constraint and exponential retry. It performs two tasks:

1. upload unsynchronized game sessions; and
2. prefetch generated Word Association and Daily Routine content when the
   unused local cache contains fewer than 20 items.

An immediate one-time sync can also be enqueued. WorkManager controls the exact
execution time according to Android background limits.

## Offline AI behavior

The full offline companion requires a compatible
`gemma-3-1b-it.task` model. The model is not committed to the repository. A
distributor must accept its license and package it under `app/src/main/assets`,
or a development device must provide the path through `LITERT_MODEL_PATH`.

When the model is unavailable and Gemini cannot be reached, the deterministic
fallback can:

- save a life-story response using the original transcript;
- recognize a few English/Hinglish skip and mood keywords; and
- return a general companionship response.

It cannot provide broad question answering or reliably parse arbitrary reminder
phrasing. The UI should avoid presenting this fallback as equivalent to the
generative engines.

## Reminder resilience

AlarmManager schedules one alarm per selected repeat day. After an alarm fires,
the receiver displays a notification, enqueues voice output, and rearms the
weekly occurrence. `BootReceiver` restores reminders after `BOOT_COMPLETED`
because AlarmManager registrations do not survive a restart.

Android 12 and later may require the user to grant exact-alarm access. Android
13 and later requires notification permission. Microphone permission is needed
for speech and family voice-note recording.

## Current limitations

- Creating a new patient requires Firestore because username reservation and
  patient creation are transactional cloud operations.
- A patient cannot recover or recreate local credentials without caregiver
  access on a device that already owns the profile.
- Patient edits have no durable retry queue if the immediate Firestore write
  fails.
- Local-only memories and recordings are lost when app data is cleared or the
  device is replaced.
- The startup `ModelDownloadManager` references a placeholder URL for a legacy
  adaptive-difficulty model and does not provision the Gemma task bundle.
- Connection state indicates availability, not whether Gemini or Bhashini is
  healthy or correctly configured.

## Recommended improvements

- Add an outbox table for retryable profile and domain-data mutations.
- Define conflict resolution and server timestamps for multi-device updates.
- Encrypt and synchronize opted-in life-story data and recordings through a
  caregiver-owned account.
- Provide a clear offline indicator and describe which assistant capabilities
  remain available.
- Test airplane mode, process death, reboot recovery, clock changes, battery
  restrictions, and reconnect synchronization on supported Android versions.


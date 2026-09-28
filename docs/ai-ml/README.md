# AI and machine learning

SmritiSaathi uses AI for voice-companion intent handling, personalized
life-story conversations, game content, adaptive difficulty, therapeutic
responses, and caregiver-facing cognitive summaries.

## Hybrid companion engine

All companion requests go through the `AiIntentEngine` interface. The
`HybridAiEngine` selects an implementation in this order:

1. Use the on-device LiteRT/Gemma engine when a compatible model is installed.
2. Use Gemini 2.5 Flash if local inference fails or no model is available.
3. Use deterministic fallback responses when neither generative engine returns
   a result.

Both generative engines are prompted to return a JSON object. `AiResponseParser`
extracts that object and converts it into an `AiIntentResult`. If the model
returns plain text or invalid JSON, the parser infers a limited intent from the
patient's input and preserves the response as conversational text.

Supported intents include reminder creation, reminder lookup, memory capture,
life-story answers, game navigation, anxiety support, emergency reassurance,
confirmation, skipping questions, and general conversation.

## Cloud inference

`GeminiAiEngine` uses the `gemini-2.5-flash` model. Its API key is supplied by
`GEMINI_API_KEY` in the root `local.properties` file and is compiled into
`BuildConfig`.

```properties
GEMINI_API_KEY=your_key
```

If the property is missing, the build uses `MOCK_KEY_FOR_NOW`. The APK will
still build, but Gemini calls will fail and the engine will fall through to its
offline response path.

The cloud engine receives recent chat history and selected patient context.
Life-story prompts may include profile fields, family member names and
relationships, previously recorded memories, and questions already asked in
the current session. Medical records are not intentionally inserted into the
matching profile, but patient context sent to generative services should still
be treated as sensitive health-related data.

## On-device inference

The local engine uses MediaPipe LLM Inference backed by LiteRT. It expects a
compatible 4-bit Gemma 3 1B task bundle named:

```text
gemma-3-1b-it.task
```

For production packaging, place the licensed model at
`app/src/main/assets/gemma-3-1b-it.task`. At first use it is copied to private
app storage. Development builds can set `LITERT_MODEL_PATH` in
`local.properties` to a model already present on the Android test device.

The model is deliberately absent from the repository because of its size and
license requirements. Without it, the app uses Gemini and deterministic
fallbacks. The patient-facing manual model import method is disabled.

## Life-story generation

Life-story mode loads the patient profile, family circle, and known memory
nodes. It requests one short reminiscence question and rejects exact duplicates
of previously answered or session-asked prompts. A response can contain:

- a concise memory extracted from the patient's answer;
- a warm acknowledgement;
- a related follow-up question; and
- an optional memory domain and stable key.

The answer is saved to Room as a `LifeMemoryNode` with a patient-voice source
and patient-verified status. If extraction fails, the original transcript is
saved so that the patient's answer is not lost.

## Adaptive difficulty and scoring

`AdaptiveDifficultyEngine` recommends a game and difficulty level from recent
sessions and the configured cognitive stage. It also determines parameters
such as memory grid size, sequence length, and pattern-choice count.

`CognitiveScoreCalculator` derives an overall score, weekly accuracy, per-game
breakdown, trend direction, and alert messages. These values are application
analytics and should not be interpreted as a clinical diagnosis.

## Generated game content

`GeminiContentGenerator` produces Word Association pairs and Daily Routine
sequences. `SyncWorker` periodically fills the Room content cache while a
network connection is available. Dedicated game ViewModels can therefore use
cached generated content when the patient later plays offline.

## Bhashini and device speech services

Bhashini exposes ASR, translation, and TTS operations configured with
`BHASHINI_USER_ID` and `BHASHINI_API_KEY` in `local.properties`. Current usage
is limited:

- voice reminders request Bhashini TTS with Assamese as the fixed language;
- Word Association uses Bhashini translation support; and
- the main companion uses Android speech recognition and Android TextToSpeech.

The companion tries Hindi TTS first and falls back to Indian English. It does
not yet consistently select the language stored in the patient profile.

## Failure behavior

AI operations use timeouts and `runCatching` so network or inference failures
do not crash the conversation. Local inference is allowed up to 45 seconds and
Gemini up to 25 seconds. The final fallback supports basic life-story capture
and companionship but cannot reliably extract complex reminders or answer
general-information questions.

## Testing and evaluation gaps

Four unit tests cover JSON and fallback behavior in `AiResponseParser`. There
are no automated evaluations for prompt safety, hallucinations, multilingual
quality, reminder extraction, duplicate-question avoidance, cognitive scoring,
or model parity between Gemini and Gemma.


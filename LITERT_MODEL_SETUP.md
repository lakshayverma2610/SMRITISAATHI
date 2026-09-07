# Internal LiteRT model packaging

Saathi uses Google AI Edge MediaPipe LLM Inference, backed by LiteRT. The old
keyword/heuristic intent model and vocabulary have been removed.

For an offline-capable release, the build pipeline must place a compatible
4-bit Gemma 3 1B task bundle at:

`app/src/main/assets/gemma-3-1b-it.task`

On first use, the app provisions this bundled asset into private app storage
and loads it locally. There is intentionally no patient-facing import or setup
screen. Development builds may instead set `LITERT_MODEL_PATH` to a model path
that exists on the Android test device. If no packaged model is present, the
online Gemini provider remains the graceful fallback.

The Gemma binary is not committed to source control because its license must be
accepted by the distributor and the quantized package is hundreds of megabytes.

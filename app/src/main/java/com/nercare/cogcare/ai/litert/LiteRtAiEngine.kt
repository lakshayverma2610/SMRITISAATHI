package com.nercare.cogcare.ai.litert

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.nercare.cogcare.BuildConfig
import com.nercare.cogcare.ai.AiIntentEngine
import com.nercare.cogcare.ai.AiIntentResult
import com.nercare.cogcare.ai.AiResponseParser
import com.nercare.cogcare.domain.model.LifeMemoryNode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device generative engine backed by Google AI Edge LiteRT.
 *
 * Gemma is intentionally stored outside the APK because the quantized model is
 * hundreds of megabytes. Put `gemma-3-1b-it.task` in the app's external
 * `models` directory, or set LITERT_MODEL_PATH in local.properties.
 */
@Singleton
class LiteRtAiEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : AiIntentEngine {
    private val initMutex = Mutex()
    @Volatile private var inference: LlmInference? = null
    @Volatile private var initializationAttempted = false

    val isModelInstalled: Boolean
        get() = resolveModelFile() != null || hasBundledModel()

    @Deprecated("Models are provisioned internally; no user-facing import is supported")
    suspend fun installModel(uri: Uri): Unit = error("Manual model installation is disabled")

    override suspend fun process(
        input: String,
        isAwaitingQuestionAnswer: Boolean,
        chatHistory: List<Pair<String, String>>,
        patient: com.nercare.cogcare.domain.model.Patient?,
        activeQuestion: String?
    ): AiIntentResult = withContext(Dispatchers.Default) {
        val prompt = buildAssistantPrompt(input, isAwaitingQuestionAnswer, chatHistory)
        AiResponseParser.parse(requireInference().generateResponse(prompt), input)
    }

    override suspend fun generatePersonalizedStarterQuestion(
        knownMemories: List<LifeMemoryNode>,
        patient: com.nercare.cogcare.domain.model.Patient?,
        familyMembers: List<com.nercare.cogcare.domain.model.FamilyMember>,
        sessionAskedPrompts: Set<String>
    ): String = withContext(Dispatchers.Default) {
        val contextText = knownMemories.sortedBy { if (it.source.name.startsWith("PATIENT")) 0 else 1 }.take(30).joinToString("\n") {
            "- source=${it.source}: ${it.questionPrompt}: ${it.value}"
        }.ifBlank { "No answers have been recorded yet." }
        val prompt = """
            You are Saathi, a calm companion helping an older adult record their life story.
            Already recorded:
            $contextText

            Ask exactly one short, gentle, open-ended question. First follow up naturally on patient-provided memories, then use caregiver context. If there is no context, start with a simple introduction or present-day preference.
            Do not repeat an already recorded question or topic. Return only the question.
        """.trimIndent()
        requireInference().generateResponse(prompt).trim().ifBlank { error("The on-device model returned an empty question") }
    }

    private suspend fun requireInference(): LlmInference {
        inference?.let { return it }
        return initMutex.withLock {
            inference?.let { return@withLock it }
            val model = resolveModelFile() ?: extractBundledModel()
                ?: throw IllegalStateException("LiteRT Gemma model is not installed")
            initializationAttempted = true
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(model.absolutePath)
                .setMaxTokens(1024)
                .setMaxTopK(64)
                .build()
            LlmInference.createFromOptions(context, options).also {
                inference = it
                Log.i(TAG, "Loaded LiteRT model from ${model.absolutePath}")
            }
        }
    }

    private fun resolveModelFile(): File? {
        val configured = BuildConfig.LITERT_MODEL_PATH.trim()
        val candidates = buildList {
            if (configured.isNotEmpty()) add(File(configured))
            context.getExternalFilesDir(null)?.let { add(File(it, "models/$MODEL_FILE")) }
            add(File(context.filesDir, "models/$MODEL_FILE"))
        }
        return candidates.firstOrNull { it.isFile && it.length() > 0L }
    }

    private fun hasBundledModel(): Boolean = runCatching {
        context.assets.list("")?.contains(MODEL_FILE) == true
    }.getOrDefault(false)

    private fun extractBundledModel(): File? {
        if (!hasBundledModel()) return null
        val modelsDir = File(context.filesDir, "models").apply { mkdirs() }
        val target = File(modelsDir, MODEL_FILE)
        if (target.isFile && target.length() > 0) return target
        val temporary = File(modelsDir, "$MODEL_FILE.copying")
        context.assets.open(MODEL_FILE).use { input ->
            temporary.outputStream().use { output -> input.copyTo(output) }
        }
        check(temporary.renameTo(target)) { "Could not provision bundled LiteRT model" }
        return target
    }

    private fun buildAssistantPrompt(
        input: String,
        isLifeStoryAnswer: Boolean,
        chatHistory: List<Pair<String, String>>
    ): String {
        val history = chatHistory.takeLast(8).joinToString("\n") { "${it.first}: ${it.second}" }
        val modeInstruction = if (isLifeStoryAnswer) {
            "The patient is answering a life-story question. Extract the memory and suggest one short follow-up question on a new detail."
        } else {
            "First fulfil the patient's request. Do not ask a questionnaire or life-story question. Do not add a wellness question; the app will offer that only after the task is complete."
        }
        return """
            You are Saathi, a concise, reassuring assistant for an older adult with cognitive impairment.
            $modeInstruction
            Never claim that an external action succeeded unless the intent JSON requests that action.
            Use 24-hour time. For PLAY_GAME targetGameRoute must be games. Keep assistantResponse under 45 words.

            Recent conversation:
            ${history.ifBlank { "None" }}
            Patient: $input

            Return only valid JSON:
            {
              "intent":"SET_REMINDER|REMEMBER_THIS|TELL_ME_ABOUT|FEELING_ANXIOUS|PLAY_GAME|CHECK_REMINDERS|CONFIRM_YES|CONFIRM_NO|SKIP_QUESTION|COMPANION_CHITCHAT|ANSWER_QUESTION|CALM_ACKNOWLEDGEMENT|EMERGENCY_SOS|UNKNOWN",
              "confidence":0.0,
              "assistantResponse":"direct helpful response",
              "reassuranceMessage":"optional calming response",
              "generatedFollowUpQuestion":"only in life-story mode",
              "extractedMemory":"optional concise memory",
              "memoryDomain":"optional category",
              "memoryKey":"optional stable key",
              "targetGameRoute":"optional",
              "queryTarget":"optional",
              "parsedReminder":{"title":"title","hour":8,"minute":30}
            }
        """.trimIndent()
    }

    companion object {
        private const val TAG = "LiteRtAiEngine"
        private const val MODEL_FILE = "gemma-3-1b-it.task"
    }
}

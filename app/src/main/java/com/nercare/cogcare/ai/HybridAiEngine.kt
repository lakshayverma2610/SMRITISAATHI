package com.nercare.cogcare.ai

import com.nercare.cogcare.ai.cloud.GeminiAiEngine
import com.nercare.cogcare.ai.litert.LiteRtAiEngine
import com.nercare.cogcare.domain.model.FamilyMember
import com.nercare.cogcare.domain.model.LifeMemoryNode
import com.nercare.cogcare.domain.model.Patient
import com.nercare.cogcare.util.NetworkMonitor
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HybridAiEngine @Inject constructor(
    private val liteRtAiEngine: LiteRtAiEngine,
    private val geminiAiEngine: GeminiAiEngine,
    private val networkMonitor: NetworkMonitor
) : AiIntentEngine {

    val isOnDeviceModelInstalled: Boolean
        get() = liteRtAiEngine.isModelInstalled

    override suspend fun process(
        input: String,
        isAwaitingQuestionAnswer: Boolean,
        chatHistory: List<Pair<String, String>>,
        patient: Patient?,
        activeQuestion: String?
    ): AiIntentResult {
        // 1. Try on-device model if installed
        if (liteRtAiEngine.isModelInstalled) {
            runCatching {
                withTimeoutOrNull(45_000) {
                    liteRtAiEngine.process(input, isAwaitingQuestionAnswer, chatHistory)
                }
            }.getOrNull()?.let { return it }
        }

        // 2. Try Gemini 2.5 Flash cloud engine (primary intelligent engine)
        val cloudResult = runCatching {
            withTimeoutOrNull(25_000) {
                geminiAiEngine.process(input, isAwaitingQuestionAnswer, chatHistory, patient, activeQuestion)
            }
        }.getOrNull()
        if (cloudResult != null) return cloudResult

        // 3. Graceful offline fallback
        val clean = input.lowercase().trim()
        if (isAwaitingQuestionAnswer) {
            if (clean in listOf("skip", "next", "pass", "chodo", "aage badho", "no")) {
                return AiIntentResult(
                    intent = NluIntent.SKIP_QUESTION,
                    confidence = 0.95f,
                    rawText = input,
                    cleanText = clean,
                    assistantResponse = "No problem at all. Let's explore another question."
                )
            }

            val warmResponse = when {
                clean.contains("happy") || clean.contains("khush") || clean.contains("good") || clean.contains("great") || clean.contains("nice") || clean.contains("fine") ->
                    "I'm so glad to hear you're feeling happy! Your positivity and good spirits bring a warm smile to my day."
                clean.contains("sad") || clean.contains("dard") || clean.contains("tired") || clean.contains("upset") || clean.contains("worried") ->
                    "I hear you, and I am right here by your side. Take a slow, gentle breath; you are safe and cherished."
                clean.contains("food") || clean.contains("chai") || clean.contains("tea") || clean.contains("roti") || clean.contains("sweet") || clean.contains("kheer") || clean.contains("cook") ->
                    "Mmm, that sounds absolutely wonderful! Few things comfort the soul like a delicious home-cooked favorite."
                clean.contains("family") || clean.contains("son") || clean.contains("daughter") || clean.contains("wife") || clean.contains("husband") || clean.contains("child") || clean.contains("beta") || clean.contains("beti") ->
                    "What a precious and heartwarming memory. Family holds the most tender place in our lives."
                clean.contains("job") || clean.contains("work") || clean.contains("teacher") || clean.contains("doctor") || clean.contains("office") || clean.contains("service") || clean.contains("farm") ->
                    "That was such dedicated and respectable work. You should be very proud of everything you accomplished."
                else ->
                    "Thank you so much for sharing that with me! It is truly special getting to know you better."
            }

            return AiIntentResult(
                intent = NluIntent.ANSWER_QUESTION,
                confidence = 0.90f,
                rawText = input,
                cleanText = clean,
                extractedMemory = input.trim(),
                assistantResponse = warmResponse
            )
        }

        return AiIntentResult(
            intent = NluIntent.COMPANION_CHITCHAT,
            confidence = 0.8f,
            rawText = input,
            cleanText = clean,
            assistantResponse = "I'm right here with you! Tell me what is on your mind."
        )
    }

    override suspend fun generatePersonalizedStarterQuestion(
        knownMemories: List<LifeMemoryNode>,
        patient: Patient?,
        familyMembers: List<FamilyMember>,
        sessionAskedPrompts: Set<String>
    ): String {
        // 1. Try on-device LiteRT model if installed
        if (liteRtAiEngine.isModelInstalled) {
            val local = runCatching {
                liteRtAiEngine.generatePersonalizedStarterQuestion(knownMemories)
            }.getOrNull()
            if (!local.isNullOrBlank() && !isDuplicate(local, knownMemories, sessionAskedPrompts)) {
                return local
            }
        }

        // 2. Try Gemini 2.5 Flash with full patient profile, previous sessions, and caregiver data
        val cloud = runCatching {
            geminiAiEngine.generatePersonalizedStarterQuestion(knownMemories, patient, familyMembers, sessionAskedPrompts)
        }.getOrNull()
        if (!cloud.isNullOrBlank() && !isDuplicate(cloud, knownMemories, sessionAskedPrompts)) {
            return cloud
        }

        // 3. Offline clinical reminiscence fallback
        return com.nercare.cogcare.domain.model.ReminiscenceQuestionCatalog
            .getNextUnansweredQuestion(knownMemories, sessionAskedPrompts).prompt
    }

    private fun isDuplicate(question: String, memories: List<LifeMemoryNode>, sessionAskedPrompts: Set<String>): Boolean {
        fun normalize(value: String) = value.lowercase().replace(Regex("[^a-z0-9 ]"), "").replace(Regex("\\s+"), " ").trim()
        val candidate = normalize(question)
        if (sessionAskedPrompts.any { normalize(it) == candidate }) return true
        return memories.any { normalize(it.questionPrompt) == candidate }
    }
}

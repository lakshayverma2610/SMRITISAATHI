package com.nercare.cogcare.ai.cloud

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.nercare.cogcare.ai.AiIntentEngine
import com.nercare.cogcare.ai.AiIntentResult
import com.nercare.cogcare.ai.AiResponseParser
import com.nercare.cogcare.ai.NluIntent
import com.nercare.cogcare.domain.model.FamilyMember
import com.nercare.cogcare.domain.model.LifeMemoryNode
import com.nercare.cogcare.domain.model.Patient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiAiEngine @Inject constructor(
    private val generativeModel: GenerativeModel
) : AiIntentEngine {

    override suspend fun process(
        input: String,
        isAwaitingQuestionAnswer: Boolean,
        chatHistory: List<Pair<String, String>>,
        patient: Patient?,
        activeQuestion: String?
    ): AiIntentResult {
        val validIntents = NluIntent.values().joinToString(" | ") { "\"${it.name}\"" }
        return try {
            val patientName = patient?.name ?: "Friend"
            val promptText = if (isAwaitingQuestionAnswer) {
                """
                You are Saathi, an affectionate, respectful companion talking with an elderly person named $patientName.
                The person is answering a life-story question you asked.
                
                Question that was asked: "${activeQuestion ?: "Tell me about yourself"}"
                The patient's answer: "$input"
                
                Chat History:
                ${if (chatHistory.isEmpty()) "None" else chatHistory.takeLast(6).joinToString("\n") { "${it.first}: ${it.second}" }}
                
                YOUR TWO MANDATORY TASKS:
                1. EXTRACT & ANALYZE: Extract the specific fact, memory, preference, or event from their answer into a clear, concise statement to save in their life story (e.g. "Loved flying kites on rooftops with childhood friends", "Worked as a teacher in Lucknow").
                2. CONVERSATIONAL CONTINUATION & FOLLOW-UP:
                   - Write a warm, encouraging, joyful reaction acknowledging their answer (max 25 words).
                   - Ask a natural, directly related, curious follow-up question that builds on their answer to keep the conversation flowing naturally!
                
                Return a JSON object:
                {
                    "intent": "ANSWER_QUESTION",
                    "confidence": 0.95,
                    "extractedMemory": "The concise fact or memory extracted to save",
                    "assistantResponse": "Warm reaction acknowledging what they shared (max 25 words)",
                    "generatedFollowUpQuestion": "A curious, friendly follow-up question building directly on what they just said"
                }
                """.trimIndent()
            } else {
                """
                You are Saathi, a caring, warm, and helpful AI companion for an older adult named $patientName.
                Patient Details: City: ${patient?.city.orEmpty()}, Hobbies: ${patient?.hobbies.orEmpty()}, Profession: ${patient?.profession.orEmpty()}
                
                The patient said: "$input"
                
                Chat History:
                ${if (chatHistory.isEmpty()) "None" else chatHistory.takeLast(8).joinToString("\n") { "${it.first}: ${it.second}" }}
                
                Your Task:
                Respond kindly, helpfully, and conversationally in simple, warm words (max 45 words).
                - If they ask for information, answering kindly.
                - If they ask to set an alarm/reminder, detect the reminder title and time.
                - If they feel anxious or afraid, provide deep comfort and reassurance.
                - NEVER ask to save normal chitchat to memory. Just be an endearing companion.
                
                Return a JSON object:
                {
                    "intent": $validIntents,
                    "confidence": 0.95,
                    "assistantResponse": "Your direct, kind, engaging response in patient's language",
                    "reassuranceMessage": "Comforting words if anxious, otherwise omit",
                    "parsedReminder": { "title": "...", "hour": 8, "minute": 30 }
                }
                """.trimIndent()
            }

            val response = generativeModel.generateContent(
                content {
                    text(promptText)
                }
            )

            AiResponseParser.parse(response.text, input)
        } catch (e: Exception) {
            e.printStackTrace()
            // Provide a natural conversational fallback rather than dropping the request
            AiResponseParser.parse(null, input)
        }
    }

    override suspend fun generatePersonalizedStarterQuestion(
        knownMemories: List<LifeMemoryNode>,
        patient: Patient?,
        familyMembers: List<FamilyMember>,
        sessionAskedPrompts: Set<String>
    ): String {
        return try {
            val patientName = patient?.name ?: "Friend"

            // 1. Caregiver profile information
            val caregiverContext = buildList {
                if (!patient?.city.isNullOrBlank()) add("Hometown/City: ${patient?.city}")
                if (!patient?.profession.isNullOrBlank()) add("Profession: ${patient?.profession}")
                if (!patient?.hobbies.isNullOrBlank()) add("Hobbies: ${patient?.hobbies}")
                if (!patient?.favoriteFoods.isNullOrBlank()) add("Favorite Foods: ${patient?.favoriteFoods}")
                if (!patient?.favoriteMusic.isNullOrBlank()) add("Favorite Music: ${patient?.favoriteMusic}")
                if (familyMembers.isNotEmpty()) {
                    val fam = familyMembers.take(4).joinToString(", ") { "${it.fullName} (${it.relation.displayLabel})" }
                    add("Loved Ones: $fam")
                }
            }.joinToString("\n").ifBlank { "No caregiver notes available." }

            // 2. Previous recorded memories
            val memoriesContext = if (knownMemories.isEmpty()) {
                "No previous life story memories have been recorded yet."
            } else {
                knownMemories.takeLast(25).joinToString("\n") {
                    "- Previously answered: \"${it.questionPrompt}\" -> \"${it.value}\""
                }
            }

            // 3. Topics covered in this session
            val sessionContext = if (sessionAskedPrompts.isEmpty()) {
                "None"
            } else {
                sessionAskedPrompts.joinToString("\n") { "- $it" }
            }

            val prompt = """
                You are Saathi, an eager, deeply affectionate and curious companion talking with an elderly person named $patientName.
                You are very eager to know about their life stories, roots, happy memories, and wisdom!
                
                Caregiver Profile Data:
                $caregiverContext
                
                Answers Recorded in Previous Sessions:
                $memoriesContext
                
                Questions Already Asked This Session (DO NOT REPEAT):
                $sessionContext
                
                Your Task:
                Generate EXACTLY ONE warm, personal, open-ended question:
                - IF PREVIOUS ANSWERS EXIST: Follow up enthusiastically on something they shared before (e.g. asking about specific details, feelings, people, or memories related to it).
                - IF NO PREVIOUS SESSIONS: Use caregiver data (hometown, profession, favorite food, hobbies, family members) to ask an eager, personalized question that shows you care.
                - IF VERY LITTLE/NO DATA: Ask a fresh, heartfelt question eager to get to know their childhood home, childhood games, or favorite festival.
                
                Rules:
                - Do NOT repeat any question from the list above.
                - Keep it gentle, simple, and warm (under 25 words).
                - Return ONLY the question text. No quotes, no markdown, no preamble.
            """.trimIndent()

            val response = generativeModel.generateContent(
                content {
                    text(prompt)
                }
            )

            response.text?.trim()?.takeIf { it.isNotBlank() }
                ?: error("Empty response from Gemini")
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}

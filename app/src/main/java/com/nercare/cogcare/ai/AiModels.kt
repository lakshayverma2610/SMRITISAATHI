package com.nercare.cogcare.ai

data class AiIntentResult(
    val intent: NluIntent,
    val confidence: Float,
    val parsedReminder: ParsedReminder? = null,
    val memoryDomain: String? = null,
    val memoryKey: String? = null,
    val targetGameRoute: String? = null,
    val queryTarget: String? = null,
    val rawText: String = "",
    val cleanText: String = "",
    val reassuranceMessage: String? = null,
    val generatedFollowUpQuestion: String? = null,
    val extractedMemory: String? = null,
    val assistantResponse: String? = null
)

enum class NluIntent {
    SET_REMINDER,
    REMEMBER_THIS,
    TELL_ME_ABOUT,
    FEELING_ANXIOUS,
    PLAY_GAME,
    CHECK_REMINDERS,
    CONFIRM_YES,
    CONFIRM_NO,
    SKIP_QUESTION,
    COMPANION_CHITCHAT,
    ANSWER_QUESTION,
    CALM_ACKNOWLEDGEMENT,
    EMERGENCY_SOS,
    UNKNOWN
}

data class ParsedReminder(
    val title: String,
    val hour: Int,
    val minute: Int,
    val rawTimeText: String = ""
)

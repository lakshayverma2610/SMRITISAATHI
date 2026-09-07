package com.nercare.cogcare.ai

import org.json.JSONObject

object AiResponseParser {
    fun parse(response: String?, input: String): AiIntentResult {
        val raw = response.orEmpty().trim()
        if (raw.isBlank()) {
            return AiIntentResult(
                intent = NluIntent.COMPANION_CHITCHAT,
                confidence = 0.5f,
                rawText = input,
                cleanText = input.lowercase().trim(),
                assistantResponse = "I'm right here with you! Tell me what is on your mind."
            )
        }

        // Try extracting JSON if present
        val jsonStart = raw.indexOf('{')
        val jsonEnd = raw.lastIndexOf('}')
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            val jsonSnippet = raw.substring(jsonStart, jsonEnd + 1)
            val jsonResult = runCatching { JSONObject(jsonSnippet) }.getOrNull()
            if (jsonResult != null) {
                val intent = runCatching {
                    NluIntent.valueOf(jsonResult.optString("intent", "COMPANION_CHITCHAT"))
                }.getOrDefault(NluIntent.COMPANION_CHITCHAT)

                val reminder = jsonResult.optJSONObject("parsedReminder")?.let {
                    ParsedReminder(
                        title = it.optString("title", "Reminder"),
                        hour = it.optInt("hour", -1),
                        minute = it.optInt("minute", -1)
                    ).takeIf { parsed -> parsed.hour in 0..23 && parsed.minute in 0..59 }
                }

                val assistantResp = jsonResult.optionalString("assistantResponse")
                    ?: jsonResult.optionalString("reassuranceMessage")
                    ?: raw.replace(jsonSnippet, "").trim().ifBlank { null }

                return AiIntentResult(
                    intent = intent,
                    confidence = jsonResult.optDouble("confidence", 0.9).toFloat(),
                    parsedReminder = reminder,
                    memoryDomain = jsonResult.optionalString("memoryDomain"),
                    memoryKey = jsonResult.optionalString("memoryKey"),
                    targetGameRoute = jsonResult.optionalString("targetGameRoute"),
                    queryTarget = jsonResult.optionalString("queryTarget"),
                    rawText = input,
                    cleanText = input.lowercase().trim(),
                    reassuranceMessage = jsonResult.optionalString("reassuranceMessage"),
                    generatedFollowUpQuestion = jsonResult.optionalString("generatedFollowUpQuestion"),
                    extractedMemory = jsonResult.optionalString("extractedMemory"),
                    assistantResponse = assistantResp ?: "I hear you. Tell me more!"
                )
            }
        }

        // Plain text fallback (Gemini replied conversationally)
        val cleanedText = raw
            .replace(Regex("```[a-zA-Z]*"), "")
            .replace("```", "")
            .trim()

        val lowerInput = input.lowercase().trim()
        val inferredIntent = when {
            lowerInput.contains("remind") || lowerInput.contains("alarm") -> NluIntent.SET_REMINDER
            lowerInput.contains("game") || lowerInput.contains("play") -> NluIntent.PLAY_GAME
            lowerInput.contains("scared") || lowerInput.contains("afraid") || lowerInput.contains("anxious") || lowerInput.contains("help me") -> NluIntent.FEELING_ANXIOUS
            lowerInput.contains("who is") || lowerInput.contains("tell me about") -> NluIntent.TELL_ME_ABOUT
            else -> NluIntent.COMPANION_CHITCHAT
        }

        return AiIntentResult(
            intent = inferredIntent,
            confidence = 0.85f,
            rawText = input,
            cleanText = lowerInput,
            assistantResponse = cleanedText
        )
    }

    private fun JSONObject.optionalString(name: String): String? =
        optString(name, "").trim().takeIf(String::isNotEmpty)
}

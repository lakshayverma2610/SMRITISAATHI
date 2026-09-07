package com.nercare.cogcare.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiResponseParserTest {
    @Test fun parsesReminderRequest() {
        val result = AiResponseParser.parse("""{"intent":"SET_REMINDER","confidence":0.98,"parsedReminder":{"title":"Take medicine","hour":17,"minute":0}}""", "Remind me to take medicine at 5 PM")
        assertEquals(NluIntent.SET_REMINDER, result.intent)
        assertEquals(17, result.parsedReminder?.hour)
    }

    @Test fun parsesGameRequestWithoutSavingMemory() {
        val result = AiResponseParser.parse("""{"intent":"PLAY_GAME","confidence":0.96,"targetGameRoute":"games","assistantResponse":"Let's choose a game."}""", "Play a game")
        assertEquals(NluIntent.PLAY_GAME, result.intent)
        assertNull(result.extractedMemory)
    }

    @Test fun parsesConversationWithoutSavingMemory() {
        val result = AiResponseParser.parse("""{"intent":"COMPANION_CHITCHAT","confidence":0.91,"assistantResponse":"I am here with you."}""", "Can we talk?")
        assertEquals(NluIntent.COMPANION_CHITCHAT, result.intent)
        assertNull(result.extractedMemory)
    }

    @Test fun parsesLifeStoryAnswerAndFollowUp() {
        val result = AiResponseParser.parse("""{"intent":"ANSWER_QUESTION","confidence":0.94,"extractedMemory":"Enjoyed gardening with her mother","generatedFollowUpQuestion":"What did you like to grow together?"}""", "I gardened with my mother")
        assertEquals(NluIntent.ANSWER_QUESTION, result.intent)
        assertEquals("What did you like to grow together?", result.generatedFollowUpQuestion)
    }
}

package com.nercare.cogcare.ai

import com.nercare.cogcare.domain.model.FamilyMember
import com.nercare.cogcare.domain.model.LifeMemoryNode
import com.nercare.cogcare.domain.model.Patient

interface AiIntentEngine {
    suspend fun process(
        input: String,
        isAwaitingQuestionAnswer: Boolean = false,
        chatHistory: List<Pair<String, String>> = emptyList(),
        patient: Patient? = null,
        activeQuestion: String? = null
    ): AiIntentResult

    suspend fun generatePersonalizedStarterQuestion(
        knownMemories: List<LifeMemoryNode>,
        patient: Patient? = null,
        familyMembers: List<FamilyMember> = emptyList(),
        sessionAskedPrompts: Set<String> = emptySet()
    ): String
}

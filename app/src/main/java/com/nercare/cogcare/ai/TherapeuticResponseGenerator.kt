package com.nercare.cogcare.ai

import com.nercare.cogcare.data.repository.LifeStoryRepository
import com.nercare.cogcare.domain.model.FamilyMember
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class TherapeuticResponseGenerator @Inject constructor(
    private val lifeStoryRepository: LifeStoryRepository
) {

    suspend fun generateResponse(
        intentResult: AiIntentResult,
        currentState: ConversationState,
        patientId: String,
        patientName: String
    ): String {
        return when {
            intentResult.intent == NluIntent.EMERGENCY_SOS -> {
                "I understand you are in pain. Please sit somewhere safe and call your caregiver or local emergency service now."
            }
            intentResult.intent == NluIntent.FEELING_ANXIOUS || currentState is ConversationState.DE_ESCALATION -> {
                "You are safe at home, $patientName. Everything is fine. I am right here with you."
            }
            intentResult.intent == NluIntent.TELL_ME_ABOUT -> {
                val target = intentResult.queryTarget ?: ""
                val familyList = lifeStoryRepository.observeAllFamilyMembers(patientId).firstOrNull() ?: emptyList()
                
                // Try to find a family member matching the target
                val matchingFamily = familyList.find { 
                    it.fullName.contains(target, ignoreCase = true) || 
                    it.relation.displayLabel.contains(target, ignoreCase = true) 
                }

                if (matchingFamily != null) {
                    "Your ${matchingFamily.relation.displayLabel.lowercase()} ${matchingFamily.fullName} loves you very much."
                } else {
                    "I would love to learn more about $target. Can you tell me a story about them?"
                }
            }
            intentResult.reassuranceMessage != null -> {
                intentResult.reassuranceMessage
            }
            else -> {
                "I am here to help you."
            }
        }
    }
}

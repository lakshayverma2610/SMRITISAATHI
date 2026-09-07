package com.nercare.cogcare.ai

sealed class ConversationState {
    object IDLE : ConversationState()
    
    data class AWAITING_CONFIRMATION(val pendingAction: AiIntentResult) : ConversationState()
    
    object DE_ESCALATION : ConversationState()
    
    data class IN_GAME_LOOP(val gameId: String) : ConversationState()
}

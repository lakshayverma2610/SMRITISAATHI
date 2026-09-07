package com.nercare.cogcare.ai

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StateManager @Inject constructor() {
    private val stack = ArrayDeque<ConversationState>()

    init {
        // IDLE is always the base state
        stack.addLast(ConversationState.IDLE)
    }

    fun pushState(state: ConversationState) {
        if (state !is ConversationState.IDLE) {
            stack.addLast(state)
        }
    }

    fun popState(): ConversationState {
        // Never pop the last IDLE state
        if (stack.size > 1) {
            return stack.removeLast()
        }
        return stack.last()
    }

    fun peekCurrentState(): ConversationState {
        return stack.last()
    }

    fun clearToIdle() {
        stack.clear()
        stack.addLast(ConversationState.IDLE)
    }
}

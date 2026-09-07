package com.nercare.cogcare.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class GameSession(
    val id: String = "",
    val patientId: String = "",
    val gameType: GameType = GameType.MEMORY_CARD,
    val difficultyLevel: Int = 1, // 1-5
    val score: Int = 0,
    val maxPossibleScore: Int = 0,
    val accuracyPercent: Float = 0f,
    val avgResponseTimeMs: Long = 0L,
    val durationMs: Long = 0L,
    val completedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)

enum class GameType(val displayName: String, val icon: String) {
    MEMORY_CARD("Memory Cards", "🃏"),
    SEQUENCE_RECALL("Sequence Recall", "🔢"),
    PATTERN_MATCHING("Pattern Matching", "🔷"),
    WORD_ASSOCIATION("Word Association", "📝"),
    DAILY_ROUTINE("Daily Routine Quiz", "🌅")
}

@Serializable
data class CognitiveScore(
    val patientId: String = "",
    val weekNumber: Int = 0,
    val year: Int = 0,
    val averageAccuracy: Float = 0f,
    val averageResponseTime: Long = 0L,
    val sessionsCompleted: Int = 0,
    val dominantGame: GameType = GameType.MEMORY_CARD,
    val trendDirection: TrendDirection = TrendDirection.STABLE
)

enum class TrendDirection { IMPROVING, STABLE, DECLINING }

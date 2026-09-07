package com.nercare.cogcare.ai

import com.nercare.cogcare.domain.model.CognitiveScore
import com.nercare.cogcare.domain.model.GameSession
import com.nercare.cogcare.domain.model.GameType
import com.nercare.cogcare.domain.model.TrendDirection
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CognitiveScoreCalculator computes weekly cognitive performance scores
 * inspired by MMSE (Mini-Mental State Examination) sub-metrics.
 *
 * Metrics tracked:
 *  - Average accuracy across all game types
 *  - Average response time
 *  - Session engagement (frequency)
 *  - Week-over-week trend
 */
@Singleton
class CognitiveScoreCalculator @Inject constructor() {

    data class WeeklyReport(
        val score: CognitiveScore,
        val breakdown: Map<GameType, Float>,     // per-game accuracy
        val sessionFrequency: Int,
        val alertMessages: List<String>          // red-flag messages for caregiver
    )

    fun calculateWeeklyReport(
        patientId: String,
        currentWeekSessions: List<GameSession>,
        previousWeekSessions: List<GameSession>
    ): WeeklyReport {
        val cal = Calendar.getInstance()
        val weekNumber = cal.get(Calendar.WEEK_OF_YEAR)
        val year = cal.get(Calendar.YEAR)

        val avgAccuracy = if (currentWeekSessions.isEmpty()) 0f
        else currentWeekSessions.map { it.accuracyPercent }.average().toFloat()

        val avgResponseTime = if (currentWeekSessions.isEmpty()) 0L
        else currentWeekSessions.map { it.avgResponseTimeMs }.average().toLong()

        // Per-game accuracy breakdown
        val breakdown = GameType.values().associateWith { gameType ->
            val gameSessions = currentWeekSessions.filter { it.gameType == gameType }
            if (gameSessions.isEmpty()) 0f
            else gameSessions.map { it.accuracyPercent }.average().toFloat()
        }

        // Dominant game (most played)
        val dominantGame = currentWeekSessions
            .groupBy { it.gameType }
            .maxByOrNull { it.value.size }?.key ?: GameType.MEMORY_CARD

        // Trend calculation
        val prevAvgAccuracy = if (previousWeekSessions.isEmpty()) avgAccuracy
        else previousWeekSessions.map { it.accuracyPercent }.average().toFloat()

        val trend = when {
            avgAccuracy > prevAvgAccuracy + 5f -> TrendDirection.IMPROVING
            avgAccuracy < prevAvgAccuracy - 5f -> TrendDirection.DECLINING
            else -> TrendDirection.STABLE
        }

        // Red-flag alerts for caregivers
        val alerts = mutableListOf<String>()
        if (currentWeekSessions.size < 3) alerts.add("⚠️ Low engagement: fewer than 3 sessions this week")
        if (avgAccuracy < 50f) alerts.add("⚠️ Accuracy dropped below 50% — review difficulty level")
        if (trend == TrendDirection.DECLINING) alerts.add("⚠️ Cognitive performance declining vs. last week")
        if (avgResponseTime > 8000L) alerts.add("⚠️ Response time very slow — patient may need assistance")

        val score = CognitiveScore(
            patientId = patientId,
            weekNumber = weekNumber,
            year = year,
            averageAccuracy = avgAccuracy,
            averageResponseTime = avgResponseTime,
            sessionsCompleted = currentWeekSessions.size,
            dominantGame = dominantGame,
            trendDirection = trend
        )

        return WeeklyReport(
            score = score,
            breakdown = breakdown,
            sessionFrequency = currentWeekSessions.size,
            alertMessages = alerts
        )
    }

    /**
     * Compute a single overall score (0-100) for display to the patient/caregiver.
     */
    fun computeOverallScore(sessions: List<GameSession>): Int {
        if (sessions.isEmpty()) return 0
        val accuracyWeight = 0.6f
        val engagementWeight = 0.2f
        val speedWeight = 0.2f

        val avgAccuracy = sessions.map { it.accuracyPercent }.average().toFloat()
        val engagementScore = minOf(sessions.size / 7f, 1f) * 100f // 7 sessions/week = 100%
        val avgSpeed = sessions.map { it.avgResponseTimeMs }.average()
        val speedScore = when {
            avgSpeed < 2000 -> 100f
            avgSpeed < 4000 -> 80f
            avgSpeed < 6000 -> 60f
            avgSpeed < 9000 -> 40f
            else -> 20f
        }

        return ((avgAccuracy * accuracyWeight) +
                (engagementScore * engagementWeight) +
                (speedScore * speedWeight)).toInt().coerceIn(0, 100)
    }
}

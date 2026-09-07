package com.nercare.cogcare.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nercare.cogcare.domain.model.GameSession
import com.nercare.cogcare.domain.model.GameType

@Entity(tableName = "game_sessions")
data class GameSessionEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val gameType: String,
    val difficultyLevel: Int,
    val score: Int,
    val maxPossibleScore: Int,
    val accuracyPercent: Float,
    val avgResponseTimeMs: Long,
    val durationMs: Long,
    val completedAt: Long,
    val isSynced: Boolean = false
) {
    fun toDomain() = GameSession(
        id = id,
        patientId = patientId,
        gameType = GameType.valueOf(gameType),
        difficultyLevel = difficultyLevel,
        score = score,
        maxPossibleScore = maxPossibleScore,
        accuracyPercent = accuracyPercent,
        avgResponseTimeMs = avgResponseTimeMs,
        durationMs = durationMs,
        completedAt = completedAt,
        isSynced = isSynced
    )
}

fun GameSession.toEntity() = GameSessionEntity(
    id = id,
    patientId = patientId,
    gameType = gameType.name,
    difficultyLevel = difficultyLevel,
    score = score,
    maxPossibleScore = maxPossibleScore,
    accuracyPercent = accuracyPercent,
    avgResponseTimeMs = avgResponseTimeMs,
    durationMs = durationMs,
    completedAt = completedAt,
    isSynced = isSynced
)

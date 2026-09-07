package com.nercare.cogcare.data.repository

import com.nercare.cogcare.data.local.dao.GameSessionDao
import com.nercare.cogcare.data.local.entities.toEntity
import com.nercare.cogcare.data.remote.FirebaseService
import com.nercare.cogcare.domain.model.GameSession
import com.nercare.cogcare.domain.model.GameType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepository @Inject constructor(
    private val gameSessionDao: GameSessionDao,
    private val firebaseService: FirebaseService
) {
    fun getSessionsForPatient(patientId: String): Flow<List<GameSession>> =
        gameSessionDao.getSessionsForPatient(patientId).map { list -> list.map { it.toDomain() } }

    suspend fun saveSession(session: GameSession): GameSession {
        val toSave = if (session.id.isEmpty()) {
            session.copy(id = UUID.randomUUID().toString())
        } else session
        gameSessionDao.insertSession(toSave.toEntity())
        return toSave
    }

    suspend fun getRecentSessions(patientId: String, limit: Int = 10): List<GameSession> =
        gameSessionDao.getRecentSessions(patientId, limit).map { it.toDomain() }

    suspend fun getAverageAccuracy(patientId: String, since: Long): Float =
        gameSessionDao.getAverageAccuracy(patientId, since) ?: 0f

    suspend fun getAverageResponseTime(patientId: String, since: Long): Long =
        gameSessionDao.getAverageResponseTime(patientId, since) ?: 0L

    suspend fun getSessionCountSince(patientId: String, since: Long): Int =
        gameSessionDao.getSessionCountSince(patientId, since)

    suspend fun getRecentSessionsByGame(patientId: String, gameType: GameType): List<GameSession> =
        gameSessionDao.getRecentSessionsByGame(patientId, gameType.name).map { it.toDomain() }

    suspend fun syncUnsyncedSessions() {
        val unsynced = gameSessionDao.getUnsyncedSessions()
        if (unsynced.isNotEmpty()) {
            val sessions = unsynced.map { it.toDomain() }
            val success = firebaseService.syncGameSessions(sessions)
            if (success) {
                gameSessionDao.markAsSynced(unsynced.map { it.id })
            }
        }
    }
}

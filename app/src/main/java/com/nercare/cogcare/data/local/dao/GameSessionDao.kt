package com.nercare.cogcare.data.local.dao

import androidx.room.*
import com.nercare.cogcare.data.local.entities.GameSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: GameSessionEntity)

    @Query("SELECT * FROM game_sessions WHERE patientId = :patientId ORDER BY completedAt DESC")
    fun getSessionsForPatient(patientId: String): Flow<List<GameSessionEntity>>

    @Query("SELECT * FROM game_sessions WHERE patientId = :patientId ORDER BY completedAt DESC LIMIT :limit")
    suspend fun getRecentSessions(patientId: String, limit: Int = 10): List<GameSessionEntity>

    @Query("SELECT * FROM game_sessions WHERE isSynced = 0")
    suspend fun getUnsyncedSessions(): List<GameSessionEntity>

    @Query("UPDATE game_sessions SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)

    @Query("""
        SELECT AVG(accuracyPercent) FROM game_sessions 
        WHERE patientId = :patientId AND completedAt >= :since
    """)
    suspend fun getAverageAccuracy(patientId: String, since: Long): Float?

    @Query("""
        SELECT AVG(avgResponseTimeMs) FROM game_sessions 
        WHERE patientId = :patientId AND completedAt >= :since
    """)
    suspend fun getAverageResponseTime(patientId: String, since: Long): Long?

    @Query("SELECT COUNT(*) FROM game_sessions WHERE patientId = :patientId AND completedAt >= :since")
    suspend fun getSessionCountSince(patientId: String, since: Long): Int

    @Query("SELECT * FROM game_sessions WHERE patientId = :patientId AND gameType = :gameType ORDER BY completedAt DESC LIMIT 5")
    suspend fun getRecentSessionsByGame(patientId: String, gameType: String): List<GameSessionEntity>

    @Query("DELETE FROM game_sessions WHERE patientId = :patientId")
    suspend fun deleteAllForPatient(patientId: String)
}

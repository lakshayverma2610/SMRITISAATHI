package com.nercare.cogcare.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nercare.cogcare.data.local.entities.GameContentEntity

@Dao
interface GameContentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contents: List<GameContentEntity>)

    @Query("SELECT * FROM game_content WHERE gameType = :gameType AND isUsed = 0 ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomUnusedContent(gameType: String): GameContentEntity?

    @Query("UPDATE game_content SET isUsed = 1 WHERE id = :id")
    suspend fun markAsUsed(id: Long)

    @Query("SELECT COUNT(*) FROM game_content WHERE gameType = :gameType AND isUsed = 0")
    suspend fun getUnusedCount(gameType: String): Int
}

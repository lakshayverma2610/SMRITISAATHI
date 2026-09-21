package com.nercare.cogcare.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_content")
data class GameContentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameType: String,
    val contentJson: String, // Stores the serialized JSON string of the WordPair or List<String>
    val isUsed: Boolean = false, // Track if the user has already played this content
    val createdAt: Long = System.currentTimeMillis()
)

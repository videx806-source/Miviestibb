package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val subtitle: String = "",
    val category: String, // "vivo" | "canal" | "pelicula"
    val streamUrl: String,
    val thumbnailUrl: String = "",
    val isRecording: Boolean = false,
    val isDownloaded: Boolean = false,
    val durationSeconds: Int = 0,
    val localPath: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

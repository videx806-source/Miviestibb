package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items ORDER BY timestamp DESC")
    fun getAllMediaItems(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isRecording = 1 ORDER BY timestamp DESC")
    fun getRecordings(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isDownloaded = 1 ORDER BY timestamp DESC")
    fun getDownloads(): Flow<List<MediaItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: MediaItemEntity)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteItemById(id: String)

    @Query("UPDATE media_items SET isRecording = :isRecording WHERE id = :id")
    suspend fun updateRecordingStatus(id: String, isRecording: Boolean)

    @Query("UPDATE media_items SET isDownloaded = :isDownloaded, localPath = :localPath WHERE id = :id")
    suspend fun updateDownloadStatus(id: String, isDownloaded: Boolean, localPath: String)
}

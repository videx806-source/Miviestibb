package com.example.data

import kotlinx.coroutines.flow.Flow

class MediaRepository(private val mediaDao: MediaDao) {
    val allRecs: Flow<List<MediaItemEntity>> = mediaDao.getRecordings()
    val allDownloads: Flow<List<MediaItemEntity>> = mediaDao.getDownloads()

    suspend fun insert(item: MediaItemEntity) {
        mediaDao.insertItem(item)
    }

    suspend fun deleteById(id: String) {
        mediaDao.deleteItemById(id)
    }

    suspend fun updateDownload(id: String, isDownloaded: Boolean, localPath: String) {
        mediaDao.updateDownloadStatus(id, isDownloaded, localPath)
    }
}

package com.example.data.repository

import com.example.data.local.ClipDao
import com.example.domain.models.ClipItem
import kotlinx.coroutines.flow.Flow

class ClipRepository(private val clipDao: ClipDao) {
    fun getActiveClips(): Flow<List<ClipItem>> = clipDao.getAllActiveClips()
    
    fun getTrashedClips(): Flow<List<ClipItem>> = clipDao.getTrashedClips()
    
    fun getFavoriteClips(): Flow<List<ClipItem>> = clipDao.getFavoriteClips()
    
    fun getClipsByFolder(folderName: String): Flow<List<ClipItem>> = clipDao.getClipsByFolder(folderName)
    
    fun getFolders(): Flow<List<String>> = clipDao.getAllFolders()
    
    fun search(query: String): Flow<List<ClipItem>> = clipDao.searchClips(query)
    
    fun getClipById(id: Int): Flow<ClipItem?> = clipDao.getClipByIdFlow(id)
    
    suspend fun getClipOnce(id: Int): ClipItem? = clipDao.getClipByIdOnce(id)
    
    suspend fun insert(clip: ClipItem): Long = clipDao.insertClip(clip)
    
    suspend fun update(clip: ClipItem) {
        clipDao.updateClip(clip)
    }
    
    suspend fun delete(clip: ClipItem) {
        clipDao.deleteClip(clip)
    }
    
    suspend fun emptyTrash() {
        clipDao.emptyTrash()
    }
}

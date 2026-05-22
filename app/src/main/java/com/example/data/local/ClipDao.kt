package com.example.data.local

import androidx.room.*
import com.example.domain.models.ClipItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipDao {
    @Query("SELECT * FROM clips WHERE isTrash = 0 ORDER BY createdAt DESC")
    fun getAllActiveClips(): Flow<List<ClipItem>>

    @Query("SELECT * FROM clips WHERE isTrash = 1 ORDER BY createdAt DESC")
    fun getTrashedClips(): Flow<List<ClipItem>>

    @Query("SELECT * FROM clips WHERE id = :id LIMIT 1")
    suspend fun getClipByIdOnce(id: Int): ClipItem?

    @Query("SELECT * FROM clips WHERE id = :id")
    fun getClipByIdFlow(id: Int): Flow<ClipItem?>

    @Query("SELECT * FROM clips WHERE isTrash = 0 AND isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteClips(): Flow<List<ClipItem>>

    @Query("SELECT * FROM clips WHERE isTrash = 0 AND folderName = :folderName ORDER BY createdAt DESC")
    fun getClipsByFolder(folderName: String): Flow<List<ClipItem>>

    @Query("SELECT DISTINCT folderName FROM clips WHERE isTrash = 0")
    fun getAllFolders(): Flow<List<String>>

    @Query("""
        SELECT * FROM clips 
        WHERE isTrash = 0 AND (title LIKE '%' || :query || '%' OR rawText LIKE '%' || :query || '%') 
        ORDER BY createdAt DESC
    """)
    fun searchClips(query: String): Flow<List<ClipItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: ClipItem): Long

    @Update
    suspend fun updateClip(clip: ClipItem): Int

    @Delete
    suspend fun deleteClip(clip: ClipItem): Int

    @Query("DELETE FROM clips WHERE isTrash = 1")
    suspend fun emptyTrash(): Int
}

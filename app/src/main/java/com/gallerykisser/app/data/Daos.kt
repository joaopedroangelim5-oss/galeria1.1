package com.gallerykisser.app.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items WHERE folderId IS NULL ORDER BY dateAdded DESC")
    fun observeRoot(): LiveData<List<MediaItem>>

    @Query("SELECT * FROM media_items ORDER BY dateAdded DESC")
    fun observeAll(): LiveData<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE folderId = :folderId ORDER BY dateAdded DESC")
    fun observeByFolder(folderId: Long): LiveData<List<MediaItem>>

    @Insert
    suspend fun insert(item: MediaItem): Long

    @Insert
    suspend fun insertAll(items: List<MediaItem>): List<Long>

    @Update
    suspend fun update(item: MediaItem)

    @Query("UPDATE media_items SET folderId = :folderId WHERE id IN (:ids)")
    suspend fun moveToFolder(ids: List<Long>, folderId: Long?)

    @Delete
    suspend fun delete(item: MediaItem)

    @Query("DELETE FROM media_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT * FROM media_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): MediaItem?

    @Query("SELECT COUNT(*) FROM media_items WHERE folderId = :folderId")
    suspend fun countInFolder(folderId: Long): Int

    @Query("SELECT * FROM media_items WHERE isSecure = 1 ORDER BY dateAdded DESC")
    fun observeSecure(): LiveData<List<MediaItem>>

    @Query("UPDATE media_items SET isSecure = :secure WHERE id IN (:ids)")
    suspend fun setSecure(ids: List<Long>, secure: Boolean)

    @Query("UPDATE media_items SET displayName = :name WHERE id = :id")
    suspend fun renameDisplayName(id: Long, name: String)

    @Query("UPDATE media_items SET sizeBytes = :size WHERE id = :id")
    suspend fun updateSize(id: Long, size: Long)
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun observeAll(): LiveData<List<FolderEntity>>

    @Insert
    suspend fun insert(folder: FolderEntity): Long

    @Delete
    suspend fun delete(folder: FolderEntity)

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FolderEntity?
}

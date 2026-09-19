package com.gallerykisser.app.data

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GalleryRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val mediaDao = db.mediaDao()
    private val folderDao = db.folderDao()
    private val storage = MediaStorage(context)

    fun observeRoot(): LiveData<List<MediaItem>> = mediaDao.observeRoot()
    fun observeAll(): LiveData<List<MediaItem>> = mediaDao.observeAll()
    fun observeByFolder(folderId: Long): LiveData<List<MediaItem>> = mediaDao.observeByFolder(folderId)
    fun observeSecure(): LiveData<List<MediaItem>> = mediaDao.observeSecure()
    fun observeFolders(): LiveData<List<FolderEntity>> = folderDao.observeAll()

    fun fileFor(item: MediaItem) = storage.fileFor(item.fileName)

    suspend fun getById(id: Long): MediaItem? = withContext(Dispatchers.IO) { mediaDao.getById(id) }

    suspend fun importUris(uris: List<Uri>, isVideoFlags: List<Boolean>, folderId: Long?) =
        withContext(Dispatchers.IO) {
            val items = uris.mapIndexed { index, uri ->
                val isVideo = isVideoFlags.getOrElse(index) { false }
                val fileName = storage.importFrom(uri, isVideo)
                val file = storage.fileFor(fileName)
                MediaItem(
                    folderId = folderId,
                    fileName = fileName,
                    displayName = fileName,
                    isVideo = isVideo,
                    dateAdded = System.currentTimeMillis() + index, // preserva ordem de seleção
                    sizeBytes = file.length()
                )
            }
            mediaDao.insertAll(items)
        }

    suspend fun createFolder(name: String): Long = withContext(Dispatchers.IO) {
        folderDao.insert(FolderEntity(name = name))
    }

    suspend fun moveToFolder(ids: List<Long>, folderId: Long?) = withContext(Dispatchers.IO) {
        mediaDao.moveToFolder(ids, folderId)
    }

    suspend fun deleteItems(items: List<MediaItem>) = withContext(Dispatchers.IO) {
        items.forEach { storage.deleteFile(it.fileName) }
        mediaDao.deleteByIds(items.map { it.id })
    }

    suspend fun exportItems(items: List<MediaItem>): Int = withContext(Dispatchers.IO) {
        items.count { storage.exportToPublicStorage(it.fileName, it.displayName, it.isVideo) }
    }

    suspend fun setSecure(ids: List<Long>, secure: Boolean) = withContext(Dispatchers.IO) {
        mediaDao.setSecure(ids, secure)
    }

    suspend fun renameDisplayName(id: Long, name: String) = withContext(Dispatchers.IO) {
        mediaDao.renameDisplayName(id, name)
    }

    /** Atualiza o tamanho salvo no banco após uma edição (corte, rotação etc). */
    suspend fun refreshSize(item: MediaItem) = withContext(Dispatchers.IO) {
        mediaDao.updateSize(item.id, fileFor(item).length())
    }
}

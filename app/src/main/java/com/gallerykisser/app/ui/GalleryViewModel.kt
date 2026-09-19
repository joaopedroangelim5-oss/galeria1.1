package com.gallerykisser.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.gallerykisser.app.data.FolderEntity
import com.gallerykisser.app.data.GalleryRepository
import com.gallerykisser.app.data.MediaItem
import kotlinx.coroutines.launch

class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    val repository = GalleryRepository(application)

    val folders: LiveData<List<FolderEntity>> = repository.observeFolders()

    private val _currentFolderId = MutableLiveData<Long?>(null)
    val currentFolderId: LiveData<Long?> = _currentFolderId

    private var secureMode = false

    val items: LiveData<List<MediaItem>> = _currentFolderId.switchMap { folderId ->
        when {
            secureMode -> repository.observeSecure()
            folderId == null -> repository.observeRoot()
            else -> repository.observeByFolder(folderId)
        }
    }

    fun setFolder(folderId: Long?) {
        _currentFolderId.value = folderId
    }

    /** Ativa a visão do Modo Seguro (só mostra itens marcados como seguros). */
    fun setSecureMode(enabled: Boolean) {
        secureMode = enabled
        _currentFolderId.value = _currentFolderId.value // força o switchMap a reavaliar a origem dos dados
    }

    fun setSecure(ids: List<Long>, secure: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.setSecure(ids, secure)
            onDone()
        }
    }

    fun importUris(uris: List<Uri>, isVideoFlags: List<Boolean>) {
        viewModelScope.launch {
            repository.importUris(uris, isVideoFlags, _currentFolderId.value)
        }
    }

    fun createFolder(name: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.createFolder(name)
            onCreated(id)
        }
    }

    fun moveToFolder(ids: List<Long>, folderId: Long?, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.moveToFolder(ids, folderId)
            onDone()
        }
    }

    fun deleteItems(mediaItems: List<MediaItem>, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteItems(mediaItems)
            onDone()
        }
    }

    fun exportItems(mediaItems: List<MediaItem>, onDone: (Int) -> Unit) {
        viewModelScope.launch {
            val count = repository.exportItems(mediaItems)
            onDone(count)
        }
    }
}

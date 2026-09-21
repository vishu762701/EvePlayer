package com.example.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.EveApplication
import com.example.data.local.PlaylistEntity
import com.example.data.mediastore.MediaFolder
import com.example.data.mediastore.MediaItemModel
import com.example.data.preferences.UserSettings
import com.example.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel : ViewModel() {

    private val app = EveApplication.instance
    private val repo: MediaRepository = MediaRepository(app, app.database, app.preferencesRepository)
    private val prefRepo = app.preferencesRepository

    val userSettings: StateFlow<UserSettings> = prefRepo.userSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _videoSortBy = MutableStateFlow("DATE")
    val videoSortBy: StateFlow<String> = _videoSortBy.asStateFlow()

    private val _videoSortAsc = MutableStateFlow(false)
    val videoSortAsc: StateFlow<Boolean> = _videoSortAsc.asStateFlow()

    private val _audioSortBy = MutableStateFlow("NAME")
    val audioSortBy: StateFlow<String> = _audioSortBy.asStateFlow()

    private val _audioSortAsc = MutableStateFlow(true)
    val audioSortAsc: StateFlow<Boolean> = _audioSortAsc.asStateFlow()

    val videos: StateFlow<List<MediaItemModel>> = repo.getVideos(searchQuery, videoSortBy, videoSortAsc)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val audios: StateFlow<List<MediaItemModel>> = repo.getAudios(searchQuery, audioSortBy, audioSortAsc)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videoFolders: StateFlow<List<MediaFolder>> = repo.getVideoFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val audioFolders: StateFlow<List<MediaFolder>> = repo.getAudioFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> = repo.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isScanning: StateFlow<Boolean> = repo.isScanning
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            userSettings.collect { settings ->
                _videoSortBy.value = settings.videoSortBy
                _videoSortAsc.value = settings.videoSortAsc
                _audioSortBy.value = settings.audioSortBy
                _audioSortAsc.value = settings.audioSortAsc
            }
        }
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            repo.refreshLibrary()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setVideoSorting(sortBy: String, asc: Boolean) {
        _videoSortBy.value = sortBy
        _videoSortAsc.value = asc
        viewModelScope.launch {
            prefRepo.setVideoSorting(sortBy, asc)
        }
    }

    fun setAudioSorting(sortBy: String, asc: Boolean) {
        _audioSortBy.value = sortBy
        _audioSortAsc.value = asc
        viewModelScope.launch {
            prefRepo.setAudioSorting(sortBy, asc)
        }
    }

    fun toggleVideoGridView() {
        val current = userSettings.value.isVideoGridView
        viewModelScope.launch {
            prefRepo.setVideoGridView(!current)
        }
    }

    fun toggleAudioGridView() {
        val current = userSettings.value.isAudioGridView
        viewModelScope.launch {
            prefRepo.setAudioGridView(!current)
        }
    }

    fun toggleFavorite(item: MediaItemModel) {
        viewModelScope.launch {
            repo.toggleFavorite(item)
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repo.createPlaylist(name)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repo.deletePlaylist(playlistId)
        }
    }

    fun addMediaToPlaylist(playlistId: Long, item: MediaItemModel) {
        viewModelScope.launch {
            repo.addMediaToPlaylist(playlistId, listOf(item))
        }
    }

    suspend fun getMediaInFolder(folderName: String, isVideo: Boolean): List<MediaItemModel> {
        return repo.getMediaInFolder(folderName, isVideo)
    }

    suspend fun resolveMedia(uri: android.net.Uri): MediaItemModel? {
        return repo.resolveMedia(uri)
    }
}

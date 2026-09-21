package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.EveDatabase
import com.example.data.local.FavoriteEntity
import com.example.data.local.PlaybackHistoryEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistItemEntity
import com.example.data.mediastore.MediaFolder
import com.example.data.mediastore.MediaItemModel
import com.example.data.mediastore.MediaStoreScanner
import com.example.data.preferences.AppPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class MediaRepository(
    private val context: Context,
    private val database: EveDatabase,
    private val preferencesRepository: AppPreferencesRepository
) {
    private val scanner = MediaStoreScanner(context)
    private val historyDao = database.playbackHistoryDao()
    private val favoriteDao = database.favoriteDao()
    private val playlistDao = database.playlistDao()

    private val _rawVideos = MutableStateFlow<List<MediaItemModel>>(emptyList())
    private val _rawAudios = MutableStateFlow<List<MediaItemModel>>(emptyList())
    private val _isScanning = MutableStateFlow(false)
    val isScanning: Flow<Boolean> = _isScanning

    suspend fun refreshLibrary() = withContext(Dispatchers.IO) {
        _isScanning.value = true
        try {
            val vids = scanner.queryVideos()
            val auds = scanner.queryAudios()
            _rawVideos.value = vids
            _rawAudios.value = auds
        } finally {
            _isScanning.value = false
        }
    }

    data class FilterCriteria(val query: String, val sortBy: String, val sortAsc: Boolean)

    fun getVideos(
        searchQuery: Flow<String>,
        sortBy: Flow<String>,
        sortAsc: Flow<Boolean>
    ): Flow<List<MediaItemModel>> {
        val criteriaFlow = combine(searchQuery, sortBy, sortAsc) { q, s, a ->
            FilterCriteria(q, s, a)
        }

        return combine(
            _rawVideos,
            historyDao.getAllHistory(),
            favoriteDao.getAllFavorites(),
            criteriaFlow
        ) { videos, history, favorites, criteria ->
            val historyMap = history.associate { it.uri to it.lastPositionMs }
            val favSet = favorites.map { it.uri }.toSet()

            val mapped = videos.map { item ->
                val uriStr = item.uri.toString()
                item.copy(
                    lastPositionMs = historyMap[uriStr] ?: 0L,
                    isFavorite = favSet.contains(uriStr)
                )
            }

            val filtered = if (criteria.query.isBlank()) {
                mapped
            } else {
                mapped.filter {
                    it.title.contains(criteria.query, ignoreCase = true) ||
                            it.folderName.contains(criteria.query, ignoreCase = true)
                }
            }

            sortMediaList(filtered, criteria.sortBy, criteria.sortAsc)
        }.flowOn(Dispatchers.Default)
    }

    fun getAudios(
        searchQuery: Flow<String>,
        sortBy: Flow<String>,
        sortAsc: Flow<Boolean>
    ): Flow<List<MediaItemModel>> {
        val criteriaFlow = combine(searchQuery, sortBy, sortAsc) { q, s, a ->
            FilterCriteria(q, s, a)
        }

        return combine(
            _rawAudios,
            historyDao.getAllHistory(),
            favoriteDao.getAllFavorites(),
            criteriaFlow
        ) { audios, history, favorites, criteria ->
            val historyMap = history.associate { it.uri to it.lastPositionMs }
            val favSet = favorites.map { it.uri }.toSet()

            val mapped = audios.map { item ->
                val uriStr = item.uri.toString()
                item.copy(
                    lastPositionMs = historyMap[uriStr] ?: 0L,
                    isFavorite = favSet.contains(uriStr)
                )
            }

            val filtered = if (criteria.query.isBlank()) {
                mapped
            } else {
                mapped.filter {
                    it.title.contains(criteria.query, ignoreCase = true) ||
                            it.artist.contains(criteria.query, ignoreCase = true) ||
                            it.album.contains(criteria.query, ignoreCase = true)
                }
            }

            sortMediaList(filtered, criteria.sortBy, criteria.sortAsc)
        }.flowOn(Dispatchers.Default)
    }

    fun getVideoFolders(): Flow<List<MediaFolder>> {
        return combine(_rawVideos, _rawAudios) { videos, _ ->
            videos.groupBy { it.folderName.ifEmpty { "Videos" } }
                .map { (name, items) ->
                    MediaFolder(
                        folderName = name,
                        folderPath = items.firstOrNull()?.folderPath ?: "",
                        itemCount = items.size,
                        isVideo = true,
                        sampleUri = items.firstOrNull()?.uri
                    )
                }.sortedBy { it.folderName.lowercase() }
        }.flowOn(Dispatchers.Default)
    }

    fun getAudioFolders(): Flow<List<MediaFolder>> {
        return combine(_rawVideos, _rawAudios) { _, audios ->
            audios.groupBy { it.folderName.ifEmpty { "Music" } }
                .map { (name, items) ->
                    MediaFolder(
                        folderName = name,
                        folderPath = items.firstOrNull()?.folderPath ?: "",
                        itemCount = items.size,
                        isVideo = false,
                        sampleUri = items.firstOrNull()?.uri
                    )
                }.sortedBy { it.folderName.lowercase() }
        }.flowOn(Dispatchers.Default)
    }

    suspend fun getMediaInFolder(folderName: String, isVideo: Boolean): List<MediaItemModel> {
        val pool = if (isVideo) _rawVideos.value else _rawAudios.value
        return pool.filter { it.folderName.equals(folderName, ignoreCase = true) }
    }

    private fun sortMediaList(
        list: List<MediaItemModel>,
        sortBy: String,
        asc: Boolean
    ): List<MediaItemModel> {
        val comparator = when (sortBy.uppercase()) {
            "NAME" -> compareBy<MediaItemModel> { it.title.lowercase() }
            "DATE" -> compareBy { it.dateModified }
            "SIZE" -> compareBy { it.size }
            "DURATION" -> compareBy { it.duration }
            else -> compareBy { it.dateModified }
        }
        return if (asc) list.sortedWith(comparator) else list.sortedWith(comparator.reversed())
    }

    suspend fun resolveMedia(uri: Uri): MediaItemModel? {
        return scanner.resolveMediaItem(uri)
    }

    // Playback History
    fun getRecentlyPlayedHistory(): Flow<List<PlaybackHistoryEntity>> = historyDao.getAllHistory()

    suspend fun savePlaybackPosition(
        uri: String,
        title: String,
        artist: String = "",
        isVideo: Boolean,
        durationMs: Long,
        positionMs: Long
    ) = withContext(Dispatchers.IO) {
        val isCompleted = durationMs > 0 && positionMs >= durationMs - 5000 // Last 5 seconds
        historyDao.insertOrUpdate(
            PlaybackHistoryEntity(
                uri = uri,
                title = title,
                artist = artist,
                mediaType = if (isVideo) "video" else "audio",
                durationMs = durationMs,
                lastPositionMs = if (isCompleted) 0L else positionMs,
                lastPlayedTimestamp = System.currentTimeMillis(),
                isCompleted = isCompleted
            )
        )
    }

    suspend fun getResumePosition(uri: String): Long = withContext(Dispatchers.IO) {
        historyDao.getLastPosition(uri) ?: 0L
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        historyDao.clearAll()
    }

    // Favorites
    fun getAllFavorites(): Flow<List<FavoriteEntity>> = favoriteDao.getAllFavorites()

    fun isFavorite(uri: String): Flow<Boolean> = favoriteDao.isFavorite(uri)

    suspend fun toggleFavorite(media: MediaItemModel) = withContext(Dispatchers.IO) {
        val uriStr = media.uri.toString()
        val isFav = favoriteDao.isFavoriteSync(uriStr)
        if (isFav) {
            favoriteDao.removeFavorite(uriStr)
        } else {
            favoriteDao.addFavorite(
                FavoriteEntity(
                    uri = uriStr,
                    title = media.title,
                    mediaType = if (media.isVideo) "video" else "audio"
                )
            )
        }
    }

    // Playlists
    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    fun getPlaylistItems(playlistId: Long): Flow<List<PlaylistItemEntity>> =
        playlistDao.getItemsForPlaylist(playlistId)

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylist(PlaylistEntity(name = name))
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) = withContext(Dispatchers.IO) {
        val existing = playlistDao.getPlaylistById(playlistId)
        if (existing != null) {
            playlistDao.updatePlaylist(existing.copy(name = newName, updatedTimestamp = System.currentTimeMillis()))
        }
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addMediaToPlaylist(playlistId: Long, items: List<MediaItemModel>) = withContext(Dispatchers.IO) {
        val current = playlistDao.getItemsForPlaylistSync(playlistId)
        var nextPos = current.size
        for (item in items) {
            playlistDao.insertPlaylistItem(
                PlaylistItemEntity(
                    playlistId = playlistId,
                    uri = item.uri.toString(),
                    title = item.title,
                    artist = item.artist,
                    durationMs = item.duration,
                    mediaType = if (item.isVideo) "video" else "audio",
                    positionInPlaylist = nextPos++
                )
            )
        }
        val p = playlistDao.getPlaylistById(playlistId)
        if (p != null) {
            playlistDao.updatePlaylist(p.copy(updatedTimestamp = System.currentTimeMillis()))
        }
    }

    suspend fun removePlaylistItem(itemId: Long) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylistItem(itemId)
    }
}

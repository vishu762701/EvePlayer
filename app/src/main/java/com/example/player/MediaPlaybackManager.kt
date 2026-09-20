package com.example.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.EveApplication
import com.example.data.mediastore.MediaItemModel
import com.example.service.PlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class MediaPlaybackManager private constructor(private val context: Context) {

    private val trackSelector = DefaultTrackSelector(context)
    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setTrackSelector(trackSelector)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    val equalizerManager = AudioEqualizerManager()

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    // State Flows
    private val _currentMediaItem = MutableStateFlow<MediaItemModel?>(null)
    val currentMediaItem: StateFlow<MediaItemModel?> = _currentMediaItem.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _shuffleMode = MutableStateFlow(false)
    val shuffleMode: StateFlow<Boolean> = _shuffleMode.asStateFlow()

    private val _queue = MutableStateFlow<List<MediaItemModel>>(emptyList())
    val queue: StateFlow<List<MediaItemModel>> = _queue.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(0)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    private val _aspectRatioMode = MutableStateFlow(AspectRatioMode.FIT)
    val aspectRatioMode: StateFlow<AspectRatioMode> = _aspectRatioMode.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<TrackOption>>(emptyList())
    val audioTracks: StateFlow<List<TrackOption>> = _audioTracks.asStateFlow()

    private val _subtitleTracks = MutableStateFlow<List<TrackOption>>(emptyList())
    val subtitleTracks: StateFlow<List<TrackOption>> = _subtitleTracks.asStateFlow()

    private val _audioDelayMs = MutableStateFlow(0L)
    val audioDelayMs: StateFlow<Long> = _audioDelayMs.asStateFlow()

    private val _subtitleDelayMs = MutableStateFlow(0L)
    val subtitleDelayMs: StateFlow<Long> = _subtitleDelayMs.asStateFlow()

    private val _abRepeatPointA = MutableStateFlow<Long?>(null)
    val abRepeatPointA: StateFlow<Long?> = _abRepeatPointA.asStateFlow()

    private val _abRepeatPointB = MutableStateFlow<Long?>(null)
    val abRepeatPointB: StateFlow<Long?> = _abRepeatPointB.asStateFlow()

    private val _sleepTimerRemainingSeconds = MutableStateFlow<Int?>(null)
    val sleepTimerRemainingSeconds: StateFlow<Int?> = _sleepTimerRemainingSeconds.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    saveCurrentPosition()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _duration.value = exoPlayer.duration.coerceAtLeast(0L)
                    equalizerManager.attachAudioSession(exoPlayer.audioSessionId)
                } else if (playbackState == Player.STATE_ENDED) {
                    saveCurrentPosition(completed = true)
                    if (exoPlayer.hasNextMediaItem()) {
                        playNext()
                    }
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                updateTracksList(tracks)
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _errorMessage.value = "Playback error: ${error.localizedMessage ?: "Unknown format or corrupt file"}"
            }
        })
    }

    fun getExoPlayer(): ExoPlayer = exoPlayer

    fun playMedia(
        item: MediaItemModel,
        startPositionMs: Long? = null,
        newQueue: List<MediaItemModel> = listOf(item)
    ) {
        startPlaybackService()

        _queue.value = newQueue
        val index = newQueue.indexOfFirst { it.uri == item.uri }.coerceAtLeast(0)
        _currentQueueIndex.value = index
        _currentMediaItem.value = item
        _errorMessage.value = null

        // Clear A-B repeat
        _abRepeatPointA.value = null
        _abRepeatPointB.value = null

        val mediaItem = buildMediaItem(item)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        val resumePos = startPositionMs ?: item.lastPositionMs
        if (resumePos > 0) {
            exoPlayer.seekTo(resumePos)
            _currentPosition.value = resumePos
        } else {
            exoPlayer.seekTo(0)
            _currentPosition.value = 0L
        }

        exoPlayer.play()
    }

    fun playNext() {
        val q = _queue.value
        if (q.isEmpty()) return
        var nextIdx = _currentQueueIndex.value + 1
        if (nextIdx >= q.size) {
            if (_repeatMode.value == Player.REPEAT_MODE_ALL) {
                nextIdx = 0
            } else {
                return
            }
        }
        playMedia(q[nextIdx], startPositionMs = 0L, newQueue = q)
    }

    fun playPrevious() {
        val q = _queue.value
        if (q.isEmpty()) return
        if (_currentPosition.value > 3000L) {
            seekTo(0L)
            return
        }
        var prevIdx = _currentQueueIndex.value - 1
        if (prevIdx < 0) {
            prevIdx = if (_repeatMode.value == Player.REPEAT_MODE_ALL) q.size - 1 else 0
        }
        playMedia(q[prevIdx], startPositionMs = 0L, newQueue = q)
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun pause() {
        exoPlayer.pause()
        _isPlaying.value = false
        saveCurrentPosition()
    }

    fun resume() {
        startPlaybackService()
        exoPlayer.play()
        _isPlaying.value = true
    }

    fun seekTo(positionMs: Long) {
        val target = positionMs.coerceIn(0L, _duration.value.coerceAtLeast(0L))
        exoPlayer.seekTo(target)
        _currentPosition.value = target
    }

    fun seekBy(deltaMs: Long) {
        val current = exoPlayer.currentPosition
        seekTo(current + deltaMs)
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.25f, 4.0f)
        _playbackSpeed.value = clamped
        exoPlayer.playbackParameters = PlaybackParameters(clamped)
    }

    fun setRepeatMode(mode: Int) {
        _repeatMode.value = mode
        exoPlayer.repeatMode = mode
    }

    fun toggleRepeatMode() {
        val next = when (_repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        setRepeatMode(next)
    }

    fun toggleShuffle() {
        val newShuffle = !_shuffleMode.value
        _shuffleMode.value = newShuffle
        exoPlayer.shuffleModeEnabled = newShuffle
    }

    fun setAspectRatioMode(mode: AspectRatioMode) {
        _aspectRatioMode.value = mode
    }

    fun setAudioDelay(delayMs: Long) {
        _audioDelayMs.value = delayMs.coerceIn(-5000L, 5000L)
    }

    fun setSubtitleDelay(delayMs: Long) {
        _subtitleDelayMs.value = delayMs.coerceIn(-10000L, 10000L)
    }

    fun setAbRepeatPointA(pos: Long?) {
        _abRepeatPointA.value = pos
    }

    fun setAbRepeatPointB(pos: Long?) {
        _abRepeatPointB.value = pos
    }

    fun clearAbRepeat() {
        _abRepeatPointA.value = null
        _abRepeatPointB.value = null
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerRemainingSeconds.value = null
            return
        }

        val totalSec = minutes * 60
        _sleepTimerRemainingSeconds.value = totalSec

        sleepTimerJob = coroutineScope.launch {
            var remaining = totalSec
            while (isActive && remaining > 0) {
                delay(1000L)
                remaining--
                _sleepTimerRemainingSeconds.value = remaining
            }
            if (remaining <= 0) {
                pause()
                _sleepTimerRemainingSeconds.value = null
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingSeconds.value = null
    }

    fun loadExternalSubtitle(uri: Uri, mimeType: String = MimeTypes.APPLICATION_SUBRIP, label: String = "External") {
        val currentItem = _currentMediaItem.value ?: return
        val currentPos = exoPlayer.currentPosition
        val subConfig = MediaItem.SubtitleConfiguration.Builder(uri)
            .setMimeType(mimeType)
            .setLanguage("und")
            .setLabel(label)
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(currentItem.uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(currentItem.title)
                    .setArtist(currentItem.artist)
                    .build()
            )
            .setSubtitleConfigurations(listOf(subConfig))
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.seekTo(currentPos)
        exoPlayer.play()
    }

    fun selectAudioTrack(groupIndex: Int, trackIndex: Int) {
        val tracks = exoPlayer.currentTracks
        var currentAudioGroup = 0
        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_AUDIO) {
                if (currentAudioGroup == groupIndex) {
                    val override = TrackSelectionOverride(group.mediaTrackGroup, trackIndex)
                    trackSelector.parameters = trackSelector.buildUponParameters()
                        .setOverrideForType(override)
                        .build()
                    break
                }
                currentAudioGroup++
            }
        }
    }

    fun selectSubtitleTrack(groupIndex: Int, trackIndex: Int) {
        if (groupIndex == -1) {
            // Disable subtitles
            trackSelector.parameters = trackSelector.buildUponParameters()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            return
        }

        trackSelector.parameters = trackSelector.buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .build()

        val tracks = exoPlayer.currentTracks
        var currentTextGroup = 0
        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_TEXT) {
                if (currentTextGroup == groupIndex) {
                    val override = TrackSelectionOverride(group.mediaTrackGroup, trackIndex)
                    trackSelector.parameters = trackSelector.buildUponParameters()
                        .setOverrideForType(override)
                        .build()
                    break
                }
                currentTextGroup++
            }
        }
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        val current = _queue.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            _queue.value = current

            val active = _currentMediaItem.value
            if (active != null) {
                _currentQueueIndex.value = current.indexOfFirst { it.uri == active.uri }.coerceAtLeast(0)
            }
        }
    }

    fun removeFromQueue(index: Int) {
        val current = _queue.value.toMutableList()
        if (index in current.indices) {
            val removed = current.removeAt(index)
            _queue.value = current

            if (_currentMediaItem.value?.uri == removed.uri) {
                if (current.isNotEmpty()) {
                    val nextIdx = index.coerceAtMost(current.size - 1)
                    playMedia(current[nextIdx], startPositionMs = 0L, newQueue = current)
                } else {
                    exoPlayer.stop()
                    _currentMediaItem.value = null
                    _isPlaying.value = false
                }
            }
        }
    }

    fun addToQueueNext(item: MediaItemModel) {
        val current = _queue.value.toMutableList()
        val insertPos = (_currentQueueIndex.value + 1).coerceAtMost(current.size)
        current.add(insertPos, item)
        _queue.value = current
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = coroutineScope.launch {
            while (isActive && exoPlayer.isPlaying) {
                val pos = exoPlayer.currentPosition
                _currentPosition.value = pos

                // Check A-B repeat
                val a = _abRepeatPointA.value
                val b = _abRepeatPointB.value
                if (a != null && b != null && b > a && pos >= b) {
                    seekTo(a)
                }

                delay(250L)
            }
        }
    }

    private fun saveCurrentPosition(completed: Boolean = false) {
        val item = _currentMediaItem.value ?: return
        val pos = if (completed) 0L else exoPlayer.currentPosition
        val dur = exoPlayer.duration.coerceAtLeast(0L)

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val repo = (context.applicationContext as EveApplication).database
                repo.playbackHistoryDao().insertOrUpdate(
                    com.example.data.local.PlaybackHistoryEntity(
                        uri = item.uri.toString(),
                        title = item.title,
                        artist = item.artist,
                        mediaType = if (item.isVideo) "video" else "audio",
                        durationMs = dur,
                        lastPositionMs = pos,
                        lastPlayedTimestamp = System.currentTimeMillis(),
                        isCompleted = completed
                    )
                )
            } catch (_: Exception) {
            }
        }
    }

    private fun updateTracksList(tracks: Tracks) {
        val audioList = mutableListOf<TrackOption>()
        val subList = mutableListOf<TrackOption>()

        var audioGroupIdx = 0
        var textGroupIdx = 0

        for (group in tracks.groups) {
            val mediaTrackGroup = group.mediaTrackGroup
            if (group.type == C.TRACK_TYPE_AUDIO) {
                for (i in 0 until mediaTrackGroup.length) {
                    val format = mediaTrackGroup.getFormat(i)
                    val label = format.label ?: format.language ?: "Track ${i + 1}"
                    audioList.add(
                        TrackOption(
                            id = format.id ?: "$audioGroupIdx-$i",
                            groupIndex = audioGroupIdx,
                            trackIndex = i,
                            label = label,
                            language = format.language,
                            mimeType = format.sampleMimeType,
                            isSelected = group.isTrackSelected(i)
                        )
                    )
                }
                audioGroupIdx++
            } else if (group.type == C.TRACK_TYPE_TEXT) {
                for (i in 0 until mediaTrackGroup.length) {
                    val format = mediaTrackGroup.getFormat(i)
                    val label = format.label ?: format.language ?: "Subtitle ${i + 1}"
                    subList.add(
                        TrackOption(
                            id = format.id ?: "$textGroupIdx-$i",
                            groupIndex = textGroupIdx,
                            trackIndex = i,
                            label = label,
                            language = format.language,
                            mimeType = format.sampleMimeType,
                            isSelected = group.isTrackSelected(i)
                        )
                    )
                }
                textGroupIdx++
            }
        }

        _audioTracks.value = audioList
        _subtitleTracks.value = subList
    }

    private fun buildMediaItem(item: MediaItemModel): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(item.title)
            .setDisplayTitle(item.title)
            .setArtist(item.artist)
            .setAlbumTitle(item.album)
            .build()

        return MediaItem.Builder()
            .setUri(item.uri)
            .setMediaId(item.uri.toString())
            .setMediaMetadata(metadata)
            .build()
    }

    private fun startPlaybackService() {
        try {
            val intent = Intent(context, PlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (_: Exception) {
        }
    }

    fun release() {
        progressJob?.cancel()
        sleepTimerJob?.cancel()
        saveCurrentPosition()
        equalizerManager.release()
        exoPlayer.release()
    }

    companion object {
        @Volatile
        private var INSTANCE: MediaPlaybackManager? = null

        fun getInstance(context: Context): MediaPlaybackManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MediaPlaybackManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

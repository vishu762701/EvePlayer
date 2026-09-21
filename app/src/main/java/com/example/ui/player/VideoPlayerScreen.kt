package com.example.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.Build
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.mediastore.MediaItemModel
import com.example.player.AspectRatioMode
import com.example.player.MediaPlaybackManager
import com.example.ui.components.AudioDelayDialog
import com.example.ui.components.EqualizerBottomSheet
import com.example.ui.components.SleepTimerDialog
import com.example.ui.components.SubtitleSettingsDialog
import com.example.ui.components.TrackSelectionDialog
import com.example.ui.theme.EveRedPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    playbackManager: MediaPlaybackManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentMedia by playbackManager.currentMediaItem.collectAsState()
    val isPlaying by playbackManager.isPlaying.collectAsState()
    val currentPosition by playbackManager.currentPosition.collectAsState()
    val duration by playbackManager.duration.collectAsState()
    val playbackSpeed by playbackManager.playbackSpeed.collectAsState()
    val aspectRatioMode by playbackManager.aspectRatioMode.collectAsState()

    val audioTracks by playbackManager.audioTracks.collectAsState()
    val subtitleTracks by playbackManager.subtitleTracks.collectAsState()
    val audioDelayMs by playbackManager.audioDelayMs.collectAsState()
    val subtitleDelayMs by playbackManager.subtitleDelayMs.collectAsState()
    val pointA by playbackManager.abRepeatPointA.collectAsState()
    val pointB by playbackManager.abRepeatPointB.collectAsState()
    val sleepTimerSec by playbackManager.sleepTimerRemainingSeconds.collectAsState()

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }

    // UI Overlay state
    var areControlsVisible by remember { mutableStateOf(true) }
    var isScreenLocked by remember { mutableStateOf(false) }

    // Gesture HUD indicators
    var hudBrightness by remember { mutableStateOf<Float?>(null) }
    var hudVolume by remember { mutableStateOf<Int?>(null) }
    var hudSeekTarget by remember { mutableStateOf<Long?>(null) }
    var doubleTapSeekBadge by remember { mutableStateOf<Pair<Boolean, Int>?>(null) } // isForward to seconds

    // Video Zoom / Pan Scale
    var videoScale by remember { mutableFloatStateOf(1.0f) }

    // Dialog toggles
    var showAudioTrackDialog by remember { mutableStateOf(false) }
    var showSubtitleTrackDialog by remember { mutableStateOf(false) }
    var showAudioDelayDialog by remember { mutableStateOf(false) }
    var showSubtitleSettingsDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    // Subtitle file picker
    val subtitlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            playbackManager.loadExternalSubtitle(uri, label = uri.lastPathSegment ?: "External")
            scope.launch { snackbarHostState.showSnackbar("Subtitle loaded: ${uri.lastPathSegment}") }
        }
    }

    // Auto-hide controls timer
    LaunchedEffect(areControlsVisible, isPlaying) {
        if (areControlsVisible && isPlaying) {
            delay(4000L)
            areControlsVisible = false
        }
    }

    // System bars immersion management
    DisposableEffect(areControlsVisible) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (!areControlsVisible) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            val window = activity?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Keep screen on during playback
    DisposableEffect(Unit) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    var componentWidth by remember { mutableIntStateOf(0) }
    var componentHeight by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged {
                componentWidth = it.width
                componentHeight = it.height
            }
    ) {
        // Player Surface View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = playbackManager.getExoPlayer()
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.player = playbackManager.getExoPlayer()
                playerView.resizeMode = when (aspectRatioMode) {
                    AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    AspectRatioMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    AspectRatioMode.CROP -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    AspectRatioMode.RATIO_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    AspectRatioMode.RATIO_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = videoScale,
                    scaleY = videoScale
                )
        )

        // Gesture Touch Overlay Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isScreenLocked) {
                    if (isScreenLocked) {
                        detectTapGestures(
                            onTap = { areControlsVisible = !areControlsVisible }
                        )
                        return@pointerInput
                    }

                    detectTapGestures(
                        onTap = {
                            areControlsVisible = !areControlsVisible
                        },
                        onDoubleTap = { offset ->
                            val width = size.width
                            if (offset.x < width * 0.35f) {
                                // Double tap left: seek back 10s
                                playbackManager.seekBy(-10000L)
                                doubleTapSeekBadge = Pair(false, 10)
                                scope.launch {
                                    delay(800L)
                                    doubleTapSeekBadge = null
                                }
                            } else if (offset.x > width * 0.65f) {
                                // Double tap right: seek forward 10s
                                playbackManager.seekBy(10000L)
                                doubleTapSeekBadge = Pair(true, 10)
                                scope.launch {
                                    delay(800L)
                                    doubleTapSeekBadge = null
                                }
                            } else {
                                // Center double tap: toggle play/pause
                                playbackManager.togglePlayPause()
                            }
                        }
                    )
                }
                .pointerInput(isScreenLocked) {
                    if (isScreenLocked) return@pointerInput

                    // Pinch-to-zoom
                    detectTransformGestures { _, _, zoom, _ ->
                        videoScale = (videoScale * zoom).coerceIn(0.5f, 3.0f)
                    }
                }
                .pointerInput(isScreenLocked) {
                    if (isScreenLocked) return@pointerInput

                    awaitPointerEventScope {
                        var isDragging = false
                        var dragType = 0 // 1: Brightness, 2: Volume, 3: Seek
                        var startX = 0f
                        var startY = 0f
                        var initialVolume = 0
                        var initialBrightness = 0.5f
                        var initialPosition = 0L

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue

                            if (change.pressed) {
                                if (!isDragging) {
                                    isDragging = true
                                    startX = change.position.x
                                    startY = change.position.y
                                    initialPosition = playbackManager.currentPosition.value
                                    initialVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    initialBrightness = activity?.window?.attributes?.screenBrightness
                                        ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                                    if (initialBrightness < 0f) initialBrightness = 0.5f
                                } else {
                                    val deltaX = change.position.x - startX
                                    val deltaY = change.position.y - startY

                                    if (dragType == 0) {
                                        if (Math.abs(deltaY) > 20 && Math.abs(deltaY) > Math.abs(deltaX)) {
                                            dragType = if (startX < size.width / 2) 1 else 2
                                        } else if (Math.abs(deltaX) > 30) {
                                            dragType = 3
                                        }
                                    }

                                    when (dragType) {
                                        1 -> { // Brightness
                                            val fraction = (-deltaY / size.height)
                                            val newBrightness = (initialBrightness + fraction).coerceIn(0.01f, 1.0f)
                                            val lp = activity?.window?.attributes
                                            if (lp != null) {
                                                lp.screenBrightness = newBrightness
                                                activity.window.attributes = lp
                                            }
                                            hudBrightness = newBrightness
                                        }
                                        2 -> { // Volume
                                            val fraction = (-deltaY / size.height)
                                            val deltaVol = (fraction * maxVolume).toInt()
                                            val newVol = (initialVolume + deltaVol).coerceIn(0, maxVolume)
                                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                            hudVolume = newVol
                                        }
                                        3 -> { // Seek
                                            val seekFraction = (deltaX / size.width)
                                            val target = (initialPosition + (seekFraction * 60000L).toLong())
                                                .coerceIn(0L, duration.coerceAtLeast(0L))
                                            hudSeekTarget = target
                                        }
                                    }
                                }
                            } else {
                                if (isDragging) {
                                    if (dragType == 3 && hudSeekTarget != null) {
                                        playbackManager.seekTo(hudSeekTarget!!)
                                    }
                                    isDragging = false
                                    dragType = 0
                                    hudBrightness = null
                                    hudVolume = null
                                    hudSeekTarget = null
                                }
                            }
                        }
                    }
                }
        )

        // Gesture HUD Overlays (Center Cards)
        if (hudBrightness != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.75f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        Icons.Default.BrightnessMedium,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "${(hudBrightness!! * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (hudVolume != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.75f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val pct = (hudVolume!!.toFloat() / maxVolume.toFloat() * 100).toInt()
                    Text(
                        "$pct%",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (hudSeekTarget != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.75f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp)
                ) {
                    Text(
                        MediaItemModel.formatDuration(hudSeekTarget!!),
                        color = EveRedPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "/ ${MediaItemModel.formatDuration(duration)}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Double Tap Seek Ripple Badge
        doubleTapSeekBadge?.let { (isForward, sec) ->
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.75f),
                modifier = Modifier
                    .align(if (isForward) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 48.dp)
                    .size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (isForward) Icons.Default.Forward10 else Icons.Default.Replay10,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            "${if (isForward) "+$sec" else "-$sec"}s",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Screen Lock Indicator Floating Button (Always accessible if locked)
        if (isScreenLocked) {
            AnimatedVisibility(
                visible = areControlsVisible,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(24.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = EveRedPrimary,
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(onClick = { isScreenLocked = false }) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Unlock Screen",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Main Player Controls (Top, Bottom, Center)
        if (!isScreenLocked) {
            AnimatedVisibility(
                visible = areControlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Top Bar with Gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                                )
                            )
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentMedia?.title ?: "Video",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (playbackSpeed != 1.0f) {
                                    Text(
                                        text = "${playbackSpeed}x Speed",
                                        color = EveRedPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Audio track selection
                            IconButton(onClick = { showAudioTrackDialog = true }) {
                                Icon(Icons.Default.Audiotrack, contentDescription = "Audio Tracks", tint = Color.White)
                            }

                            // Subtitle track selection
                            IconButton(onClick = { showSubtitleTrackDialog = true }) {
                                Icon(Icons.Default.Subtitles, contentDescription = "Subtitles", tint = Color.White)
                            }

                            // Aspect ratio mode toggle
                            IconButton(onClick = {
                                val next = when (aspectRatioMode) {
                                    AspectRatioMode.FIT -> AspectRatioMode.FILL
                                    AspectRatioMode.FILL -> AspectRatioMode.CROP
                                    AspectRatioMode.CROP -> AspectRatioMode.RATIO_16_9
                                    AspectRatioMode.RATIO_16_9 -> AspectRatioMode.RATIO_4_3
                                    AspectRatioMode.RATIO_4_3 -> AspectRatioMode.FIT
                                }
                                playbackManager.setAspectRatioMode(next)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Aspect Ratio: ${next.label}")
                                }
                            }) {
                                Icon(Icons.Default.AspectRatio, contentDescription = "Aspect Ratio", tint = Color.White)
                            }

                            // Overflow menu
                            Box {
                                IconButton(onClick = { showMoreMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                                }

                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Equalizer") },
                                        leadingIcon = { Icon(Icons.Default.GraphicEq, contentDescription = null) },
                                        onClick = {
                                            showMoreMenu = false
                                            showEqualizerSheet = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Audio Delay") },
                                        leadingIcon = { Icon(Icons.Default.Audiotrack, contentDescription = null) },
                                        onClick = {
                                            showMoreMenu = false
                                            showAudioDelayDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Subtitle Settings") },
                                        leadingIcon = { Icon(Icons.Default.Subtitles, contentDescription = null) },
                                        onClick = {
                                            showMoreMenu = false
                                            showSubtitleSettingsDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Sleep Timer") },
                                        leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
                                        onClick = {
                                            showMoreMenu = false
                                            showSleepTimerDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                                                    "Sensor Orientation" else "Lock Landscape"
                                            )
                                        },
                                        leadingIcon = { Icon(Icons.Default.ScreenRotation, contentDescription = null) },
                                        onClick = {
                                            showMoreMenu = false
                                            if (activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                            } else {
                                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Center Playback Buttons
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        IconButton(
                            onClick = { playbackManager.playPrevious() },
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = EveRedPrimary,
                            modifier = Modifier.size(68.dp)
                        ) {
                            IconButton(
                                onClick = { playbackManager.togglePlayPause() }
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { playbackManager.playNext() },
                            modifier = Modifier.size(52.dp)
                        ) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // Bottom Bar Controls with Gradient
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Time & Scrubber Slider
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = MediaItemModel.formatDuration(currentPosition),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            var sliderPosition by remember { mutableFloatStateOf(0f) }
                            var isUserScrubbing by remember { mutableStateOf(false) }

                            val currentProg = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f

                            Slider(
                                value = if (isUserScrubbing) sliderPosition else currentProg,
                                onValueChange = {
                                    isUserScrubbing = true
                                    sliderPosition = it
                                },
                                onValueChangeFinished = {
                                    val targetMs = (sliderPosition * duration).toLong()
                                    playbackManager.seekTo(targetMs)
                                    isUserScrubbing = false
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = EveRedPrimary,
                                    activeTrackColor = EveRedPrimary,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 10.dp)
                            )

                            Text(
                                text = MediaItemModel.formatDuration(duration),
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Bottom Actions Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Screen Lock Button
                            IconButton(onClick = { isScreenLocked = true }) {
                                Icon(Icons.Default.LockOpen, contentDescription = "Lock Controls", tint = Color.White)
                            }

                            // Speed Selector
                            TextButton(onClick = { showSpeedDialog = true }) {
                                Text(
                                    "${playbackSpeed}x",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            // A-B Repeat Button
                            IconButton(onClick = {
                                if (pointA == null) {
                                    playbackManager.setAbRepeatPointA(currentPosition)
                                    scope.launch { snackbarHostState.showSnackbar("Point A set at ${MediaItemModel.formatDuration(currentPosition)}") }
                                } else if (pointB == null) {
                                    playbackManager.setAbRepeatPointB(currentPosition)
                                    scope.launch { snackbarHostState.showSnackbar("Point B set at ${MediaItemModel.formatDuration(currentPosition)}") }
                                } else {
                                    playbackManager.clearAbRepeat()
                                    scope.launch { snackbarHostState.showSnackbar("A-B Repeat cleared") }
                                }
                            }) {
                                Icon(
                                    Icons.Default.Repeat,
                                    contentDescription = "A-B Repeat",
                                    tint = if (pointA != null) EveRedPrimary else Color.White
                                )
                            }

                            // Screenshot / Frame Capture
                            IconButton(onClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Frame captured at ${MediaItemModel.formatDuration(currentPosition)}")
                                }
                            }) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Screenshot", tint = Color.White)
                            }

                            // Picture-in-Picture Button
                            IconButton(onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity != null) {
                                    val params = PictureInPictureParams.Builder().build()
                                    activity.enterPictureInPictureMode(params)
                                }
                            }) {
                                Icon(Icons.Default.PictureInPictureAlt, contentDescription = "Picture-in-Picture", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Snackbar Host for status toasts
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp)
        )
    }

    // Dialogs
    if (showSpeedDialog) {
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 3.0f)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Playback Speed") },
            text = {
                Column {
                    speeds.forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    playbackManager.setPlaybackSpeed(speed)
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = playbackSpeed == speed,
                                onClick = {
                                    playbackManager.setPlaybackSpeed(speed)
                                    showSpeedDialog = false
                                },
                                colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = EveRedPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${speed}x", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSpeedDialog = false }) { Text("Close") }
            }
        )
    }

    if (showAudioTrackDialog) {
        TrackSelectionDialog(
            title = "Select Audio Track",
            tracks = audioTracks,
            onSelectTrack = { group, track ->
                playbackManager.selectAudioTrack(group, track)
            },
            onDismiss = { showAudioTrackDialog = false }
        )
    }

    if (showSubtitleTrackDialog) {
        TrackSelectionDialog(
            title = "Select Subtitle",
            tracks = subtitleTracks,
            onSelectTrack = { group, track ->
                playbackManager.selectSubtitleTrack(group, track)
            },
            onDisableTrack = {
                playbackManager.selectSubtitleTrack(-1, -1)
            },
            onDismiss = { showSubtitleTrackDialog = false }
        )
    }

    if (showAudioDelayDialog) {
        AudioDelayDialog(
            currentDelayMs = audioDelayMs,
            onDelayChange = { playbackManager.setAudioDelay(it) },
            onDismiss = { showAudioDelayDialog = false }
        )
    }

    if (showSubtitleSettingsDialog) {
        SubtitleSettingsDialog(
            currentDelayMs = subtitleDelayMs,
            onDelayChange = { playbackManager.setSubtitleDelay(it) },
            onLoadExternalSubtitle = {
                subtitlePickerLauncher.launch(arrayOf("*/*"))
                showSubtitleSettingsDialog = false
            },
            onDismiss = { showSubtitleSettingsDialog = false }
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            currentRemainingSec = sleepTimerSec,
            onSetTimer = { playbackManager.startSleepTimer(it) },
            onCancelTimer = { playbackManager.cancelSleepTimer() },
            onDismiss = { showSleepTimerDialog = false }
        )
    }

    if (showEqualizerSheet) {
        val eqState by playbackManager.equalizerManager.state.collectAsState()
        EqualizerBottomSheet(
            equalizerState = eqState,
            onToggleEnabled = { playbackManager.equalizerManager.setEnabled(it) },
            onBandLevelChange = { b, l -> playbackManager.equalizerManager.setBandLevel(b, l) },
            onSelectPreset = { playbackManager.equalizerManager.setPreset(it) },
            onBassBoostChange = { playbackManager.equalizerManager.setBassBoost(it) },
            onDismiss = { showEqualizerSheet = false }
        )
    }
}

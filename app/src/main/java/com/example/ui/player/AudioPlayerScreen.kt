package com.example.ui.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.example.data.mediastore.MediaItemModel
import com.example.player.MediaPlaybackManager
import com.example.ui.components.EqualizerBottomSheet
import com.example.ui.components.QueueBottomSheet
import com.example.ui.components.SleepTimerDialog
import com.example.ui.theme.EveRedPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    playbackManager: MediaPlaybackManager,
    onToggleFavorite: (MediaItemModel) -> Unit,
    onBack: () -> Unit
) {
    val currentMedia by playbackManager.currentMediaItem.collectAsState()
    val isPlaying by playbackManager.isPlaying.collectAsState()
    val currentPosition by playbackManager.currentPosition.collectAsState()
    val duration by playbackManager.duration.collectAsState()
    val playbackSpeed by playbackManager.playbackSpeed.collectAsState()
    val repeatMode by playbackManager.repeatMode.collectAsState()
    val shuffleMode by playbackManager.shuffleMode.collectAsState()
    val queue by playbackManager.queue.collectAsState()
    val currentQueueIndex by playbackManager.currentQueueIndex.collectAsState()
    val sleepTimerSec by playbackManager.sleepTimerRemainingSeconds.collectAsState()

    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    // Vinyl spin animation
    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinRotation"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Now Playing", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEqualizerSheet = true }) {
                        Icon(Icons.Default.GraphicEq, contentDescription = "Equalizer", tint = EveRedPrimary)
                    }
                    IconButton(onClick = { showSleepTimerDialog = true }) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = "Sleep Timer",
                            tint = if (sleepTimerSec != null) EveRedPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Vinyl Record Disc Art
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF26080B), Color(0xFF111114), Color(0xFF070709))
                        )
                    )
                    .rotate(if (isPlaying) rotation else 0f)
            ) {
                // Grooves
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxSize(0.85f)
                ) {}

                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxSize(0.65f)
                ) {}

                // Center Label
                Surface(
                    shape = CircleShape,
                    color = EveRedPrimary,
                    modifier = Modifier.fillMaxSize(0.35f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Center Hole
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.size(16.dp)
                ) {}
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Track Information
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentMedia?.title ?: "No Track Selected",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentMedia?.artist ?: "Unknown Artist",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!currentMedia?.album.isNullOrEmpty() && currentMedia?.album != "<unknown>") {
                            Text(
                                text = currentMedia!!.album,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    currentMedia?.let { media ->
                        IconButton(onClick = { onToggleFavorite(media) }) {
                            Icon(
                                imageVector = if (media.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (media.isFavorite) EveRedPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Slider
            Column(modifier = Modifier.fillMaxWidth()) {
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
                        activeTrackColor = EveRedPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = MediaItemModel.formatDuration(currentPosition),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = MediaItemModel.formatDuration(duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Shuffle
                IconButton(onClick = { playbackManager.toggleShuffle() }) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffleMode) EveRedPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Previous
                IconButton(
                    onClick = { playbackManager.playPrevious() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Play / Pause
                Surface(
                    shape = CircleShape,
                    color = EveRedPrimary,
                    modifier = Modifier.size(68.dp)
                ) {
                    IconButton(onClick = { playbackManager.togglePlayPause() }) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                // Next
                IconButton(
                    onClick = { playbackManager.playNext() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Repeat Mode
                IconButton(onClick = { playbackManager.toggleRepeatMode() }) {
                    val (icon, tint) = when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne to EveRedPrimary
                        Player.REPEAT_MODE_ALL -> Icons.Default.Repeat to EveRedPrimary
                        else -> Icons.Default.Repeat to MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Icon(icon, contentDescription = "Repeat", tint = tint)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary Audio Controls (Speed, Queue)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { showSpeedDialog = true }) {
                    Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${playbackSpeed}x")
                }

                TextButton(onClick = { showQueueSheet = true }) {
                    Icon(Icons.Default.PlaylistPlay, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Queue (${queue.size})")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Sheets & Dialogs
    if (showSpeedDialog) {
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        AlertDialog(
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
                            RadioButton(
                                selected = playbackSpeed == speed,
                                onClick = {
                                    playbackManager.setPlaybackSpeed(speed)
                                    showSpeedDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = EveRedPrimary)
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

    if (showQueueSheet) {
        QueueBottomSheet(
            queue = queue,
            currentIndex = currentQueueIndex,
            onSelectTrack = { index ->
                val track = queue.getOrNull(index)
                if (track != null) {
                    playbackManager.playMedia(track, startPositionMs = 0L, newQueue = queue)
                }
            },
            onRemoveTrack = { playbackManager.removeFromQueue(it) },
            onDismiss = { showQueueSheet = false }
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

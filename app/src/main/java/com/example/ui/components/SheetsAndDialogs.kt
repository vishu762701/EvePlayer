package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PlaylistEntity
import com.example.data.mediastore.MediaItemModel
import com.example.player.EqualizerState
import com.example.player.TrackOption
import com.example.ui.theme.EveRedPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    currentSortBy: String,
    currentSortAsc: Boolean,
    onSortSelected: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val options = listOf(
        "DATE" to "Date Modified",
        "NAME" to "File Name / Title",
        "SIZE" to "File Size",
        "DURATION" to "Length / Duration"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Sort, contentDescription = null, tint = EveRedPrimary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Sort By", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            options.forEach { (key, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSortSelected(key, currentSortAsc) }
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentSortBy.equals(key, ignoreCase = true),
                        onClick = { onSortSelected(key, currentSortAsc) },
                        colors = RadioButtonDefaults.colors(selectedColor = EveRedPrimary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onSortSelected(currentSortBy, true) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentSortAsc) EveRedPrimary else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ascending")
                }

                Button(
                    onClick = { onSortSelected(currentSortBy, false) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!currentSortAsc) EveRedPrimary else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Descending")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onConfirm(name.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = EveRedPrimary)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddToPlaylistDialog(
    playlists: List<PlaylistEntity>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (Long) -> Unit,
    onCreateNewPlaylist: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Playlist") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onCreateNewPlaylist,
                    colors = ButtonDefaults.buttonColors(containerColor = EveRedPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create New Playlist")
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (playlists.isEmpty()) {
                    Text(
                        "No playlists yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(playlists) { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSelectPlaylist(p.id) }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = EveRedPrimary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(p.name, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerBottomSheet(
    equalizerState: EqualizerState,
    onToggleEnabled: (Boolean) -> Unit,
    onBandLevelChange: (Int, Short) -> Unit,
    onSelectPreset: (Int) -> Unit,
    onBassBoostChange: (Short) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .fillMaxWidth()
        ) {
            // Header with Enable switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = EveRedPrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Equalizer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }

                Switch(
                    checked = equalizerState.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(checkedThumbColor = EveRedPrimary)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Presets
            if (equalizerState.presets.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(equalizerState.presets) { index, presetName ->
                        FilterChip(
                            selected = equalizerState.currentPreset == index,
                            onClick = { onSelectPreset(index) },
                            label = { Text(presetName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EveRedPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Frequency Bands Sliders
            if (equalizerState.bandLevels.isNotEmpty()) {
                Text(
                    "Frequency Bands",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                val minL = equalizerState.minBandLevel.toFloat()
                val maxL = equalizerState.maxBandLevel.toFloat()

                LazyColumn(
                    modifier = Modifier.height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(equalizerState.bandLevels) { index, level ->
                        val freq = equalizerState.bandFrequencies.getOrNull(index) ?: 0
                        val freqStr = if (freq >= 1000) "${freq / 1000} kHz" else "$freq Hz"
                        val db = level / 100

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = freqStr,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(64.dp)
                            )

                            Slider(
                                value = level.toFloat(),
                                onValueChange = { onBandLevelChange(index, it.toInt().toShort()) },
                                valueRange = minL..maxL,
                                enabled = equalizerState.isEnabled,
                                colors = SliderDefaults.colors(
                                    thumbColor = EveRedPrimary,
                                    activeTrackColor = EveRedPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = "${if (db > 0) "+$db" else "$db"} dB",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(50.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bass Boost Slider
            if (equalizerState.isBassBoostSupported) {
                Text(
                    "Bass Boost: ${equalizerState.bassBoostStrength / 10}%",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = equalizerState.bassBoostStrength.toFloat(),
                    onValueChange = { onBassBoostChange(it.toInt().toShort()) },
                    valueRange = 0f..1000f,
                    enabled = equalizerState.isEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = EveRedPrimary,
                        activeTrackColor = EveRedPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    queue: List<MediaItemModel>,
    currentIndex: Int,
    onSelectTrack: (Int) -> Unit,
    onRemoveTrack: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = EveRedPrimary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Playing Queue (${queue.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (queue.isEmpty()) {
                Text(
                    "Queue is empty",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.height(350.dp)) {
                    itemsIndexed(queue) { index, item ->
                        val isCurrent = index == currentIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isCurrent) EveRedPrimary.copy(alpha = 0.15f) else Color.Transparent
                                )
                                .clickable { onSelectTrack(index) }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isCurrent) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = EveRedPrimary, modifier = Modifier.size(20.dp))
                            } else {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCurrent) EveRedPrimary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (item.isVideo) item.formattedDuration else "${item.artist} • ${item.formattedDuration}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }

                            IconButton(
                                onClick = { onRemoveTrack(index) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SleepTimerDialog(
    currentRemainingSec: Int?,
    onSetTimer: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        15 to "15 minutes",
        30 to "30 minutes",
        45 to "45 minutes",
        60 to "60 minutes",
        90 to "90 minutes"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = EveRedPrimary)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Sleep Timer")
            }
        },
        text = {
            Column {
                if (currentRemainingSec != null && currentRemainingSec > 0) {
                    val m = currentRemainingSec / 60
                    val s = currentRemainingSec % 60
                    Text(
                        "Timer active: ${m}m ${s}s remaining",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EveRedPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                options.forEach { (mins, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSetTimer(mins)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            if (currentRemainingSec != null) {
                Button(
                    onClick = {
                        onCancelTimer()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Turn Off")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AudioDelayDialog(
    currentDelayMs: Long,
    onDelayChange: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var delay by remember { mutableFloatStateOf(currentDelayMs.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Audio Delay") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${delay.toInt()} ms",
                    style = MaterialTheme.typography.headlineMedium,
                    color = EveRedPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Slider(
                    value = delay,
                    onValueChange = { delay = it },
                    valueRange = -3000f..3000f,
                    steps = 59,
                    colors = SliderDefaults.colors(
                        thumbColor = EveRedPrimary,
                        activeTrackColor = EveRedPrimary
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("-3.0s", style = MaterialTheme.typography.bodySmall)
                    Text("0s", style = MaterialTheme.typography.bodySmall)
                    Text("+3.0s", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDelayChange(delay.toLong())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EveRedPrimary)
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SubtitleSettingsDialog(
    currentDelayMs: Long,
    onDelayChange: (Long) -> Unit,
    onLoadExternalSubtitle: () -> Unit,
    onDismiss: () -> Unit
) {
    var delay by remember { mutableFloatStateOf(currentDelayMs.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Subtitles") },
        text = {
            Column {
                Button(
                    onClick = onLoadExternalSubtitle,
                    colors = ButtonDefaults.buttonColors(containerColor = EveRedPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Load External Subtitle File (.srt, .vtt, .ass)")
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text("Subtitle Delay", style = MaterialTheme.typography.titleSmall)
                Text(
                    "${delay.toInt()} ms",
                    style = MaterialTheme.typography.titleMedium,
                    color = EveRedPrimary,
                    fontWeight = FontWeight.Bold
                )

                Slider(
                    value = delay,
                    onValueChange = { delay = it },
                    valueRange = -5000f..5000f,
                    steps = 99,
                    colors = SliderDefaults.colors(
                        thumbColor = EveRedPrimary,
                        activeTrackColor = EveRedPrimary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("-5.0s", style = MaterialTheme.typography.bodySmall)
                    Text("0s", style = MaterialTheme.typography.bodySmall)
                    Text("+5.0s", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDelayChange(delay.toLong())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EveRedPrimary)
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun TrackSelectionDialog(
    title: String,
    tracks: List<TrackOption>,
    onSelectTrack: (Int, Int) -> Unit,
    onDisableTrack: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(modifier = Modifier.height(250.dp)) {
                if (onDisableTrack != null) {
                    val noneSelected = tracks.none { it.isSelected }
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onDisableTrack()
                                    onDismiss()
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = noneSelected,
                                onClick = {
                                    onDisableTrack()
                                    onDismiss()
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = EveRedPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Disable / None")
                        }
                    }
                }

                items(tracks) { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSelectTrack(track.groupIndex, track.trackIndex)
                                onDismiss()
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = track.isSelected,
                            onClick = {
                                onSelectTrack(track.groupIndex, track.trackIndex)
                                onDismiss()
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = EveRedPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(track.label, style = MaterialTheme.typography.bodyMedium, fontWeight = if (track.isSelected) FontWeight.Bold else FontWeight.Normal)
                            if (!track.language.isNullOrEmpty()) {
                                Text(track.language, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

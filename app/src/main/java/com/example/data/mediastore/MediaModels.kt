package com.example.data.mediastore

import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MediaItemModel(
    val id: Long,
    val uri: Uri,
    val title: String,
    val displayName: String,
    val artist: String = "",
    val album: String = "",
    val duration: Long = 0L,
    val size: Long = 0L,
    val dateModified: Long = 0L,
    val mimeType: String = "",
    val folderName: String = "",
    val folderPath: String = "",
    val isVideo: Boolean = false,
    val width: Int = 0,
    val height: Int = 0,
    val lastPositionMs: Long = 0L,
    val isFavorite: Boolean = false
) {
    val formattedDuration: String
        get() = formatDuration(duration)

    val formattedSize: String
        get() = formatFileSize(size)

    val formattedDate: String
        get() = formatDate(dateModified)

    companion object {
        fun formatDuration(durationMs: Long): String {
            if (durationMs <= 0) return "00:00"
            val totalSeconds = durationMs / 1000
            val seconds = totalSeconds % 60
            val minutes = (totalSeconds / 60) % 60
            val hours = totalSeconds / 3600
            return if (hours > 0) {
                String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            }
        }

        fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(Locale.getDefault(), "%.1f GB", gb)
                mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
                kb >= 1.0 -> String.format(Locale.getDefault(), "%.1f KB", kb)
                else -> "$bytes B"
            }
        }

        fun formatDate(timestampSec: Long): String {
            if (timestampSec <= 0) return ""
            val date = Date(timestampSec * 1000)
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            return sdf.format(date)
        }
    }
}

data class MediaFolder(
    val folderName: String,
    val folderPath: String,
    val itemCount: Int,
    val isVideo: Boolean,
    val sampleUri: Uri?
)

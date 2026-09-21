package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "eve_settings")

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED
}

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.AMOLED,
    val defaultPlaybackSpeed: Float = 1.0f,
    val defaultSeekIntervalSec: Int = 10,
    val resumeMode: String = "ALWAYS", // ALWAYS, ASK, NEVER
    val gestureBrightnessEnabled: Boolean = true,
    val gestureVolumeEnabled: Boolean = true,
    val gestureSeekEnabled: Boolean = true,
    val gestureDoubleTapEnabled: Boolean = true,
    val gesturePinchZoomEnabled: Boolean = true,
    val subtitlesDefaultSize: Float = 18f,
    val subtitlesDefaultColor: Long = 0xFFFFFFFF,
    val subtitlesBackground: Boolean = true,
    val isVideoGridView: Boolean = true,
    val isAudioGridView: Boolean = false,
    val videoSortBy: String = "DATE",
    val videoSortAsc: Boolean = false,
    val audioSortBy: String = "NAME",
    val audioSortAsc: Boolean = true
)

class AppPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_PLAYBACK_SPEED = floatPreferencesKey("default_playback_speed")
        val DEFAULT_SEEK_INTERVAL = intPreferencesKey("default_seek_interval")
        val RESUME_MODE = stringPreferencesKey("resume_mode")
        val GESTURE_BRIGHTNESS = booleanPreferencesKey("gesture_brightness")
        val GESTURE_VOLUME = booleanPreferencesKey("gesture_volume")
        val GESTURE_SEEK = booleanPreferencesKey("gesture_seek")
        val GESTURE_DOUBLE_TAP = booleanPreferencesKey("gesture_double_tap")
        val GESTURE_PINCH_ZOOM = booleanPreferencesKey("gesture_pinch_zoom")
        val SUBTITLE_SIZE = floatPreferencesKey("subtitle_size")
        val SUBTITLE_COLOR = longPreferencesKey("subtitle_color")
        val SUBTITLE_BG = booleanPreferencesKey("subtitle_bg")
        val VIDEO_GRID_VIEW = booleanPreferencesKey("video_grid_view")
        val AUDIO_GRID_VIEW = booleanPreferencesKey("audio_grid_view")
        val VIDEO_SORT_BY = stringPreferencesKey("video_sort_by")
        val VIDEO_SORT_ASC = booleanPreferencesKey("video_sort_asc")
        val AUDIO_SORT_BY = stringPreferencesKey("audio_sort_by")
        val AUDIO_SORT_ASC = booleanPreferencesKey("audio_sort_asc")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data.map { preferences ->
        val themeString = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.AMOLED.name
        val theme = try {
            ThemeMode.valueOf(themeString)
        } catch (_: Exception) {
            ThemeMode.AMOLED
        }

        UserSettings(
            themeMode = theme,
            defaultPlaybackSpeed = preferences[PreferencesKeys.DEFAULT_PLAYBACK_SPEED] ?: 1.0f,
            defaultSeekIntervalSec = preferences[PreferencesKeys.DEFAULT_SEEK_INTERVAL] ?: 10,
            resumeMode = preferences[PreferencesKeys.RESUME_MODE] ?: "ALWAYS",
            gestureBrightnessEnabled = preferences[PreferencesKeys.GESTURE_BRIGHTNESS] ?: true,
            gestureVolumeEnabled = preferences[PreferencesKeys.GESTURE_VOLUME] ?: true,
            gestureSeekEnabled = preferences[PreferencesKeys.GESTURE_SEEK] ?: true,
            gestureDoubleTapEnabled = preferences[PreferencesKeys.GESTURE_DOUBLE_TAP] ?: true,
            gesturePinchZoomEnabled = preferences[PreferencesKeys.GESTURE_PINCH_ZOOM] ?: true,
            subtitlesDefaultSize = preferences[PreferencesKeys.SUBTITLE_SIZE] ?: 18f,
            subtitlesDefaultColor = preferences[PreferencesKeys.SUBTITLE_COLOR] ?: 0xFFFFFFFF,
            subtitlesBackground = preferences[PreferencesKeys.SUBTITLE_BG] ?: true,
            isVideoGridView = preferences[PreferencesKeys.VIDEO_GRID_VIEW] ?: true,
            isAudioGridView = preferences[PreferencesKeys.AUDIO_GRID_VIEW] ?: false,
            videoSortBy = preferences[PreferencesKeys.VIDEO_SORT_BY] ?: "DATE",
            videoSortAsc = preferences[PreferencesKeys.VIDEO_SORT_ASC] ?: false,
            audioSortBy = preferences[PreferencesKeys.AUDIO_SORT_BY] ?: "NAME",
            audioSortAsc = preferences[PreferencesKeys.AUDIO_SORT_ASC] ?: true
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun setDefaultPlaybackSpeed(speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_PLAYBACK_SPEED] = speed
        }
    }

    suspend fun setDefaultSeekInterval(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_SEEK_INTERVAL] = seconds
        }
    }

    suspend fun setResumeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RESUME_MODE] = mode
        }
    }

    suspend fun setGestureBrightness(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GESTURE_BRIGHTNESS] = enabled
        }
    }

    suspend fun setGestureVolume(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GESTURE_VOLUME] = enabled
        }
    }

    suspend fun setGestureSeek(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GESTURE_SEEK] = enabled
        }
    }

    suspend fun setGestureDoubleTap(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GESTURE_DOUBLE_TAP] = enabled
        }
    }

    suspend fun setGesturePinchZoom(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GESTURE_PINCH_ZOOM] = enabled
        }
    }

    suspend fun setSubtitlePreferences(size: Float, color: Long, background: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SUBTITLE_SIZE] = size
            preferences[PreferencesKeys.SUBTITLE_COLOR] = color
            preferences[PreferencesKeys.SUBTITLE_BG] = background
        }
    }

    suspend fun setVideoGridView(isGrid: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VIDEO_GRID_VIEW] = isGrid
        }
    }

    suspend fun setAudioGridView(isGrid: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUDIO_GRID_VIEW] = isGrid
        }
    }

    suspend fun setVideoSorting(sortBy: String, asc: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VIDEO_SORT_BY] = sortBy
            preferences[PreferencesKeys.VIDEO_SORT_ASC] = asc
        }
    }

    suspend fun setAudioSorting(sortBy: String, asc: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUDIO_SORT_BY] = sortBy
            preferences[PreferencesKeys.AUDIO_SORT_ASC] = asc
        }
    }
}

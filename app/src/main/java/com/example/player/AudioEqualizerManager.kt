package com.example.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EqualizerState(
    val isEnabled: Boolean = true,
    val isSupported: Boolean = false,
    val numberOfBands: Short = 5,
    val minBandLevel: Short = -1500,
    val maxBandLevel: Short = 1500,
    val bandLevels: List<Short> = emptyList(),
    val bandFrequencies: List<Int> = emptyList(),
    val presets: List<String> = emptyList(),
    val currentPreset: Int = -1, // -1 is custom
    val bassBoostStrength: Short = 0,
    val isBassBoostSupported: Boolean = false
)

class AudioEqualizerManager {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var currentAudioSessionId: Int = 0

    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    fun attachAudioSession(audioSessionId: Int) {
        if (audioSessionId == 0 || audioSessionId == currentAudioSessionId) return
        release()
        currentAudioSessionId = audioSessionId

        try {
            val eq = Equalizer(0, audioSessionId).apply {
                enabled = _state.value.isEnabled
            }
            equalizer = eq

            val bands = eq.numberOfBands
            val minLevel = eq.bandLevelRange[0]
            val maxLevel = eq.bandLevelRange[1]

            val freqs = mutableListOf<Int>()
            val levels = mutableListOf<Short>()
            for (i in 0 until bands) {
                val band = i.toShort()
                freqs.add(eq.getCenterFreq(band) / 1000) // in Hz
                levels.add(eq.getBandLevel(band))
            }

            val presetsList = mutableListOf<String>()
            val numPresets = eq.numberOfPresets
            for (p in 0 until numPresets) {
                presetsList.add(eq.getPresetName(p.toShort()))
            }

            var bbSupported = false
            var bbStrength: Short = 0
            try {
                val bb = BassBoost(0, audioSessionId).apply {
                    enabled = true
                }
                bassBoost = bb
                bbSupported = bb.strengthSupported
                if (bbSupported) {
                    bbStrength = bb.roundedStrength
                }
            } catch (_: Exception) {
            }

            _state.value = _state.value.copy(
                isSupported = true,
                numberOfBands = bands,
                minBandLevel = minLevel,
                maxBandLevel = maxLevel,
                bandLevels = levels,
                bandFrequencies = freqs,
                presets = presetsList,
                bassBoostStrength = bbStrength,
                isBassBoostSupported = bbSupported
            )
        } catch (_: Exception) {
            _state.value = _state.value.copy(isSupported = false)
        }
    }

    fun setEnabled(enabled: Boolean) {
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            _state.value = _state.value.copy(isEnabled = enabled)
        } catch (_: Exception) {
        }
    }

    fun setBandLevel(bandIndex: Int, level: Short) {
        try {
            equalizer?.setBandLevel(bandIndex.toShort(), level)
            val updated = _state.value.bandLevels.toMutableList()
            if (bandIndex in updated.indices) {
                updated[bandIndex] = level
            }
            _state.value = _state.value.copy(
                bandLevels = updated,
                currentPreset = -1 // marked custom
            )
        } catch (_: Exception) {
        }
    }

    fun setPreset(presetIndex: Int) {
        if (presetIndex < 0) return
        try {
            equalizer?.usePreset(presetIndex.toShort())
            val bands = equalizer?.numberOfBands ?: 0
            val levels = mutableListOf<Short>()
            for (i in 0 until bands) {
                levels.add(equalizer?.getBandLevel(i.toShort()) ?: 0)
            }
            _state.value = _state.value.copy(
                currentPreset = presetIndex,
                bandLevels = levels
            )
        } catch (_: Exception) {
        }
    }

    fun setBassBoost(strength: Short) {
        try {
            bassBoost?.setStrength(strength)
            _state.value = _state.value.copy(bassBoostStrength = strength)
        } catch (_: Exception) {
        }
    }

    fun release() {
        try {
            equalizer?.release()
        } catch (_: Exception) {
        }
        try {
            bassBoost?.release()
        } catch (_: Exception) {
        }
        equalizer = null
        bassBoost = null
        currentAudioSessionId = 0
    }
}

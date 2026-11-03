package com.ibramusic.mobile.playback.audiofx

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class SoundPreset(val key: String, val title: String, val description: String) {
    FLAT("flat", "Flat", "Respuesta de estudio original y equilibrada"),
    BASS("bass", "Bass Booster", "Potenciación de graves y sub-bajos"),
    VOCAL("vocal", "Vocal Booster", "Realce de medios y claridad en voces"),
    ELECTRONIC("electronic", "Electronic", "Graves profundos y agudos brillantes"),
    ROCK("rock", "Rock", "Pegada en frecuencias graves y distorsión limpia"),
    ACOUSTIC("acoustic", "Acoustic", "Calidez orgánica para cuerdas e instrumentos"),
    CUSTOM("custom", "Personalizado", "Ajuste manual de bandas de frecuencia")
}

@Singleton
class EqualizerManager @Inject constructor() {

    companion object {
        private const val TAG = "EqualizerManager"
        val PRESET_MAP = mapOf(
            SoundPreset.FLAT to listOf(0, 0, 0, 0, 0),
            SoundPreset.BASS to listOf(600, 450, 0, 0, 0),
            SoundPreset.VOCAL to listOf(-200, 300, 550, 350, 100),
            SoundPreset.ELECTRONIC to listOf(500, 300, -100, 250, 450),
            SoundPreset.ROCK to listOf(450, 200, 100, 300, 450),
            SoundPreset.ACOUSTIC to listOf(200, 100, 300, 200, 200)
        )
        val BASS_BOOST_MAP = mapOf(
            SoundPreset.FLAT to 0,
            SoundPreset.BASS to 650,
            SoundPreset.VOCAL to 0,
            SoundPreset.ELECTRONIC to 500,
            SoundPreset.ROCK to 350,
            SoundPreset.ACOUSTIC to 150
        )
    }

    private var currentAudioSessionId: Int = 0
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    private val _enabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _activePreset = MutableStateFlow(SoundPreset.FLAT)
    val activePreset: StateFlow<SoundPreset> = _activePreset.asStateFlow()

    private val _bandLevels = MutableStateFlow(listOf(0, 0, 0, 0, 0))
    val bandLevels: StateFlow<List<Int>> = _bandLevels.asStateFlow()

    private val _bassStrength = MutableStateFlow(0)
    val bassStrength: StateFlow<Int> = _bassStrength.asStateFlow()

    /**
     * Binds audio effect instances to the active ExoPlayer audioSessionId.
     */
    fun attachAudioSession(audioSessionId: Int) {
        if (audioSessionId == 0 || audioSessionId == currentAudioSessionId) return
        currentAudioSessionId = audioSessionId

        release()

        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = _enabled.value
            }
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = _enabled.value
                if (strengthSupported) {
                    setStrength(_bassStrength.value.toShort())
                }
            }

            applyCurrentSettingsToHardware()
            Log.d(TAG, "Equalizer & BassBoost attached to session $audioSessionId (bands: ${equalizer?.numberOfBands})")
        } catch (e: Exception) {
            Log.w(TAG, "AudioFX setup warning on session $audioSessionId: ${e.message}")
        }
    }

    fun setEnabled(enabled: Boolean) {
        _enabled.value = enabled
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
        } catch (e: Exception) {
            Log.w(TAG, "Error setting EQ enabled state: ${e.message}")
        }
    }

    fun selectPreset(preset: SoundPreset) {
        _activePreset.value = preset
        if (preset != SoundPreset.CUSTOM) {
            val levels = PRESET_MAP[preset] ?: listOf(0, 0, 0, 0, 0)
            val bass = BASS_BOOST_MAP[preset] ?: 0
            _bandLevels.value = levels
            _bassStrength.value = bass
            applyCurrentSettingsToHardware()
        }
    }

    fun setBandLevel(bandIndex: Int, levelInMilliBels: Int) {
        if (bandIndex in 0..4) {
            val current = _bandLevels.value.toMutableList()
            current[bandIndex] = levelInMilliBels
            _bandLevels.value = current
            _activePreset.value = SoundPreset.CUSTOM
            try {
                if (equalizer != null && bandIndex < (equalizer?.numberOfBands ?: 0)) {
                    equalizer?.setBandLevel(bandIndex.toShort(), levelInMilliBels.toShort())
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error setting band $bandIndex: ${e.message}")
            }
        }
    }

    fun setBassStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _bassStrength.value = clamped
        try {
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(clamped.toShort())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error setting bass boost strength: ${e.message}")
        }
    }

    private fun applyCurrentSettingsToHardware() {
        try {
            val eq = equalizer ?: return
            val numBands = eq.numberOfBands.toInt()
            val levels = _bandLevels.value

            for (i in 0 until numBands) {
                val level = levels.getOrNull(i) ?: 0
                eq.setBandLevel(i.toShort(), level.toShort())
            }

            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(_bassStrength.value.toShort())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed applying settings to hardware: ${e.message}")
        }
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
        } catch (e: Exception) {
            // Ignore
        } finally {
            equalizer = null
            bassBoost = null
        }
    }
}


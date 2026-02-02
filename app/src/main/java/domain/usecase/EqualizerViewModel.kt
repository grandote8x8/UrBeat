package domain.usecase

import android.app.Application
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.equalizer2.data.repository.AudioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import data.network.ESP32Service

class EqualizerViewModel(application: Application) : AndroidViewModel(application) {

    private val audioRepository = AudioRepository(application)
    private var mediaPlayer: MediaPlayer? = null

    // 🆕 Servicio ESP32
    private val esp32Service = ESP32Service()

    // 🆕 Estado de conexión ESP32
    private val _esp32Connected = MutableStateFlow(false)
    val esp32Connected: StateFlow<Boolean> = _esp32Connected.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    val equalizerState = audioRepository.equalizerState
    val visualizerData = audioRepository.visualizerData

    // Estado del volumen
    private val _volume = MutableStateFlow(0.7f) // Volumen inicial al 70%
    val volume: StateFlow<Float> = _volume.asStateFlow()

    // 🆕 Función para cambiar el volumen (con envío a ESP32)
    fun setVolume(newVolume: Float) {
        _volume.value = newVolume.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(_volume.value, _volume.value)

        // 🚀 Envía el volumen al ESP32
        viewModelScope.launch {
            val volumePercent = (_volume.value * 100).toInt()
            esp32Service.sendVolume(volumePercent)
        }
    }

    fun playUserAudio(uri: Uri) {
        mediaPlayer?.release()

        mediaPlayer = MediaPlayer().apply {
            setDataSource(getApplication(), uri)
            setOnPreparedListener {
                val sessionId = audioSessionId
                audioRepository.initializeEqualizer(sessionId)
                audioRepository.initializeVisualizer(sessionId)
                setVolume(_volume.value, _volume.value)
                start()
                _isPlaying.value = true
            }
            setOnCompletionListener {
                _isPlaying.value = false
                audioRepository.pauseVisualizer()
            }
            prepareAsync()
        }
    }

    fun togglePlayback() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
                audioRepository.pauseVisualizer()

                // 🚀 Opcional: Enviar estado de pausa al ESP32
                viewModelScope.launch {
                    esp32Service.sendPlayPause(false)
                }
            } else {
                it.start()
                _isPlaying.value = true
                audioRepository.resumeVisualizer()

                // 🚀 Opcional: Enviar estado de play al ESP32
                viewModelScope.launch {
                    esp32Service.sendPlayPause(true)
                }
            }
        }
    }

    // 🆕 Función actualizada para enviar banda al ESP32
    fun setBandLevel(bandIndex: Int, level: Float) {
        viewModelScope.launch {
            audioRepository.setBandLevel(bandIndex, level)

            // 🚀 Envía la banda al ESP32
            // Mapeo de índices a frecuencias
            val frequencyMap = mapOf(
                0 to "60Hz",
                1 to "230Hz",
                2 to "910Hz",
                3 to "3.6kHz",
                4 to "14kHz"
            )

            frequencyMap[bandIndex]?.let { frequency ->
                esp32Service.sendEqualizerBand(frequency, level)
            }
        }
    }

    // 🆕 Función actualizada para enviar preset al ESP32
    fun applyPreset(preset: ui.common.EqualizerPreset) {
        if (preset == ui.common.EqualizerPreset.CUSTOM) return

        preset.gains.forEachIndexed { index, gain ->
            audioRepository.setBandLevel(index, gain)
        }

        // 🚀 Envía todas las bandas al ESP32
        viewModelScope.launch {
            val bandsMap = mapOf(
                "60Hz" to preset.gains.getOrElse(0) { 0f },
                "230Hz" to preset.gains.getOrElse(1) { 0f },
                "910Hz" to preset.gains.getOrElse(2) { 0f },
                "3.6kHz" to preset.gains.getOrElse(3) { 0f },
                "14kHz" to preset.gains.getOrElse(4) { 0f }
            )
            esp32Service.sendAllEqualizerBands(bandsMap)
        }
    }

    fun toggleEqualizer(enabled: Boolean) {
        viewModelScope.launch {
            audioRepository.toggleEqualizer(enabled)
        }
    }

    // 🆕 Función para verificar conexión con ESP32
    fun checkESP32Connection() {
        viewModelScope.launch {
            _esp32Connected.value = esp32Service.checkConnection()
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        audioRepository.release()
    }

    private val repository: AudioRepository = AudioRepository(application)

    val signalState = repository.signalState

    // Métodos comentados del generador de señales
    // fun setSignalFrequency(freq: Float) {
    //     repository.startSignalGenerator(freq, signalState.value.amplitude)
    // }

    // fun setSignalAmplitude(amp: Float) {
    //     repository.startSignalGenerator(signalState.value.frequency, amp)
    // }

    // fun stopSignalGenerator() {
    //     repository.stopSignalGenerator()
    // }
}
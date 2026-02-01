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

class EqualizerViewModel(application: Application) : AndroidViewModel(application) {

    private val audioRepository = AudioRepository(application)
    private var mediaPlayer: MediaPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    val equalizerState = audioRepository.equalizerState
    val visualizerData = audioRepository.visualizerData

    // 🆕 Estado del volumen
    private val _volume = MutableStateFlow(0.7f) // Volumen inicial al 70%
    val volume: StateFlow<Float> = _volume.asStateFlow()

    // 🆕 Función para cambiar el volumen
    fun setVolume(newVolume: Float) {
        _volume.value = newVolume.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(_volume.value, _volume.value)
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
                // 🔹 NUEVO: Pausar visualizer cuando termina la canción
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
                // 🔹 NUEVO: Pausar visualizer y resetear datos
                audioRepository.pauseVisualizer()
            } else {
                it.start()
                _isPlaying.value = true
                // 🔹 NUEVO: Reanudar visualizer
                audioRepository.resumeVisualizer()
            }
        }
    }

    fun setBandLevel(bandIndex: Int, level: Float) {
        viewModelScope.launch {
            audioRepository.setBandLevel(bandIndex, level)
        }
    }

    fun applyPreset(preset: ui.common.EqualizerPreset) {
        if (preset == ui.common.EqualizerPreset.CUSTOM) return

        preset.gains.forEachIndexed { index, gain ->
            audioRepository.setBandLevel(index, gain)
        }
    }

    fun toggleEqualizer(enabled: Boolean) {
        viewModelScope.launch {
            audioRepository.toggleEqualizer(enabled)
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        audioRepository.release()
    }

    private val repository: AudioRepository = AudioRepository(application)

    val signalState = repository.signalState

    //fun setSignalFrequency(freq: Float) {
    //    repository.startSignalGenerator(freq, signalState.value.amplitude)
        //}

    //fun setSignalAmplitude(amp: Float) {
        //   repository.startSignalGenerator(signalState.value.frequency, amp)
        // }

    //fun stopSignalGenerator() {
    //    repository.stopSignalGenerator()
    //}
}
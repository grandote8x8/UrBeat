package com.example.equalizer2.data.repository

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.audiofx.Equalizer
import android.media.audiofx.Visualizer
import android.util.Log
import com.example.equalizer2.domain.model.AudioVisualizerData
import com.example.equalizer2.domain.model.EqualizerBand
import ui.common.EqualizerPreset
import com.example.equalizer2.domain.model.EqualizerState
import domain.model.SignalGeneratorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

class AudioRepository(private val context: Context) {

    private var equalizer: Equalizer? = null
    private var visualizer: Visualizer? = null

    private val _equalizerState = MutableStateFlow(EqualizerState())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    private val _visualizerData = MutableStateFlow(AudioVisualizerData())
    val visualizerData: StateFlow<AudioVisualizerData> = _visualizerData.asStateFlow()

    companion object {
        private const val TAG = "AudioRepository"
        private const val CAPTURE_SIZE = 1024
        private const val BAR_COUNT = 32
    }

    // Dentro de AudioRepository.kt
    private val _signalState = MutableStateFlow(SignalGeneratorState())
    val signalState: StateFlow<SignalGeneratorState> = _signalState.asStateFlow()

    private var audioTrack: AudioTrack? = null

    fun startSignalGenerator(frequency: Float, amplitude: Float) {
        stopSignalGenerator()

        _signalState.value = SignalGeneratorState(frequency, amplitude, true)

        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
            AudioTrack.MODE_STREAM
        ).apply { play() }

        Thread {
            val buffer = ShortArray(bufferSize)
            var phase = 0.0
            val twoPi = 2 * Math.PI

            while (_signalState.value.isEnabled) {
                for (i in buffer.indices) {
                    buffer[i] = (_signalState.value.amplitude *
                            Short.MAX_VALUE *
                            kotlin.math.sin(twoPi * _signalState.value.frequency * phase / sampleRate)).toInt().toShort()
                    phase += 1
                }
                audioTrack?.write(buffer, 0, buffer.size)
            }
        }.start()
    }

    fun stopSignalGenerator() {
        _signalState.value = _signalState.value.copy(isEnabled = false)
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }

    // 🔹 SE LLAMA CUANDO EL USUARIO ELIGE CANCIÓN
    fun initializeEqualizer(audioSessionId: Int) {
        try {
            releaseEqualizer()

            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
            }

            val bands = createEqualizerBands()
            _equalizerState.value = _equalizerState.value.copy(
                isEnabled = true,
                bands = bands
            )

            Log.d(TAG, "Equalizer initialized for session $audioSessionId")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing equalizer", e)
        }
    }

    // 🔹 VISUALIZER REAL (canción del usuario)
    fun initializeVisualizer(audioSessionId: Int) {
        try {
            releaseVisualizer()

            visualizer = Visualizer(audioSessionId).apply {
                captureSize = CAPTURE_SIZE

                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {

                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) {
                            waveform ?: return

                            val normalized = normalizeWaveform(waveform)

                            _visualizerData.value = _visualizerData.value.copy(
                                waveform = normalized
                            )
                        }

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            fft ?: return

                            val amplitudes = calculateAmplitudes(fft)

                            _visualizerData.value = _visualizerData.value.copy(
                                amplitudes = amplitudes
                            )
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    true,
                    true
                )

                enabled = true
            }

            Log.d(TAG, "Visualizer initialized for session $audioSessionId")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing visualizer", e)
        }
    }

    // 🔹 NUEVO: Pausar visualizer y resetear datos
    fun pauseVisualizer() {
        try {
            visualizer?.enabled = false
            // Resetear los datos a valores vacíos/cero
            _visualizerData.value = AudioVisualizerData(
                waveform = FloatArray(CAPTURE_SIZE) { 0.5f }, // 0.5f = línea central
                amplitudes = FloatArray(BAR_COUNT) { 0f }      // 0f = sin altura
            )
            Log.d(TAG, "Visualizer paused and data reset")
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing visualizer", e)
        }
    }

    // 🔹 NUEVO: Reanudar visualizer
    fun resumeVisualizer() {
        try {
            visualizer?.enabled = true
            Log.d(TAG, "Visualizer resumed")
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming visualizer", e)
        }
    }

    // 🔹 EQUALIZER BANDS (sin cambios de lógica)
    private fun createEqualizerBands(): List<EqualizerBand> {
        val eq = equalizer ?: return emptyList()
        val numberOfBands = eq.numberOfBands.toInt()
        val bandFrequencies = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")

        return (0 until numberOfBands).map { i ->
            val frequency = eq.getCenterFreq(i.toShort()) / 1000
            val label = bandFrequencies.getOrNull(i) ?: "${frequency}Hz"

            EqualizerBand(
                frequency = frequency,
                label = label,
                gain = 0f,
                minGain = eq.bandLevelRange[0] / 100f,
                maxGain = eq.bandLevelRange[1] / 100f
            )
        }
    }

    // 🔹 CONTROL DE BANDAS
    fun setBandLevel(bandIndex: Int, level: Float) {
        try {
            equalizer?.let { eq ->
                val millibels = (level * 100).toInt().toShort()
                eq.setBandLevel(bandIndex.toShort(), millibels)

                val updatedBands = _equalizerState.value.bands.toMutableList()
                if (bandIndex < updatedBands.size) {
                    updatedBands[bandIndex] =
                        updatedBands[bandIndex].copy(gain = level)

                    _equalizerState.value = _equalizerState.value.copy(
                        bands = updatedBands
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting band level", e)
        }
    }

    fun toggleEqualizer(enabled: Boolean) {
        equalizer?.enabled = enabled
        _equalizerState.value = _equalizerState.value.copy(isEnabled = enabled)
    }

    // 🔹 NORMALIZACIÓN (perfecta para tu WaveformVisualizer)
    private fun normalizeWaveform(waveform: ByteArray): FloatArray {
        return FloatArray(waveform.size) { i ->
            (waveform[i].toInt() + 128) / 255f
        }
    }

    // 🔹 AMPLITUDES SUAVES (32 barras) - CORREGIDO
    private fun calculateAmplitudes(fft: ByteArray): FloatArray {
        val amplitudes = FloatArray(BAR_COUNT)

        // FFT data viene en pares (real, imaginario)
        // Usamos solo la mitad del FFT (frecuencias positivas)
        val usableSize = fft.size / 2
        val barSize = usableSize / BAR_COUNT

        for (i in 0 until BAR_COUNT) {
            var sum = 0.0
            var count = 0

            // Promediamos las magnitudes dentro de cada rango de frecuencia
            val startIdx = i * barSize
            val endIdx = minOf(startIdx + barSize, usableSize - 1)

            for (j in startIdx until endIdx step 2) {
                if (j + 1 < fft.size) {
                    val real = fft[j].toInt()
                    val imag = fft[j + 1].toInt()
                    val magnitude = sqrt((real * real + imag * imag).toDouble())
                    sum += magnitude
                    count++
                }
            }

            // Normalizamos y aplicamos una curva logarítmica para mejor visualización
            val avgMagnitude = if (count > 0) sum / count else 0.0
            amplitudes[i] = (avgMagnitude / 100.0).toFloat().coerceIn(0f, 1f)
        }

        return amplitudes
    }

    fun releaseEqualizer() {
        try {
            equalizer?.release()
            equalizer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing equalizer", e)
        }
    }

    fun releaseVisualizer() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
            visualizer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing visualizer", e)
        }
    }

    fun release() {
        releaseEqualizer()
        releaseVisualizer()
        stopSignalGenerator()
    }
}
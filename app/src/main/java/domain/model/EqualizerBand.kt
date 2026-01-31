package com.example.equalizer2.domain.model

data class EqualizerBand(
    val frequency: Int, // Frequency in Hz
    val label: String,  // Display label (e.g., "60Hz", "230Hz")
    val gain: Float = 0f, // Gain in dB (-15 to +15)
    val minGain: Float = -15f,
    val maxGain: Float = 15f
)

data class EqualizerState(
    val isEnabled: Boolean = true,
    val bands: List<EqualizerBand> = emptyList(),
    val preset: EqualizerPreset = EqualizerPreset.FLAT
)

enum class EqualizerPreset {
    FLAT,
    ROCK,
    POP,
    JAZZ,
    CLASSICAL,
    DANCE,
    BASS_BOOST,
    TREBLE_BOOST,
    CUSTOM
}

data class AudioVisualizerData(
    val waveform: FloatArray = FloatArray(0),
    val amplitudes: FloatArray = FloatArray(0)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AudioVisualizerData

        if (!waveform.contentEquals(other.waveform)) return false
        if (!amplitudes.contentEquals(other.amplitudes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = waveform.contentHashCode()
        result = 31 * result + amplitudes.contentHashCode()
        return result
    }
}
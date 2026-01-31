package domain.model

data class SignalGeneratorState(
    val frequency: Float = 1000f,  // Hz
    val amplitude: Float = 0.5f,   // 0..1
    val isEnabled: Boolean = false // si el generador está activo
)

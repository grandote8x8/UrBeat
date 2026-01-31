package ui.common

enum class EqualizerPreset(
    val gains: List<Float>
) {
    FLAT(listOf(0f, 0f, 0f, 0f, 0f)),
    ROCK(listOf(5f, 3f, -1f, 0f, 4f)),
    POP(listOf(-1f, 2f, 4f, 3f, -1f)),
    JAZZ(listOf(3f, 2f, -1f, 2f, 4f)),
    CLASSICAL(listOf(4f, 3f, -1f, 2f, 3f)),
    DANCE(listOf(6f, 4f, 0f, 2f, 4f)),

    CUSTOM(emptyList())
}
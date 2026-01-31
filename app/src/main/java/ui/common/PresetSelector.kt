package ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PresetSelector(
    selectedPreset: EqualizerPreset,
    onPresetSelected: (EqualizerPreset) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        EqualizerPreset.values()
            .filter { it != EqualizerPreset.CUSTOM }
            .forEach { preset ->
                PresetChip(
                    name = preset.name.lowercase().replaceFirstChar { it.uppercase() },
                    selected = preset == selectedPreset,
                    onClick = { onPresetSelected(preset) }
                )
            }
    }
}

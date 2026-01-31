package ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.example.equalizer2.domain.model.EqualizerBand
import com.example.equalizer2.ui.theme.*

@Composable
fun EqualizerControl(
    bands: List<EqualizerBand>,
    onBandChanged: (Int, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = SurfaceVariant,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Ecualizador",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bands.forEachIndexed { index, band ->
                VerticalSlider(
                    label = band.label,
                    value = band.gain,
                    minValue = band.minGain,
                    maxValue = band.maxGain,
                    onValueChange = { newValue ->
                        onBandChanged(index, newValue)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun VerticalSlider(
    label: String,
    value: Float,
    minValue: Float,
    maxValue: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var localValue by remember { mutableStateOf(value) }

    LaunchedEffect(value) {
        localValue = value
    }

    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (localValue >= 0) "+${localValue.toInt()}" else "${localValue.toInt()}",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (localValue != 0f) NeonPink else TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .width(40.dp)
                .height(200.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(SliderTrack)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val normalizedY =
                                1f - (offset.y / size.height).coerceIn(0f, 1f)
                            val newValue =
                                minValue + normalizedY * (maxValue - minValue)
                            localValue = newValue
                            onValueChange(newValue)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val normalizedY =
                                1f - (change.position.y / size.height).coerceIn(0f, 1f)
                            val newValue =
                                minValue + normalizedY * (maxValue - minValue)
                            localValue = newValue
                            onValueChange(newValue)
                        }
                    )
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            val normalizedValue =
                ((localValue - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(normalizedValue)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(NeonPink, NeonPurple)
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = -(200.dp * normalizedValue))
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonPink, NeonPurple)
                        )
                    )
            )
        }

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun PresetSelector(
    selectedPreset: String,
    presets: List<String>,
    onPresetSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = SurfaceVariant,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = "Elige tu ecualización automática...",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        presets.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { preset ->
                    PresetButton(
                        text = preset,
                        isSelected = preset == selectedPreset,
                        onClick = { onPresetSelected(preset) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun PresetButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) {
                    Brush.horizontalGradient(
                        colors = listOf(NeonPink, NeonPurple)
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(SliderTrack, SliderTrack)
                    )
                }
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else TextSecondary
        )
    }
}

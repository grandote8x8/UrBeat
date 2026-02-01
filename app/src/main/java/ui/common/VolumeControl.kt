package ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.equalizer2.ui.theme.*

@Composable
fun VolumeControl(
    volume: Float, // Valor entre 0f y 1f
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var localVolume by remember { mutableStateOf(volume) }
    var sliderWidth by remember { mutableStateOf(0) }

    LaunchedEffect(volume) {
        localVolume = volume
    }

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
        // Título
        Text(
            text = "Volumen",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Fila con icono, barra y porcentaje
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icono de volumen
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SliderTrack),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (localVolume == 0f) "🔇" else if (localVolume < 0.5f) "🔉" else "🔊",
                    fontSize = 20.sp
                )
            }

            // Barra horizontal de volumen
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SliderTrack)
                    .onSizeChanged { size ->
                        sliderWidth = size.width
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val normalizedX = (offset.x / size.width).coerceIn(0f, 1f)
                                localVolume = normalizedX
                                onVolumeChange(normalizedX)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val normalizedX =
                                    (change.position.x / size.width).coerceIn(0f, 1f)
                                localVolume = normalizedX
                                onVolumeChange(normalizedX)
                            }
                        )
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                // Barra de progreso con gradiente
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(localVolume)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(NeonPink, NeonPurple)
                            )
                        )
                )

                // Indicador circular
                if (localVolume > 0f && sliderWidth > 0) {
                    val thumbPosition = (sliderWidth * localVolume - 12 * 3).coerceAtLeast(0f)
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = (thumbPosition / 3).dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(NeonPink, NeonPurple)
                                )
                            )
                    )
                }
            }

            // Porcentaje
            Text(
                text = "${(localVolume * 100).toInt()}%",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (localVolume > 0f) NeonPink else TextSecondary,
                modifier = Modifier.width(50.dp)
            )
        }
    }
}
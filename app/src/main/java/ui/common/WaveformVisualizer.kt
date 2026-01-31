package ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.equalizer2.ui.theme.NeonPink
import com.example.equalizer2.ui.theme.NeonPurple
import com.example.equalizer2.ui.theme.SurfaceVariant
import kotlin.math.sin

private fun smooth(data: FloatArray, window: Int = 5): FloatArray {
    return FloatArray(data.size) { i ->
        val start = maxOf(0, i - window)
        val end = minOf(data.lastIndex, i + window)
        (start..end).map { data[it] }.average().toFloat()
    }
}

@Composable
fun WaveformVisualizer(
    waveform: FloatArray,
    modifier: Modifier = Modifier,
    isAnimating: Boolean = true
) {
    var animationProgress by remember { mutableStateOf(0f) }
    var trailPoints by remember { mutableStateOf(listOf<Float>()) }

    // Guardamos los últimos 1024 puntos del waveform
    LaunchedEffect(waveform) {
        trailPoints = waveform.toList().takeLast(1024)
    }

    // Animación tipo scroll horizontal
    LaunchedEffect(isAnimating) {
        if (isAnimating) {
            while (true) {
                animationProgress = (animationProgress + 0.02f) % 1f
                kotlinx.coroutines.delay(16) // ~60fps
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                color = SurfaceVariant,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f
            val path = Path()

            if (trailPoints.isEmpty()) {
                // Demo sine wave
                val points = 512
                for (i in 0..points) {
                    val x = (i.toFloat() / points) * width
                    val y = centerY + sin((i.toFloat() / points + animationProgress) * Math.PI * 4).toFloat() * height * 0.2f
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
            } else {
                // Dibujamos el waveform real tipo osciloscopio
                val smoothed = smooth(trailPoints.toFloatArray())
                val displayPoints = 512
                val step = smoothed.size / displayPoints.toFloat()

                for (i in 0 until displayPoints) {
                    val idx = (i * step).toInt().coerceIn(0, smoothed.lastIndex)
                    val nextIdx = ((i + 1) * step).toInt().coerceIn(0, smoothed.lastIndex)
                    val interpolated = (smoothed[idx] + smoothed[nextIdx]) / 2f
                    val x = (i.toFloat() / (displayPoints - 1)) * width
                    val y = centerY + (interpolated - 0.5f) * height * 0.8f
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
            }

            // Línea principal con gradiente AC/DC
            drawPath(
                path = path,
                brush = Brush.horizontalGradient(
                    colors = listOf(NeonPurple, NeonPink, NeonPurple),
                    startX = 0f,
                    endX = width
                ),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Glow electrico
            drawPath(
                path = path,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        NeonPurple.copy(alpha = 0.3f),
                        NeonPink.copy(alpha = 0.3f),
                        NeonPurple.copy(alpha = 0.3f)
                    ),
                    startX = 0f,
                    endX = width
                ),
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )

            // Línea central
            drawLine(
                color = Color.White.copy(alpha = 0.1f),
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

@Composable
fun AmplitudeVisualizer(
    amplitudes: FloatArray,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(
                color = SurfaceVariant,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val barCount = 32
            val barWidth = width / barCount
            val spacing = 4.dp.toPx()

            if (amplitudes.isEmpty()) {
                for (i in 0 until barCount) {
                    val barHeight = height * (0.3f + sin(i * 0.5f).toFloat() * 0.3f)
                    val x = i * barWidth
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(NeonPink, NeonPurple),
                            startY = height - barHeight,
                            endY = height
                        ),
                        topLeft = Offset(x + spacing, height - barHeight),
                        size = androidx.compose.ui.geometry.Size(barWidth - spacing * 2, barHeight)
                    )
                }
            } else {
                val step = amplitudes.size / barCount.coerceAtLeast(1)
                for (i in 0 until barCount) {
                    val index = (i * step).coerceIn(0, amplitudes.size - 1)
                    val amplitude = amplitudes[index].coerceIn(0f, 1f)
                    val barHeight = height * amplitude
                    val x = i * barWidth
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(NeonPink, NeonPurple),
                            startY = height - barHeight,
                            endY = height
                        ),
                        topLeft = Offset(x + spacing, height - barHeight),
                        size = androidx.compose.ui.geometry.Size(
                            barWidth - spacing * 2,
                            barHeight.coerceAtLeast(2.dp.toPx())
                        )
                    )
                }
            }
        }
    }
}

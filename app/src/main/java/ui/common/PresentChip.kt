package ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PresetChip(
    name: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val glowSize by animateDpAsState(
        targetValue = if (selected) 12.dp else 0.dp,
        label = "Glow"
    )

    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else Color(0xFFB0A7FF),
        label = "TextColor"
    )

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFB84CFF), // morado neon
            Color(0xFFFF4FD8)  // rosa neon
        )
    )

    Text(
        text = name,
        color = textColor,
        modifier = Modifier
            .shadow(
                elevation = glowSize,
                shape = RoundedCornerShape(50),
                ambientColor = Color(0xFFB84CFF),
                spotColor = Color(0xFFFF4FD8)
            )
            .border(
                width = 1.5.dp,
                brush = if (selected) borderBrush else Brush.linearGradient(
                    listOf(Color.DarkGray, Color.DarkGray)
                ),
                shape = RoundedCornerShape(50)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

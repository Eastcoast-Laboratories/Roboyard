package roboyard.ui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * Color variants for the fancy gradient buttons, matching the original
 * button_fancy_*.xml ripple drawables from the fragment-app branch.
 */
enum class FancyButtonColor(
    val startColor: Color,
    val endColor: Color,
    val strokeColor: Color,
    val textColor: Color
) {
    RED(Color(0xFFFF6B6B), Color(0xFFE55555), Color(0xFFFF8A8A), Color.White),
    GREEN(Color(0xFF50C878), Color(0xFF3A9B5C), Color(0xFF66D68A), Color.White),
    BLUE(Color(0xFF4A90E2), Color(0xFF357ABD), Color(0xFF5BA3F5), Color.White),
    GRAY(Color(0xFF6C7B7F), Color(0xFF556065), Color(0xFF8A9499), Color.White),
    HINT(Color(0xFFFFE082), Color(0xFFFFCC02), Color(0xFFFFB300), Color(0xFF1A1A1A)),
    PURPLE(Color(0xFF9C27B0), Color(0xFF7B1FA2), Color(0xFFBA68C8), Color.White)
}

/**
 * Color variants for circular buttons in header/footer, matching
 * circular_button_*.xml drawables from the fragment-app branch.
 */
enum class CircularButtonColor(
    val fillColor: Color,
    val strokeColor: Color,
    val textColor: Color
) {
    TURQUOISE(Color(0xFF00BCD4), Color.Black, Color.White),
    YELLOW(Color(0xFFFFC107), Color.Black, Color.Black),
    ORANGE(Color(0xFFFF9800), Color.Black, Color.White),
    PURPLE(Color(0xFF9C27B0), Color.Black, Color.White),
    GRAY(Color(0xFF757575), Color.Black, Color.White)
}

/**
 * A fully-rounded gradient button matching the original fancy buttons.
 * Uses a 135-degree diagonal gradient, a 1dp border, and pill-shaped corners.
 */
@Composable
fun FancyButton(
    text: String,
    color: FancyButtonColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    // 135-degree gradient: top-left (start) to bottom-right (end)
    val brush = Brush.linearGradient(
        colors = listOf(color.startColor, color.endColor),
        start = Offset(0f, 0f),
        end = Offset.Infinite
    )
    val shape = RoundedCornerShape(percent = 50)

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(brush, shape)
            .border(BorderStroke(1.dp, color.strokeColor), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color.textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * A circular button matching the original circular_button_*.xml drawables.
 * Used for header/footer icon buttons.
 */
@Composable
fun CircularButton(
    text: String? = null,
    color: CircularButtonColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(shape)
            .background(color.fillColor, shape)
            .border(BorderStroke(2.dp, color.strokeColor), shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        text?.let {
            Text(
                text = it,
                color = color.textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp
            )
        }
    }
}

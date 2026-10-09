package roboyard.ui.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import roboyard.logic.core.Constants

/**
 * Unit test for the live move counter status text formatting
 * (mirrors the Android SpannableString spans of the liveMoveCounterText
 * observer in GameFragment: number 1.5x, label 0.9x, "Δ+x" 1.5x in the
 * deviation color; high-contrast mode forces all colors to black).
 *
 * Run: ./gradlew :composeApp:desktopTest --tests "roboyard.ui.compose.LiveCounterStatusTextTest"
 */
class LiveCounterStatusTextTest {

    private val green = Color(0xFF006400)

    private fun spanAt(spans: List<androidx.compose.ui.text.AnnotatedString.Range<androidx.compose.ui.text.SpanStyle>>, start: Int) =
        spans.single { it.start == start }

    @Test
    fun deltaTextHasNumberLabelDeltaStyling() {
        // "5 moves from here Δ+1" — Δ at index 18, first space at index 1
        val result = liveCounterStatusText("5 moves from here Δ+1", 1, false)

        val number = spanAt(result.spanStyles, 0)
        assertEquals(0 to 1, number.start to number.end)
        assertEquals(18.sp * 1.5f, number.item.fontSize)
        assertEquals(green, number.item.color)

        val label = spanAt(result.spanStyles, 1)
        assertEquals(1 to 18, label.start to label.end)
        assertEquals(18.sp * 0.9f, label.item.fontSize)
        assertEquals(green, label.item.color)

        val delta = spanAt(result.spanStyles, 18)
        assertEquals(18 to result.text.length, delta.start to delta.end)
        assertEquals(18.sp * 1.5f, delta.item.fontSize)
        assertEquals(Color(Constants.liveCounterDeviationColor(1)), delta.item.color)
    }

    @Test
    fun noDeltaKeepsNumberEmphasis() {
        val result = liveCounterStatusText("12 moves from here", 0, false)

        val number = spanAt(result.spanStyles, 0)
        assertEquals(0 to 2, number.start to number.end)
        assertEquals(18.sp * 1.5f, number.item.fontSize)
        assertEquals(green, number.item.color)

        val label = spanAt(result.spanStyles, 2)
        assertEquals(2 to result.text.length, label.start to label.end)
        assertEquals(18.sp * 0.9f, label.item.fontSize)
        assertEquals(green, label.item.color)
    }

    @Test
    fun unsolvableMarkerIsPlainGreen() {
        val result = liveCounterStatusText("?", 0, false)
        assertEquals(1, result.spanStyles.size)
        assertEquals(green, result.spanStyles[0].item.color)
    }

    @Test
    fun highContrastForcesBlackButKeepsSizes() {
        val result = liveCounterStatusText("5 moves from here Δ+3", 3, true)
        for (span in result.spanStyles) {
            assertEquals(Color.Black, span.item.color)
        }
        assertEquals(18.sp * 1.5f, spanAt(result.spanStyles, 0).item.fontSize)
        assertEquals(18.sp * 0.9f, spanAt(result.spanStyles, 1).item.fontSize)
        assertEquals(18.sp * 1.5f, spanAt(result.spanStyles, 18).item.fontSize)
    }

    @Test
    fun deviationColorMapping() {
        assertEquals(0xFF006400L, Constants.liveCounterDeviationColor(-2))
        assertEquals(0xFF006400L, Constants.liveCounterDeviationColor(0))
        assertEquals(0xFF7CB342L, Constants.liveCounterDeviationColor(1))
        assertEquals(0xFFC6A700L, Constants.liveCounterDeviationColor(2))
        assertEquals(0xFFE65100L, Constants.liveCounterDeviationColor(3))
        assertEquals(0xFFD50000L, Constants.liveCounterDeviationColor(4))
        assertEquals(0xFF8B0000L, Constants.liveCounterDeviationColor(5))
        assertEquals(0xFF8B0000L, Constants.liveCounterDeviationColor(99))
    }
}

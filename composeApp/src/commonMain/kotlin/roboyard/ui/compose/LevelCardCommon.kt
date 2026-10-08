package roboyard.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import roboyard.composeapp.generated.resources.Res
import roboyard.composeapp.generated.resources.ic_check_green
import roboyard.composeapp.generated.resources.star

/** Gold card visuals matching Android bg_level_card_gold. */
internal val LevelCardGoldBrush: Brush = Brush.linearGradient(
    listOf(Color(0xFFC8A415), Color(0xFFE8C840), Color(0xFFA07A10))
)
internal val LevelCardGoldBorder: Color = Color(0xFFFFD700)

/**
 * Stars row shown on completed level cards and above history maps:
 * green check when completed with 0 stars, otherwise up to 4 star icons.
 */
@Composable
internal fun StarsRow(stars: Int, showCheckWhenZero: Boolean, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        if (stars == 0 && showCheckWhenZero) {
            Image(
                painter = painterResource(Res.drawable.ic_check_green),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        } else {
            repeat(stars.coerceIn(0, 4)) {
                Image(
                    painter = painterResource(Res.drawable.star),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

package roboyard.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import roboyard.logic.ui.getStringProvider

/**
 * Help screen — Compose port of Android HelpFragment.
 * Sections: Goal, Movement, Controls, Accessibility, Tips, Star Rating System,
 * Level Editor. All text comes from localized string resources.
 */
@Composable
fun HelpScreen(
    onBack: () -> Unit = {}
) {
    val stringProvider = getStringProvider()
    fun s(key: String, fallback: String): String =
        stringProvider.getString(key) ?: fallback

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Text(
            text = s("help_screen_content_title", "How to Play Roboyard"),
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp)
                .semantics { contentDescription = s("help_a11y", "How to Play") }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Goal section
            SectionHeading(s("help_goal_section_title", "Goal"))
            Paragraph(s("help_goal_description", "Move the colored robots to their matching colored goals."))

            // Movement section
            SectionHeading(s("help_movement_section_title", "Movement"))
            Paragraph(
                s("help_movement_point_1", "- Tap on a robot to select it.") + "\n" +
                s("help_movement_point_2", "- Tap on an empty cell to move the selected robot in that direction.") + "\n" +
                s("help_movement_point_3", "- Robots will move in straight lines until they hit a wall, another robot, or the edge of the board.")
            )

            // Controls section
            SectionHeading(s("help_controls_section_title", "Controls"))
            Paragraph(
                s("help_controls_point_1", "- Hint: Shows a suggested move.") + "\n" +
                s("help_controls_point_2", "- Reset: Restarts the current level.") + "\n" +
                s("help_controls_point_3", "- Save: Saves your current map.") + "\n" +
                s("help_controls_point_4", "- Menu: Returns to the main menu.")
            )

            // Accessibility section
            SectionHeading(s("help_accessibility_section_title", "Accessibility"))
            Paragraph(s("help_accessibility_description", "This game is fully compatible with screen readers."))

            // Tips section
            SectionHeading(s("help_tips_section_title", "Tips"))
            Paragraph(
                s("help_tips_point_1", "- Try to solve puzzles in the minimum number of moves.") + "\n" +
                s("help_tips_point_2", "- Sometimes you need to move robots to specific positions to clear paths for other robots.") + "\n" +
                s("help_tips_point_3", "- If you're stuck, use the Hint feature to get a suggestion.")
            )

            // Star system section
            SectionHeading(s("help_star_system_section_title", "Star Rating System"))
            Paragraph(s("help_star_system_description", "Your performance on each level is rated with stars."))
            Paragraph(
                s("help_star_system_4_stars", "⭐⭐⭐⭐ - Hyper-optimal solution") + "\n" +
                s("help_star_system_3_stars", "⭐⭐⭐ - Optimal solution") + "\n" +
                s("help_star_system_2_stars", "⭐⭐ - Near-optimal solution") + "\n" +
                s("help_star_system_1_star", "⭐ - Good solution") + "\n" +
                s("help_star_system_0_stars", "No stars: All other cases.")
            )
            Paragraph(s("help_star_system_hint_penalty", "Note: Using hints will reduce your star rating."))

            // Level Editor section
            SectionHeading(s("help_level_editor_section_title", "Level Editor"))
            Paragraph(s("help_level_editor_description", "Once you have unlocked all 140 levels, a Level Editor button will appear at the bottom of the level selection screen."))
        }

        FancyButton(
            text = "◂ " + s("back", "Back"),
            color = FancyButtonColor.GRAY,
            onClick = onBack,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/** Section heading — matches Android addSectionHeading (18sp). */
@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 18.sp,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

/** Section paragraph — matches Android addParagraph (14sp). */
@Composable
private fun Paragraph(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 14.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

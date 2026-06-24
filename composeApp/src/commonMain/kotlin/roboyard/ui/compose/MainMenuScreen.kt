package roboyard.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import roboyard.composeapp.generated.resources.Res
import roboyard.composeapp.generated.resources.title_bg_optimized
import roboyard.logic.storage.getPlatformStorage
@Composable
fun MainMenuScreen(
    onNewRandomGame: () -> Unit = {},
    onLevelSelection: () -> Unit = {},
    onSettings: () -> Unit = {},
    onHelp: () -> Unit = {},
    onCredits: () -> Unit = {},
    onSaveLoad: () -> Unit = {},
    onAchievements: () -> Unit = {}
) {
    var hasSavedGames by remember { mutableStateOf(false) }

    // Check if there are saved games (same logic as main game)
    LaunchedEffect(Unit) {
        val storage = getPlatformStorage()
        hasSavedGames = storage.hasSavedGames()
    }

    val barBrush = Brush.linearGradient(
        colors = listOf(Color(0xCC000000), Color(0xCC000000)),
        start = Offset(0f, 0f),
        end = Offset.Infinite
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Background image (same as main game)
        Image(
            painter = painterResource(Res.drawable.title_bg_optimized),
            contentDescription = "Background image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header bar with title and profile button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(barBrush)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 44.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ROBOYARD",
                        color = Color.White,
                        fontSize = 39.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = Shadow(
                                color = Color.Black,
                                offset = Offset(2f, 2f),
                                blurRadius = 3f
                            )
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularButton(
                        text = null,
                        color = CircularButtonColor.TURQUOISE,
                        onClick = { },
                        modifier = Modifier.size(48.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color.Black)
                )
            }

            // Scrollable content with fancy buttons
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                // Button container with 70% width
                Column(
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    FancyButton(
                        text = "New Random Game",
                        color = FancyButtonColor.GREEN,
                        onClick = onNewRandomGame,
                        modifier = Modifier.fillMaxWidth().semantics { testTag = "newRandomGameButton" }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    FancyButton(
                        text = "Level Game",
                        color = FancyButtonColor.BLUE,
                        onClick = onLevelSelection,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    // Load Game button - always visible (same as main game)
                    FancyButton(
                        text = "Load Game",
                        color = FancyButtonColor.RED,
                        onClick = onSaveLoad,
                        modifier = Modifier.fillMaxWidth().semantics { testTag = "loadGameButton" }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Footer bar with icon buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(barBrush)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color.Black)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 0.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Credits button - X symbol
                    CircularButton(
                        text = "©",
                        color = CircularButtonColor.YELLOW,
                        onClick = onCredits,
                        modifier = Modifier.size(48.dp).padding(8.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // Help button
                    CircularButton(
                        text = null,
                        color = CircularButtonColor.ORANGE,
                        onClick = onHelp,
                        modifier = Modifier.size(48.dp).padding(8.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // Achievements button
                    CircularButton(
                        text = null,
                        color = CircularButtonColor.PURPLE,
                        onClick = onAchievements,
                        modifier = Modifier.size(48.dp).padding(8.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // Settings button
                    CircularButton(
                        text = null,
                        color = CircularButtonColor.GRAY,
                        onClick = onSettings,
                        modifier = Modifier.size(48.dp).padding(8.dp)
                    )
                }
            }
        }
    }
}

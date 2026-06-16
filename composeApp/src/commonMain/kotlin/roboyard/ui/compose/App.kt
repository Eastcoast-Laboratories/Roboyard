package roboyard.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            MainMenuScreen()
        }
    }
}

@Composable
fun MainMenuScreen(
    onNewRandomGame: () -> Unit = {},
    onLevelSelection: () -> Unit = {},
    onSettings: () -> Unit = {},
    onHelp: () -> Unit = {},
    onCredits: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Roboyard",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = "Ricochet Robots",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        MenuButton(text = "New Random Game", onClick = onNewRandomGame)
        Spacer(modifier = Modifier.height(16.dp))
        MenuButton(text = "Level Selection", onClick = onLevelSelection)
        Spacer(modifier = Modifier.height(16.dp))
        MenuButton(text = "Settings", onClick = onSettings)
        Spacer(modifier = Modifier.height(16.dp))
        MenuButton(text = "Help", onClick = onHelp)
        Spacer(modifier = Modifier.height(16.dp))
        MenuButton(text = "Credits", onClick = onCredits)
    }
}

@Composable
fun MenuButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}

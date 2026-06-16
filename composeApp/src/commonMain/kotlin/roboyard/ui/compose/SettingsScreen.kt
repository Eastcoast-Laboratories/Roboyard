package roboyard.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    onBack: () -> Unit = {}
) {
    var difficulty by remember { mutableStateOf("Beginner") }
    var robotCount by remember { mutableStateOf(4) }
    var soundEnabled by remember { mutableStateOf(true) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onBack) {
                    Text("Back")
                }
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.width(80.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            SettingItem(
                title = "Difficulty",
                value = difficulty,
                options = listOf("Beginner", "Intermediate", "Advanced", "Expert"),
                onValueChange = { difficulty = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingItem(
                title = "Robot Count",
                value = robotCount.toString(),
                options = listOf("2", "3", "4", "5"),
                onValueChange = { robotCount = it.toInt() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingToggle(
                title = "Sound Effects",
                value = soundEnabled,
                onValueChange = { soundEnabled = it }
            )
        }
    }
}

@Composable
fun SettingItem(
    title: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
        Row {
            options.forEach { option ->
                TextButton(
                    onClick = { onValueChange(option) }
                ) {
                    Text(
                        text = option,
                        color = if (option == value) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingToggle(
    title: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
        Button(onClick = { onValueChange(!value) }) {
            Text(if (value) "On" else "Off")
        }
    }
}

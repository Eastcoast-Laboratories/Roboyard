package roboyard.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import driftingdroids.model.Board
import roboyard.logic.core.LevelLoader

@Composable
fun LoadingScreen(
    levelId: Int,
    onLoadComplete: (Board) -> Unit = {},
    onBack: () -> Unit = {}
) {
    var isLoading by remember { mutableStateOf(true) }
    var board by remember { mutableStateOf<Board?>(null) }
    var showError by remember { mutableStateOf(false) }

    LaunchedEffect(levelId) {
        try {
            val loadedBoard = LevelLoader.loadLevel(levelId)
            if (loadedBoard != null) {
                board = loadedBoard
                onLoadComplete(loadedBoard)
            } else {
                showError = true
            }
            isLoading = false
        } catch (e: Exception) {
            println("[LOADING_SCREEN] Error loading level: ${e.message}")
            showError = true
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        if (isLoading) {
            Text(
                text = "Loading Level $levelId...",
                color = Color.White,
                fontSize = 24.sp
            )
        } else if (showError) {
            AlertDialog(
                onDismissRequest = onBack,
                title = { Text("Error") },
                text = { Text("Failed to load level $levelId") },
                confirmButton = { 
                    androidx.compose.material3.Button(onClick = onBack) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

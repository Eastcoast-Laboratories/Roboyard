package roboyard.ui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = Color(0xFF616161),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "",
                color = Color(0xFF616161),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun HistoryInfoDialog(
    entry: roboyard.logic.core.GameHistoryEntry,
    onDismiss: () -> Unit
) {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())

    println("[HISTORY_INFO_DIALOG] entry.bestTime=${entry.bestTime}, entry.bestMoves=${entry.bestMoves}, entry.completionCount=${entry.completionCount}")
    
    val message = buildString {
        append("Completions: ${entry.completionCount}\n")
        append("First started: ${sdf.format(Date(entry.timestamp))}\n")
        if (entry.lastCompletionTimestamp > 0) {
            append("Last played: ${sdf.format(Date(entry.lastCompletionTimestamp))}\n")
        }
        
        val timestamps = entry.getCompletionTimestamps()
        if (timestamps != null && timestamps.size > 1) {
            val isLevelGame = entry.mapName?.startsWith("Level ") == true
            val completionStars = entry.getCompletionStars()
            val completionMoves = entry.getCompletionMoves()
            
            append("\nAll completions:\n")
            for (i in timestamps.indices) {
                append("  ${i + 1}. ${sdf.format(Date(timestamps[i]))}")
                if (isLevelGame) {
                    val stars = if (completionStars != null && i < completionStars.size) {
                        completionStars[i]
                    } else {
                        entry.starsEarned
                    }
                    val moves = if (completionMoves != null && i < completionMoves.size) {
                        completionMoves[i]
                    } else {
                        entry.movesMade
                    }
                    if (stars == 0) {
                        append(" ✓")
                    } else {
                        repeat(stars) { append("★") }
                    }
                    append(" - $moves")
                } else {
                    val moves = if (completionMoves != null && i < completionMoves.size) {
                        completionMoves[i]
                    } else {
                        entry.movesMade
                    }
                    append(" - $moves")
                }
                append("\n")

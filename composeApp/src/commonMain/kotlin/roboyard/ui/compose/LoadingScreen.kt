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
        }
        
        append("\nBest time: ")
        println("[HISTORY_INFO_DIALOG] Displaying bestTime: ${entry.bestTime}, condition: ${entry.bestTime > 0}")
        if (entry.bestTime > 0) {
            append("${entry.bestTime / 60}m ${entry.bestTime % 60}s")
        } else {
            append("—")
        }
        append("\n")
        
        append("Best moves: ")
        println("[HISTORY_INFO_DIALOG] Displaying bestMoves: ${entry.bestMoves}, condition: ${entry.bestMoves > 0}")
        append(if (entry.bestMoves > 0) entry.bestMoves else "—")
        append("\n")
        
        append("Optimal moves: ")
        if (entry.optimalMoves > 0) {
            append(entry.optimalMoves)
            if (entry.bestMoves > 0 && entry.bestMoves == entry.optimalMoves) {
                append(" ✓ (Perfect)")
            } else if (entry.bestMoves > 0) {
                append(" (+${entry.bestMoves - entry.optimalMoves} extra moves)")
            }
        } else {
            append("—")
        }
        append("\n")
        
        append("\nHint usage (last): ")
        val maxHint = entry.maxHintUsed
        when {
            maxHint < 0 -> append("No hints used")
            maxHint == 0 -> append("Pre-hint viewed")
            else -> append("Up to hint ${maxHint + 1}")
        }
        append("\n")
        
        append("Hints ever used: ")
        append(if (entry.isEverUsedHints()) "Yes" else "No")
        append("\n")
        
        append("Qualifies for no-hints achievement: ")
        append(if (entry.qualifiesForNoHintsAchievement()) "Yes" else "No")
        append("\n")
        
        append("Qualifies for perfect no-hints achievement: ")
        append(if (entry.qualifiesForPerfectNoHintsAchievement()) "Yes" else "No")
        append("\n")
        
        append("Last solved without hints: ")
        val lastNoHints = entry.lastSolvedWithoutHints
        append(if (lastNoHints > 0) sdf.format(Date(lastNoHints)) else "—")
        append("\n")
        
        append("Last perfectly solved without hints: ")
        val lastPerfect = entry.lastPerfectlySolvedWithoutHints
        append(if (lastPerfect > 0) sdf.format(Date(lastPerfect)) else "—")
    }
    
        onDismissRequest = onDismiss,
        title = { Text(entry.mapName ?: "Unknown Map") },
        text = { Text(message, fontSize = 12.sp) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )

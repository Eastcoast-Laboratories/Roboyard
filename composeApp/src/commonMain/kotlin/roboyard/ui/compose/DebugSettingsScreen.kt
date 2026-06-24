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
                    text = "Wall",
                    color = if (selectedTool == "Wall") FancyButtonColor.BLUE else FancyButtonColor.GRAY,
                    onClick = { selectedTool = "Wall" },
                    modifier = Modifier.weight(1f)
                )
                FancyButton(
                    text = "Eraser",
                    color = if (selectedTool == "Eraser") FancyButtonColor.BLUE else FancyButtonColor.GRAY,
                    onClick = { selectedTool = "Eraser" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Target Tool Selection
            Text(
                text = "Target Tool",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FancyButton(
                    text = "None",
                    color = if (selectedTarget == "None") FancyButtonColor.BLUE else FancyButtonColor.GRAY,
                    onClick = { selectedTarget = "None" },
                    modifier = Modifier.weight(1f)
                )
                FancyButton(
                    text = "R",
                    color = if (selectedTarget == "R") FancyButtonColor.RED else FancyButtonColor.GRAY,
                    onClick = { selectedTarget = "R" },
                    modifier = Modifier.weight(1f)
                )
                FancyButton(
                    text = "G",
                    color = if (selectedTarget == "G") FancyButtonColor.GREEN else FancyButtonColor.GRAY,
                    onClick = { selectedTarget = "G" },
                    modifier = Modifier.weight(1f)
                )
                FancyButton(
                    text = "B",
                    color = if (selectedTarget == "B") FancyButtonColor.BLUE else FancyButtonColor.GRAY,
                    onClick = { selectedTarget = "B" },
                    modifier = Modifier.weight(1f)
                )
                FancyButton(
                    text = "Y",
                    color = if (selectedTarget == "Y") FancyButtonColor.YELLOW else FancyButtonColor.GRAY,
                    onClick = { selectedTarget = "Y" },
                    modifier = Modifier.weight(1f)
                )
                FancyButton(
                    text = "S",
                    color = if (selectedTarget == "S") FancyButtonColor.GRAY else FancyButtonColor.GRAY,
                    onClick = { selectedTarget = "S" },
                    modifier = Modifier.weight(1f)
                )
                FancyButton(
                    text = "M",
                    color = if (selectedTarget == "M") FancyButtonColor.PURPLE else FancyButtonColor.GRAY,
                    onClick = { selectedTarget = "M" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Board Preview
            Text(
                text = "Board Preview",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Board preview will appear here",
                    color = Color(0xFF888888),
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Board Size Configuration
            Text(
                text = "Board Size",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

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
@Composable
fun CreditsScreen(
    onBack: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Text(
            text = "Credits",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp)
        )
        
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Version section
            Text(
                text = "Version",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Text(
                text = "v1.9",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Based on section
            Text(
                text = "Based on",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Text(
                text = "Ricochet Robots®",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Links section
            Text(
                text = "Links",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Text(
                text = "Imprint",
                color = Color(0xFF0000FF),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Open Source",
                color = Color(0xFF0000FF),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Contact",
                color = Color(0xFF0000FF),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        FancyButton(
            text = "BACK",
            color = FancyButtonColor.GRAY,
            onClick = onBack,
            modifier = Modifier.padding(16.dp)
        )
    }
}

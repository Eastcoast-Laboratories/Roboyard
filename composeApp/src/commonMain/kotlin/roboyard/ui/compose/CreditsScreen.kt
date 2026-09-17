package roboyard.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import roboyard.logic.achievements.AchievementManager
import roboyard.logic.platform.PlatformInfo
import roboyard.logic.platform.openUrl
import roboyard.logic.storage.getPlatformStorage
import roboyard.logic.ui.getStringProvider

/**
 * Credits screen — Compose port of Android CreditsFragment.
 * Shows version, "Based on Ricochet Robots", imprint/opensource/contact links
 * and creator credits.
 */
@Composable
fun CreditsScreen(
    onBack: () -> Unit = {}
) {
    val stringProvider = getStringProvider()
    fun s(key: String, fallback: String): String =
        stringProvider.getString(key) ?: fallback

    // Android parity: CreditsFragment.onViewCreated shows the update nudge (no cooldown)
    LaunchedEffect(Unit) {
        AchievementManager.getInstance(getPlatformStorage(), stringProvider)
            .showUpdateNudgeForCredits()
    }

    val urlImprint = s("url_imprint", "https://roboyard.z11.de/impressum")
    val urlOpensource = s("url_opensource", "https://github.com/Eastcoast-Laboratories/Roboyard")
    val urlContact = s("url_contact", "https://eclabs.de/#kontakt")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Text(
            text = s("credits", "Credits"),
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
            // Based on (matches fragment_credits.xml)
            Text(
                text = "Based on",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
            Text(
                text = "Ricochet Robots®",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Imprint / Privacy Policy link
            Text(
                text = "Imprint / Privacy Policy",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
            LinkText("roboyard.z11.de/impressum", urlImprint)

            // Open Source link
            Text(
                text = "Open Source",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
            LinkText("github.com/Eastcoast-Laboratories/Roboyard", urlOpensource)

            // Version (matches version_format string; version from PlatformInfo)
            Text(
                text = s("version_format", "Version: {0}")
                    .replace("{0}", PlatformInfo.getAppVersionName())
                    .replace("{1}", ""),
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            // Contact link
            Text(
                text = "Contact Us",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
            LinkText("eclabs.de/#kontakt", urlContact)

            // Created by (matches fragment_credits.xml)
            Text(
                text = "Created by",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
            Text(
                text = "Alain Caillaud",
                color = Color.White,
                fontSize = 14.sp
            )
            Text(
                text = "Pierre Michel",
                color = Color.White,
                fontSize = 14.sp
            )
            Text(
                text = "Ruben Barkow-Kuder",
                color = Color.White,
                fontSize = 14.sp
            )
        }

        // Android fragment_credits.xml shows "← Back" hardcoded
        FancyButton(
            text = "← " + s("back", "Back"),
            color = FancyButtonColor.GRAY,
            onClick = onBack,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/** Blue underlined link — matches Android ClickableSpan link styling. */
@Composable
private fun LinkText(label: String, url: String) {
    Text(
        text = label,
        color = Color(0xFF0000FF),
        fontSize = 14.sp,
        style = TextStyle(textDecoration = TextDecoration.Underline),
        modifier = Modifier
            .padding(bottom = 8.dp)
            .clickable { openUrl(url) }
    )
}

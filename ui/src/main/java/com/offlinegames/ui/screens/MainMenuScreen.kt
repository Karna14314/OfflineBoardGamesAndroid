package com.offlinegames.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlinegames.ui.viewmodel.MainMenuViewModel

/**
 * Entry screen shown on app launch.
 * Presents play, settings and statistics actions.
 */
@Composable
fun MainMenuScreen(
    viewModel: MainMenuViewModel,
    onPlayClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onStatsClicked: () -> Unit
) {
    val playerName by viewModel.playerOneName.collectAsState()
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.background, colors.surface)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Text(
                text = "🎲",
                fontSize = 72.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Offline Games",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.primary
                )
            )
            Text(
                text = "Classic board games, offline & free",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = colors.onSurface.copy(alpha = 0.6f)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (playerName.isNotBlank()) {
                Text(
                    text = "Welcome back, $playerName!",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = colors.secondary,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Play button
            Button(
                onClick = onPlayClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) {
                Text(
                    text = "Play",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = colors.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats button
            OutlinedButton(
                onClick = onStatsClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Statistics",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Settings button
            OutlinedButton(
                onClick = onSettingsClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

package com.offlinegames.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.offlinegames.engine.GameSurfaceView
import com.offlinegames.ui.theme.LocalSpacing

@Composable
fun DistractionFreeGameScreen(
    playerNameTurn: String,
    surfaceView: GameSurfaceView,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    soundEnabled: Boolean,
    onToggleSound: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val spacing = LocalSpacing.current
    var showPauseMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Top Bar: Player turn and Pause button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$playerNameTurn's Turn",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.onBackground
            )

            IconButton(onClick = { showPauseMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Pause Menu",
                    tint = colors.onBackground
                )
            }
        }

        // Center: The Game Surface Wrapper
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f) // Keep it roughly square according to the design plan
                .align(Alignment.Center)
        ) {
            AndroidView(
                factory = { surfaceView },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (showPauseMenu) {
            PauseMenuDialog(
                onDismiss = { showPauseMenu = false },
                onRestart = {
                    showPauseMenu = false
                    onRestart()
                },
                onExit = {
                    showPauseMenu = false
                    onExit()
                },
                soundEnabled = soundEnabled,
                onToggleSound = onToggleSound
            )
        }
    }
}

@Composable
private fun PauseMenuDialog(
    onDismiss: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    soundEnabled: Boolean,
    onToggleSound: (Boolean) -> Unit
) {
    val spacing = LocalSpacing.current
    val colors = MaterialTheme.colorScheme

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Column(
                modifier = Modifier.padding(spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                Text(
                    text = "Paused",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onSurface
                )
                
                Spacer(modifier = Modifier.height(spacing.small))

                Button(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text("Restart Game", style = MaterialTheme.typography.bodyLarge)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable { onToggleSound(!soundEnabled) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Sound", style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { onToggleSound(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.onPrimary,
                            checkedTrackColor = colors.primary
                        )
                    )
                }

                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Exit to Hub", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

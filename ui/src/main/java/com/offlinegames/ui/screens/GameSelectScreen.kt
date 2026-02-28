package com.offlinegames.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Data class holding display information for a selectable game.
 */
data class GameEntry(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String
)

/** Hardcoded registry of available games. Add new games here. */
private val availableGames = listOf(
    GameEntry(
        id = "tictactoe",
        title = "Tic-Tac-Toe (2 Player)",
        description = "Classic 3×3 noughts and crosses",
        emoji = "⭕"
    ),
    GameEntry(
        id = "tictactoe_ai",
        title = "Tic-Tac-Toe (vs AI)",
        description = "Play against the computer",
        emoji = "🤖"
    ),
    GameEntry(
        id = "connect4",
        title = "Connect 4 (2 Player)",
        description = "Drop pieces, connect four to win",
        emoji = "🔴"
    ),
    GameEntry(
        id = "connect4_ai",
        title = "Connect 4 (vs AI)",
        description = "Challenge the AI at Connect 4",
        emoji = "🟡"
    ),
    GameEntry(
        id = "sos",
        title = "SOS (2 Player)",
        description = "Form SOS patterns to score points",
        emoji = "🅰️"
    ),
    GameEntry(
        id = "sos_ai",
        title = "SOS (vs AI)",
        description = "Play SOS against the computer",
        emoji = "🆘"
    ),
    GameEntry(
        id = "dotsandboxes",
        title = "Dots & Boxes (2 Player)",
        description = "Draw lines, complete boxes to win",
        emoji = "🔲"
    ),
    GameEntry(
        id = "dotsandboxes_ai",
        title = "Dots & Boxes (vs AI)",
        description = "Challenge the AI at Dots & Boxes",
        emoji = "📦"
    )
)

/**
 * Screen listing all playable games.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSelectScreen(
    onGameSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose a Game") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(availableGames) { game ->
                GameCard(game = game, onClick = { onGameSelected(game.id) })
            }
        }
    }
}

@Composable
private fun GameCard(game: GameEntry, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = game.emoji, fontSize = 42.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                )
                Text(
                    text = game.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = colors.onSurface.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
}

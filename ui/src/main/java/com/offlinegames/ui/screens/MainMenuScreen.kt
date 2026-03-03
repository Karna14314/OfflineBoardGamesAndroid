package com.offlinegames.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offlinegames.ui.theme.LocalSpacing
import com.offlinegames.ui.viewmodel.MainMenuViewModel

/**
 * Data class holding display information for a selectable game.
 */
data class GameEntry(
    val id: String,
    val title: String,
    val emoji: String
)

/** Grouped games based on the Hub Model requirement */
private val quickGames = listOf(
    GameEntry("tictactoe_ai", "TicTacToe (1P)", "🤖"),
    GameEntry("tictactoe", "TicTacToe (2P)", "⭕"),
    GameEntry("connect4_ai", "Connect 4 (1P)", "🤖"),
    GameEntry("connect4", "Connect 4 (2P)", "🔴"),
    GameEntry("sos_ai", "SOS (1P)", "🤖"),
    GameEntry("sos", "SOS (2P)", "🅰️")
)

private val strategyGames = listOf(
    GameEntry("checkers_ai", "Checkers (1P)", "🤖"),
    GameEntry("checkers", "Checkers (2P)", "⚫"),
    GameEntry("dotsandboxes_ai", "Dots & Boxes (1P)", "🤖"),
    GameEntry("dotsandboxes", "Dots & Boxes (2P)", "🔲"),
    GameEntry("ludo", "Ludo", "🎲")
)

private val soloPuzzleGames = listOf(
    GameEntry("game2048", "2048", "🔢"),
    GameEntry("minesweeper", "Minesweeper", "💣"),
    // Memory Match doesn't exist yet, we'll leave it out for now to ensure compile safety
    // GameEntry("memory", "Memory Match", "🧠")
)

private val arcadeGames = listOf(
    GameEntry("airhockey", "AirHockey", "🏒"),
    GameEntry("airhockey_ai", "AirHockey (1P)", "🤖") // We need to add AI support for Air Hockey
)

/**
 * Entry screen shown on app launch acting as the Game Hub.
 */
@Composable
fun MainMenuScreen(
    viewModel: MainMenuViewModel,
    onGameSelected: (String) -> Unit,
    onSettingsClicked: () -> Unit,
    onStatsClicked: () -> Unit
) {
    var showLudoDialog by remember { mutableStateOf(false) }

    val handleGameSelected = { id: String ->
        if (id == "ludo") {
            showLudoDialog = true
        } else {
            onGameSelected(id)
        }
    }

    val playerName by viewModel.playerOneName.collectAsState()
    val colors = MaterialTheme.colorScheme
    val spacing = LocalSpacing.current
    val typography = MaterialTheme.typography

    Scaffold(
        containerColor = colors.background,
        bottomBar = {
            // Bottom Section: Statistics & Settings Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.medium),
                horizontalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                OutlinedButton(
                    onClick = onStatsClicked,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Statistics", style = typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                }
                
                OutlinedButton(
                    onClick = onSettingsClicked,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Settings", style = typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.large)
        ) {
            // Top Section
            item {
                Column {
                    Text(
                        text = "Offline Hub",
                        style = typography.headlineLarge,
                        color = colors.onBackground
                    )
                    
                    if (playerName.isNotBlank()) {
                        Text(
                            text = "Welcome back, $playerName",
                            style = typography.bodyLarge,
                            color = colors.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = spacing.tiny)
                        )
                    }

                    Spacer(modifier = Modifier.height(spacing.medium))

                    // Prominent 'Resume Last Game' Card
                    ResumeLastGameCard(
                        gameName = "TicTacToe", // Hardcoded for now until persistence saves last game
                        onClick = { onGameSelected("tictactoe") }
                    )
                }
            }

            // Main Section: Categories
            item { GameCategorySection("Quick Games", quickGames, handleGameSelected) }
            item { GameCategorySection("Strategy", strategyGames, handleGameSelected) }
            item { GameCategorySection("Solo / Puzzle", soloPuzzleGames, handleGameSelected) }
            item { GameCategorySection("Arcade", arcadeGames, handleGameSelected) }
        }
    }

    if (showLudoDialog) {
        AlertDialog(
            onDismissRequest = { showLudoDialog = false },
            title = { Text("Ludo Configuration") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(modifier = Modifier.fillMaxWidth(), onClick = { showLudoDialog = false; onGameSelected("ludo") }) {
                        Text("2 Players (Offline)")
                    }
                    Button(modifier = Modifier.fillMaxWidth(), onClick = { showLudoDialog = false; onGameSelected("ludo_4p") }) {
                        Text("4 Players (Offline)")
                    }
                    Button(modifier = Modifier.fillMaxWidth(), onClick = { showLudoDialog = false; onGameSelected("ludo_ai") }) {
                        Text("1 Player vs 1 Bot")
                    }
                    Button(modifier = Modifier.fillMaxWidth(), onClick = { showLudoDialog = false; onGameSelected("ludo_3bots") }) {
                        Text("1 Player vs 3 Bots")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLudoDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ResumeLastGameCard(gameName: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val spacing = LocalSpacing.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = colors.primaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Resume Last Game",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = gameName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onPrimaryContainer
                )
            }
            
            IconButton(
                onClick = onClick,
                modifier = Modifier
                    .background(colors.primary, RoundedCornerShape(8.dp))
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Resume",
                    tint = colors.onPrimary
                )
            }
        }
    }
}

@Composable
private fun GameCategorySection(
    title: String,
    games: List<GameEntry>,
    onGameSelected: (String) -> Unit
) {
    val spacing = LocalSpacing.current
    
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = spacing.small)
        )
        
        // Horizontal list for categorized games to avoid a messy single grid
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            items(games) { game ->
                GameCardBadge(game, onClick = { onGameSelected(game.id) })
            }
        }
    }
}

@Composable
private fun GameCardBadge(game: GameEntry, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val spacing = LocalSpacing.current

    Card(
        modifier = Modifier
            .width(140.dp)
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.small),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = game.emoji, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(spacing.tiny))
            Text(
                text = game.title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onSurface,
                maxLines = 1
            )
        }
    }
}

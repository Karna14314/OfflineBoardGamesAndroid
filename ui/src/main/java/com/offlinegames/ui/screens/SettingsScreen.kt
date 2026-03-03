package com.offlinegames.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offlinegames.ui.viewmodel.SettingsViewModel

/** Difficulty display options. */
private val difficultyOptions = listOf("EASY", "MEDIUM", "HARD", "EXPERT")

/**
 * Settings screen allowing the user to configure sound, haptics, theme,
 * player names and AI difficulty.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val soundEnabled    by viewModel.soundEnabled.collectAsState()
    val hapticsEnabled  by viewModel.hapticsEnabled.collectAsState()
    val darkTheme       by viewModel.darkTheme.collectAsState()
    val p1Name          by viewModel.playerOneName.collectAsState()
    val p2Name          by viewModel.playerTwoName.collectAsState()
    val aiDifficulty    by viewModel.aiDifficulty.collectAsState()

    var p1Input by remember(p1Name) { mutableStateOf(p1Name) }
    var p2Input by remember(p2Name) { mutableStateOf(p2Name) }
    var diffExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            SettingsCard {
                SectionHeader("Audio & Haptics")
                ToggleRow(label = "Sound Effects", checked = soundEnabled) { viewModel.setSoundEnabled(it) }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                ToggleRow(label = "Haptic Feedback", checked = hapticsEnabled) { viewModel.setHapticsEnabled(it) }
            }

            SettingsCard {
                SectionHeader("Appearance")
                ToggleRow(label = "Dark Theme", checked = darkTheme) { viewModel.setDarkTheme(it) }
            }

            SettingsCard {
                SectionHeader("Player Profiles")
                OutlinedTextField(
                    value = p1Input,
                    onValueChange = { p1Input = it },
                    label = { Text("Player 1 Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    ),
                    trailingIcon = {
                        if (p1Input != p1Name) {
                            TextButton(onClick = { viewModel.setPlayerOneName(p1Input) }) {
                                Text("Save", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                )
                
                OutlinedTextField(
                    value = p2Input,
                    onValueChange = { p2Input = it },
                    label = { Text("Player 2 / CPU Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    ),
                    trailingIcon = {
                        if (p2Input != p2Name) {
                            TextButton(onClick = { viewModel.setPlayerTwoName(p2Input) }) {
                                Text("Save", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                )
            }

            SettingsCard {
                SectionHeader("AI Difficulty")
                ExposedDropdownMenuBox(
                    expanded = diffExpanded,
                    onExpandedChange = { diffExpanded = !diffExpanded }
                ) {
                    OutlinedTextField(
                        value = aiDifficulty,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Selected Difficulty") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = diffExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = diffExpanded,
                        onDismissRequest = { diffExpanded = false }
                    ) {
                        difficultyOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, style = MaterialTheme.typography.bodyLarge) },
                                onClick = {
                                    viewModel.setAiDifficulty(option)
                                    diffExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

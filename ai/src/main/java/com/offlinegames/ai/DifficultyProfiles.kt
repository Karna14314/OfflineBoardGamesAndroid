package com.offlinegames.ai

import com.offlinegames.core.GameState
import com.offlinegames.core.Move
import com.offlinegames.core.Player

/**
 * Difficulty tiers for the AI engine.
 *
 * Each profile controls the search depth (half-moves / plies) that
 * [MinimaxEngine] explores. Lower depths are faster and "weaker".
 */
enum class DifficultyProfile(
    val label: String,
    val maxDepth: Int
) {
    EASY(label = "Easy", maxDepth = 1),
    MEDIUM(label = "Medium", maxDepth = 3),
    HARD(label = "Hard", maxDepth = 7),
    EXPERT(label = "Expert", maxDepth = 12)
}

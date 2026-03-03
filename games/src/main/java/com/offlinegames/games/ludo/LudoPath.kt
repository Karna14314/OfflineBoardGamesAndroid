package com.offlinegames.games.ludo

import com.offlinegames.core.Position

/**
 * Predefined movement paths for Ludo.
 *
 * A standard Ludo board has:
 * - A shared outer track of 52 cells
 * - 4 home columns (6 cells each), one per player colour
 * - 4 base/start areas (off-board position -1)
 *
 * Each player's token enters the board at their START position,
 * travels around the outer track, then turns into their home column.
 *
 * The coordinates here map to a 15×15 logical grid used for rendering.
 * Index 0 = player's entry cell, indices 1–50 on shared track,
 * indices 51–56 in the home column, index 57 = HOME (finished).
 *
 * Safe cells (star positions on a standard board) protect tokens
 * from capture.
 */
object LudoPath {

    /** Number of cells on the shared outer track. */
    const val TRACK_LENGTH = 52

    /** Number of home column cells (after entering home stretch). */
    const val HOME_COLUMN_LENGTH = 6

    /** Total steps from start to home (inclusive). */
    const val TOTAL_STEPS = TRACK_LENGTH + HOME_COLUMN_LENGTH  // 58

    /** Special index meaning "at base" (not yet on board). */
    const val AT_BASE = -1

    /** Special index meaning "reached home" (finished). */
    const val AT_HOME = TOTAL_STEPS  // 58

    // ── Shared track coordinates (15×15 grid) ──────────────────────────

    /**
     * The 52 cells of the shared outer track in clockwise order.
     * Each position maps to a (row, col) on the 15×15 grid.
     *
     * The track starts at a canonical position (top of the board, player 3/green start)
     * and goes clockwise.
     */
    val sharedTrack: Array<Position> = arrayOf(
        // Top arm going right (cells 0-4)
        Position(1, 6), Position(2, 6), Position(3, 6), Position(4, 6), Position(5, 6),
        // Turn right (cells 5-6)
        Position(6, 5), Position(6, 4), Position(6, 3), Position(6, 2), Position(6, 1), Position(6, 0),
        // Turn down (cells 11-12)
        Position(7, 0), Position(8, 0),
        // Bottom-left arm going right (cells 13-17)
        Position(8, 1), Position(8, 2), Position(8, 3), Position(8, 4), Position(8, 5),
        // Turn down (cell 18)
        Position(9, 6), Position(10, 6), Position(11, 6), Position(12, 6), Position(13, 6),
        // Bottom side going left → right (cells 23-24)
        Position(14, 6), Position(14, 7), Position(14, 8),
        // Right-bottom arm going up (cells 26-30)
        Position(13, 8), Position(12, 8), Position(11, 8), Position(10, 8), Position(9, 8),
        // Turn right (cells 31-36)
        Position(8, 9), Position(8, 10), Position(8, 11), Position(8, 12), Position(8, 13), Position(8, 14),
        // Turn up (cells 37-38)
        Position(7, 14), Position(6, 14),
        // Top-right arm going left (cells 39-43)
        Position(6, 13), Position(6, 12), Position(6, 11), Position(6, 10), Position(6, 9),
        // Turn up (cells 44-48)
        Position(5, 8), Position(4, 8), Position(3, 8), Position(2, 8), Position(1, 8),
        // Top side going left (cells 49-51)
        Position(0, 8), Position(0, 7), Position(0, 6)
    )

    // ── Player entry points on the shared track ──────────────────────────

    /**
     * Index into [sharedTrack] where each player enters the board.
     * Player 0 (Red)    → cell 0
     * Player 1 (Green)  → cell 13
     * Player 2 (Yellow) → cell 26
     * Player 3 (Blue)   → cell 39
     */
    val playerEntryIndex = intArrayOf(0, 13, 26, 39)

    // ── Home column coordinates per player ──────────────────────────────

    /**
     * The 6-cell home column for each player, ending at the center.
     * Player enters home when they've gone around the full track.
     */
    val homeColumns: Array<Array<Position>> = arrayOf(
        // Player 0 (Red) - enters from top, goes down through column 7
        arrayOf(
            Position(1, 7), Position(2, 7), Position(3, 7),
            Position(4, 7), Position(5, 7), Position(6, 7)
        ),
        // Player 1 (Green) - enters from left, goes right through row 7
        arrayOf(
            Position(7, 1), Position(7, 2), Position(7, 3),
            Position(7, 4), Position(7, 5), Position(7, 6)
        ),
        // Player 2 (Yellow) - enters from bottom, goes up through column 7
        arrayOf(
            Position(13, 7), Position(12, 7), Position(11, 7),
            Position(10, 7), Position(9, 7), Position(8, 7)
        ),
        // Player 3 (Blue) - enters from right, goes left through row 7
        arrayOf(
            Position(7, 13), Position(7, 12), Position(7, 11),
            Position(7, 10), Position(7, 9), Position(7, 8)
        )
    )

    // ── Safe cells ──────────────────────────────────────────────────────

    /**
     * Indices into [sharedTrack] that are safe cells.
     * Tokens on safe cells cannot be captured.
     * Includes entry positions and the traditional star positions.
     */
    val safeCellIndices: Set<Int> = setOf(
        0, 8, 13, 21, 26, 34, 39, 47  // Entry points + star cells
    )

    // ── Base positions (off-board starting areas) ────────────────────────

    /**
     * Visual positions for tokens sitting in their base area.
     * Each player has 4 tokens, each with a position in their base quadrant.
     */
    val basePositions: Array<Array<Position>> = arrayOf(
        // Player 0 (Red) - top-left quadrant
        arrayOf(Position(1, 1), Position(1, 4), Position(4, 1), Position(4, 4)),
        // Player 1 (Green) - bottom-left quadrant
        arrayOf(Position(10, 1), Position(10, 4), Position(13, 1), Position(13, 4)),
        // Player 2 (Yellow) - bottom-right quadrant
        arrayOf(Position(10, 10), Position(10, 13), Position(13, 10), Position(13, 13)),
        // Player 3 (Blue) - top-right quadrant
        arrayOf(Position(1, 10), Position(1, 13), Position(4, 10), Position(4, 13))
    )

    // ── Path resolution ─────────────────────────────────────────────────

    /**
     * Resolve the absolute track index for a token's logical step.
     *
     * @param playerId The owning player (0–3)
     * @param step     The token's current logical step (0 = just entered,
     *                 TRACK_LENGTH-1 = about to enter home column)
     * @return index into [sharedTrack], or -1 if in home column
     */
    fun trackIndexForStep(playerId: Int, step: Int): Int {
        if (step < 0 || step >= TRACK_LENGTH) return -1
        return (playerEntryIndex[playerId] + step) % TRACK_LENGTH
    }

    /**
     * Get the visual Position for a token at a given logical step.
     *
     * @param playerId Token owner (0–3)
     * @param step     Logical step (AT_BASE, 0..TOTAL_STEPS, AT_HOME)
     * @param tokenIdx Token index (0–3) for base position disambiguation
     */
    fun positionForStep(playerId: Int, step: Int, tokenIdx: Int = 0): Position {
        return when {
            step == AT_BASE -> basePositions[playerId][tokenIdx]
            step >= TRACK_LENGTH -> {
                val homeIdx = step - TRACK_LENGTH
                if (homeIdx < HOME_COLUMN_LENGTH) {
                    homeColumns[playerId][homeIdx]
                } else {
                    // AT_HOME — center
                    Position(7, 7)
                }
            }
            else -> {
                val trackIdx = trackIndexForStep(playerId, step)
                sharedTrack[trackIdx]
            }
        }
    }

    /**
     * Check if a step position is a safe cell.
     */
    fun isSafeCell(playerId: Int, step: Int): Boolean {
        if (step < 0 || step >= TRACK_LENGTH) return true  // Base and home are always safe
        val trackIdx = trackIndexForStep(playerId, step)
        return trackIdx in safeCellIndices
    }
}

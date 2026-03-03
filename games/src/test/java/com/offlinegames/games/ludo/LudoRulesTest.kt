package com.offlinegames.games.ludo

import com.offlinegames.core.*
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for Ludo game rules.
 *
 * Tests cover:
 * - Capture logic
 * - Safe cell protection
 * - Exact home entry
 * - Extra turn on rolling 6
 * - Token entry from base on 6
 * - Win detection
 */
class LudoRulesTest {

    private val rules = LudoRules()

    // ═══════════════════════════════════════════════════════════════════════
    // Capture Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `capture - token sent back to base when landed on`() {
        // Place player 0 token at step 5, player 1 token so it lands on same absolute position
        val token0 = LudoToken(id = 0, playerId = 0, tokenIdx = 0, step = 10)
        val token1 = LudoToken(id = 4, playerId = 1, tokenIdx = 0, step = 0)

        // Calculate where token0 is on the absolute track
        val absIdx0 = LudoPath.trackIndexForStep(0, 10)

        // Set up player 1 token to land on same absolute position
        // Player 1 entry is at index 13. We need step such that (13 + step) % 52 == absIdx0
        val step1 = (absIdx0 - LudoPath.playerEntryIndex[1] + LudoPath.TRACK_LENGTH) % LudoPath.TRACK_LENGTH
        val token1OnTrack = token1.copy(step = step1 - 3)  // 3 steps before target

        // Ensure the target cell is NOT a safe cell for this test
        // Use a non-safe cell
        val nonSafeStep0 = findNonSafeStep(0)
        val token0AtNonSafe = token0.copy(step = nonSafeStep0)

        val absNonSafe = LudoPath.trackIndexForStep(0, nonSafeStep0)
        val step1ToReach = (absNonSafe - LudoPath.playerEntryIndex[1] + LudoPath.TRACK_LENGTH) % LudoPath.TRACK_LENGTH
        val token1Before = token1.copy(step = step1ToReach - 3)

        val board = LudoBoard(
            tokens = listOf(token0AtNonSafe, token1Before),
            playerCount = 2,
            diceValue = 3,
            diceRolled = true,
            turnPlayerId = 1
        )

        val state = createGameState(board, 2)
        val action = GameAction.TokenMoveAction(
            tokenId = token1Before.id,
            playerId = 1,
            steps = 3
        )

        assertTrue("Action should be valid", rules.isValidAction(state, action))

        val result = rules.applyAction(board, action, Player(2, "Player 2"))
        val newBoard = result.newBoardData

        // Token0 should be back at base
        val capturedToken = newBoard.getToken(token0AtNonSafe.id)
        assertNotNull("Captured token should exist", capturedToken)
        assertTrue("Captured token should be at base", capturedToken!!.isAtBase)
    }

    @Test
    fun `capture - cannot capture on safe cell`() {
        // Place opponent token on a safe cell
        val safeStep0 = findSafeStep(0)
        val token0 = LudoToken(id = 0, playerId = 0, tokenIdx = 0, step = safeStep0)

        val absIdx = LudoPath.trackIndexForStep(0, safeStep0)
        val step1 = (absIdx - LudoPath.playerEntryIndex[1] + LudoPath.TRACK_LENGTH) % LudoPath.TRACK_LENGTH
        val token1 = LudoToken(id = 4, playerId = 1, tokenIdx = 0, step = step1 - 2)

        val board = LudoBoard(
            tokens = listOf(token0, token1),
            playerCount = 2,
            diceValue = 2,
            diceRolled = true,
            turnPlayerId = 1
        )

        val result = rules.applyAction(board, GameAction.TokenMoveAction(
            tokenId = token1.id, playerId = 1, steps = 2
        ), Player(2, "Player 2"))

        val capturedToken = result.newBoardData.getToken(token0.id)
        assertNotNull(capturedToken)
        assertFalse("Token on safe cell should NOT be captured", capturedToken!!.isAtBase)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Safe Cell Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `safe cell - entry positions are safe`() {
        for (p in 0 until 4) {
            assertTrue(
                "Entry position for player $p should be safe",
                LudoPath.isSafeCell(p, 0)
            )
        }
    }

    @Test
    fun `safe cell - base is always safe`() {
        assertTrue(LudoPath.isSafeCell(0, LudoPath.AT_BASE))
    }

    @Test
    fun `safe cell - home column is always safe`() {
        assertTrue(LudoPath.isSafeCell(0, LudoPath.TRACK_LENGTH + 3))
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Exact Home Entry Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `exact home - token reaches home with exact roll`() {
        val lastStep = LudoPath.TOTAL_STEPS - 1  // One step before home
        val token = LudoToken(id = 0, playerId = 0, tokenIdx = 0, step = lastStep)

        val board = LudoBoard(
            tokens = listOf(token),
            playerCount = 2,
            diceValue = 1,
            diceRolled = true,
            turnPlayerId = 0
        )

        assertTrue("Should be able to move", board.canMoveToken(token, 1))

        val result = rules.applyAction(board, GameAction.TokenMoveAction(
            tokenId = 0, playerId = 0, steps = 1
        ), Player(1, "Player 1"))

        val movedToken = result.newBoardData.getToken(0)
        assertNotNull(movedToken)
        assertTrue("Token should be home", movedToken!!.isHome)
    }

    @Test
    fun `exact home - cannot overshoot home`() {
        val lastStep = LudoPath.TOTAL_STEPS - 1  // One step before home
        val token = LudoToken(id = 0, playerId = 0, tokenIdx = 0, step = lastStep)

        val board = LudoBoard(
            tokens = listOf(token),
            playerCount = 2,
            diceValue = 3,
            diceRolled = true,
            turnPlayerId = 0
        )

        assertFalse("Should NOT be able to overshoot home", board.canMoveToken(token, 3))
    }

    @Test
    fun `exact home - token 2 steps from home needs exactly 2`() {
        val step = LudoPath.TOTAL_STEPS - 2
        val token = LudoToken(id = 0, playerId = 0, tokenIdx = 0, step = step)

        val board = LudoBoard(
            tokens = listOf(token),
            playerCount = 2
        )

        assertTrue("Should move with exactly 2", board.canMoveToken(token, 2))
        assertFalse("Should NOT move with 3", board.canMoveToken(token, 3))
        assertTrue("Should move with 1 (not home yet but valid)", board.canMoveToken(token, 1))
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Extra Turn on Six Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `extra turn - rolling 6 grants extra turn`() {
        val token = LudoToken(id = 0, playerId = 0, tokenIdx = 0, step = 5)
        val board = LudoBoard(
            tokens = listOf(token),
            playerCount = 2,
            turnPlayerId = 0
        )

        val state = createGameState(board, 1)
        val diceResult = rules.applyAction(board, GameAction.DiceRollAction(6), Player(1, "P1"))
        assertTrue("Extra turn should be set after rolling 6", diceResult.newBoardData.extraTurn)
    }

    @Test
    fun `extra turn - rolling non-6 does not grant extra turn`() {
        val token = LudoToken(id = 0, playerId = 0, tokenIdx = 0, step = 5)
        val board = LudoBoard(
            tokens = listOf(token),
            playerCount = 2,
            turnPlayerId = 0
        )

        val diceResult = rules.applyAction(board, GameAction.DiceRollAction(3), Player(1, "P1"))
        assertFalse("Extra turn should NOT be set after rolling 3", diceResult.newBoardData.extraTurn)
    }

    @Test
    fun `extra turn - shouldContinueTurn returns true after 6`() {
        val token = LudoToken(id = 0, playerId = 0, tokenIdx = 0, step = 5)
        val board = LudoBoard(
            tokens = listOf(token),
            playerCount = 2,
            extraTurn = true,
            turnPlayerId = 0
        )

        val state = createGameState(board, 1)
        val result = rules.shouldContinueTurn(state, GameAction.TokenMoveAction(0, 0, 6))
        assertTrue("Should continue turn when extraTurn is set", result)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Token Entry from Base Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `entry - token enters board on rolling 6`() {
        val token = LudoToken(id = 0, playerId = 0, tokenIdx = 0, step = LudoPath.AT_BASE)

        val board = LudoBoard(
            tokens = listOf(token),
            playerCount = 2,
            diceValue = 6,
            diceRolled = true,
            turnPlayerId = 0
        )

        assertTrue("Token at base can move on 6", board.canMoveToken(token, 6))

        val result = rules.applyAction(board, GameAction.TokenMoveAction(
            tokenId = 0, playerId = 0, steps = 6
        ), Player(1, "P1"))

        val movedToken = result.newBoardData.getToken(0)
        assertEquals("Token should be at step 0 (entered board)", 0, movedToken!!.step)
    }

    @Test
    fun `entry - token cannot enter board without rolling 6`() {
        val token = LudoToken(id = 0, playerId = 0, tokenIdx = 0, step = LudoPath.AT_BASE)

        val board = LudoBoard(
            tokens = listOf(token),
            playerCount = 2
        )

        assertFalse("Token at base cannot move on 1", board.canMoveToken(token, 1))
        assertFalse("Token at base cannot move on 5", board.canMoveToken(token, 5))
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Win Detection Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `win - game ends when all tokens reach home`() {
        val tokens = (0 until 4).map { idx ->
            LudoToken(id = idx, playerId = 0, tokenIdx = idx, step = LudoPath.AT_HOME)
        } + listOf(
            LudoToken(id = 4, playerId = 1, tokenIdx = 0, step = 10)
        )

        val board = LudoBoard(tokens = tokens, playerCount = 2, winnerId = 0)
        val state = createGameState(board, 1)

        assertEquals(GameResult.WIN, rules.evaluateResult(state))
    }

    @Test
    fun `win - game not over when some tokens remain`() {
        val tokens = listOf(
            LudoToken(id = 0, playerId = 0, tokenIdx = 0, step = LudoPath.AT_HOME),
            LudoToken(id = 1, playerId = 0, tokenIdx = 1, step = LudoPath.AT_HOME),
            LudoToken(id = 2, playerId = 0, tokenIdx = 2, step = LudoPath.AT_HOME),
            LudoToken(id = 3, playerId = 0, tokenIdx = 3, step = 40),  // Not home yet
            LudoToken(id = 4, playerId = 1, tokenIdx = 0, step = 10)
        )

        val board = LudoBoard(tokens = tokens, playerCount = 2)
        val state = createGameState(board, 1)

        assertEquals(GameResult.IN_PROGRESS, rules.evaluateResult(state))
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════════

    private fun createGameState(board: LudoBoard, currentPlayerId: Int): GameState {
        val players = (1..board.playerCount).map { Player(it, "Player $it") }
        return GameState(
            gameId = "test",
            players = players,
            currentPlayer = players.find { it.id == currentPlayerId } ?: players.first(),
            boardData = board,
            result = GameResult.IN_PROGRESS
        )
    }

    /** Find a step for player 0 that is NOT a safe cell. */
    private fun findNonSafeStep(playerId: Int): Int {
        for (step in 1 until LudoPath.TRACK_LENGTH) {
            if (!LudoPath.isSafeCell(playerId, step)) return step
        }
        return 5  // fallback
    }

    /** Find a step for a player that IS a safe cell. */
    private fun findSafeStep(playerId: Int): Int {
        for (step in 0 until LudoPath.TRACK_LENGTH) {
            if (LudoPath.isSafeCell(playerId, step)) return step
        }
        return 0
    }
}

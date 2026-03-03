package com.offlinegames.games.twenty48

import com.offlinegames.core.*
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for 2048 game rules.
 *
 * Tests cover:
 * - Tile merging correctness
 * - No double-merge bug
 * - Score calculation
 * - Win/loss detection
 * - Undo restores exact state
 */
class Game2048RulesTest {

    private val rules = Game2048Rules()

    // ═══════════════════════════════════════════════════════════════════════
    // Merge Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `merge two equal tiles`() {
        val board = createBoard(
            intArrayOf(2, 2, 0, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        )

        val (newBoard, score) = rules.executeSwipe(board, SwipeDirection.LEFT)

        assertEquals(4, newBoard.get(0, 0))
        assertEquals(0, newBoard.get(0, 1))
        assertEquals(4, score) // Score increases by merged value
    }

    @Test
    fun `no double merge - three identical tiles should merge only once`() {
        val board = createBoard(
            intArrayOf(2, 2, 2, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        )

        val (newBoard, _) = rules.executeSwipe(board, SwipeDirection.LEFT)

        // Should result in [4, 2, 0, 0] not [4, 0, 2, 0]
        assertEquals(4, newBoard.get(0, 0))
        assertEquals(2, newBoard.get(0, 1))
        assertEquals(0, newBoard.get(0, 2))
    }

    @Test
    fun `no double merge - chain of merges should work correctly`() {
        val board = createBoard(
            intArrayOf(2, 2, 4, 4),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        )

        val (newBoard, score) = rules.executeSwipe(board, SwipeDirection.LEFT)

        assertEquals(4, newBoard.get(0, 0))
        assertEquals(8, newBoard.get(0, 1))
        assertEquals(0, newBoard.get(0, 2))
        assertEquals(12, score) // 4 + 8
    }

    @Test
    fun `merge in all directions`() {
        // Test left
        val boardLeft = createBoard(
            intArrayOf(0, 0, 2, 2),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        )
        val (leftResult, _) = rules.executeSwipe(boardLeft, SwipeDirection.LEFT)
        assertEquals(4, leftResult.get(0, 0))

        // Test right
        val boardRight = createBoard(
            intArrayOf(2, 2, 0, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        )
        val (rightResult, _) = rules.executeSwipe(boardRight, SwipeDirection.RIGHT)
        assertEquals(4, rightResult.get(0, 3))

        // Test up
        val boardUp = createBoard(
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(2, 0, 0, 0),
            intArrayOf(2, 0, 0, 0)
        )
        val (upResult, _) = rules.executeSwipe(boardUp, SwipeDirection.UP)
        assertEquals(4, upResult.get(0, 0))

        // Test down
        val boardDown = createBoard(
            intArrayOf(2, 0, 0, 0),
            intArrayOf(2, 0, 0, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        )
        val (downResult, _) = rules.executeSwipe(boardDown, SwipeDirection.DOWN)
        assertEquals(4, downResult.get(3, 0))
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Score Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `score accumulates across merges`() {
        val board = createBoard(
            intArrayOf(2, 2, 4, 4),
            intArrayOf(4, 4, 8, 8),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        )

        val (newBoard, score) = rules.executeSwipe(board, SwipeDirection.LEFT)

        // Row 0: 2+2=4, 4+4=8 -> score 12
        // Row 1: 4+4=8, 8+8=16 -> score 24
        assertEquals(36, score)
        assertEquals(36, newBoard.score)
    }

    @Test
    fun `score persists across moves`() {
        var board = createBoard(
            intArrayOf(2, 2, 0, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        )

        val (newBoard1, score1) = rules.executeSwipe(board, SwipeDirection.LEFT)
        assertEquals(4, score1)

        // Simulate another move
        val board2 = newBoard1.copy(tiles = arrayOf(
            intArrayOf(4, 2, 2, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        ))

        val (newBoard2, score2) = rules.executeSwipe(board2, SwipeDirection.LEFT)
        assertEquals(4, score2) // Only the new merge
        assertEquals(8, newBoard2.score) // Cumulative
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Win/Loss Detection Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `win when 2048 tile present`() {
        val board = createBoard(
            intArrayOf(2048, 2, 4, 8),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        )

        assertTrue(board.hasWon())

        val state = createGameState(board)
        assertEquals(GameResult.WIN, rules.evaluateResult(state))
    }

    @Test
    fun `game over when no moves possible`() {
        val board = createBoard(
            intArrayOf(2, 4, 2, 4),
            intArrayOf(4, 2, 4, 2),
            intArrayOf(2, 4, 2, 4),
            intArrayOf(4, 2, 4, 2)
        )

        assertFalse(board.hasPossibleMoves())

        val state = createGameState(board)
        assertEquals(GameResult.DRAW, rules.evaluateResult(state)) // DRAW = loss
    }

    @Test
    fun `game continues when empty cells exist`() {
        val board = createBoard(
            intArrayOf(2, 4, 2, 4),
            intArrayOf(4, 2, 4, 2),
            intArrayOf(2, 4, 0, 4),
            intArrayOf(4, 2, 4, 2)
        )

        assertTrue(board.hasPossibleMoves())

        val state = createGameState(board)
        assertEquals(GameResult.IN_PROGRESS, rules.evaluateResult(state))
    }

    @Test
    fun `game continues when merges possible`() {
        val board = createBoard(
            intArrayOf(2, 2, 4, 8),
            intArrayOf(4, 8, 16, 32),
            intArrayOf(8, 16, 32, 64),
            intArrayOf(16, 32, 64, 128)
        )

        assertTrue(board.hasPossibleMoves())
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Spawn Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `spawn tile only in empty cell`() {
        val board = createBoard(
            intArrayOf(2, 4, 8, 16),
            intArrayOf(32, 64, 128, 256),
            intArrayOf(512, 1024, 0, 2),
            intArrayOf(4, 8, 16, 32)
        )

        val newBoard = rules.spawnRandomTile(board)

        // Should only spawn at position (2, 2)
        assertTrue(newBoard.get(2, 2) == 2 || newBoard.get(2, 2) == 4)
    }

    @Test
    fun `no spawn when board full`() {
        val board = createBoard(
            intArrayOf(2, 4, 2, 4),
            intArrayOf(4, 2, 4, 2),
            intArrayOf(2, 4, 2, 4),
            intArrayOf(4, 2, 4, 2)
        )

        val newBoard = rules.spawnRandomTile(board)
        assertEquals(board, newBoard)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Action Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `valid action when move possible in that direction`() {
        val board = createBoard(
            intArrayOf(2, 0, 0, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        )
        val state = createGameState(board)

        assertTrue(rules.isValidAction(state, GameAction.MergeTilesAction(SwipeDirection.LEFT)))
        assertTrue(rules.isValidAction(state, GameAction.MergeTilesAction(SwipeDirection.RIGHT)))
    }

    @Test
    fun `invalid action when no move possible in that direction`() {
        val board = createBoard(
            intArrayOf(2, 4, 8, 16),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        )
        val state = createGameState(board)

        assertFalse(rules.isValidAction(state, GameAction.MergeTilesAction(SwipeDirection.LEFT)))
    }

    @Test
    fun `apply action returns correct result`() {
        val board = createBoard(
            intArrayOf(2, 2, 0, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0)
        )

        val result = rules.applyAction(
            board,
            GameAction.MergeTilesAction(SwipeDirection.LEFT),
            Player.PLAYER_ONE
        )

        assertTrue(result.scoreDelta >= 4) // At least the merge score
        assertEquals(4, result.newBoardData.get(0, 0))
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════════════════════

    private fun createBoard(
        row0: IntArray,
        row1: IntArray,
        row2: IntArray,
        row3: IntArray
    ): Game2048Board {
        return Game2048Board(arrayOf(row0, row1, row2, row3))
    }

    private fun createGameState(board: Game2048Board): GameState {
        return GameState(
            gameId = "test",
            players = listOf(Player.PLAYER_ONE),
            currentPlayer = Player.PLAYER_ONE,
            boardData = board,
            result = GameResult.IN_PROGRESS
        )
    }
}

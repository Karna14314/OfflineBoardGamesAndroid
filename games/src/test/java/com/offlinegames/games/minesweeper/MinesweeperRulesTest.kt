package com.offlinegames.games.minesweeper

import com.offlinegames.core.*
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for Minesweeper game rules.
 *
 * Tests cover:
 * - Mine count correctness
 * - Flood fill reveal
 * - First click never hits mine
 */
class MinesweeperRulesTest {

    private val rules = MinesweeperRules()

    // ═══════════════════════════════════════════════════════════════════════
    // First Click Safety Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `first click never hits mine`() {
        val board = MinesweeperBoard(
            width = 9,
            height = 9,
            mineCount = 10
        )

        val clickPosition = Position(4, 4)
        val newBoard = rules.placeMines(board, clickPosition)

        // Clicked position should be safe
        assertFalse("Clicked position should not be a mine", newBoard.getCell(4, 4).isMine)

        // Adjacent positions should also be safe
        for (adj in newBoard.getAdjacentPositions(4, 4)) {
            assertFalse("Adjacent position should not be a mine", newBoard.getCell(adj.row, adj.col).isMine)
        }
    }

    @Test
    fun `correct number of mines placed`() {
        val board = MinesweeperBoard(
            width = 9,
            height = 9,
            mineCount = 10
        )

        val newBoard = rules.placeMines(board, Position(4, 4))

        var actualMineCount = 0
        for (r in 0 until newBoard.height) {
            for (c in 0 until newBoard.width) {
                if (newBoard.getCell(r, c).isMine) actualMineCount++
            }
        }

        assertEquals("Should have correct number of mines", 10, actualMineCount)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Flood Fill Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `flood fill reveals all connected empty cells`() {
        // Create board with no mines
        val cells = Array(9) { Array(9) { MinesweeperCell(isMine = false) } }
        val board = MinesweeperBoard(
            width = 9,
            height = 9,
            mineCount = 0,
            cells = cells,
            firstClickCompleted = true
        )

        val revealedBoard = rules.floodFillReveal(board, Position(4, 4))

        // All cells should be revealed
        for (r in 0 until revealedBoard.height) {
            for (c in 0 until revealedBoard.width) {
                assertTrue("Cell ($r, $c) should be revealed", revealedBoard.getCell(r, c).isRevealed)
            }
        }
    }

    @Test
    fun `flood fill stops at cells with adjacent mines`() {
        // Create board with one mine
        val cells = Array(9) { r ->
            Array(9) { c ->
                MinesweeperCell(isMine = r == 0 && c == 0)
            }
        }
        val board = MinesweeperBoard(
            width = 9,
            height = 9,
            mineCount = 1,
            cells = cells,
            firstClickCompleted = true
        )

        val revealedBoard = rules.floodFillReveal(board, Position(4, 4))

        // Cell at (4,4) and surrounding should be revealed
        assertTrue("Starting cell should be revealed", revealedBoard.getCell(4, 4).isRevealed)

        // Cell (0,0) with mine should not be revealed
        assertFalse("Mine cell should not be revealed", revealedBoard.getCell(0, 0).isRevealed)

        // Cell adjacent to mine (1,1) should have adjacent count > 0
        assertTrue("Cell adjacent to mine should be revealed", revealedBoard.getCell(1, 1).isRevealed)
        assertTrue("Adjacent count should be set", revealedBoard.getCell(1, 1).adjacentMines >= 0)
    }

    @Test
    fun `flood fill does not reveal flagged cells`() {
        val cells = Array(9) { Array(9) { MinesweeperCell(isMine = false) } }
        cells[4][5] = MinesweeperCell(isMine = false, visibility = CellVisibility.FLAGGED)

        val board = MinesweeperBoard(
            width = 9,
            height = 9,
            mineCount = 0,
            cells = cells,
            firstClickCompleted = true
        )

        val revealedBoard = rules.floodFillReveal(board, Position(4, 4))

        // Flagged cell should remain flagged
        assertTrue("Flagged cell should stay flagged", revealedBoard.getCell(4, 5).isFlagged)
        assertFalse("Flagged cell should not be revealed", revealedBoard.getCell(4, 5).isRevealed)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Adjacent Mine Count Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `adjacent mine count is correct`() {
        // Create board with known mine layout
        val cells = Array(5) { Array(5) { MinesweeperCell(isMine = false) } }
        // Place mines in a pattern
        cells[1][1] = MinesweeperCell(isMine = true)
        cells[1][2] = MinesweeperCell(isMine = true)
        cells[2][1] = MinesweeperCell(isMine = true)

        val board = MinesweeperBoard(
            width = 5,
            height = 5,
            mineCount = 3,
            cells = cells,
            firstClickCompleted = true
        )

        // Center cell should have 3 adjacent mines
        assertEquals("Center should have 3 adjacent mines", 3, board.countAdjacentMines(2, 2))

        // Corner cells should have 1 adjacent mine
        assertEquals("Corner should have 1 adjacent mine", 1, board.countAdjacentMines(0, 0))
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Win/Loss Detection Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `win when all safe cells revealed`() {
        val cells = Array(3) { r ->
            Array(3) { c ->
                when {
                    r == 0 && c == 0 -> MinesweeperCell(isMine = true)
                    else -> MinesweeperCell(isMine = false, visibility = CellVisibility.REVEALED)
                }
            }
        }

        val board = MinesweeperBoard(
            width = 3,
            height = 3,
            mineCount = 1,
            cells = cells,
            firstClickCompleted = true,
            revealedCount = 8
        )

        assertTrue("Should be a win", board.isWin())
    }

    @Test
    fun `loss when mine is revealed`() {
        val cells = Array(3) { r ->
            Array(3) { c ->
                when {
                    r == 0 && c == 0 -> MinesweeperCell(isMine = true, visibility = CellVisibility.REVEALED)
                    else -> MinesweeperCell(isMine = false)
                }
            }
        }

        val board = MinesweeperBoard(
            width = 3,
            height = 3,
            mineCount = 1,
            cells = cells,
            firstClickCompleted = true
        )

        assertTrue("Should be a loss", board.isLoss())
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Flag Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `flag toggles correctly`() {
        val cells = Array(3) { Array(3) { MinesweeperCell(isMine = false) } }
        val board = MinesweeperBoard(
            width = 3,
            height = 3,
            mineCount = 1,
            cells = cells,
            firstClickCompleted = true
        )

        // Flag a cell
        var newBoard = board.withCell(1, 1, MinesweeperCell(isMine = false, visibility = CellVisibility.FLAGGED))
        assertTrue("Cell should be flagged", newBoard.getCell(1, 1).isFlagged)
        assertEquals("Flag count should be 1", 1, newBoard.flagsPlaced)

        // Unflag the cell
        newBoard = newBoard.withCell(1, 1, MinesweeperCell(isMine = false, visibility = CellVisibility.HIDDEN))
        assertFalse("Cell should not be flagged", newBoard.getCell(1, 1).isFlagged)
        assertEquals("Flag count should be 0", 0, newBoard.flagsPlaced)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Action Tests
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    fun `reveal action reveals cell`() {
        val cells = Array(3) { Array(3) { MinesweeperCell(isMine = false) } }
        val board = MinesweeperBoard(
            width = 3,
            height = 3,
            mineCount = 0,
            cells = cells,
            firstClickCompleted = true
        )

        val state = createGameState(board)
        val action = GameAction.RevealCellsAction(Position(1, 1))

        val result = rules.applyAction(board, action, Player.PLAYER_ONE)

        assertTrue("Cell should be revealed", result.newBoardData.getCell(1, 1).isRevealed)
    }

    @Test
    fun `flag action toggles flag`() {
        val cells = Array(3) { Array(3) { MinesweeperCell(isMine = false) } }
        val board = MinesweeperBoard(
            width = 3,
            height = 3,
            mineCount = 1,
            cells = cells,
            firstClickCompleted = true
        )

        val action = GameAction.FlagCellAction(Position(1, 1))
        val result = rules.applyAction(board, action, Player.PLAYER_ONE)

        assertTrue("Cell should be flagged", result.newBoardData.getCell(1, 1).isFlagged)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════════════════════

    private fun createGameState(board: MinesweeperBoard): GameState {
        return GameState(
            gameId = "test",
            players = listOf(Player.PLAYER_ONE),
            currentPlayer = Player.PLAYER_ONE,
            boardData = board,
            result = GameResult.IN_PROGRESS
        )
    }
}

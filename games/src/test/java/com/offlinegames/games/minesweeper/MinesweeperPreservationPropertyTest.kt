package com.offlinegames.games.minesweeper

import com.offlinegames.core.*
import org.junit.Test
import org.junit.Assert.*
import kotlin.random.Random

/**
 * Preservation Property Tests for Minesweeper Gameplay Fixes
 *
 * These tests verify that non-buggy behaviors remain unchanged after the fix.
 * They use property-based testing methodology with randomized inputs to provide
 * strong guarantees across the input domain.
 *
 * **IMPORTANT**: These tests should PASS on UNFIXED code to establish baseline behavior.
 * After fixes are implemented, these tests should continue to PASS, confirming preservation.
 *
 * **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**
 */
class MinesweeperPreservationPropertyTest {

    private val rules = MinesweeperRules()
    private val seed = 42L // Fixed seed for reproducibility
    private val random = Random(seed)
    
    companion object {
        // Number of test cases to generate for each property
        const val NUM_TEST_CASES = 50
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Property 2.1: Numbered Cell Single Reveal Preservation
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Property 2.1: Numbered cell single reveal preservation test
     *
     * **Validates: Requirements 3.6**
     *
     * Property: For all numbered cells (adjacentMines > 0), clicking reveals only that
     * single cell without flood fill.
     *
     * This test generates random board configurations with numbered cells and verifies
     * that clicking them reveals only the clicked cell, not adjacent cells.
     *
     * EXPECTED OUTCOME on UNFIXED code: PASS (baseline behavior)
     * EXPECTED OUTCOME on FIXED code: PASS (behavior preserved)
     */
    @Test
    fun `property - numbered cell reveals only itself without flood fill`() {
        repeat(NUM_TEST_CASES) { iteration ->
            // Generate random board with mines
            val boardSize = random.nextInt(5, 10)
            val mineCount = random.nextInt(1, boardSize * boardSize / 3)
            
            // Create board with random mine placement
            val cells = Array(boardSize) { r ->
                Array(boardSize) { c ->
                    MinesweeperCell(isMine = false)
                }
            }
            
            // Place mines randomly
            val minePositions = mutableSetOf<Pair<Int, Int>>()
            while (minePositions.size < mineCount) {
                val r = random.nextInt(boardSize)
                val c = random.nextInt(boardSize)
                minePositions.add(r to c)
            }
            
            for ((r, c) in minePositions) {
                cells[r][c] = MinesweeperCell(isMine = true)
            }
            
            val board = MinesweeperBoard(
                width = boardSize,
                height = boardSize,
                mineCount = mineCount,
                cells = cells,
                firstClickCompleted = true
            )
            
            // Find a numbered cell (adjacentMines > 0, not a mine, not empty)
            var numberedCellPos: Position? = null
            for (r in 0 until boardSize) {
                for (c in 0 until boardSize) {
                    if (!board.getCell(r, c).isMine) {
                        val adjMines = board.countAdjacentMines(r, c)
                        if (adjMines > 0) {
                            numberedCellPos = Position(r, c)
                            break
                        }
                    }
                }
                if (numberedCellPos != null) break
            }
            
            // Skip if no numbered cell found
            if (numberedCellPos == null) return@repeat
            
            // Click on the numbered cell
            val action = GameAction.RevealCellsAction(numberedCellPos)
            val result = rules.applyAction(board, action, Player.PLAYER_ONE)
            val newBoard = result.newBoardData
            
            // Verify only the clicked cell is revealed
            assertTrue(
                "Iteration $iteration: Clicked numbered cell should be revealed",
                newBoard.getCell(numberedCellPos.row, numberedCellPos.col).isRevealed
            )
            
            // Verify adjacent cells are NOT revealed (no flood fill)
            val adjacents = board.getAdjacentPositions(numberedCellPos.row, numberedCellPos.col)
            for (adj in adjacents) {
                assertFalse(
                    "Iteration $iteration: Adjacent cell (${adj.row},${adj.col}) should NOT be revealed by numbered cell click",
                    newBoard.getCell(adj.row, adj.col).isRevealed
                )
            }
            
            // Verify revealedCount is exactly 1
            assertEquals(
                "Iteration $iteration: Only 1 cell should be revealed",
                1,
                newBoard.revealedCount
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Property 2.2: Mine Reveal Game-Ending Preservation
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Property 2.2: Mine reveal game-ending preservation test
     *
     * **Validates: Requirements 3.1**
     *
     * Property: For all mine cells, clicking reveals the mine and ends the game with loss.
     *
     * This test generates random board configurations with mines and verifies that
     * clicking on a mine cell ends the game with gameEnded == true.
     *
     * EXPECTED OUTCOME on UNFIXED code: PASS (baseline behavior)
     * EXPECTED OUTCOME on FIXED code: PASS (behavior preserved)
     */
    @Test
    fun `property - mine reveal ends game with loss`() {
        repeat(NUM_TEST_CASES) { iteration ->
            // Generate random board with mines
            val boardSize = random.nextInt(5, 10)
            val mineCount = random.nextInt(1, boardSize * boardSize / 3)
            
            // Create board with random mine placement
            val cells = Array(boardSize) { r ->
                Array(boardSize) { c ->
                    MinesweeperCell(isMine = false)
                }
            }
            
            // Place mines randomly
            val minePositions = mutableSetOf<Pair<Int, Int>>()
            while (minePositions.size < mineCount) {
                val r = random.nextInt(boardSize)
                val c = random.nextInt(boardSize)
                minePositions.add(r to c)
            }
            
            for ((r, c) in minePositions) {
                cells[r][c] = MinesweeperCell(isMine = true)
            }
            
            val board = MinesweeperBoard(
                width = boardSize,
                height = boardSize,
                mineCount = mineCount,
                cells = cells,
                firstClickCompleted = true
            )
            
            // Pick a random mine position
            val minePos = minePositions.random(random)
            val clickPosition = Position(minePos.first, minePos.second)
            
            // Click on the mine
            val action = GameAction.RevealCellsAction(clickPosition)
            val result = rules.applyAction(board, action, Player.PLAYER_ONE)
            
            // Verify game ended
            assertTrue(
                "Iteration $iteration: Clicking mine should end the game",
                result.gameEnded
            )
            
            // Verify mine cell is revealed
            assertTrue(
                "Iteration $iteration: Mine cell should be revealed",
                result.newBoardData.getCell(clickPosition.row, clickPosition.col).isRevealed
            )
            
            // Verify it's a loss (not a win)
            assertTrue(
                "Iteration $iteration: Game should be a loss",
                result.newBoardData.isLoss()
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Property 2.3: Win Detection Preservation
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Property 2.3: Win detection preservation test
     *
     * **Validates: Requirements 3.2**
     *
     * Property: For all board states with all non-mine cells revealed, game ends with win.
     *
     * This test generates random board configurations where all non-mine cells are revealed
     * and verifies that the game correctly detects a win condition.
     *
     * EXPECTED OUTCOME on UNFIXED code: PASS (baseline behavior)
     * EXPECTED OUTCOME on FIXED code: PASS (behavior preserved)
     */
    @Test
    fun `property - revealing all non-mine cells ends game with win`() {
        repeat(NUM_TEST_CASES) { iteration ->
            // Generate random board with mines
            val boardSize = random.nextInt(3, 8)
            val mineCount = random.nextInt(1, boardSize * boardSize / 3)
            
            // Create board with random mine placement
            val cells = Array(boardSize) { r ->
                Array(boardSize) { c ->
                    MinesweeperCell(isMine = false)
                }
            }
            
            // Place mines randomly
            val minePositions = mutableSetOf<Pair<Int, Int>>()
            while (minePositions.size < mineCount) {
                val r = random.nextInt(boardSize)
                val c = random.nextInt(boardSize)
                minePositions.add(r to c)
            }
            
            for ((r, c) in minePositions) {
                cells[r][c] = MinesweeperCell(isMine = true)
            }
            
            // Reveal all non-mine cells
            var revealedCount = 0
            for (r in 0 until boardSize) {
                for (c in 0 until boardSize) {
                    if (!cells[r][c].isMine) {
                        val adjMines = countAdjacentMines(cells, boardSize, boardSize, r, c)
                        cells[r][c] = MinesweeperCell(
                            isMine = false,
                            visibility = CellVisibility.REVEALED,
                            adjacentMines = adjMines
                        )
                        revealedCount++
                    }
                }
            }
            
            val board = MinesweeperBoard(
                width = boardSize,
                height = boardSize,
                mineCount = mineCount,
                cells = cells,
                firstClickCompleted = true,
                revealedCount = revealedCount
            )
            
            // Verify win condition
            assertTrue(
                "Iteration $iteration: All non-mine cells revealed should result in win",
                board.isWin()
            )
            
            // Verify no mines are revealed
            for (r in 0 until boardSize) {
                for (c in 0 until boardSize) {
                    if (board.getCell(r, c).isMine) {
                        assertFalse(
                            "Iteration $iteration: Mine at ($r,$c) should not be revealed in win state",
                            board.getCell(r, c).isRevealed
                        )
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Property 2.4: First-Click Safety Preservation
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Property 2.4: First-click safety preservation test
     *
     * **Validates: Requirements 3.3**
     *
     * Property: For all first-click positions, mines are placed after click and clicked
     * area (cell + neighbors) is mine-free.
     *
     * This test generates random first-click positions and verifies that the placeMines
     * function ensures the clicked cell and its neighbors are safe.
     *
     * EXPECTED OUTCOME on UNFIXED code: PASS (baseline behavior)
     * EXPECTED OUTCOME on FIXED code: PASS (behavior preserved)
     */
    @Test
    fun `property - first click places mines safely`() {
        repeat(NUM_TEST_CASES) { iteration ->
            // Generate random board configuration
            val boardSize = random.nextInt(5, 12)
            val mineCount = random.nextInt(1, boardSize * boardSize / 3)
            
            val board = MinesweeperBoard(
                width = boardSize,
                height = boardSize,
                mineCount = mineCount,
                firstClickCompleted = false
            )
            
            // Pick a random first-click position
            val clickRow = random.nextInt(boardSize)
            val clickCol = random.nextInt(boardSize)
            val clickPosition = Position(clickRow, clickCol)
            
            // Place mines
            val newBoard = rules.placeMines(board, clickPosition)
            
            // Verify clicked cell is mine-free
            assertFalse(
                "Iteration $iteration: Clicked cell ($clickRow,$clickCol) should not be a mine",
                newBoard.getCell(clickRow, clickCol).isMine
            )
            
            // Verify all neighbors are mine-free
            val adjacents = newBoard.getAdjacentPositions(clickRow, clickCol)
            for (adj in adjacents) {
                assertFalse(
                    "Iteration $iteration: Neighbor (${adj.row},${adj.col}) should not be a mine",
                    newBoard.getCell(adj.row, adj.col).isMine
                )
            }
            
            // Verify correct number of mines placed
            var actualMineCount = 0
            for (r in 0 until boardSize) {
                for (c in 0 until boardSize) {
                    if (newBoard.getCell(r, c).isMine) {
                        actualMineCount++
                    }
                }
            }
            
            assertEquals(
                "Iteration $iteration: Should have correct number of mines",
                mineCount,
                actualMineCount
            )
            
            // Verify firstClickCompleted flag is set
            assertTrue(
                "Iteration $iteration: firstClickCompleted should be true",
                newBoard.firstClickCompleted
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Property 2.5: Flagged Cell Reveal Prevention Preservation
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Property 2.5: Flagged cell reveal prevention preservation test
     *
     * **Validates: Requirements 3.5**
     *
     * Property: For all flagged cells, attempting to reveal them does nothing.
     *
     * This test generates random board configurations with flagged cells and verifies
     * that revealCell action does not change the state of flagged cells.
     *
     * EXPECTED OUTCOME on UNFIXED code: PASS (baseline behavior)
     * EXPECTED OUTCOME on FIXED code: PASS (behavior preserved)
     */
    @Test
    fun `property - flagged cells cannot be revealed`() {
        repeat(NUM_TEST_CASES) { iteration ->
            // Generate random board
            val boardSize = random.nextInt(5, 10)
            val mineCount = random.nextInt(1, boardSize * boardSize / 3)
            
            // Create board with random mine placement
            val cells = Array(boardSize) { r ->
                Array(boardSize) { c ->
                    MinesweeperCell(isMine = false)
                }
            }
            
            // Place mines randomly
            val minePositions = mutableSetOf<Pair<Int, Int>>()
            while (minePositions.size < mineCount) {
                val r = random.nextInt(boardSize)
                val c = random.nextInt(boardSize)
                minePositions.add(r to c)
            }
            
            for ((r, c) in minePositions) {
                cells[r][c] = MinesweeperCell(isMine = true)
            }
            
            // Flag a random cell
            val flagRow = random.nextInt(boardSize)
            val flagCol = random.nextInt(boardSize)
            cells[flagRow][flagCol] = cells[flagRow][flagCol].copy(visibility = CellVisibility.FLAGGED)
            
            val board = MinesweeperBoard(
                width = boardSize,
                height = boardSize,
                mineCount = mineCount,
                cells = cells,
                firstClickCompleted = true,
                flagsPlaced = 1
            )
            
            // Attempt to reveal the flagged cell
            val action = GameAction.RevealCellsAction(Position(flagRow, flagCol))
            val result = rules.applyAction(board, action, Player.PLAYER_ONE)
            val newBoard = result.newBoardData
            
            // Verify flagged cell remains flagged
            assertTrue(
                "Iteration $iteration: Flagged cell ($flagRow,$flagCol) should remain flagged",
                newBoard.getCell(flagRow, flagCol).isFlagged
            )
            
            // Verify flagged cell is not revealed
            assertFalse(
                "Iteration $iteration: Flagged cell ($flagRow,$flagCol) should not be revealed",
                newBoard.getCell(flagRow, flagCol).isRevealed
            )
            
            // Verify game did not end
            assertFalse(
                "Iteration $iteration: Game should not end from attempting to reveal flagged cell",
                result.gameEnded
            )
            
            // Verify board state unchanged
            assertEquals(
                "Iteration $iteration: revealedCount should remain 0",
                0,
                newBoard.revealedCount
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Property 2.6: Incorrect Chord Count Preservation
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Property 2.6: Incorrect chord count preservation test
     *
     * **Validates: Requirements 3.4**
     *
     * Property: For all revealed cells where adjacentFlags != adjacentMines, chord action
     * does nothing.
     *
     * This test generates random board configurations with revealed numbered cells and
     * incorrect flag counts, verifying that chord action does not reveal any cells.
     *
     * EXPECTED OUTCOME on UNFIXED code: PASS (baseline behavior)
     * EXPECTED OUTCOME on FIXED code: PASS (behavior preserved)
     */
    @Test
    fun `property - chord with incorrect flag count does nothing`() {
        repeat(NUM_TEST_CASES) { iteration ->
            // Generate random board
            val boardSize = random.nextInt(5, 10)
            val mineCount = random.nextInt(2, boardSize * boardSize / 3)
            
            // Create board with random mine placement
            val cells = Array(boardSize) { r ->
                Array(boardSize) { c ->
                    MinesweeperCell(isMine = false)
                }
            }
            
            // Place mines randomly
            val minePositions = mutableSetOf<Pair<Int, Int>>()
            while (minePositions.size < mineCount) {
                val r = random.nextInt(boardSize)
                val c = random.nextInt(boardSize)
                minePositions.add(r to c)
            }
            
            for ((r, c) in minePositions) {
                cells[r][c] = MinesweeperCell(isMine = true)
            }
            
            val board = MinesweeperBoard(
                width = boardSize,
                height = boardSize,
                mineCount = mineCount,
                cells = cells,
                firstClickCompleted = true
            )
            
            // Find a numbered cell (adjacentMines > 0)
            var numberedCellPos: Position? = null
            var adjacentMines = 0
            for (r in 0 until boardSize) {
                for (c in 0 until boardSize) {
                    if (!board.getCell(r, c).isMine) {
                        val adjMines = board.countAdjacentMines(r, c)
                        if (adjMines > 0) {
                            numberedCellPos = Position(r, c)
                            adjacentMines = adjMines
                            break
                        }
                    }
                }
                if (numberedCellPos != null) break
            }
            
            // Skip if no numbered cell found
            if (numberedCellPos == null) return@repeat
            
            // Reveal the numbered cell
            cells[numberedCellPos.row][numberedCellPos.col] = MinesweeperCell(
                isMine = false,
                visibility = CellVisibility.REVEALED,
                adjacentMines = adjacentMines
            )
            
            // Place INCORRECT number of flags (not matching adjacentMines)
            val adjacents = board.getAdjacentPositions(numberedCellPos.row, numberedCellPos.col)
            val incorrectFlagCount = if (adjacentMines > 1) adjacentMines - 1 else adjacentMines + 1
            var flagsPlaced = 0
            for (adj in adjacents) {
                if (flagsPlaced < incorrectFlagCount && flagsPlaced < adjacents.size) {
                    cells[adj.row][adj.col] = cells[adj.row][adj.col].copy(visibility = CellVisibility.FLAGGED)
                    flagsPlaced++
                }
            }
            
            val boardWithFlags = MinesweeperBoard(
                width = boardSize,
                height = boardSize,
                mineCount = mineCount,
                cells = cells,
                firstClickCompleted = true,
                flagsPlaced = flagsPlaced,
                revealedCount = 1
            )
            
            // Attempt chord action
            val action = GameAction.ChordAction(numberedCellPos)
            val result = rules.applyAction(boardWithFlags, action, Player.PLAYER_ONE)
            val newBoard = result.newBoardData
            
            // Verify NO new cells were revealed
            assertEquals(
                "Iteration $iteration: revealedCount should remain 1 (chord should not activate)",
                1,
                newBoard.revealedCount
            )
            
            // Verify unflagged neighbors remain hidden
            for (adj in adjacents) {
                if (!newBoard.getCell(adj.row, adj.col).isFlagged) {
                    assertFalse(
                        "Iteration $iteration: Unflagged neighbor (${adj.row},${adj.col}) should remain hidden",
                        newBoard.getCell(adj.row, adj.col).isRevealed
                    )
                }
            }
            
            // Verify game did not end
            assertFalse(
                "Iteration $iteration: Game should not end from incorrect chord",
                result.gameEnded
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Count adjacent mines for a cell in a 2D array.
     */
    private fun countAdjacentMines(
        cells: Array<Array<MinesweeperCell>>,
        height: Int,
        width: Int,
        row: Int,
        col: Int
    ): Int {
        var count = 0
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val nr = row + dr
                val nc = col + dc
                if (nr in 0 until height && nc in 0 until width && cells[nr][nc].isMine) {
                    count++
                }
            }
        }
        return count
    }
}

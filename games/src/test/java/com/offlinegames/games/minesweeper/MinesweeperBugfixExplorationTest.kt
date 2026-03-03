package com.offlinegames.games.minesweeper

import com.offlinegames.core.*
import org.junit.Test
import org.junit.Assert.*

/**
 * Bug Condition Exploration Tests for Minesweeper Gameplay Fixes
 *
 * These tests encode the EXPECTED behavior and are designed to FAIL on unfixed code.
 * When these tests fail, they confirm the bugs exist and provide counterexamples.
 * After the bugs are fixed, these same tests should PASS, validating the fixes.
 *
 * **Validates: Requirements 2.1, 2.2, 2.3**
 */
class MinesweeperBugfixExplorationTest {

    private val rules = MinesweeperRules()

    // ═══════════════════════════════════════════════════════════════════════
    // Bug 1: Flood Fill Not Working
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Test 1.1: Empty cell flood fill exploration test
     *
     * **Validates: Requirements 2.1**
     *
     * Tests that clicking on an empty cell (adjacentMines == 0) reveals all connected
     * empty cells and their numbered borders through flood fill.
     *
     * EXPECTED OUTCOME on UNFIXED code: Test FAILS - only single cell revealed
     * EXPECTED OUTCOME on FIXED code: Test PASSES - flood fill reveals 10-20+ cells
     */
    @Test
    fun `empty cell click should trigger flood fill revealing connected region`() {
        // Create a 9x9 board with a region of empty cells
        // Place a single mine in the corner to create numbered boundaries
        val cells = Array(9) { r ->
            Array(9) { c ->
                // Mine at (0, 0) creates a numbered boundary around it
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

        // Click on an empty cell far from the mine (center of board)
        val clickPosition = Position(4, 4)
        val action = GameAction.RevealCellsAction(clickPosition)

        val result = rules.applyAction(board, action, Player.PLAYER_ONE)
        val newBoard = result.newBoardData

        // Verify the clicked cell is revealed
        assertTrue(
            "Clicked cell (4,4) should be revealed",
            newBoard.getCell(4, 4).isRevealed
        )

        // Verify flood fill occurred - count revealed cells
        var revealedCount = 0
        for (r in 0 until newBoard.height) {
            for (c in 0 until newBoard.width) {
                if (newBoard.getCell(r, c).isRevealed) {
                    revealedCount++
                }
            }
        }

        // With only one mine in corner, clicking center should reveal most of the board
        // Expected: 10-20+ cells revealed (all empty cells + numbered borders)
        // Bug behavior: Only 1 cell revealed
        assertTrue(
            "Flood fill should reveal multiple cells (expected 10+, got $revealedCount)",
            revealedCount >= 10
        )

        // Verify numbered border cells are revealed
        // Cells adjacent to the mine should be revealed with adjacentMines > 0
        assertTrue(
            "Cell (1,1) adjacent to mine should be revealed",
            newBoard.getCell(1, 1).isRevealed
        )
        assertTrue(
            "Cell (1,1) should have adjacentMines > 0",
            newBoard.getCell(1, 1).adjacentMines > 0
        )
    }

    /**
     * Test 1.4: Flood fill boundary exploration test
     *
     * **Validates: Requirements 2.1**
     *
     * Tests that flood fill stops at numbered cells (doesn't continue through them).
     *
     * EXPECTED OUTCOME: Should pass if flood fill logic correctly stops at boundaries
     */
    @Test
    fun `flood fill should stop at numbered boundary cells`() {
        // Create a board with mines forming a boundary
        // Pattern: mines at top row, empty cells below
        val cells = Array(5) { r ->
            Array(5) { c ->
                // Mines in top row create a numbered boundary at row 1
                MinesweeperCell(isMine = r == 0)
            }
        }

        val board = MinesweeperBoard(
            width = 5,
            height = 5,
            mineCount = 5,
            cells = cells,
            firstClickCompleted = true
        )

        // Click on an empty cell in the bottom area
        val clickPosition = Position(4, 2)
        val action = GameAction.RevealCellsAction(clickPosition)

        val result = rules.applyAction(board, action, Player.PLAYER_ONE)
        val newBoard = result.newBoardData

        // Verify flood fill revealed empty cells in bottom area
        assertTrue("Cell (4,2) should be revealed", newBoard.getCell(4, 2).isRevealed)
        assertTrue("Cell (3,2) should be revealed", newBoard.getCell(3, 2).isRevealed)
        assertTrue("Cell (2,2) should be revealed", newBoard.getCell(2, 2).isRevealed)

        // Verify numbered boundary at row 1 is revealed
        assertTrue("Boundary cell (1,2) should be revealed", newBoard.getCell(1, 2).isRevealed)
        assertTrue(
            "Boundary cell (1,2) should have adjacentMines > 0",
            newBoard.getCell(1, 2).adjacentMines > 0
        )

        // Verify flood fill did NOT continue through numbered cells to mines
        assertFalse("Mine cell (0,2) should NOT be revealed", newBoard.getCell(0, 2).isRevealed)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Bug 2: Flag Placement Not Working
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Test 1.2: Flag placement exploration test
     *
     * **Validates: Requirements 2.2**
     *
     * Tests that flagCell action toggles cell visibility to FLAGGED.
     *
     * EXPECTED OUTCOME on UNFIXED code: Test FAILS - cell remains HIDDEN
     * EXPECTED OUTCOME on FIXED code: Test PASSES - cell becomes FLAGGED
     */
    @Test
    fun `flag action should toggle cell visibility to FLAGGED`() {
        // Create a simple board with hidden cells
        val cells = Array(3) { Array(3) { MinesweeperCell(isMine = false) } }

        val board = MinesweeperBoard(
            width = 3,
            height = 3,
            mineCount = 1,
            cells = cells,
            firstClickCompleted = true
        )

        // Apply flag action to a hidden cell
        val flagPosition = Position(1, 1)
        val action = GameAction.FlagCellAction(flagPosition)

        val result = rules.applyAction(board, action, Player.PLAYER_ONE)
        val newBoard = result.newBoardData

        // Verify cell is now flagged
        assertTrue(
            "Cell (1,1) should be FLAGGED after flag action",
            newBoard.getCell(1, 1).isFlagged
        )

        // Verify flagsPlaced counter incremented
        assertEquals(
            "flagsPlaced should increment to 1",
            1,
            newBoard.flagsPlaced
        )

        // Verify cell visibility is FLAGGED
        assertEquals(
            "Cell visibility should be FLAGGED",
            CellVisibility.FLAGGED,
            newBoard.getCell(1, 1).visibility
        )
    }

    /**
     * Test 1.5: Flag toggle exploration test
     *
     * **Validates: Requirements 2.2**
     *
     * Tests that flagging a cell again removes the flag (toggle behavior).
     *
     * EXPECTED OUTCOME: Should pass if toggle logic works correctly
     */
    @Test
    fun `flag action on flagged cell should remove flag`() {
        // Create a board with a flagged cell
        val cells = Array(3) { r ->
            Array(3) { c ->
                if (r == 1 && c == 1) {
                    MinesweeperCell(isMine = false, visibility = CellVisibility.FLAGGED)
                } else {
                    MinesweeperCell(isMine = false)
                }
            }
        }

        val board = MinesweeperBoard(
            width = 3,
            height = 3,
            mineCount = 1,
            cells = cells,
            firstClickCompleted = true,
            flagsPlaced = 1
        )

        // Apply flag action again to toggle off
        val flagPosition = Position(1, 1)
        val action = GameAction.FlagCellAction(flagPosition)

        val result = rules.applyAction(board, action, Player.PLAYER_ONE)
        val newBoard = result.newBoardData

        // Verify cell is no longer flagged
        assertFalse(
            "Cell (1,1) should NOT be flagged after toggle",
            newBoard.getCell(1, 1).isFlagged
        )

        // Verify flagsPlaced counter decremented
        assertEquals(
            "flagsPlaced should decrement to 0",
            0,
            newBoard.flagsPlaced
        )

        // Verify cell visibility is HIDDEN
        assertEquals(
            "Cell visibility should be HIDDEN",
            CellVisibility.HIDDEN,
            newBoard.getCell(1, 1).visibility
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Bug 3: Chord Reveal Not Working
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Test 1.3: Chord reveal exploration test
     *
     * **Validates: Requirements 2.3**
     *
     * Tests that chord action reveals unflagged neighbors when flag count matches.
     *
     * EXPECTED OUTCOME on UNFIXED code: Test FAILS - unflagged neighbors remain hidden
     * EXPECTED OUTCOME on FIXED code: Test PASSES - unflagged neighbors revealed
     */
    @Test
    fun `chord action should reveal unflagged neighbors when flag count matches`() {
        // Create a board with a revealed cell showing "3"
        // Pattern:
        //   M F H
        //   F 3 H
        //   F H H
        // Where M=mine, F=flagged, H=hidden, 3=revealed with adjacentMines=3
        val cells = Array(3) { r ->
            Array(3) { c ->
                when {
                    // Mines at (0,0), (1,0), (2,0)
                    c == 0 -> MinesweeperCell(isMine = true)
                    // Center cell revealed with adjacentMines=3
                    r == 1 && c == 1 -> MinesweeperCell(
                        isMine = false,
                        visibility = CellVisibility.REVEALED,
                        adjacentMines = 3
                    )
                    else -> MinesweeperCell(isMine = false)
                }
            }
        }

        // Flag the three mine cells
        cells[0][0] = cells[0][0].copy(visibility = CellVisibility.FLAGGED)
        cells[1][0] = cells[1][0].copy(visibility = CellVisibility.FLAGGED)
        cells[2][0] = cells[2][0].copy(visibility = CellVisibility.FLAGGED)

        val board = MinesweeperBoard(
            width = 3,
            height = 3,
            mineCount = 3,
            cells = cells,
            firstClickCompleted = true,
            flagsPlaced = 3,
            revealedCount = 1
        )

        // Apply chord action on the revealed cell
        val chordPosition = Position(1, 1)
        val action = GameAction.ChordAction(chordPosition)

        val result = rules.applyAction(board, action, Player.PLAYER_ONE)
        val newBoard = result.newBoardData

        // Verify unflagged neighbors are revealed
        // Unflagged neighbors: (0,1), (0,2), (1,2), (2,1), (2,2)
        assertTrue(
            "Cell (0,1) should be revealed by chord",
            newBoard.getCell(0, 1).isRevealed
        )
        assertTrue(
            "Cell (0,2) should be revealed by chord",
            newBoard.getCell(0, 2).isRevealed
        )
        assertTrue(
            "Cell (1,2) should be revealed by chord",
            newBoard.getCell(1, 2).isRevealed
        )
        assertTrue(
            "Cell (2,1) should be revealed by chord",
            newBoard.getCell(2, 1).isRevealed
        )
        assertTrue(
            "Cell (2,2) should be revealed by chord",
            newBoard.getCell(2, 2).isRevealed
        )

        // Verify flagged cells remain flagged
        assertTrue(
            "Flagged cell (0,0) should remain flagged",
            newBoard.getCell(0, 0).isFlagged
        )
        assertTrue(
            "Flagged cell (1,0) should remain flagged",
            newBoard.getCell(1, 0).isFlagged
        )
        assertTrue(
            "Flagged cell (2,0) should remain flagged",
            newBoard.getCell(2, 0).isFlagged
        )
    }

    /**
     * Test 1.6: Chord incorrect flag count exploration test
     *
     * **Validates: Requirements 2.3**
     *
     * Tests that chord does nothing when flag count doesn't match adjacentMines.
     *
     * EXPECTED OUTCOME: Should pass if validation logic is correct
     */
    @Test
    fun `chord action should do nothing when flag count is incorrect`() {
        // Create a board with a revealed cell showing "3" but only 2 flags
        val cells = Array(3) { r ->
            Array(3) { c ->
                when {
                    // Mines at (0,0), (1,0), (2,0)
                    c == 0 -> MinesweeperCell(isMine = true)
                    // Center cell revealed with adjacentMines=3
                    r == 1 && c == 1 -> MinesweeperCell(
                        isMine = false,
                        visibility = CellVisibility.REVEALED,
                        adjacentMines = 3
                    )
                    else -> MinesweeperCell(isMine = false)
                }
            }
        }

        // Flag only TWO of the three mine cells (incorrect count)
        cells[0][0] = cells[0][0].copy(visibility = CellVisibility.FLAGGED)
        cells[1][0] = cells[1][0].copy(visibility = CellVisibility.FLAGGED)

        val board = MinesweeperBoard(
            width = 3,
            height = 3,
            mineCount = 3,
            cells = cells,
            firstClickCompleted = true,
            flagsPlaced = 2,
            revealedCount = 1
        )

        // Apply chord action on the revealed cell
        val chordPosition = Position(1, 1)
        val action = GameAction.ChordAction(chordPosition)

        val result = rules.applyAction(board, action, Player.PLAYER_ONE)
        val newBoard = result.newBoardData

        // Verify NO cells were revealed (chord should do nothing)
        // Check that unflagged neighbors remain hidden
        assertFalse(
            "Cell (0,1) should remain hidden (chord should not activate)",
            newBoard.getCell(0, 1).isRevealed
        )
        assertFalse(
            "Cell (0,2) should remain hidden",
            newBoard.getCell(0, 2).isRevealed
        )
        assertFalse(
            "Cell (1,2) should remain hidden",
            newBoard.getCell(1, 2).isRevealed
        )
        assertFalse(
            "Cell (2,1) should remain hidden",
            newBoard.getCell(2, 1).isRevealed
        )
        assertFalse(
            "Cell (2,2) should remain hidden",
            newBoard.getCell(2, 2).isRevealed
        )

        // Verify revealedCount unchanged
        assertEquals(
            "revealedCount should remain 1 (no new reveals)",
            1,
            newBoard.revealedCount
        )
    }
}

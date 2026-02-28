package com.offlinegames.games.dotsandboxes

import com.offlinegames.core.*

/**
 * Simple heuristic AI for Dots & Boxes.
 *
 * Strategy (priority order):
 * 1. If a move completes a box → play it (greedy capture).
 * 2. Avoid moves that give the opponent a 3-sided box (which they can then complete).
 * 3. Fallback: random legal move.
 */
class DotsAndBoxesAI {

    private val rules = DotsAndBoxesRules()

    fun selectMove(state: GameState, aiPlayer: Player): Move? {
        val legalMoves = rules.getLegalMoves(state, aiPlayer)
        if (legalMoves.isEmpty()) return null

        val board = state.boardData as GridBoard

        // 1. Find moves that complete a box
        val captureMoves = mutableListOf<Move>()
        for (move in legalMoves) {
            val newBoard = rules.applyMove(board, move, aiPlayer)
            val newBoxes = countNewBoxes(board, newBoard)
            if (newBoxes > 0) captureMoves.add(move)
        }
        if (captureMoves.isNotEmpty()) return captureMoves.maxByOrNull { move ->
            val newBoard = rules.applyMove(board, move, aiPlayer)
            countNewBoxes(board, newBoard)
        }

        // 2. Find safe moves (don't create a 3-sided box for opponent)
        val safeMoves = legalMoves.filter { move ->
            val newBoard = rules.applyMove(board, move, aiPlayer)
            !creates3SidedBox(newBoard, move.position.row, move.position.col)
        }
        if (safeMoves.isNotEmpty()) return safeMoves.random()

        // 3. Fallback: random
        return legalMoves.random()
    }

    private fun countNewBoxes(before: GridBoard, after: GridBoard): Int {
        val gridSize = DotsAndBoxesRules.GRID_SIZE
        var count = 0
        for (r in 1 until gridSize step 2) {
            for (c in 1 until gridSize step 2) {
                val wasClaimed = before.get(r, c) != 0 && before.get(r, c) != DotsAndBoxesRules.DOT
                val isClaimed = after.get(r, c) != 0 && after.get(r, c) != DotsAndBoxesRules.DOT
                if (!wasClaimed && isClaimed) count++
            }
        }
        return count
    }

    /**
     * Check if drawing edge at (r,c) creates any adjacent box with exactly 3 sides
     * (which would let the opponent complete it).
     */
    private fun creates3SidedBox(board: GridBoard, edgeR: Int, edgeC: Int): Boolean {
        val gridSize = DotsAndBoxesRules.GRID_SIZE
        val adjacentBoxes = mutableListOf<Pair<Int, Int>>()

        if (DotsAndBoxesRules.isHorizontalEdge(edgeR, edgeC)) {
            if (edgeR > 0) adjacentBoxes.add(edgeR - 1 to edgeC)
            if (edgeR < gridSize - 1) adjacentBoxes.add(edgeR + 1 to edgeC)
        } else if (DotsAndBoxesRules.isVerticalEdge(edgeR, edgeC)) {
            if (edgeC > 0) adjacentBoxes.add(edgeR to edgeC - 1)
            if (edgeC < gridSize - 1) adjacentBoxes.add(edgeR to edgeC + 1)
        }

        for ((br, bc) in adjacentBoxes) {
            if (!DotsAndBoxesRules.isBoxCenter(br, bc)) continue
            val edgeCount = rules.countEdges(board, br, bc)
            // If the box now has exactly 3 edges and is not yet complete (4 = complete),
            // that's dangerous — opponent can complete it next turn
            if (edgeCount == 3 && board.get(br, bc) == 0) return true
        }
        return false
    }
}

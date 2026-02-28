package com.offlinegames.core

/**
 * Contract that every game's rule set must satisfy.
 *
 * Implementations know nothing about rendering or AI — they only
 * determine if moves are legal and when the game has ended.
 *
 * [S] is the game-specific board type (e.g. TicTacToeBoard).
 */
interface GameRules<S : Any> {

    /**
     * Returns true if [move] is valid given the current [state].
     * Must be a pure function with no side effects.
     */
    fun isValidMove(state: GameState, move: Move): Boolean

    /**
     * Returns all moves that [player] may legally make in [state].
     */
    fun getLegalMoves(state: GameState, player: Player): List<Move>

    /**
     * Evaluates [state] after the latest move and returns the updated
     * [GameResult]. Called by the reducer after every move application.
     */
    fun evaluateResult(state: GameState): GameResult

    /**
     * Applies [move] to [boardData] and returns the new board object.
     * This is the only place where board mutation logic lives.
     */
    fun applyMove(boardData: S, move: Move, player: Player): S

    /**
     * Returns true if the turn should advance to the next player after this move.
     * Override to return false for games with extra-turn mechanics (SOS, Dots & Boxes).
     * Called after applyMove with the NEW state (after move applied).
     */
    fun shouldAdvanceTurn(state: GameState): Boolean = true

    /**
     * Compute updated scores after a move. Default returns existing scores.
     * Override for scoring games like SOS and Dots & Boxes.
     */
    fun computeScores(state: GameState, move: Move): Map<Int, Int> = state.scores
}

package com.offlinegames.core

/**
 * A discrete action that alters the game state.
 * This is the language of the Reducer.
 * ViewModels or specific Intent mappings translate user Intents into these GameActions.
 *
 * The GameAction layer sits between Intent and Reducer, enabling:
 * - Multiple cell changes in one turn
 * - Chain reactions
 * - Automatic state transitions
 * - Complex game mechanics (merging, capturing, flood fill)
 */
sealed class GameAction {

    // ─────────────────────────────────────────────────────────────────────────
    // Standard placement or sliding move (used by TicTacToe, Connect4, SOS, etc.)
    // ─────────────────────────────────────────────────────────────────────────
    data class MovePieceAction(val move: Move) : GameAction()

    // ─────────────────────────────────────────────────────────────────────────
    // 2048 - Tile sliding and merging
    // ─────────────────────────────────────────────────────────────────────────
    data class MergeTilesAction(val direction: SwipeDirection) : GameAction()

    // ─────────────────────────────────────────────────────────────────────────
    // Minesweeper - Cell reveal and flagging
    // ─────────────────────────────────────────────────────────────────────────
    data class RevealCellsAction(val position: Position) : GameAction()
    data class FlagCellAction(val position: Position) : GameAction()
    data class RevealAreaAction(val positions: List<Position>) : GameAction()
    data class ChordAction(val position: Position) : GameAction()

    // ─────────────────────────────────────────────────────────────────────────
    // Checkers - Captures and chain moves
    // ─────────────────────────────────────────────────────────────────────────
    data class CaptureAction(
        val move: Move,
        val capturedPiece: Position,
        val continueChain: Boolean = false
    ) : GameAction()

    data class ChainMoveAction(
        val moves: List<Move>,
        val capturedPositions: List<Position>
    ) : GameAction()

    data class PromotePieceAction(
        val position: Position,
        val newType: PieceType
    ) : GameAction()

    // ─────────────────────────────────────────────────────────────────────────
    // Ludo - Dice roll and token movement
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Roll the dice. [result] is 1–6.
     * If result is 6, the current player gets an extra turn.
     */
    data class DiceRollAction(val result: Int) : GameAction()

    /**
     * Move a specific token along its path.
     *
     * @param tokenId  Index of the token to move (0–3)
     * @param playerId Owner of the token
     * @param steps    Number of steps to move (dice result)
     */
    data class TokenMoveAction(
        val tokenId: Int,
        val playerId: Int,
        val steps: Int
    ) : GameAction()

    /**
     * Pass the turn to the next player.
     * Used when a player rolled the dice but has no valid moves.
     */
    data class PassTurnAction(val playerId: Int) : GameAction()

    // ─────────────────────────────────────────────────────────────────────────
    // Air Hockey - Real-time physics
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Update a paddle position from touch input.
     *
     * @param playerId 1 or 2
     * @param x        Normalised x position (0.0 – 1.0)
     * @param y        Normalised y position (0.0 – 1.0)
     */
    data class PaddleMoveAction(
        val playerId: Int,
        val x: Float,
        val y: Float
    ) : GameAction()

    /**
     * Advance the puck physics by one tick (dispatched by game loop).
     */
    data class PuckUpdateAction(
        val deltaSeconds: Float
    ) : GameAction()

    // ─────────────────────────────────────────────────────────────────────────
    // System actions
    // ─────────────────────────────────────────────────────────────────────────
    object RestartAction : GameAction()
    object UndoAction : GameAction()
    object SaveAndExitAction : GameAction()
}

/**
 * Swipe directions for gesture-based games like 2048.
 */
enum class SwipeDirection { UP, DOWN, LEFT, RIGHT }

/**
 * Piece types for games with different piece classes (Checkers, Chess, etc.)
 */
enum class PieceType {
    NONE,       // Empty cell
    MAN,        // Regular piece (Checkers)
    KING,       // Promoted piece (Checkers)
    PAWN,       // Chess
    KNIGHT,     // Chess
    BISHOP,     // Chess
    ROOK,       // Chess
    QUEEN,      // Chess
    KING_CHESS  // Chess king
}

/**
 * Cell state for games with hidden information (Minesweeper).
 */
enum class CellVisibility {
    HIDDEN,     // Cell not revealed
    REVEALED,   // Cell content visible
    FLAGGED,    // Cell marked as mine
    QUESTIONED  // Cell marked with question
}

/**
 * Result of applying a GameAction to a game state.
 * Contains the new state and any metadata about the action.
 *
 * @param newBoardData The transformed board data
 * @param scoreDelta Points gained/lost from this action
 * @param chainActions Follow-up actions to process (for chain reactions)
 * @param gameEnded Whether this action ended the game
 */
data class ActionResult<S : Any>(
    val newBoardData: S,
    val scoreDelta: Int = 0,
    val chainActions: List<GameAction> = emptyList(),
    val gameEnded: Boolean = false,
    val moveRecord: MoveRecord? = null
)

/**
 * Interface for games that support the new GameAction-based architecture.
 * Games implementing this can support complex multi-cell mutations and chain reactions.
 *
 * @param S The concrete board type
 */
interface ActionBasedRules<S : Any> : GameRules<S> {

    /**
     * Apply a GameAction to the board and return the result.
     * This is the primary method for state transitions in action-based games.
     */
    fun applyAction(boardData: S, action: GameAction, player: Player): ActionResult<S>

    /**
     * Check if an action is valid in the current state.
     */
    fun isValidAction(state: GameState, action: GameAction): Boolean

    /**
     * Get all valid actions for a player (optional - for AI/hinting).
     */
    fun getValidActions(state: GameState, player: Player): List<GameAction> = emptyList()

    /**
     * Returns true if the current player must continue their turn (chain capture, etc.).
     */
    fun shouldContinueTurn(state: GameState, lastAction: GameAction): Boolean = false

    /**
     * Support for undo: return the inverse action if possible.
     */
    fun getUndoAction(state: GameState): GameAction? = null
}

/**
 * Extension function to convert legacy GameIntent to GameAction.
 * Used for backward compatibility with existing placement-based games.
 */
fun GameIntent.toGameAction(): GameAction = when (this) {
    is GameIntent.MakeMove -> GameAction.MovePieceAction(move)
    is GameIntent.RestartGame -> GameAction.RestartAction
    is GameIntent.UndoMove -> GameAction.UndoAction
    is GameIntent.SaveAndExit -> GameAction.SaveAndExitAction
}

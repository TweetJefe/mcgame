package game.moves;

/**
 * Rejected requests preserve chess state; elapsed thinking time still counts.
 */
public interface MoveResult {
    boolean isSuccess();

    String getMessage();
}

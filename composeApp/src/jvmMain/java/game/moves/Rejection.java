package game.moves;

public interface Rejection extends MoveResult {
    @Override
    default boolean isSuccess() {
        return false;
    }
}

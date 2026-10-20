package game.moves;

public enum Success implements MoveResult {
    SUCCESS("Ход выполнен");

    private final String message;

    Success(String message) {
        this.message = message;
    }

    @Override
    public boolean isSuccess() {
        return true;
    }

    @Override
    public String getMessage() {
        return message;
    }
}

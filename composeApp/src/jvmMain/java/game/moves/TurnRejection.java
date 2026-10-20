package game.moves;

public enum TurnRejection implements Rejection {
    NO_TURN_CONTINUATION("После действия нельзя безопасно завершить ход"),
    MOVE_ALREADY_USED("Обычное перемещение в этом ходу уже выполнено"),
    ABILITY_ALREADY_USED("В этом ходу уже применён скилл или ульта"),
    MOVE_REQUIRED("Сначала сделайте обычное перемещение фигуры"),
    UNSUPPORTED_ACTION("Действие доступно только в режиме Moba Chess"),
    NO_MOVE_AFTER_ABILITY("После способности нет допустимого обязательного перемещения"),
    GAME_OVER("Партия уже завершена"),
    WRONG_PLAYER("Сейчас ход другого игрока");

    private final String message;

    TurnRejection(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}

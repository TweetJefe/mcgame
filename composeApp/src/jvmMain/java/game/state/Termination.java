package game.state;


public enum Termination {
    IN_PROGRESS("Партия идёт"),
    CHECKMATE("Мат"), STALEMATE("Пат"),
    INSUFFICIENT_MATERIAL("Недостаточно материала для мата"),
    THREEFOLD_REPETITION("Троекратное повторение: ничья заявлена"),
    FIVEFOLD_REPETITION("Пятикратное повторение"),
    FIFTY_MOVE_RULE("Правило 50 ходов: ничья заявлена"),
    SEVENTY_FIVE_MOVE_RULE("Правило 75 ходов"),
    RESIGNATION("Сдача"), RESIGNATION_DRAW("Сдача: соперник не может поставить мат"),
    DRAW_AGREEMENT("Ничья по соглашению"),
    TIMEOUT("Время истекло"), TIMEOUT_DRAW("Время истекло: соперник не может поставить мат");

    private final String message;
    Termination(String message) { this.message = message; }
    public String getMessage() { return message; }
}

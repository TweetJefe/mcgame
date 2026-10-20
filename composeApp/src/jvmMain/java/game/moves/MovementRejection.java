package game.moves;

public enum MovementRejection implements Rejection {
    PROMOTION_REQUIRED("Выберите фигуру для превращения пешки"),
    INVALID_PROMOTION("Превращение возможно только для пешки на последней горизонтали"),
    OUT_OF_BOUNDS("Координаты отсутствуют или находятся за пределами доски"),
    SAME_SQUARE("Начальная и конечная клетки совпадают"),
    EMPTY_SOURCE("На начальной клетке нет фигуры"),
    WRONG_FIGURE_COLOR("Эта фигура принадлежит другому игроку"),
    OWN_PIECE_AT_TARGET("Нельзя взять собственную фигуру"),
    KING_CAPTURE_NOT_ALLOWED("Короля нельзя взять"),
    ILLEGAL_MOVEMENT("Фигура не может перейти на эту клетку"),
    KING_IN_CHECK("После этого хода ваш король окажется под шахом"),
    INVALID_BOARD("Состояние доски некорректно");

    private final String message;

    MovementRejection(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}

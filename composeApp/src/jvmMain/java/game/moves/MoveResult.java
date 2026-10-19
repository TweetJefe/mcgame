package game.moves;


/** Rejected requests preserve chess state; elapsed thinking time still counts. */
public enum MoveResult {
    MUD_BLOCKED("Путь перекрыт вражеским болотом"),
    TERRAIN_OCCUPIED("Клетка уже занята другим болотом"),
    NO_TURN_CONTINUATION("После действия нельзя безопасно завершить ход"),
    ECONOMY_DISABLED("В этой партии экономика отключена"),
    NO_GOLD_TO_STEAL("У соперника нет золота для кражи"),
    STUNNED("Фигура оглушена и не может действовать"),
    SILENCED("Внутри тучи нельзя применять скиллы и ульты"),
    TARGET_INVISIBLE("Вражеская фигура невидима"),
    NO_USED_ABILITY("Цель ещё не применяла активных способностей"),
    NO_BORROWED_ABILITY("Нет такой украденной способности"),
    BORROWED_NOT_READY("Украденная способность доступна со следующего собственного хода"),
    SUCCESS("Ход выполнен"),
    MOVE_ALREADY_USED("Обычное перемещение в этом ходу уже выполнено"),
    ABILITY_ALREADY_USED("В этом ходу уже применён скилл или ульта"),
    MOVE_REQUIRED("Сначала сделайте обычное перемещение фигуры"),
    UNSUPPORTED_ACTION("Действие доступно только в режиме Moba Chess"),
    INVALID_ABILITY("Неизвестная способность"),
    NO_ABILITY("У этой фигуры нет выбранной способности"),
    ABILITY_COOLDOWN("Способность ещё перезаряжается"),
    BLINDED_CANNOT_CAPTURE("Ослеплённая фигура не может брать фигуры"),
    WRONG_SQUARE_COLOR("Телепорт доступен только на клетки исходного цвета Lumeshard"),
    ULTIMATE_NOT_READY("Ульта ещё не заряжена"),
    ALREADY_SWALLOWED("В Пудже уже находится союзная фигура"),
    INVALID_ABILITY_TARGET("Недопустимая цель способности"),
    ABILITY_OUT_OF_RANGE("Цель вне направления или радиуса способности"),
    ABILITY_BLOCKED("Хук перекрыт другой фигурой"),
    NO_MOVE_AFTER_ABILITY("После способности нет допустимого обязательного перемещения"),
    KING_IMMUNE("Король невосприимчив к способностям"),
    INVALID_ITEM_SLOT("У фигуры только три слота предметов"),
    EMPTY_ITEM_SLOT("В этом слоте нет предмета"),
    ITEM_NOT_AVAILABLE("Этот предмет ещё не реализован"),
    GAME_OVER("Партия уже завершена"),
    PROMOTION_REQUIRED("Выберите фигуру для превращения пешки"),
    INVALID_PROMOTION("Превращение возможно только для пешки на последней горизонтали"),
    INVALID_DRAW_CLAIM("Условий для заявления ничьей нет"),
    NO_DRAW_OFFER("Нет предложения ничьей от соперника"),
    DRAW_OFFER_PENDING("Предложение ничьей уже ожидает ответа"),
    DRAW_AGREEMENT_TOO_EARLY("Для соглашения на ничью обе стороны должны сделать хотя бы один ход"),
    WRONG_PLAYER("Сейчас ход другого игрока"),
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

    MoveResult(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return this == SUCCESS;
    }

    public String getMessage() {
        return message;
    }
}

package game.moves;

public enum AbilityRejection implements Rejection {
    NO_SUMMON_SPACE("Для призыва нет свободного или заменяемого места"),
    MAX_EVOLUTION("Достигнута последняя ступень улучшения"),
    NO_GRAPPLE_MOVEMENT("Фигура уже стоит рядом; притягиваться некуда"),
    MUD_BLOCKED("Путь перекрыт вражеским болотом"),
    TERRAIN_OCCUPIED("Клетка уже занята другим болотом"),
    NO_CHARGED_TARGETS("У этого героя нет доступных заряженных врагов"),
    NO_USED_ABILITY("Цель ещё не применяла активных способностей"),
    NO_BORROWED_ABILITY("Нет такой украденной способности"),
    BORROWED_NOT_READY("Украденная способность доступна со следующего собственного хода"),
    INVALID_ABILITY("Неизвестная способность"),
    NO_ABILITY("У этой фигуры нет выбранной способности"),
    ABILITY_COOLDOWN("Способность ещё перезаряжается"),
    WRONG_SQUARE_COLOR("Телепорт доступен только на клетки исходного цвета Lumeshard"),
    ULTIMATE_NOT_READY("Ульта ещё не заряжена"),
    ALREADY_SWALLOWED("В Пудже уже находится союзная фигура"),
    INVALID_ABILITY_TARGET("Недопустимая цель способности"),
    ABILITY_OUT_OF_RANGE("Цель вне направления или радиуса способности"),
    ABILITY_BLOCKED("Путь способности перекрыт другой фигурой");

    private final String message;

    AbilityRejection(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}

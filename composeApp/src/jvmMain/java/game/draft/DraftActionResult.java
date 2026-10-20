package game.draft;

public enum DraftActionResult {
    SUCCESS("Готово"), WRONG_SIDE("Сейчас очередь другой стороны"), WRONG_PHASE("Это действие недоступно на данном этапе"),
    DRAFT_COMPLETE("Драфт уже завершён"), INVALID_HERO("Выберите героя из каталога"), HERO_BANNED("Этот герой забанен"),
    HERO_PICKED("Этот герой уже выбран и недоступен обеим сторонам"), INVALID_SQUARE("Выберите подходящую стартовую клетку своей стороны"),
    SQUARE_OCCUPIED("На этой клетке уже выбран другой герой"), PICK_LIMIT("Состав уже заполнен");
    private final String message;

    DraftActionResult(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return this == SUCCESS;
    }
}

package game.heroes;


public enum HeroType {
    NONE("Обычная фигура"), PUDGE_ROOK("Пудж-Ладья"), LUMESHARD_BISHOP("Lumeshard-Слон"), BOUNTY_CROW_PAWN("Bounty Crow-Пешка"), BOLOTNIK_KNIGHT("Bolotnik-Конь"), IZBA_ROOK("Изба-Ладья"), BABA_YAGA_PAWN("Баба-яга"), GINSENG_BISHOP("Ginseng-Слон");
    private final String name;
    HeroType(String name) { this.name = name; }
    public String getName() { return name; }
    public boolean supports(Class<? extends model.Figure> type) {
        return switch (this) {
            case NONE -> false;
            case PUDGE_ROOK, IZBA_ROOK -> type == model.figures.Rook.class;
            case LUMESHARD_BISHOP, GINSENG_BISHOP -> type == model.figures.Bishop.class;
            case BOUNTY_CROW_PAWN, BABA_YAGA_PAWN -> type == model.figures.Pawn.class;
            case BOLOTNIK_KNIGHT -> type == model.figures.Knight.class;
        };
    }
}

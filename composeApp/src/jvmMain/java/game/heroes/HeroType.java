package game.heroes;


public enum HeroType {
    KIKIMORA_BISHOP("Kikimora-Слон"), CHRONOMLIN_PAWN("Chronomlin-Пешка"), CLOWNGANGER_KNIGHT("Clownganger-Конь"), ILLUSION("Иллюзия"), MOLEMINER_PAWN("Moleminer-Пешка"), ZEUS_ROOK("Zeus-Ладья"), NONE("Обычная фигура"), PUDGE_ROOK("Пудж-Ладья"), LUMESHARD_BISHOP("Lumeshard-Слон"), BOUNTY_CROW_PAWN("Bounty Crow-Пешка"), BOLOTNIK_KNIGHT("Bolotnik-Конь"), IZBA_ROOK("Изба-Ладья"), BABA_YAGA_PAWN("Баба-яга"), GINSENG_BISHOP("Ginseng-Слон"), FAUN_KNIGHT("Faun-Конь"), ARES_BISHOP("Ares-Слон");
    private final String name;

    HeroType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean supports(Class<? extends model.Figure> type) {
        return switch (this) {
            case NONE -> false;
            case ZEUS_ROOK, PUDGE_ROOK, IZBA_ROOK -> type == model.figures.Rook.class;
            case KIKIMORA_BISHOP, LUMESHARD_BISHOP, GINSENG_BISHOP, ARES_BISHOP -> type == model.figures.Bishop.class;
            case CHRONOMLIN_PAWN, MOLEMINER_PAWN, BOUNTY_CROW_PAWN, BABA_YAGA_PAWN -> type == model.figures.Pawn.class;
            case ILLUSION -> type == model.figures.Knight.class || type == model.figures.Bishop.class || type == model.figures.Rook.class;
            case CLOWNGANGER_KNIGHT, BOLOTNIK_KNIGHT, FAUN_KNIGHT -> type == model.figures.Knight.class;
        };
    }
}

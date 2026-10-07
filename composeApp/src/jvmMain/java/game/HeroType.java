package game;

public enum HeroType {
    NONE("Обычная фигура"), PUDGE_ROOK("Пудж-Ладья");
    private final String name;
    HeroType(String name) { this.name = name; }
    public String getName() { return name; }
}

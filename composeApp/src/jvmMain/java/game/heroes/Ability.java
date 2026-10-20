package game.heroes;

public enum Ability {
    HOOK("Хук"), SWALLOW("Проглотить"), LASER("Лазер"), TELEPORT("Телепорт"), BOUNTY("Награда"), STEAL("Украсть золото"),
    MUD("Грязь"), AMBUSH("Засада"), SUMMON_YAGA("Призвать Бабу-ягу"), DISPLACE("Переместить фигуру"), COPY_SPELL("Копировать последнее"), CLOUD("Туча"), INVISIBILITY("Невидимость"), SPEAR("Копьё");
    private final String displayName;

    Ability(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

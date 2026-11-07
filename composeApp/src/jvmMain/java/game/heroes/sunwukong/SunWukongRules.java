package game.heroes.sunwukong;

public record SunWukongRules(int shapeshiftCooldown, int shapeshiftTurns, int ultimateCaptures) {
    public SunWukongRules {
        if (shapeshiftCooldown < 0 || shapeshiftCooldown > 100 || shapeshiftTurns < 1
                || shapeshiftTurns > 100 || ultimateCaptures < 0 || ultimateCaptures > 100)
            throw new IllegalArgumentException("Invalid Sun Wukong rules");
    }
    public static SunWukongRules prototype() { return new SunWukongRules(4, 1, 1); }
    public static SunWukongRules sandbox() { return new SunWukongRules(0, 1, 0); }
}

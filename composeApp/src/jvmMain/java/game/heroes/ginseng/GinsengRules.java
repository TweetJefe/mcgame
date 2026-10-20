package game.heroes.ginseng;

public record GinsengRules(int displacementCooldown, int ultimateCaptures) {
    public GinsengRules {
        if (displacementCooldown < 0 || displacementCooldown > 100 || ultimateCaptures < 0 || ultimateCaptures > 100)
            throw new IllegalArgumentException("Invalid Ginseng rules");
    }

    public static GinsengRules prototype() {
        return new GinsengRules(3, 2);
    }

    public static GinsengRules sandbox() {
        return new GinsengRules(0, 0);
    }
}

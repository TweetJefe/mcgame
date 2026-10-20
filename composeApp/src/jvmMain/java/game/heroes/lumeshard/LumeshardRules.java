package game.heroes.lumeshard;

public record LumeshardRules(int laserCooldown, int blindnessTurns, int ultimateCaptures) {
    public LumeshardRules {
        if (laserCooldown < 0 || blindnessTurns < 1 || ultimateCaptures < 0)
            throw new IllegalArgumentException("Invalid Lumeshard parameters");
    }

    public static LumeshardRules prototype() {
        return new LumeshardRules(3, 1, 2);
    }

    public static LumeshardRules sandbox() {
        return new LumeshardRules(0, 1, 0);
    }
}

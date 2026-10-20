package game.heroes.faun;

public record FaunRules(int cloudCooldown, int cloudTurns, int invisibilityTurns, int ultimateCaptures) {
    public FaunRules {
        if (cloudCooldown < 0 || cloudCooldown > 100 || cloudTurns < 1 || cloudTurns > 100 || invisibilityTurns < 1 || invisibilityTurns > 100 || ultimateCaptures < 0 || ultimateCaptures > 100)
            throw new IllegalArgumentException("Invalid Faun rules");
    }

    public static FaunRules prototype() {
        return new FaunRules(3, 2, 2, 2);
    }

    public static FaunRules sandbox() {
        return new FaunRules(0, 2, 2, 0);
    }
}

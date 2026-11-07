package game.heroes.strix;

public record StrixRules(int rewindCooldown, int ultimateCaptures) {
    public StrixRules {
        if (rewindCooldown < 0 || rewindCooldown > 100 || ultimateCaptures < 0 || ultimateCaptures > 100)
            throw new IllegalArgumentException("Invalid Strix rules");
    }
    public static StrixRules prototype() { return new StrixRules(4, 2); }
    public static StrixRules sandbox() { return new StrixRules(0, 0); }
}

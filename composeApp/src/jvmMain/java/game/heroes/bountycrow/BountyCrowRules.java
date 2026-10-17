package game.heroes.bountycrow;

public record BountyCrowRules(int bountyCooldown, int bountyTurns, int ultimateCaptures) {
    public BountyCrowRules {
        if (bountyCooldown < 0 || bountyCooldown > 100 || bountyTurns < 1 || bountyTurns > 100 || ultimateCaptures < 0 || ultimateCaptures > 100)
            throw new IllegalArgumentException("Invalid Crow rules");
    }
    public static BountyCrowRules prototype() { return new BountyCrowRules(3, 2, 2); }
    public static BountyCrowRules sandbox() { return new BountyCrowRules(0, 2, 0); }
}

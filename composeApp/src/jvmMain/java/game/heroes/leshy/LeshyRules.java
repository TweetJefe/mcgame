package game.heroes.leshy;

public record LeshyRules(int vineLength, int vineTurns, int vineCooldown, int treeTurns, int ultimateCaptures) {
    public LeshyRules {
        if (vineLength < 1 || vineLength > 7 || vineTurns < 1 || vineTurns > 100 || vineCooldown < 0 || vineCooldown > 100 || treeTurns < 1 || treeTurns > 100 || ultimateCaptures < 0 || ultimateCaptures > 100)
            throw new IllegalArgumentException("Invalid Leshy rules");
    }
    public static LeshyRules prototype() { return new LeshyRules(3, 3, 4, 2, 1); }
}

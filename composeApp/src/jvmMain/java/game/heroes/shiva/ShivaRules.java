package game.heroes.shiva;

public record ShivaRules(int castRadius, int revealRadius, int lotusTurns, int lotusCooldown, int ultimateCaptures) {
    public ShivaRules {
        if (castRadius < 1 || castRadius > 7 || revealRadius < 1 || revealRadius > 7 || lotusTurns < 1
                || lotusTurns > 100 || lotusCooldown < 0 || lotusCooldown > 100 || ultimateCaptures < 0 || ultimateCaptures > 100)
            throw new IllegalArgumentException("Invalid Shiva rules");
    }
    public static ShivaRules prototype() { return new ShivaRules(2, 3, 2, 3, 2); }
}

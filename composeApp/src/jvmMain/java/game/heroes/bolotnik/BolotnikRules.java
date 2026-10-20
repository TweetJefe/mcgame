package game.heroes.bolotnik;

public record BolotnikRules(int mudCooldown, int mudTurns, int ultimateCaptures) {
    public BolotnikRules {
        if (mudCooldown < 0 || mudCooldown > 100 || mudTurns < 1 || mudTurns > 100 || ultimateCaptures < 0 || ultimateCaptures > 100)
            throw new IllegalArgumentException("Invalid Bolotnik rules");
    }

    public static BolotnikRules prototype() {
        return new BolotnikRules(4, 2, 2);
    }

    public static BolotnikRules sandbox() {
        return new BolotnikRules(0, 2, 0);
    }
}

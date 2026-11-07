package game.heroes.supernova;

public record SupernovaRules(int pullCooldown, int ultimateCaptures, int castRadius, int effectRadius) {
    public SupernovaRules {
        if (pullCooldown < 0 || pullCooldown > 100 || ultimateCaptures < 0 || ultimateCaptures > 100 || castRadius < 1 || castRadius > 100 || effectRadius < 1 || effectRadius > 100) throw new IllegalArgumentException("Invalid Supernova rules");
    }
    public static SupernovaRules prototype() { return new SupernovaRules(2, 1, 2, 3); }
}

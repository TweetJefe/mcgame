package game.heroes.phoenix;

public record PhoenixRules(int ultimateCaptures, int eggTurns) {
    public PhoenixRules {
        if (ultimateCaptures < 0 || ultimateCaptures > 100 || eggTurns < 1 || eggTurns > 100) throw new IllegalArgumentException("Invalid Phoenix rules");
    }
    public static PhoenixRules prototype() { return new PhoenixRules(2, 2); }
}

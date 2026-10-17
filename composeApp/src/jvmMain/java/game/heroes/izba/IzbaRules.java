package game.heroes.izba;

public record IzbaRules(int ultimateCaptures, int lifetimeTurns) {
    public IzbaRules {
        if (ultimateCaptures < 0 || ultimateCaptures > 100 || lifetimeTurns < 1 || lifetimeTurns > 100)
            throw new IllegalArgumentException("Invalid Izba rules");
    }
    public static IzbaRules prototype() { return new IzbaRules(2, 3); }
    public static IzbaRules sandbox() { return new IzbaRules(0, 3); }
}

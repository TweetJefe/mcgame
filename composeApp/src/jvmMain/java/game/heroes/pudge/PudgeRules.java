package game.heroes.pudge;


/** Balance parameters are explicit and can be changed without altering the action model. */
public record PudgeRules(int hookCooldown, int ultimateCaptures, boolean hookDiagonals, boolean swallowDiagonals) {
    /** Current prototype: hook cooldown remains provisional; ultimate has no cooldown. */
    public static PudgeRules prototype() { return new PudgeRules(2, 2, true, true); }
    public static PudgeRules sandbox() { return new PudgeRules(0, 0, true, true); }
    public PudgeRules {
        if (hookCooldown < 0 || ultimateCaptures < 0) throw new IllegalArgumentException("Negative ability parameter");
    }
}

package game;

/** Balance parameters are explicit and can be changed without altering the action model. */
public record PudgeRules(int hookCooldown, int ultimateCaptures, boolean hookDiagonals, boolean swallowDiagonals) {
    public PudgeRules {
        if (hookCooldown < 0 || ultimateCaptures < 0) throw new IllegalArgumentException("Negative ability parameter");
    }
}

package game.heroes;

import model.Position;

/**
 * An explicit casting channel and optional landing cell for two-stage targeting.
 */
public record AbilityRequest(Position actor, Abilities ability, Position target, Position destination, boolean borrowed) {
    public AbilityRequest(Position actor, Abilities ability, Position target) {
        this(actor, ability, target, null, false);
    }
}

package game.heroes.chronomlin;

import game.history.GameEvent;
import game.mechanics.MechanicDefinition;
import game.mechanics.MechanicKey;
import game.mechanics.PieceBodyChange;
import java.util.List;
import java.util.Map;
import model.Figure;

/** Метаданные эволюции принадлежат её реализации, в том числе при копировании. */
public final class ChronomlinFormMechanic implements MechanicDefinition<ChronomlinFormMechanic.State> {
    public enum State { INSTANCE }
    public static final ChronomlinFormMechanic INSTANCE = new ChronomlinFormMechanic();
    private static final MechanicKey<State> KEY = new MechanicKey<>("chronomlin.forms", State.class, State.INSTANCE);

    private ChronomlinFormMechanic() {}
    @Override public MechanicKey<State> key() { return KEY; }

    @Override public List<PieceBodyChange> bodyChanges(GameEvent event, Map<Long, Class<? extends Figure>> before) {
        if (!event.isAbility(ChronomlinAbility.EVOLVE)) return List.of();
        Class<? extends Figure> body = before.get(event.actorId());
        if (body == null) return List.of();
        Class<? extends Figure> next = ChronomlinHero.nextType(body);
        if (next == null || event.targetId() != null || !event.from().equals(event.to()))
            throw new IllegalArgumentException("Invalid evolution step");
        return List.of(new PieceBodyChange(event.actorId(), body, next));
    }
}

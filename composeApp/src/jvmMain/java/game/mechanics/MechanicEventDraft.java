package game.mechanics;

import game.history.GameEvent;
import java.util.Objects;
import model.FigureColor;
import model.Position;

/** Предложенная запись без номера; номер назначается только при подготовке общего журнала. */
public record MechanicEventDraft(long turn, FigureColor color, Long actorId, Long targetId,
                                 Position from, Position to, String message, MechanicEvent<?> effect) {
    public MechanicEventDraft { Objects.requireNonNull(effect); Objects.requireNonNull(message); }
    public GameEvent numbered(long sequence) {
        return GameEvent.mechanicEffect(sequence, turn, color, actorId, targetId, from, to, message, effect);
    }
}

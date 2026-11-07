package game.mechanics;

import java.util.HashSet;
import java.util.Set;

/** Чистое вычисляемое представление; собственные сроки хранит модуль воздействия. */
public record PieceRestrictions(Set<PieceRestriction> blocked) {
    public static final PieceRestrictions NONE = new PieceRestrictions(Set.of());
    public PieceRestrictions { blocked = Set.copyOf(blocked); }
    public static PieceRestrictions of(PieceRestriction... values) { return new PieceRestrictions(Set.of(values)); }
    public boolean blocks(PieceRestriction value) { return blocked.contains(value); }
    public PieceRestrictions merge(PieceRestrictions other) {
        var all = new HashSet<>(blocked);
        all.addAll(other.blocked);
        return new PieceRestrictions(all);
    }
}

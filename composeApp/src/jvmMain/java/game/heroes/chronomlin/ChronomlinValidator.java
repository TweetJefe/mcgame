package game.heroes.chronomlin;

import game.heroes.HeroType;
import game.mechanics.PieceBodyHistory;
import game.state.GameSnapshot;
import game.state.PieceState;
import java.util.Set;
import java.util.stream.Collectors;

/** Собственные ограничения притяжения и сверка форм через общую историю. */
public final class ChronomlinValidator {
    private ChronomlinValidator() {}

    public static void validate(GameSnapshot snapshot) {
        var identities = game.mechanics.PieceHeroHistory.replay(snapshot);
        Set<Long> ids = snapshot.pieces().values().stream()
                .filter(p -> p.hero() == HeroType.CHRONOMLIN || identities.hasHeld(p.id(), HeroType.GINSENG))
                .map(PieceState::id).collect(Collectors.toUnmodifiableSet());
        var history = PieceBodyHistory.forPieces(snapshot, ids);
        for (long id : ids)
            require(snapshot.pieces().get(id).type() == history.latest(id), "Hero form differs from history");
        for (var event : snapshot.events()) if (event.isAbility(ChronomlinAbility.GRAPPLE)) {
            require(event.targetId() != null && ids.contains(event.actorId()), "Invalid grapple source or anchor");
            int dr = Math.abs(event.from().getRow() - event.to().getRow());
            int dc = Math.abs(event.from().getColumn() - event.to().getColumn());
            require((dr == 0 || dc == 0 || dr == dc) && Math.max(dr, dc) > 1, "Invalid grapple ray");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}

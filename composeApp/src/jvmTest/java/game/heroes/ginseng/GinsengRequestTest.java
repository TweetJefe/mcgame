package game.heroes.ginseng;

import game.core.Game;
import game.heroes.AbilityRequest;
import game.moves.AbilityRejection;
import game.moves.MovementRejection;
import game.moves.Success;
import org.junit.Test;

import java.util.Map;

import static game.core.Positions.pos;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class GinsengRequestTest {
    @Test public void nativeAndCopiedHighlightsContainExactlyExecutableCompleteRequests() {
        checkHighlights(GinsengFixtures.hook(), false);
        checkHighlights(GinsengFixtures.readyDisplacement(), true);
    }

    private void checkHighlights(Game game, boolean borrowed) {
        var before = game.getSnapshot();
        var targets = game.getAbilityTargets(pos("c2"), GinsengAbility.DISPLACE, borrowed);
        assertFalse(targets.isEmpty());
        for (var target : before.field().getGameField().keySet()) {
            var destinations = game.getAbilityDestinations(pos("c2"), GinsengAbility.DISPLACE, target, borrowed);
            assertEquals(targets.contains(target), !destinations.isEmpty());
            for (var destination : destinations) {
                Game attempt = Game.fromSnapshot(before, () -> 0L);
                assertEquals(Success.SUCCESS, attempt.useAbility(attempt.getCurrentPlayer(),
                        new AbilityRequest(pos("c2"), GinsengAbility.DISPLACE, target, destination, borrowed)));
            }
        }
        assertEquals(before, game.getSnapshot());
    }

    @Test public void missingDestinationDoesNotOverrideAnEarlierRangeRejection() {
        Game game = GinsengFixtures.create(GinsengFixtures.board(), Map.of());
        assertEquals(AbilityRejection.ABILITY_OUT_OF_RANGE, game.useAbility(game.getCurrentPlayer(),
                new AbilityRequest(pos("c2"), GinsengAbility.DISPLACE, pos("g8"), null, false)));
        assertEquals(MovementRejection.OUT_OF_BOUNDS, game.useAbility(game.getCurrentPlayer(),
                new AbilityRequest(pos("c2"), GinsengAbility.DISPLACE, pos("b1"), null, false)));
    }
}

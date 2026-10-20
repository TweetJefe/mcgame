package game.core;

import game.heroes.pudge.PudgeAbility;

import game.moves.Success;
import game.heroes.*;
import game.scenarios.PudgeScenario;
import game.state.PieceState;
import model.FigureColor;
import model.Position;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static game.core.Positions.pos;
import static org.junit.Assert.*;

public class HeroDispatchTest {
    @Test public void engineUsesRegisteredTargetingRequestsAndEffectsAndRetainsTheRegistry() {
        var snapshot = PudgeScenario.DIAGONAL_HOOK.createGame(() -> 0L).getSnapshot();
        Hero custom = new AbstractHero(HeroType.PUDGE_ROOK, List.of(PudgeAbility.HOOK), null, 0) {
            @Override public HeroTargeting targeting(Abilities ability) { return HeroTargeting.untargeted(); }
            @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor,
                                                            PieceState target, Position square, Abilities ability) {
                return HeroEffect.success(Map.of(actor.id(), actor.withAbilityState(7, 0, null)));
            }
        };
        HeroRegistry heroes = new HeroRegistry().register(custom);
        var context = new ActionEngine.Context(snapshot.field(), snapshot.pieces(), snapshot.castlingRights(),
                snapshot.enPassantTarget(), snapshot.economy(), snapshot.turn().number(), snapshot.terrain(),
                snapshot.summons(), snapshot.spells(), snapshot.faun(), snapshot.clown(), snapshot.mines(),
                snapshot.lightning(), heroes);
        List<AbilityRequest> requests = ActionEngine.requests(context, FigureColor.WHITE, pos("e2"),
                PudgeAbility.HOOK, false, snapshot.heroRules());
        assertEquals(List.of(new AbilityRequest(pos("e2"), PudgeAbility.HOOK, null)), requests);
        var preview = ActionEngine.ability(context, FigureColor.WHITE, requests.getFirst(), snapshot.heroRules());
        assertEquals(Success.SUCCESS, preview.result());
        assertEquals(7, preview.next().at(pos("e2")).cooldown());
        assertSame(heroes, preview.next().heroes());
        assertEquals(0, context.at(pos("e2")).cooldown());
        assertEquals(pos("h5"), preview.next().at(pos("h5")).position());
    }

    @Test public void deathResolverCallsRegisteredReactionsWithoutKnowingTheHeroType() {
        var snapshot = PudgeScenario.DIAGONAL_HOOK.createGame(() -> 0L).getSnapshot();
        PieceState victim = snapshot.pieceAt(pos("h5")), winner = snapshot.pieceAt(pos("e2"));
        Hero reactive = new AbstractHero(HeroType.NONE, List.of(), null, 0) {
            @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor,
                                                            PieceState target, Position square, Abilities ability) {
                return HeroEffect.empty();
            }
            @Override public HeroEffect onCaptured(HeroActionContext context, PieceState dead,
                                                   long captor, Position square) {
                // A registered passive may propose further state changes without changing the resolver.
                return HeroEffect.success(Map.of(captor, context.pieceById(captor).withBlindness(1)));
            }
        };
        HeroRegistry heroes = new HeroRegistry().register(reactive);
        var prepared = new HashMap<>(snapshot.pieces());
        prepared.put(victim.id(), victim.hidden(PieceState.Status.CAPTURED));
        HeroEffect effect = CaptureReactions.resolve(new HeroActionContext(snapshot.field(), prepared, 1),
                victim, winner.id(), victim.position(), heroes);
        assertEquals(Success.SUCCESS, effect.result());
        assertEquals(1, effect.updates().get(winner.id()).blindTurns());
        assertEquals(0, winner.blindTurns());
        assertTrue(effect.captures().isEmpty());
    }
}

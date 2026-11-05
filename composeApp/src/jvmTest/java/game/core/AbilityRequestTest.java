package game.core;

import game.heroes.Abilities;
import game.heroes.AbstractHero;
import game.heroes.AbilityRequest;
import game.heroes.HeroActionContext;
import game.heroes.HeroDefinition;
import game.heroes.HeroDefinitions;
import game.heroes.HeroEffect;
import game.heroes.HeroRules;
import game.heroes.HeroTargeting;
import game.heroes.HeroType;
import game.heroes.NoHeroRules;
import game.heroes.plain.NoneDefinition;
import game.mechanics.MechanicDefinition;
import game.mechanics.MechanicKey;
import game.mechanics.MechanicPhase;
import game.mechanics.MechanicStep;
import game.moves.AbilityRejection;
import game.moves.Success;
import game.state.CastlingRights;
import game.state.GamePosition;
import game.state.PieceState;
import model.Field;
import model.FigureColor;
import model.Position;
import model.figures.Rook;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static game.core.Positions.pos;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AbilityRequestTest {
    private enum Shift implements Abilities {
        CAST;
        @Override public String id() { return "request.integration.shift"; }
        @Override public String getDisplayName() { return "Shift"; }
        @Override public HeroTargeting targeting() { return HeroTargeting.targetAndDestination(); }
    }

    private static final MechanicKey<Boolean> KEY = new MechanicKey<>("request.guard", Boolean.class, false);
    private static final MechanicDefinition<Boolean> GUARD = new MechanicDefinition<>() {
        @Override public MechanicKey<Boolean> key() { return KEY; }
        @Override public List<MechanicStep> steps() {
            return List.of(new MechanicStep("request.guard", MechanicPhase.ABILITY_GUARD, session -> {
                AbilityRequest request = session.request();
                return pos("c3").equals(request.destination()) && pos("b1").equals(request.target())
                        ? AbilityRejection.ABILITY_BLOCKED : Success.SUCCESS;
            }));
        }
    };
    private static final HeroDefinition<NoHeroRules> DEFINITION = new HeroDefinition<>(HeroType.NONE,
            "Request test", Set.of(Rook.class), NoHeroRules.class, NoHeroRules.INSTANCE, null,
            ShiftHero::new, dependencies());

    private static List<MechanicDefinition<?>> dependencies() {
        var modules = new ArrayList<>(NoneDefinition.INSTANCE.mechanics());
        modules.add(GUARD);
        return List.copyOf(modules);
    }

    private static final class ShiftHero extends AbstractHero {
        ShiftHero(NoHeroRules rules) { super(DEFINITION, List.of(Shift.CAST), null, 0); }
        @Override public List<AbilityRequest> requests(HeroActionContext context, PieceState actor,
                                                     Abilities ability, boolean borrowed) {
            return List.of(new AbilityRequest(actor.position(), ability, pos("b1"), pos("b3"), borrowed),
                    new AbilityRequest(actor.position(), ability, pos("b1"), pos("c3"), borrowed),
                    new AbilityRequest(actor.position(), ability, pos("a2"), pos("b3"), borrowed));
        }
        @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor,
                                                        PieceState target, AbilityRequest request) {
            if (target == null || !pos("b1").equals(target.position()))
                return HeroEffect.rejected(AbilityRejection.INVALID_ABILITY_TARGET);
            return HeroEffect.success(Map.of(target.id(), target.placed(request.destination(), target.type())));
        }
    }

    private static Game game() {
        var definitions = new ArrayList<>(HeroDefinitions.all());
        definitions.removeIf(definition -> definition.type() == HeroType.NONE);
        definitions.add(DEFINITION);
        return Game.fromMobaPosition(new GamePosition(new Field(8).getGameField(), FigureColor.WHITE,
                CastlingRights.initial(), null, 0), Map.of(), HeroRules.defaults(definitions), () -> 0L);
    }

    @Test public void bothHighlightStagesAndExecutionUseFullRequestsForAnIndependentAbility() {
        Game game = game();
        var before = game.getSnapshot();
        assertEquals(List.of(pos("b1")), game.getAbilityTargets(pos("a1"), Shift.CAST));
        assertEquals(List.of(pos("b3")), game.getAbilityDestinations(pos("a1"), Shift.CAST, pos("b1")));
        assertTrue(game.getAbilityDestinations(pos("a1"), Shift.CAST, pos("a2")).isEmpty());
        assertEquals(before, game.getSnapshot());
        assertEquals(AbilityRejection.ABILITY_BLOCKED, game.useAbility(game.getCurrentPlayer(),
                new AbilityRequest(pos("a1"), Shift.CAST, pos("b1"), pos("c3"), false)));
        assertEquals(before, game.getSnapshot());
        long target = game.getPieceState(pos("b1")).id();
        assertEquals(Success.SUCCESS, game.useAbility(game.getCurrentPlayer(),
                new AbilityRequest(pos("a1"), Shift.CAST, pos("b1"), pos("b3"), false)));
        assertEquals(target, game.getPieceState(pos("b3")).id());
        assertNull(game.getPieceState(pos("b1")));
        assertTrue(game.getTurnState().abilityUsed());
    }
}

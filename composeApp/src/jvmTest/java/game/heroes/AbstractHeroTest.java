package game.heroes;

import game.heroes.pudge.PudgeAbility;
import game.heroes.lumeshard.LumeshardAbility;

import game.moves.AbilityRejection;
import game.moves.MovementRejection;
import game.moves.Success;
import game.heroes.pudge.PudgeRules;
import game.moves.MoveResult;
import game.state.PieceState;
import model.Field;
import model.Position;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class AbstractHeroTest {
    private final Field field = new Field(8);
    private final PudgeRules rules = new PudgeRules(2, 3, true, true);
    private final Hero hero = new PudgeBehavior(rules);
    private final PieceState actor = PieceState.initial(1, field.getFigureByPosition(new Position(0, 0)), HeroType.PUDGE);
    private final PieceState target = PieceState.initial(2, field.getFigureByPosition(new Position(1, 0)), HeroType.NONE);

    @Test public void availabilityRejectsMissingActorsWrongHeroesAndUnsupportedAbilities() {
        assertEquals(AbilityRejection.INVALID_ABILITY, hero.availability(null, null));
        assertEquals(MovementRejection.EMPTY_SOURCE, hero.availability(null, PudgeAbility.HOOK));
        assertEquals(MovementRejection.EMPTY_SOURCE, hero.availability(actor.hidden(PieceState.Status.CAPTURED), PudgeAbility.HOOK));
        assertEquals(MovementRejection.EMPTY_SOURCE, hero.availability(actor.hidden(PieceState.Status.SWALLOWED), PudgeAbility.HOOK));
        assertEquals(AbilityRejection.NO_ABILITY, hero.availability(target, PudgeAbility.HOOK));
        assertEquals(AbilityRejection.NO_ABILITY, hero.availability(actor, LumeshardAbility.LASER));
    }

    @Test public void normalCooldownAndUltimateChargeAreIndependent() {
        assertEquals(Success.SUCCESS, hero.availability(actor, PudgeAbility.HOOK));
        assertEquals(AbilityRejection.ULTIMATE_NOT_READY, hero.availability(actor, PudgeAbility.SWALLOW));
        PieceState ready = actor.withAbilityState(5, 3, null);
        assertEquals(AbilityRejection.ABILITY_COOLDOWN, hero.availability(ready, PudgeAbility.HOOK));
        assertEquals(Success.SUCCESS, hero.availability(ready, PudgeAbility.SWALLOW));
        assertEquals(Success.SUCCESS, hero.availability(actor.withAbilityState(0, 9, null), PudgeAbility.SWALLOW));
    }

    @Test public void heroSpecificRequirementsRunBeforeUltimateChargeValidation() {
        PieceState loaded = actor.withAbilityState(0, 0, target.id());
        assertEquals(AbilityRejection.ALREADY_SWALLOWED, hero.availability(loaded, PudgeAbility.SWALLOW));
        var context = new HeroActionContext(field, Map.of(actor.id(), loaded), 0);
        assertEquals(HeroEffect.rejected(AbilityRejection.ALREADY_SWALLOWED),
                hero.preview(context, new AbilityRequest(loaded.position(), PudgeAbility.SWALLOW, target.position())));
    }

    @Test public void rejectedPreviewsHaveNoUpdatesOrCaptures() {
        var context = new HeroActionContext(field, Map.of(actor.id(), actor), 0);
        assertEquals(HeroEffect.rejected(AbilityRejection.ULTIMATE_NOT_READY),
                hero.preview(context, new AbilityRequest(actor.position(), PudgeAbility.SWALLOW, null)));
        assertEquals(HeroEffect.rejected(AbilityRejection.NO_ABILITY),
                hero.preview(context, new AbilityRequest(actor.position(), LumeshardAbility.LASER, target.position())));
        assertThrows(NullPointerException.class, () -> hero.preview(null, new AbilityRequest(actor.position(), PudgeAbility.HOOK, target.position())));
    }

    @Test public void oneStrategyCanPreviewDifferentPiecesWithoutMutatingSnapshots() {
        PieceState charged = actor.withAbilityState(0, 3, null);
        var context = new HeroActionContext(field, Map.of(charged.id(), charged, target.id(), target), 7);
        HeroEffect effect = hero.preview(context, new AbilityRequest(charged.position(), PudgeAbility.SWALLOW, target.position()));

        assertEquals(Success.SUCCESS, effect.result());
        assertEquals(PieceState.Status.SWALLOWED, effect.updates().get(target.id()).status());
        assertEquals(0, effect.updates().get(charged.id()).ultimateCharge());
        assertEquals(charged, context.pieceById(charged.id()));
        assertEquals(target, context.pieceAt(target.position()));

        PieceState another = PieceState.initial(3, field.getFigureByPosition(new Position(0, 7)), HeroType.PUDGE);
        assertEquals(AbilityRejection.ULTIMATE_NOT_READY, hero.availability(another, PudgeAbility.SWALLOW));
        assertEquals(Success.SUCCESS, hero.availability(charged, PudgeAbility.SWALLOW));
        assertEquals(effect, hero.preview(context, new AbilityRequest(charged.position(), PudgeAbility.SWALLOW, target.position())));
    }

    @Test public void captureChargeIsCappedAndPreservesAllOtherPieceState() {
        PieceState loaded = actor.withAbilityState(5, 1, target.id()).withBlindness(2).withStun(42);
        PieceState next = hero.afterCapture(loaded);
        assertEquals(loaded.withAbilityState(5, 2, target.id()), next);
        assertEquals(1, loaded.ultimateCharge());
        PieceState full = loaded.withAbilityState(5, 3, target.id());
        assertSame(full, hero.afterCapture(full));
        assertSame(target, hero.afterCapture(target));
        assertEquals(3, hero.afterCapture(loaded.withAbilityState(5, Integer.MAX_VALUE, target.id())).ultimateCharge());
    }

    @Test public void zeroThresholdMakesUltimateFreeAndDisablesCaptureCharging() {
        Hero free = new PudgeBehavior(new PudgeRules(0, 0, true, true));
        assertEquals(Success.SUCCESS, free.availability(actor, PudgeAbility.SWALLOW));
        assertSame(actor, free.afterCapture(actor));
    }

    @Test public void heroesWithoutActiveUltimatesCanStillChargePassives() {
        Hero passive = new AbstractHero(HeroType.PUDGE, List.of(PudgeAbility.HOOK), null, 2) {
            @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor,
                                                             PieceState target, AbilityRequest request) {
                throw new AssertionError("Unavailable abilities must not reach the effect hook");
            }
        };
        PieceState cooling = actor.withAbilityState(1, 0, null);
        assertEquals(1, passive.afterCapture(cooling).ultimateCharge());
        var context = new HeroActionContext(field, Map.of(cooling.id(), cooling), 0);
        assertEquals(HeroEffect.rejected(AbilityRejection.ABILITY_COOLDOWN),
                passive.preview(context, new AbilityRequest(cooling.position(), PudgeAbility.HOOK, target.position())));
    }

    @Test public void configurationIsCopiedAndValidated() {
        var abilities = new ArrayList<>(List.of(PudgeAbility.HOOK, PudgeAbility.SWALLOW));
        Hero copied = new PudgeBehavior(rules, abilities);
        abilities.clear();
        assertEquals(HeroType.PUDGE, copied.type());
        assertEquals(List.of(PudgeAbility.HOOK, PudgeAbility.SWALLOW), copied.abilities());
        assertThrows(UnsupportedOperationException.class, () -> copied.abilities().clear());
        assertThrows(IllegalArgumentException.class, () -> new PudgeBehavior(rules, List.of(PudgeAbility.HOOK)));
    }

    /** Контролируемая стратегия для проверки общего шаблона и стоимости эффекта. */
    private static final class PudgeBehavior extends AbstractHero {
        private PudgeBehavior(PudgeRules rules) {
            this(rules, PudgeAbility.getAbilities());
        }

        private PudgeBehavior(PudgeRules rules, List<? extends Abilities> abilities) {
            super(HeroType.PUDGE, abilities, PudgeAbility.SWALLOW, rules.ultimateCaptures());
        }

        @Override protected MoveResult availabilitySpecific(PieceState actor, Abilities ability) {
            return ability == PudgeAbility.SWALLOW && actor.storedPieceId() != null
                    ? AbilityRejection.ALREADY_SWALLOWED : Success.SUCCESS;
        }

        @Override protected HeroEffect previewAvailable(HeroActionContext context, PieceState actor,
                                                         PieceState target, AbilityRequest request) {
            return HeroEffect.success(Map.of(actor.id(), actor.withAbilityState(actor.cooldown(),
                    actor.ultimateCharge(), target.id()), target.id(), target.hidden(PieceState.Status.SWALLOWED)))
                    .withCost(AbilityCost.ultimate(ultimateCaptures()));
        }
    }
}

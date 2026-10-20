package game.heroes.pudge;

import game.heroes.lumeshard.LumeshardAbility;

import game.moves.AbilityRejection;
import game.moves.StatusRejection;
import game.moves.Success;
import game.heroes.Abilities;
import game.heroes.HeroActionContext;
import game.heroes.HeroEffect;
import game.heroes.HeroType;
import game.state.PieceState;
import model.Field;
import model.Figure;
import model.FigureColor;
import model.Position;
import model.figures.Empty;
import model.figures.King;
import model.figures.Pawn;
import model.figures.Rook;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

public class PudgeHeroTest {
    private static final PudgeRules RULES = new PudgeRules(4, 3, true, true);
    private static final Position ORIGIN = new Position(3, 3);

    @Test public void configurationRequiresRulesAndExposesOnlyPudgeAbilities() {
        assertThrows(NullPointerException.class, () -> new PudgeHero(null));
        var hero = new PudgeHero(RULES);
        assertEquals(HeroType.PUDGE_ROOK, hero.type());
        assertEquals(List.of(PudgeAbility.HOOK, PudgeAbility.SWALLOW), hero.abilities());
    }

    @Test public void hookPullsEitherColorFromAllEightDirections() {
        for (FigureColor color : List.of(FigureColor.WHITE, FigureColor.BLACK)) {
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) continue;
                    for (int distance : new int[]{1, 2, 3}) {
                        Position square = new Position(3 + dr * distance, 3 + dc * distance);
                        if (square.equals(new Position(0, 0))) continue; // White king.
                        Fixture f = fixture(square, color, null).withState(0, 2, null);
                        HeroEffect effect = preview(RULES, f, f.target(), PudgeAbility.HOOK);
                        assertEquals(Success.SUCCESS, effect.result());
                        assertEquals(new Position(3 + dr, 3 + dc), effect.updates().get(2L).position());
                        assertEquals(f.actor().withAbilityState(4, 2, null), effect.updates().get(1L));
                        assertTrue(effect.captures().isEmpty());
                    }
                }
            }
        }
    }

    @Test public void hookRejectsBlockedPathsBadDirectionsAndTargetsOutsideItsRange() {
        assertEquals(AbilityRejection.ABILITY_BLOCKED,
                preview(RULES, fixture(new Position(3, 6), FigureColor.BLACK, new Position(3, 4)), PudgeAbility.HOOK).result());
        assertEquals(AbilityRejection.ABILITY_OUT_OF_RANGE,
                preview(RULES, fixture(new Position(3, 7), FigureColor.BLACK, null), PudgeAbility.HOOK).result());
        assertEquals(AbilityRejection.ABILITY_OUT_OF_RANGE,
                preview(RULES, fixture(new Position(5, 4), FigureColor.BLACK, null), PudgeAbility.HOOK).result());
        assertEquals(AbilityRejection.ABILITY_OUT_OF_RANGE,
                preview(new PudgeRules(4, 3, false, true),
                        fixture(new Position(5, 5), FigureColor.BLACK, null), PudgeAbility.HOOK).result());
    }

    @Test public void swallowSpendsConfiguredChargeAndPreservesCooldownAndOtherState() {
        Fixture f = fixture(new Position(4, 4), FigureColor.WHITE, null).withState(7, 5, null);
        HeroEffect effect = preview(RULES, f, f.target(), PudgeAbility.SWALLOW);
        assertEquals(Success.SUCCESS, effect.result());
        assertEquals(f.actor().withAbilityState(7, 2, f.target().id()), effect.updates().get(1L));
        assertEquals(f.target().hidden(PieceState.Status.SWALLOWED), effect.updates().get(2L));
        assertEquals(5, f.context().pieceById(1).ultimateCharge());
        assertEquals(f.target(), f.context().pieceAt(f.target().position()));
        assertTrue(effect.captures().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> effect.updates().clear());
        assertEquals(effect, preview(RULES, f, f.target(), PudgeAbility.SWALLOW));
    }

    @Test public void swallowRequiresAnAdjacentAllyAndRespectsDiagonalConfiguration() {
        Fixture enemy = fixture(new Position(3, 4), FigureColor.BLACK, null).withState(0, 3, null);
        assertEquals(AbilityRejection.INVALID_ABILITY_TARGET, preview(RULES, enemy, PudgeAbility.SWALLOW).result());
        Fixture far = fixture(new Position(3, 5), FigureColor.WHITE, null).withState(0, 3, null);
        assertEquals(AbilityRejection.ABILITY_OUT_OF_RANGE, preview(RULES, far, PudgeAbility.SWALLOW).result());
        Fixture diagonal = fixture(new Position(4, 4), FigureColor.WHITE, null).withState(0, 3, null);
        PudgeRules orthogonal = new PudgeRules(4, 3, true, false);
        assertEquals(AbilityRejection.ABILITY_OUT_OF_RANGE, preview(orthogonal, diagonal, PudgeAbility.SWALLOW).result());
        Fixture adjacent = fixture(new Position(3, 4), FigureColor.WHITE, null).withState(0, 3, null);
        assertEquals(Success.SUCCESS, preview(orthogonal, adjacent, PudgeAbility.SWALLOW).result());
    }

    @Test public void bothAbilitiesRejectMissingHiddenSelfAndKingTargets() {
        Fixture f = fixture(new Position(3, 4), FigureColor.WHITE, null).withState(0, 3, null);
        for (Abilities ability : List.of(PudgeAbility.HOOK, PudgeAbility.SWALLOW)) {
            assertEquals(AbilityRejection.INVALID_ABILITY_TARGET, preview(RULES, f, null, ability).result());
            assertEquals(AbilityRejection.INVALID_ABILITY_TARGET, preview(RULES, f, f.actor(), ability).result());
            assertEquals(AbilityRejection.INVALID_ABILITY_TARGET,
                    preview(RULES, f, f.target().hidden(PieceState.Status.SWALLOWED), ability).result());
            for (Position kingSquare : List.of(new Position(0, 0), new Position(7, 7)))
                assertEquals(StatusRejection.KING_IMMUNE,
                        preview(RULES, f, f.context().pieceAt(kingSquare), ability).result());
        }
    }

    @Test public void availabilityAndCaptureChargingMatchLegacyRules() {
        Fixture f = fixture(new Position(3, 4), FigureColor.WHITE, null);
        for (PudgeRules rules : List.of(RULES, PudgeRules.prototype(), PudgeRules.sandbox())) {
            var hero = new PudgeHero(rules);
            PieceState[] actors = {null, f.actor(), f.actor().hidden(PieceState.Status.CAPTURED), f.target(),
                    f.actor().withAbilityState(1, 0, null), f.actor().withAbilityState(1, 5, null),
                    f.actor().withAbilityState(0, 0, 2L), f.actor().withAbilityState(0, 5, 2L)};
            for (PieceState actor : actors) {
                for (Abilities ability : new Abilities[]{null, PudgeAbility.HOOK, PudgeAbility.SWALLOW, LumeshardAbility.LASER})
                    assertEquals(PudgeAbilities.availability(actor, ability, rules), hero.availability(actor, ability));
                if (actor != null)
                    assertEquals(PudgeAbilities.afterCapture(actor, rules), hero.afterCapture(actor));
            }
        }
    }

    @Test public void rejectedAvailabilityNeverProducesUpdatesAndSandboxUltimatesAreFree() {
        Fixture f = fixture(new Position(3, 4), FigureColor.WHITE, null);
        assertEquals(HeroEffect.rejected(AbilityRejection.ULTIMATE_NOT_READY), preview(RULES, f, PudgeAbility.SWALLOW));
        assertEquals(HeroEffect.rejected(AbilityRejection.ABILITY_COOLDOWN),
                preview(RULES, f.withState(1, 3, null), PudgeAbility.HOOK));
        assertEquals(HeroEffect.rejected(AbilityRejection.ALREADY_SWALLOWED),
                preview(RULES, f.withState(0, 0, 2L), PudgeAbility.SWALLOW));
        HeroEffect free = preview(PudgeRules.sandbox(), f, PudgeAbility.SWALLOW);
        assertEquals(Success.SUCCESS, free.result());
        assertEquals(0, free.updates().get(1L).ultimateCharge());
    }

    private static HeroEffect preview(PudgeRules rules, Fixture f, Abilities ability) {
        return preview(rules, f, f.target(), ability);
    }

    private static HeroEffect preview(PudgeRules rules, Fixture f, PieceState target, Abilities ability) {
        HeroEffect effect = new PudgeHero(rules).preview(f.context(), f.actor(), target,
                target == null ? null : target.position(), ability);
        assertEquals(PudgeAbilities.preview(f.context().field(), f.actor(), target, ability, rules), effect);
        return effect;
    }

    private static Fixture fixture(Position targetSquare, FigureColor targetColor, Position blocker) {
        Field factory = new Field(8);
        var board = new HashMap<Position, Figure>();
        for (Position square : factory.getGameField().keySet())
            board.put(square, factory.createFigure(Empty.class, square, FigureColor.NONE));
        Position whiteKing = new Position(0, 0), blackKing = new Position(7, 7);
        board.put(whiteKing, factory.createFigure(King.class, whiteKing, FigureColor.WHITE));
        board.put(blackKing, factory.createFigure(King.class, blackKing, FigureColor.BLACK));
        board.put(ORIGIN, factory.createFigure(Rook.class, ORIGIN, FigureColor.WHITE));
        board.put(targetSquare, factory.createFigure(Pawn.class, targetSquare, targetColor));
        if (blocker != null) board.put(blocker, factory.createFigure(Pawn.class, blocker, FigureColor.WHITE));
        Field field = Field.fromMobaPosition(board);
        PieceState actor = PieceState.initial(1, field.getFigureByPosition(ORIGIN), HeroType.PUDGE_ROOK)
                .withBlindness(2).withStun(8);
        PieceState target = PieceState.initial(2, field.getFigureByPosition(targetSquare), HeroType.NONE);
        var pieces = new HashMap<Long, PieceState>();
        pieces.put(1L, actor);
        pieces.put(2L, target);
        pieces.put(3L, PieceState.initial(3, field.getFigureByPosition(whiteKing), HeroType.NONE));
        pieces.put(4L, PieceState.initial(4, field.getFigureByPosition(blackKing), HeroType.NONE));
        if (blocker != null) pieces.put(5L, PieceState.initial(5, field.getFigureByPosition(blocker), HeroType.NONE));
        return new Fixture(new HeroActionContext(field, pieces, 10), actor, target);
    }

    private record Fixture(HeroActionContext context, PieceState actor, PieceState target) {
        Fixture withState(int cooldown, int charge, Long stored) {
            PieceState next = actor.withAbilityState(cooldown, charge, stored);
            var pieces = new HashMap<>(context.pieces());
            pieces.put(next.id(), next);
            return new Fixture(new HeroActionContext(context.field(), pieces, context.currentTurn()), next, target);
        }
    }
}

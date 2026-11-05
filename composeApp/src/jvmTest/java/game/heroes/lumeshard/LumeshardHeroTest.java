package game.heroes.lumeshard;

import game.heroes.AbilityRequest;
import game.heroes.HeroActionContext;
import game.heroes.HeroType;
import game.moves.AbilityRejection;
import game.moves.StatusRejection;
import game.moves.Success;
import game.state.ItemSlot;
import game.state.PieceState;
import game.state.SquareColor;
import java.util.List;
import model.Field;
import model.Figure;
import model.FigureColor;
import model.figures.Bishop;
import model.figures.Knight;
import model.figures.Rook;
import org.junit.Test;

import static game.core.Positions.pos;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LumeshardHeroTest {
    @Test public void sharedStrategyUsesTheActualBorrowedCasterAndDoesNotSpendItsResources() {
        var hero = new LumeshardHero(new LumeshardRules(4, 2, 3));
        var caster = state(1, Knight.class, FigureColor.WHITE, "c2", HeroType.GINSENG, 8, 1, 99L).withBlindness(1);
        var enemy = state(2, Rook.class, FigureColor.BLACK, "e4", HeroType.NONE, 0, 0, null);
        var before = context(caster, enemy);
        var request = new AbilityRequest(caster.position(), LumeshardAbility.LASER, enemy.position(), null, true);
        var first = hero.preview(before, request);
        assertEquals(Success.SUCCESS, first.result());
        assertEquals(caster, first.updates().get(caster.id()));
        assertEquals(2, first.updates().get(enemy.id()).blindTurns());
        assertEquals(first, hero.preview(before, request));
        assertEquals(caster, before.pieceById(caster.id()));
        var nativeCaster = state(3, Bishop.class, FigureColor.WHITE, "c2", HeroType.LUMESHARD, 0, 3, null);
        var nativeEffect = hero.preview(context(nativeCaster, enemy),
                new AbilityRequest(nativeCaster.position(), LumeshardAbility.LASER, enemy.position()));
        assertEquals(nativeCaster.withAbilityState(4, 3, null), nativeEffect.updates().get(3L));
    }

    @Test public void teleportPaysOnlyAfterTheColorCheckAndPreservesTheCurrentBody() {
        var hero = new LumeshardHero(new LumeshardRules(4, 2, 3));
        var caster = state(1, Rook.class, FigureColor.WHITE, "c2", HeroType.GINSENG, 8, 1, 99L);
        var before = context(caster);
        var wrong = hero.preview(before, new AbilityRequest(caster.position(), LumeshardAbility.TELEPORT, pos("f4"), null, true));
        assertEquals(AbilityRejection.WRONG_SQUARE_COLOR, wrong.result());
        assertTrue(wrong.updates().isEmpty());
        var effect = hero.preview(before, new AbilityRequest(caster.position(), LumeshardAbility.TELEPORT, pos("f5"), null, true));
        assertEquals(caster.placed(pos("f5"), Rook.class), effect.updates().get(1L));
        assertEquals(caster, before.pieceById(1));
    }

    @Test public void rawFacadeRetainsItsHistoricalReadinessWithoutBypassingTheCanonicalGuard() {
        var rules = new LumeshardRules(4, 2, 3);
        var caster = state(1, Bishop.class, FigureColor.WHITE, "c2", HeroType.LUMESHARD, 0, 3, null).asPot();
        assertEquals(Success.SUCCESS, LumeshardAbilities.availability(caster, LumeshardAbility.LASER, rules));
        assertEquals(StatusRejection.POTTED, new LumeshardHero(rules).availability(caster, LumeshardAbility.LASER));
    }

    private static PieceState state(long id, Class<? extends Figure> body, FigureColor side,
                                    String square, HeroType hero, int cooldown, int charge, Long stored) {
        SquareColor starting = hero == HeroType.GINSENG || hero == HeroType.LUMESHARD
                ? SquareColor.at(pos(square)) : null;
        return new PieceState(id, body, side, pos(square), PieceState.Status.BOARD, hero, cooldown, charge,
                List.of(ItemSlot.empty(), ItemSlot.empty(), ItemSlot.empty()), stored, 0, starting);
    }

    private static HeroActionContext context(PieceState... pieces) {
        var map = new java.util.HashMap<Long, PieceState>();
        for (PieceState piece : pieces) map.put(piece.id(), piece);
        return new HeroActionContext(new Field(8), map, 5);
    }
}

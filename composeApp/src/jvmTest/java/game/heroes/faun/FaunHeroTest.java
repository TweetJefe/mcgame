package game.heroes.faun;

import game.heroes.AbilityRequest;
import game.heroes.HeroActionContext;
import game.heroes.HeroType;
import game.moves.AbilityRejection;
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
import org.junit.Test;

import static game.core.Positions.pos;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FaunHeroTest {
    @Test public void copiedCloudKeepsTheRealCasterAndNativeCloudPaysTheConfiguredCost() {
        var hero = new FaunHero(new FaunRules(4, 2, 2, 3));
        var caster = state(1, Bishop.class, FigureColor.WHITE, "c2", HeroType.GINSENG, 8, 1, 99L);
        var before = context(caster);
        var request = new AbilityRequest(caster.position(), FaunAbility.CLOUD, pos("e4"), null, true);
        var effect = hero.preview(before, request);
        assertEquals(Success.SUCCESS, effect.result());
        assertEquals(caster, effect.updates().get(1L));
        assertEquals(effect, hero.preview(before, request));
        assertEquals(caster, before.pieceById(1));
        var nativeCaster = state(2, Knight.class, FigureColor.WHITE, "c2", HeroType.FAUN, 0, 3, null);
        var nativeEffect = hero.preview(context(nativeCaster), new AbilityRequest(nativeCaster.position(), FaunAbility.CLOUD, pos("e4")));
        assertEquals(nativeCaster.withAbilityState(4, 3, null), nativeEffect.updates().get(2L));
    }

    @Test public void cloudRejectionAndCaptureChargingPreserveOtherStatuses() {
        var hero = new FaunHero(new FaunRules(4, 2, 2, 3));
        var caster = state(1, Knight.class, FigureColor.WHITE, "c2", HeroType.FAUN, 0, 1, null).withBlindness(1).withStun(8).withBox(7);
        var effect = hero.preview(context(caster), new AbilityRequest(caster.position(), FaunAbility.CLOUD, pos("h8")));
        assertEquals(AbilityRejection.ABILITY_OUT_OF_RANGE, effect.result());
        assertTrue(effect.updates().isEmpty());
        assertEquals(caster.withAbilityState(0, 2, null), hero.afterCapture(caster));
        assertEquals(caster.asPot(), hero.afterCapture(caster.asPot()));
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

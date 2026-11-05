package game.heroes.bolotnik;

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
import model.figures.Knight;
import model.figures.Rook;
import org.junit.Test;

import static game.core.Positions.pos;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BolotnikHeroTest {
    @Test public void borrowedAmbushRetainsTheCasterResourcesAndPassesItsBoardOriginToTerrain() {
        var hero = new BolotnikHero(new BolotnikRules(4, 2, 3));
        var caster = state(1, Rook.class, FigureColor.WHITE, "c2", HeroType.GINSENG, 8, 1, 99L).withBlindness(1);
        var before = context(caster);
        var request = new AbilityRequest(caster.position(), BolotnikAbility.AMBUSH, null, null, true);
        var effect = hero.preview(before, request);
        assertEquals(Success.SUCCESS, effect.result());
        assertEquals(caster.hidden(PieceState.Status.HIDDEN_BOG), effect.updates().get(1L));
        assertEquals(List.of(new game.terrain.TerrainMechanic.Place(caster, null, BolotnikAbility.AMBUSH)), effect.operations());
        assertEquals(effect, hero.preview(before, request));
        assertEquals(caster, before.pieceById(1));
        var nativeCaster = state(2, Knight.class, FigureColor.WHITE, "c2", HeroType.BOLOTNIK, 4, 3, null);
        var nativeEffect = hero.preview(context(nativeCaster), new AbilityRequest(nativeCaster.position(), BolotnikAbility.AMBUSH, null));
        assertEquals(0, nativeEffect.updates().get(2L).ultimateCharge());
        assertEquals(4, nativeEffect.updates().get(2L).cooldown());
    }

    @Test public void invalidMudHasNoCostOrPlacement() {
        var rules = new BolotnikRules(4, 2, 3);
        var hero = new BolotnikHero(rules);
        var caster = state(1, Knight.class, FigureColor.WHITE, "c2", HeroType.BOLOTNIK, 0, 3, null);
        var enemy = state(2, Rook.class, FigureColor.BLACK, "e4", HeroType.NONE, 0, 0, null);
        var rejected = hero.preview(context(caster, enemy), new AbilityRequest(caster.position(), BolotnikAbility.MUD, enemy.position()));
        assertEquals(AbilityRejection.INVALID_ABILITY_TARGET, rejected.result());
        assertTrue(rejected.updates().isEmpty());
        assertTrue(rejected.operations().isEmpty());
        var effect = hero.preview(context(caster), new AbilityRequest(caster.position(), BolotnikAbility.MUD, pos("e4")));

        assertEquals(1, effect.operations().size());
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

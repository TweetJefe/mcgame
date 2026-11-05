package game.heroes.bountycrow;

import game.heroes.AbilityRequest;
import game.heroes.HeroActionContext;
import game.heroes.HeroType;
import game.state.ItemSlot;
import game.state.PieceState;
import game.state.SquareColor;
import java.util.List;
import model.Field;
import model.Figure;
import model.FigureColor;
import model.figures.Knight;
import model.figures.Pawn;
import model.figures.Rook;
import org.junit.Test;

import static game.core.Positions.pos;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BountyCrowHeroTest {
    @Test public void nativeAndBorrowedBountyProposeOneMarkAndApplyOnlyNativeCost() {
        var hero = new BountyCrowHero(new BountyCrowRules(4, 2, 3));
        var caster = state(1, Pawn.class, FigureColor.WHITE, "c2", HeroType.BOUNTY_CROW, 0, 3, null);
        var enemy = state(2, Rook.class, FigureColor.BLACK, "e4", HeroType.NONE, 0, 0, null);
        var effect = hero.preview(context(caster, enemy), new AbilityRequest(caster.position(), BountyCrowAbility.BOUNTY, enemy.position()));
        assertEquals(caster.withAbilityState(4, 3, null), effect.updates().get(1L));
        assertEquals(List.of(new game.economy.EconomyMechanic.Mark(caster, enemy)), effect.operations());
        var borrowed = state(3, Knight.class, FigureColor.WHITE, "c2", HeroType.GINSENG, 8, 1, 99L);
        var before = context(borrowed, enemy);
        var request = new AbilityRequest(borrowed.position(), BountyCrowAbility.BOUNTY, enemy.position(), null, true);
        var copy = hero.preview(before, request);
        assertEquals(borrowed, copy.updates().get(3L));
        assertEquals(List.of(new game.economy.EconomyMechanic.Mark(borrowed, enemy)), copy.operations());
        assertEquals(copy, hero.preview(before, request));
        assertEquals(borrowed, before.pieceById(3));
    }

    @Test public void stealPreservesCooldownAndProposesAnEconomyOperation() {
        var rules = new BountyCrowRules(4, 2, 3);
        var hero = new BountyCrowHero(rules);
        var caster = state(1, Pawn.class, FigureColor.WHITE, "c2", HeroType.BOUNTY_CROW, 4, 3, null);
        var effect = hero.preview(context(caster), new AbilityRequest(caster.position(), BountyCrowAbility.STEAL, null));
        assertEquals(caster.withAbilityState(4, 0, null), effect.updates().get(1L));
        assertEquals(List.of(new game.economy.EconomyMechanic.Steal(caster.id())), effect.operations());

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

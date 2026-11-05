package game.heroes.izba;

import game.heroes.AbilityRequest;
import game.heroes.HeroActionContext;
import game.heroes.HeroType;
import game.moves.Success;
import game.state.PieceState;
import java.util.HashMap;
import java.util.List;
import model.Field;
import model.Figure;
import model.FigureColor;
import model.Position;
import model.figures.Bishop;
import model.figures.Empty;
import model.figures.King;
import model.figures.Pawn;
import model.figures.Rook;
import org.junit.Test;

import static game.core.Positions.pos;
import static org.junit.Assert.assertEquals;

public class IzbaHeroTest {

    @Test public void nativeSummonSpendsChargeOnceAndLeavesTheSourceUnchanged() {
        var hero = new IzbaHero(new IzbaRules(3, 5));
        var context = context(HeroType.IZBA, Rook.class, FigureColor.BLACK, 0, 3);
        var actor = context.pieceById(1);
        var request = new AbilityRequest(actor.position(), IzbaAbility.SUMMON_YAGA, null);
        var effect = hero.preview(context, request);
        assertEquals(Success.SUCCESS, effect.result());
        assertEquals(effect, hero.preview(context, request));
        assertEquals(0, effect.updates().get(1L).ultimateCharge());
        assertEquals(1, effect.updates().get(1L).blindTurns());
        assertEquals(List.of(new game.summoning.SummonMechanic.Cast(actor)), effect.operations());
        assertEquals(actor, context.pieceById(1));
    }

    @Test public void borrowedSummonLeavesNativeResourcesAndStoredAllyUntouched() {
        var hero = new IzbaHero(new IzbaRules(3, 5));
        var base = context(HeroType.GINSENG, Bishop.class, FigureColor.BLACK, 2, 1);
        var actor = base.pieceById(1).withAbilityState(2, 1, 2L);
        var pieces = new HashMap<>(base.pieces());
        pieces.put(1L, actor);
        var context = new HeroActionContext(base.field(), pieces, base.currentTurn(), base.mechanics());
        var effect = hero.preview(context,
                new AbilityRequest(actor.position(), IzbaAbility.SUMMON_YAGA, null, null, true));
        assertEquals(Success.SUCCESS, effect.result());
        assertEquals(actor, effect.updates().get(1L));
        assertEquals(List.of(new game.summoning.SummonMechanic.Cast(actor)), effect.operations());
        assertEquals(actor, context.pieceById(1));
    }

    private HeroActionContext context(HeroType hero, Class<? extends Figure> body, FigureColor targetSide,
                                      int cooldown, int charge) {
        Field field = new Field(8);
        var board = new HashMap<Position, Figure>();
        for (int row = 0; row < 8; row++) for (int column = 0; column < 8; column++) {
            Position square = new Position(row, column);
            board.put(square, field.createFigure(Empty.class, square, FigureColor.NONE));
        }
        var pieces = new HashMap<Long, PieceState>();
        Figure actor = field.createFigure(body, pos("d4"), FigureColor.WHITE);
        Figure target = field.createFigure(Pawn.class, pos("f5"), targetSide);
        Figure whiteKing = field.createFigure(King.class, pos("a1"), FigureColor.WHITE);
        Figure blackKing = field.createFigure(King.class, pos("h8"), FigureColor.BLACK);
        long id = 1;
        for (Figure figure : List.of(actor, target, whiteKing, blackKing)) {
            board.put(figure.getPosition(), figure);
            PieceState piece = PieceState.initial(id, figure, id == 1 ? hero : HeroType.NONE);
            if (id == 1) piece = piece.withAbilityState(cooldown, charge, null).withBlindness(1);
            pieces.put(id++, piece);
        }
        return new HeroActionContext(Field.fromMobaPosition(board), pieces, 5);
    }
}

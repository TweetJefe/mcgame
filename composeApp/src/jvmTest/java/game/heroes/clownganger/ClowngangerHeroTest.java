package game.heroes.clownganger;

import game.heroes.AbilityRequest;
import game.heroes.HeroActionContext;
import game.heroes.HeroType;
import game.moves.Success;
import game.moves.AbilityRejection;
import game.state.PieceState;
import java.util.HashMap;
import java.util.List;
import model.Field;
import model.Figure;
import model.FigureColor;
import model.Position;
import model.figures.Empty;
import model.figures.King;
import model.figures.Knight;
import model.figures.Pawn;
import model.figures.Rook;
import org.junit.Test;

import static game.core.Positions.pos;
import static org.junit.Assert.assertEquals;

public class ClowngangerHeroTest {

    @Test public void nativeSelfBoxCombinesItsDurationWithOneCooldownPayment() {
        var hero = new ClowngangerHero(new ClowngangerRules(4, 2, 2, 3));
        var context = context(HeroType.CLOWNGANGER, Knight.class, FigureColor.WHITE, 0, 2);
        var actor = context.pieceById(1);
        var request = new AbilityRequest(actor.position(), ClowngangerAbility.BOX, actor.position());
        var effect = hero.preview(context, request);
        assertEquals(Success.SUCCESS, effect.result());
        assertEquals(effect, hero.preview(context, request));
        var boxed = effect.updates().get(1L);
        assertEquals(9, boxed.boxedThroughTurn());
        assertEquals(4, boxed.cooldown());
        assertEquals(2, boxed.ultimateCharge());
        assertEquals(1, boxed.blindTurns());
        assertEquals(actor, context.pieceById(1));
    }

    @Test public void borrowedIllusionDeclaresCreationUsingTheActualCaster() {
        var hero = new ClowngangerHero(new ClowngangerRules(4, 2, 2, 3));
        var context = context(HeroType.GINSENG, Rook.class, FigureColor.WHITE, 2, 0);
        var actor = context.pieceById(1);
        var effect = hero.preview(context,
                new AbilityRequest(actor.position(), ClowngangerAbility.ILLUSION, pos("e4"), null, true));
        assertEquals(Success.SUCCESS, effect.result());
        assertEquals(List.of(new ClownMechanic.Create(actor, pos("e4"))), effect.operations());
        assertEquals(actor, effect.updates().get(1L));
        assertEquals(actor, context.pieceById(1));
    }

    @Test public void currentPiecesDetermineVacancyWhileTheDetachedFacadeKeepsFieldSemantics() {
        var rules = new ClowngangerRules(4, 2, 2, 3);
        var hero = new ClowngangerHero(rules);
        var base = context(HeroType.CLOWNGANGER, Knight.class, FigureColor.WHITE, 0, 3);
        var actor = base.pieceById(1);
        var request = new AbilityRequest(actor.position(), ClowngangerAbility.ILLUSION, pos("e4"));
        var pieces = new HashMap<>(base.pieces());
        var occupant = PieceState.initial(99,
                base.field().createFigure(Pawn.class, pos("e4"), FigureColor.BLACK), HeroType.NONE);
        pieces.put(occupant.id(), occupant);
        var occupied = new HeroActionContext(base.field(), pieces, base.currentTurn(), base.mechanics());
        assertEquals(AbilityRejection.INVALID_ABILITY_TARGET, hero.preview(occupied, request).result());
        assertEquals(Success.SUCCESS, ClowngangerAbilities.preview(base.field(), actor, null, pos("e4"),
                ClowngangerAbility.ILLUSION, base.currentTurn(), rules, false).result());

        var board = new HashMap<>(base.field().getGameField());
        board.put(pos("e4"), base.field().createFigure(Pawn.class, pos("e4"), FigureColor.BLACK));
        var vacant = new HeroActionContext(Field.fromMobaPosition(board), base.pieces(),
                base.currentTurn(), base.mechanics());
        assertEquals(Success.SUCCESS, hero.preview(vacant, request).result());
        assertEquals(AbilityRejection.INVALID_ABILITY_TARGET, ClowngangerAbilities.preview(vacant.field(), actor,
                null, pos("e4"), ClowngangerAbility.ILLUSION, base.currentTurn(), rules, false).result());
        assertEquals(actor, base.pieceById(1));
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

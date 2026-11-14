package game.heroes.ginseng;

import game.core.Game;
import game.heroes.AbilityRequest;
import game.heroes.HeroType;
import game.heroes.pudge.PudgeAbility;
import game.moves.AbilityRejection;
import java.util.Map;
import model.FigureColor;
import model.figures.Bishop;
import model.figures.Rook;
import org.junit.Test;

import static game.core.Positions.pos;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GinsengEnemyTargetsTest {
    @Test public void rejectedAllyTheftPreservesTheChargeAndTurnBudgetInAMatch() {
        var board = GinsengFixtures.board();
        GinsengFixtures.put(board, Rook.class, FigureColor.WHITE, "e2");
        GinsengFixtures.put(board, Rook.class, FigureColor.BLACK, "e5");
        Game game = GinsengFixtures.charged(GinsengFixtures.create(board,
                Map.of(pos("e2"), HeroType.PUDGE)), "c2");
        assertTrue(game.useAbility(game.getCurrentPlayer(), pos("e2"), pos("e5"), PudgeAbility.HOOK).isSuccess());
        assertTrue(game.moveFigure(game.getCurrentPlayer(), pos("b1"), pos("a3")).isSuccess());
        assertTrue(game.endTurn(game.getCurrentPlayer()).isSuccess());
        assertTrue(game.moveFigure(game.getCurrentPlayer(), pos("g8"), pos("f6")).isSuccess());
        assertTrue(game.endTurn(game.getCurrentPlayer()).isSuccess());
        var before = game.getSnapshot();
        assertEquals(2, before.pieceAt(pos("c2")).ultimateCharge());
        assertEquals(AbilityRejection.INVALID_ABILITY_TARGET,
                game.useAbility(game.getCurrentPlayer(), pos("c2"), pos("e2"), GinsengAbility.COPY_SPELL));
        assertEquals(before, game.getSnapshot());
    }

    @Test public void alliedHeroIsNotHighlightedAndRejectionLeavesTheWholeGameUnchanged() {
        for (FigureColor side : new FigureColor[]{FigureColor.WHITE, FigureColor.BLACK}) {
            Game game = Game.sandbox();
            assertTrue(game.placeEditorPiece(Bishop.class, HeroType.GINSENG, side, pos("c2")).isSuccess());
            assertTrue(game.placeEditorPiece(Rook.class, HeroType.PUDGE, side, pos("e2")).isSuccess());
            assertTrue(game.placeEditorPiece(Rook.class, HeroType.NONE, side, pos("e4")).isSuccess());
            if (side == FigureColor.BLACK) assertTrue(game.endTurn(game.getCurrentPlayer()).isSuccess());
            assertTrue(game.useAbility(game.getCurrentPlayer(), pos("e2"), pos("e4"), PudgeAbility.HOOK).isSuccess());
            var before = game.getSnapshot();
            assertFalse(game.getAbilityTargets(pos("c2"), GinsengAbility.COPY_SPELL).contains(pos("e2")));
            assertEquals(AbilityRejection.INVALID_ABILITY_TARGET, game.useAbility(game.getCurrentPlayer(),
                    new AbilityRequest(pos("c2"), GinsengAbility.COPY_SPELL, pos("e2"))));
            assertEquals(before, game.getSnapshot());
        }
    }
}

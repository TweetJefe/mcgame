package game.persistence;

import game.moves.Success;
import game.moves.TurnRejection;
import game.core.Game;
import game.heroes.Ability;
import game.state.PieceState;

import model.FigureColor;
import org.junit.Test;
import static game.core.Positions.pos;
import static org.junit.Assert.*;

/** Fixtures were written by the old game.* classes, not by this reorganized implementation. */
public class LegacySaveCompatibilityTest {
    private Game load(String name) throws Exception {
        byte[] bytes;
        try (var stream = getClass().getResourceAsStream("/saves/v1/" + name + ".xml")) {
            assertNotNull("Missing compatibility fixture " + name, stream); bytes = stream.readAllBytes();
        }
        Game restored = Game.fromSnapshot(GameSaveFile.decode(bytes), () -> 0L);
        assertArrayEquals("The saved data schema must remain unchanged", bytes, GameSaveFile.encode(restored.getSnapshot()));
        return restored;
    }
    @Test public void oldLoadedPudgeKeepsBudgetClockAndCounterCaptureAfterContinuation() throws Exception {
        Game game = load("pudge-mid-turn"); long ally = game.getPieceState(pos("f2")).storedPieceId();
        assertTrue(game.getTurnState().moveUsed()); assertTrue(game.getTurnState().abilityUsed());
        assertEquals(59_876_543_211L, game.getClockSnapshot().whiteNanos());
        assertEquals(TurnRejection.MOVE_ALREADY_USED, game.moveFigure(game.getCurrentPlayer(), pos("f2"), pos("f3")));
        assertEquals(Success.SUCCESS, game.endTurn(game.getCurrentPlayer()));
        assertEquals(Success.SUCCESS, game.moveFigure(game.getCurrentPlayer(), pos("f8"), pos("f2")));
        assertEquals(ally, game.getPieceState(pos("f2")).id());
        assertEquals(1, game.getWhitePlayer().getAttackedFigures().size()); assertEquals(1, game.getBlackPlayer().getAttackedFigures().size());
    }
    @Test public void oldChargedPudgeCanStillSwallowItsDiagonalAlly() throws Exception {
        Game game = load("pudge-charged"); long ally = game.getPieceState(pos("g4")).id();
        assertEquals(2, game.getPieceState(pos("f3")).ultimateCharge());
        assertEquals(Success.SUCCESS, game.useAbility(game.getCurrentPlayer(), pos("f3"), pos("g4"), Ability.SWALLOW));
        assertEquals(PieceState.Status.SWALLOWED, game.getPieceState(ally).status());
        assertEquals(0, game.getPieceState(pos("f3")).ultimateCharge());
    }
    @Test public void oldIntermediateCheckKeepsTheActingPlayerAndAbilityBudget() throws Exception {
        Game game = load("pudge-indirect-check");
        assertEquals(FigureColor.WHITE, game.getCurrentPlayer().color());
        assertTrue(game.getTurnState().abilityUsed()); assertFalse(game.getTurnState().moveUsed());
        assertEquals(Success.SUCCESS, game.moveFigure(game.getCurrentPlayer(), pos("b2"), pos("b3")));
        assertEquals(Success.SUCCESS, game.endTurn(game.getCurrentPlayer())); assertTrue(game.isInCheck());
    }
    @Test public void oldClassicSaveRetainsEnPassantCastlingRightsAndHistory() throws Exception {
        Game game = load("classic-en-passant");
        assertEquals(FigureColor.BLACK, game.getCurrentPlayer().color()); assertEquals(pos("e3"), game.getEnPassantTarget());
        assertTrue(game.getCastlingRights().whiteKingSide()); assertTrue(game.getCastlingRights().blackQueenSide());
        assertEquals(1, game.getMoves().size());
        assertEquals(Success.SUCCESS, game.moveFigure(game.getCurrentPlayer(), pos("e7"), pos("e5")));
    }
}

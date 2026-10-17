package game.scenarios;

import game.core.Game;
import game.heroes.Ability;
import game.heroes.HeroType;
import game.heroes.pudge.PudgeRules;
import game.history.GameEvent;
import game.moves.MoveResult;
import game.state.CastlingRights;
import game.state.GamePosition;
import game.state.PieceState;

import model.*;
import model.figures.*;
import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static game.core.Positions.pos;
import static model.FigureColor.*;
import static org.junit.Assert.*;

public class PudgeScenarioTest {
    private Game create(PudgeScenario scenario) { return scenario.createGame(() -> 0L); }
    private void move(Game game, String from, String to) {
        assertEquals(MoveResult.SUCCESS, game.moveFigure(game.getCurrentPlayer(), pos(from), pos(to)));
    }
    private void ability(Game game, String from, String target, Ability ability) {
        assertEquals(MoveResult.SUCCESS, game.useAbility(game.getCurrentPlayer(), pos(from), pos(target), ability));
    }
    private void end(Game game) { assertEquals(MoveResult.SUCCESS, game.endTurn(game.getCurrentPlayer())); }

    @Test public void diagonalHookFollowsTheDisplayedInstructions() {
        Game game = create(PudgeScenario.DIAGONAL_HOOK);
        ability(game, "e2", "h5", Ability.HOOK);
        assertTrue(game.getFigureByPosition(pos("f3")) instanceof Knight);
        move(game, "e2", "d2"); end(game); assertEquals(BLACK, game.getCurrentPlayer().color());
    }
    @Test public void indirectCheckIsCreatedByTheRooksOrdinaryAttack() {
        Game game = create(PudgeScenario.INDIRECT_CHECK);
        ability(game, "b2", "e2", Ability.HOOK);
        assertTrue(rules.standard.MoveValidator.isInCheck(game.getGameField().getGameField(), BLACK));
        assertTrue(game.getFigureByPosition(pos("c2")) instanceof Knight);
        move(game, "b2", "b3"); end(game); assertTrue(game.isInCheck());
    }
    @Test public void hookRescuesCheckBeforeTheMandatoryMove() {
        Game game = create(PudgeScenario.ESCAPE_CHECK);
        assertTrue(game.isInCheck()); assertFalse(game.getOutcome().isFinished());
        for (String square : new String[]{"f1", "h1"}) {
            assertTrue(game.getLegalMoves(pos(square)).getCanMove().isEmpty());
            assertTrue(game.getLegalMoves(pos(square)).getCanAttack().isEmpty());
        }
        ability(game, "f1", "h3", Ability.HOOK); assertFalse(game.isInCheck());
        move(game, "f1", "e1"); end(game);
    }
    @Test public void swallowedDiagonalAllyReturnsAndCapturesTheAttacker() {
        Game game = create(PudgeScenario.COUNTER_CAPTURE);
        long ally = game.getPieceState(pos("d3")).id(), pudge = game.getPieceState(pos("e2")).id();
        ability(game, "e2", "d3", Ability.SWALLOW);
        move(game, "e2", "f2"); end(game); move(game, "f8", "f2");
        assertEquals(ally, game.getPieceState(pos("f2")).id());
        assertEquals(PieceState.Status.CAPTURED, game.getPieceState(pudge).status());
        assertEquals(GameEvent.Kind.COUNTER_CAPTURE, game.getEvents().getLast().kind());
        end(game);
    }
    @Test public void twoCapturesChargeUltimateAndSuccessfulSwallowSpendsBoth() {
        Game game = create(PudgeScenario.ULTIMATE_CHARGE);
        assertEquals(MoveResult.ULTIMATE_NOT_READY, game.getAbilityAvailability(pos("e2"), Ability.SWALLOW));
        move(game, "e2", "e3"); assertEquals(1, game.getPieceState(pos("e3")).ultimateCharge());
        assertEquals(MoveResult.ULTIMATE_NOT_READY, game.getAbilityAvailability(pos("e3"), Ability.SWALLOW));
        end(game); move(game, "h8", "g8"); end(game);
        move(game, "e3", "f3"); assertEquals(2, game.getPieceState(pos("f3")).ultimateCharge());
        assertTrue(game.getAbilityTargets(pos("f3"), Ability.SWALLOW).contains(pos("g4")));
        ability(game, "f3", "g4", Ability.SWALLOW);
        assertEquals(0, game.getPieceState(pos("f3")).ultimateCharge()); end(game);
    }
    @Test public void ultimateDoesNotShareTheHooksCooldown() {
        Game seed = create(PudgeScenario.ULTIMATE_CHARGE);
        Game game = Game.fromMobaPosition(seed.getPosition(), Map.of(pos("e2"), HeroType.PUDGE_ROOK), PudgeRules.prototype(), () -> 0L);
        ability(game, "e2", "e3", Ability.HOOK); move(game, "e2", "e3"); end(game);
        move(game, "h8", "g8"); end(game);
        move(game, "e3", "f3"); assertEquals(1, game.getPieceState(pos("f3")).cooldown());
        assertEquals(MoveResult.ABILITY_COOLDOWN, game.getAbilityAvailability(pos("f3"), Ability.HOOK));
        assertEquals(MoveResult.SUCCESS, game.getAbilityAvailability(pos("f3"), Ability.SWALLOW));
        ability(game, "f3", "g4", Ability.SWALLOW);
        assertEquals(1, game.getPieceState(pos("f3")).cooldown());
        assertEquals(2, game.getPudgeRules().ultimateCaptures());
    }
    @Test public void radiusOneIncludesAllEightSurroundingSquares() {
        for (int dr = -1; dr <= 1; dr++) for (int dc = -1; dc <= 1; dc++) {
            if (dr == 0 && dc == 0) continue;
            Game seed = create(PudgeScenario.DIAGONAL_HOOK);
            var board = new HashMap<>(seed.getPosition().board());
            Position target = new Position(1 + dr, 4 + dc);
            board.put(target, seed.getGameField().createFigure(Knight.class, target, WHITE));
            Game game = Game.fromMobaPosition(new GamePosition(board, WHITE, CastlingRights.none(), null, 0),
                    Map.of(pos("e2"), HeroType.PUDGE_ROOK), PudgeRules.sandbox(), () -> 0L);
            assertTrue(game.getAbilityTargets(pos("e2"), Ability.SWALLOW).contains(target));
            assertEquals(MoveResult.SUCCESS, game.useAbility(game.getCurrentPlayer(), pos("e2"), target, Ability.SWALLOW));
        }
    }
    @Test public void restartingEveryScenarioCreatesAnIndependentCleanUntimedGame() {
        for (PudgeScenario scenario : PudgeScenario.values()) {
            Game first = create(scenario), restart = create(scenario);
            assertNotSame(first, restart); assertNotSame(first.getWhitePlayer(), restart.getWhitePlayer());
            assertEquals(first.getSnapshot().pieces(), restart.getSnapshot().pieces());
            assertEquals(first.getPosition().sideToMove(), restart.getPosition().sideToMove());
            assertEquals(first.getPudgeRules(), restart.getPudgeRules());
            for (var entry : first.getGameField().getGameField().entrySet()) {
                Figure other = restart.getFigureByPosition(entry.getKey());
                assertEquals(entry.getValue().getClass(), other.getClass());
                assertEquals(entry.getValue().getColor(), other.getColor());
            }
            assertTrue(restart.getEvents().isEmpty()); assertFalse(restart.getClockSnapshot().isEnabled());
            assertFalse(restart.getTurnState().moveUsed()); assertFalse(restart.getTurnState().abilityUsed());
            assertFalse(restart.getOutcome().isFinished());
        }
        Game changed = create(PudgeScenario.ULTIMATE_CHARGE); move(changed, "e2", "e3");
        Game restart = create(PudgeScenario.ULTIMATE_CHARGE);
        assertEquals(0, restart.getPieceState(pos("e2")).ultimateCharge()); assertTrue(restart.getMoves().isEmpty());
        assertEquals(1, changed.getPieceState(pos("e3")).ultimateCharge());
    }
}

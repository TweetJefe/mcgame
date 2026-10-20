package game.heroes.lumeshard;

import game.moves.AbilityRejection;
import game.moves.MovementRejection;
import game.moves.StatusRejection;
import game.moves.Success;
import game.moves.TurnRejection;
import game.core.Game;
import game.heroes.*;
import game.heroes.pudge.PudgeRules;
import game.moves.MoveResult;
import game.moves.Promotion;
import game.persistence.GameSaveFile;
import game.state.*;
import model.*;
import model.figures.*;
import org.junit.Test;
import java.util.*;
import static game.core.Positions.pos;
import static model.FigureColor.*;
import static org.junit.Assert.*;

public class LumeshardTest {
    private final Field factory = new Field(8);
    private Map<Position, Figure> board(String whiteKing, String blackKing) {
        var board = new HashMap<Position, Figure>();
        for (int row = 0; row < 8; row++) for (int col = 0; col < 8; col++) {
            Position p = new Position(row, col); board.put(p, factory.createFigure(Empty.class, p, NONE));
        }
        put(board, King.class, WHITE, whiteKing); put(board, King.class, BLACK, blackKing); return board;
    }
    private void put(Map<Position, Figure> board, Class<? extends Figure> type, FigureColor side, String cell) {
        board.put(pos(cell), factory.createFigure(type, pos(cell), side));
    }
    private Game create(Map<Position, Figure> board, String hero) {
        return create(board, Map.of(pos(hero), HeroType.LUMESHARD_BISHOP), HeroRules.prototype());
    }
    private Game create(Map<Position, Figure> board, Map<Position, HeroType> heroes, HeroRules rules) {
        return Game.fromMobaPosition(new GamePosition(board, WHITE, CastlingRights.none(), null, 0), heroes, rules, () -> 0L);
    }
    private MoveResult move(Game game, String from, String to) { return game.moveFigure(game.getCurrentPlayer(), pos(from), pos(to)); }
    private void play(Game game, String from, String to) { assertEquals(Success.SUCCESS, move(game, from, to)); }
    private MoveResult ability(Game game, String actor, String target, Ability ability) { return game.useAbility(game.getCurrentPlayer(), pos(actor), pos(target), ability); }
    private void end(Game game) { assertEquals(Success.SUCCESS, game.endTurn(game.getCurrentPlayer())); }
    private void reject(Game game, MoveResult expected, java.util.function.Supplier<MoveResult> command) {
        var before = game.getSnapshot(); assertEquals(expected, command.get()); assertEquals(before, game.getSnapshot());
    }
    private Game ready(boolean pudge) {
        var board = board("a1", "h8"); put(board, Bishop.class, WHITE, "d2");
        put(board, Pawn.class, BLACK, "e3"); put(board, Pawn.class, BLACK, "f4");
        var heroes = new HashMap<Position, HeroType>(); heroes.put(pos("d2"), HeroType.LUMESHARD_BISHOP);
        if (pudge) { put(board, Rook.class, WHITE, "f2"); heroes.put(pos("f2"), HeroType.PUDGE_ROOK); }
        Game game = create(board, heroes, new HeroRules(PudgeRules.sandbox(), LumeshardRules.prototype()));
        play(game, "d2", "e3"); end(game); play(game, "h8", "g8"); end(game); play(game, "e3", "f4");
        return game;
    }
    private Game laserPosition() {
        var board = board("a1", "h8"); put(board, Bishop.class, WHITE, "d2"); put(board, Pawn.class, WHITE, "e3");
        put(board, Rook.class, BLACK, "g5"); put(board, Knight.class, WHITE, "g2");
        return create(board, "d2");
    }
    private Game copy(Game game) throws Exception {
        byte[] bytes = GameSaveFile.encode(game.getSnapshot());
        Game copy = Game.fromSnapshot(GameSaveFile.decode(bytes), () -> 0L);
        assertArrayEquals(bytes, GameSaveFile.encode(copy.getSnapshot())); return copy;
    }

    @Test public void standardPrototypeContainsBothHeroesAndEachBishopsStartingColor() {
        Game game = Game.mobaPrototype(HeroRules.prototype(), 600);
        assertEquals(HeroType.PUDGE_ROOK, game.getPieceState(pos("a1")).hero());
        assertEquals(HeroType.LUMESHARD_BISHOP, game.getPieceState(pos("c1")).hero());
        assertEquals(SquareColor.DARK, game.getPieceState(pos("c1")).startingColor());
        assertEquals(SquareColor.LIGHT, game.getPieceState(pos("c8")).startingColor());
        assertEquals(3, game.getLumeshardRules().laserCooldown()); assertEquals(2, game.getLumeshardRules().ultimateCaptures());
    }
    @Test public void laserReachesTheWholeRadiusThreeAreaAndIgnoresBlockers() {
        Game game = laserPosition();
        assertEquals(Success.SUCCESS, ability(game, "d2", "g5", Ability.LASER));
        assertTrue(game.getPieceState(pos("g5")).isBlinded()); assertEquals(3, game.getPieceState(pos("d2")).cooldown());
        assertTrue(game.getTurnState().abilityUsed()); assertFalse(game.getTurnState().moveUsed());
        assertEquals(0, game.getWhitePlayer().getPoints()); assertEquals(0, game.getPieceState(pos("d2")).ultimateCharge());
        reject(game, TurnRejection.MOVE_REQUIRED, () -> game.endTurn(game.getCurrentPlayer()));
        var board = board("a1", "h8"); put(board, Bishop.class, WHITE, "d2"); put(board, Rook.class, BLACK, "f3");
        Game nonRay = create(board, "d2"); assertEquals(Success.SUCCESS, ability(nonRay, "d2", "f3", Ability.LASER));
    }
    @Test public void invalidLaserTargetsAndAnotherHerosAbilitiesDoNotSpendTheTurn() {
        var board = board("a1", "h8"); put(board, Bishop.class, WHITE, "d2"); put(board, Rook.class, BLACK, "h5"); put(board, Pawn.class, WHITE, "e2");
        Game game = create(board, "d2");
        reject(game, AbilityRejection.ABILITY_OUT_OF_RANGE, () -> ability(game, "d2", "h5", Ability.LASER));
        reject(game, AbilityRejection.INVALID_ABILITY_TARGET, () -> ability(game, "d2", "e2", Ability.LASER));
        reject(game, AbilityRejection.INVALID_ABILITY_TARGET, () -> ability(game, "d2", "e3", Ability.LASER));
        reject(game, StatusRejection.KING_IMMUNE, () -> ability(game, "d2", "h8", Ability.LASER));
        reject(game, AbilityRejection.NO_ABILITY, () -> ability(game, "d2", "h5", Ability.HOOK));
    }
    @Test public void blindnessBlocksCapturesButAllowsQuietMovesAndExpiresAtOwnersEnd() {
        Game game = laserPosition(); assertEquals(Success.SUCCESS, ability(game, "d2", "g5", Ability.LASER));
        play(game, "d2", "c1"); end(game);
        assertTrue(game.getLegalMoves(pos("g5")).getCanAttack().isEmpty());
        assertTrue(game.getLegalMoves(pos("g5")).getCanMove().contains(pos("g4")));
        reject(game, StatusRejection.BLINDED_CANNOT_CAPTURE, () -> move(game, "g5", "g2"));
        play(game, "g5", "g4"); assertTrue(game.getPieceState(pos("g4")).isBlinded());
        end(game); assertFalse(game.getPieceState(pos("g4")).isBlinded());
    }
    @Test public void blindnessBlocksEnPassantEvenThoughTheDestinationIsEmpty() {
        var board = board("a1", "h8"); put(board, Bishop.class, WHITE, "d2"); put(board, Pawn.class, WHITE, "e2"); put(board, Pawn.class, BLACK, "d4");
        Game game = create(board, "d2"); assertEquals(Success.SUCCESS, ability(game, "d2", "d4", Ability.LASER));
        play(game, "e2", "e4"); end(game); assertEquals(pos("e3"), game.getEnPassantTarget());
        reject(game, StatusRejection.BLINDED_CANNOT_CAPTURE, () -> move(game, "d4", "e3"));
        assertFalse(game.getLegalMoves(pos("d4")).getCanAttack().contains(pos("e3"))); play(game, "d4", "d3");
    }
    @Test public void blindedAttackerStopsCheckingButRemainsABlockerForOtherAttackers() throws Exception {
        var board = board("e1", "h8"); put(board, Bishop.class, WHITE, "c2"); put(board, Rook.class, BLACK, "e4"); put(board, Rook.class, BLACK, "e8");
        Game game = create(board, "c2"); assertTrue(game.isInCheck());
        assertEquals(Success.SUCCESS, ability(game, "c2", "e4", Ability.LASER)); assertFalse(game.isInCheck());
        game = copy(game); assertFalse(game.isInCheck()); play(game, "c2", "b3"); end(game);
    }
    @Test public void restoredAttacksCanGiveCheckWhenBlindnessExpires() {
        var board = board("f1", "h8"); put(board, Bishop.class, WHITE, "d2"); put(board, Rook.class, BLACK, "g5");
        Game game = create(board, "d2"); assertEquals(Success.SUCCESS, ability(game, "d2", "g5", Ability.LASER));
        play(game, "f1", "g1"); end(game); play(game, "h8", "h7");
        assertTrue(game.getPieceState(pos("g5")).isBlinded()); end(game); assertTrue(game.isInCheck());
    }
    @Test public void castlingMayPassThroughASquareAttackedOnlyByABlindRook() {
        var board = board("e1", "a8"); put(board, Rook.class, WHITE, "h1"); put(board, Bishop.class, WHITE, "d2"); put(board, Rook.class, BLACK, "f4");
        Game game = Game.fromMobaPosition(new GamePosition(board, WHITE, new CastlingRights(true, false, false, false), null, 0),
                Map.of(pos("d2"), HeroType.LUMESHARD_BISHOP), HeroRules.prototype(), () -> 0L);
        assertEquals(Success.SUCCESS, ability(game, "d2", "f4", Ability.LASER)); play(game, "e1", "g1");
    }
    @Test public void blindedPudgeCanUseASkillButSharesTheSameGlobalAbilityBudget() {
        var board = board("a1", "h8"); put(board, Bishop.class, WHITE, "d2"); put(board, Rook.class, BLACK, "g5"); put(board, Pawn.class, BLACK, "g4");
        Game game = create(board, Map.of(pos("d2"), HeroType.LUMESHARD_BISHOP, pos("g5"), HeroType.PUDGE_ROOK),
                new HeroRules(PudgeRules.sandbox(), LumeshardRules.prototype()));
        assertEquals(Success.SUCCESS, ability(game, "d2", "g5", Ability.LASER)); play(game, "d2", "c1"); end(game);
        assertEquals(Success.SUCCESS, ability(game, "g5", "g4", Ability.SWALLOW));
        reject(game, TurnRejection.ABILITY_ALREADY_USED, () -> ability(game, "g5", "c3", Ability.HOOK));
        play(game, "h8", "h7"); end(game); assertFalse(game.getPieceState(pos("g5")).isBlinded());
    }
    @Test public void laserCooldownReachesZeroAtTheThirdOwnTurnStart() {
        Game game = laserPosition(); assertEquals(Success.SUCCESS, ability(game, "d2", "g5", Ability.LASER));
        play(game, "d2", "c1"); end(game); play(game, "h8", "h7"); end(game);
        assertEquals(2, game.getPieceState(pos("c1")).cooldown());
        reject(game, AbilityRejection.ABILITY_COOLDOWN, () -> ability(game, "c1", "g5", Ability.LASER));
        play(game, "c1", "d2"); end(game); play(game, "h7", "h8"); end(game);
        assertEquals(1, game.getPieceState(pos("d2")).cooldown());
        play(game, "d2", "c1"); end(game); play(game, "h8", "h7"); end(game);
        assertEquals(0, game.getPieceState(pos("c1")).cooldown());
    }
    @Test public void twoPawnCapturesChargeTheUltimateAndTeleportSpendsBoth() throws Exception {
        Game game = ready(false); assertEquals(2, game.getPieceState(pos("f4")).ultimateCharge()); assertEquals(2, game.getWhitePlayer().getPoints());
        game = copy(game); long id = game.getPieceState(pos("f4")).id();
        assertEquals(Success.SUCCESS, ability(game, "f4", "a3", Ability.TELEPORT));
        assertEquals(id, game.getPieceState(pos("a3")).id()); assertEquals(0, game.getPieceState(id).ultimateCharge());
        assertTrue(game.getTurnState().moveUsed()); end(game);
    }
    @Test public void teleportBeforeMovementStillRequiresOneOrdinaryMoveByTheSameOrAnotherPiece() {
        Game game = ready(false); end(game); play(game, "g8", "h8"); end(game);
        assertEquals(Success.SUCCESS, ability(game, "f4", "a3", Ability.TELEPORT));
        reject(game, TurnRejection.MOVE_REQUIRED, () -> game.endTurn(game.getCurrentPlayer()));
        play(game, "a3", "b4"); end(game);
    }
    @Test public void teleportRejectsWrongColorsOccupiedSquaresAndUnchargedHeroesWithoutCost() {
        Game game = ready(false);
        reject(game, AbilityRejection.WRONG_SQUARE_COLOR, () -> ability(game, "f4", "a2", Ability.TELEPORT));
        reject(game, StatusRejection.KING_IMMUNE, () -> ability(game, "f4", "a1", Ability.TELEPORT));
        Game uncharged = laserPosition();
        reject(uncharged, AbilityRejection.ULTIMATE_NOT_READY, () -> ability(uncharged, "d2", "a3", Ability.TELEPORT));
    }
    @Test public void teleportCannotExposeItsOwnKing() {
        var board = board("e1", "h8"); put(board, Bishop.class, WHITE, "e2"); put(board, Rook.class, BLACK, "e8");
        Game game = create(board, Map.of(pos("e2"), HeroType.LUMESHARD_BISHOP), HeroRules.sandbox());
        reject(game, MovementRejection.KING_IN_CHECK, () -> ability(game, "e2", "b1", Ability.TELEPORT));
    }
    @Test public void hookDoesNotChangeLumeshardsOriginalColorAndTheGlobalSkillLimitApplies() throws Exception {
        Game game = ready(true); end(game); play(game, "g8", "h8"); end(game);
        assertEquals(Success.SUCCESS, ability(game, "f2", "f4", Ability.HOOK));
        assertEquals(SquareColor.LIGHT, SquareColor.at(game.getPieceState(pos("f3")).position()));
        assertEquals(SquareColor.DARK, game.getPieceState(pos("f3")).startingColor());
        reject(game, TurnRejection.ABILITY_ALREADY_USED, () -> ability(game, "f3", "a3", Ability.TELEPORT));
        play(game, "a1", "b1"); end(game); play(game, "h8", "g8"); end(game);
        Game restored = copy(game);
        reject(restored, AbilityRejection.WRONG_SQUARE_COLOR, () -> ability(restored, "f3", "b3", Ability.TELEPORT));
        assertEquals(Success.SUCCESS, ability(restored, "f3", "a3", Ability.TELEPORT));
    }
    @Test public void returnedLumeshardRetainsColorAndChargesFromTheCounterCapture() throws Exception {
        var board = board("a1", "h8"); put(board, Rook.class, WHITE, "e2"); put(board, Bishop.class, WHITE, "d3"); put(board, Rook.class, BLACK, "f8");
        Game game = create(board, Map.of(pos("e2"), HeroType.PUDGE_ROOK, pos("d3"), HeroType.LUMESHARD_BISHOP),
                new HeroRules(PudgeRules.sandbox(), LumeshardRules.prototype()));
        long id = game.getPieceState(pos("d3")).id(); assertEquals(Success.SUCCESS, ability(game, "e2", "d3", Ability.SWALLOW));
        play(game, "e2", "f2"); end(game); game = copy(game); play(game, "f8", "f2");
        assertEquals(id, game.getPieceState(pos("f2")).id()); assertEquals(SquareColor.LIGHT, game.getPieceState(id).startingColor());
        assertEquals(1, game.getPieceState(id).ultimateCharge()); copy(game);
    }
    @Test public void blindedPawnCanPromoteQuietlyButAttacksResumeOnlyAtTurnEnd() {
        var board = board("h1", "h8"); put(board, Bishop.class, WHITE, "c4"); put(board, Pawn.class, BLACK, "a2");
        Game game = create(board, "c4"); assertEquals(Success.SUCCESS, ability(game, "c4", "a2", Ability.LASER));
        play(game, "h1", "g1"); end(game);
        assertEquals(Success.SUCCESS, game.moveFigure(game.getCurrentPlayer(), pos("a2"), pos("a1"), Promotion.QUEEN));
        assertTrue(game.getPieceState(pos("a1")).isBlinded()); end(game); assertTrue(game.isInCheck());
    }
    @Test public void assigningLumeshardToANonBishopIsRejected() {
        var board = board("a1", "h8"); put(board, Rook.class, WHITE, "d2");
        assertThrows(IllegalArgumentException.class, () -> create(board, "d2"));
    }
    @Test public void aSwallowedBlindAllyKeepsItsEffectUntilItsOwnersTurnEnds() throws Exception {
        var board = board("a1", "h8"); put(board, Bishop.class, WHITE, "d2"); put(board, Rook.class, BLACK, "g5"); put(board, Knight.class, BLACK, "g4");
        Game game = create(board, Map.of(pos("d2"), HeroType.LUMESHARD_BISHOP, pos("g5"), HeroType.PUDGE_ROOK),
                new HeroRules(PudgeRules.sandbox(), LumeshardRules.prototype()));
        long ally = game.getPieceState(pos("g4")).id();
        assertEquals(Success.SUCCESS, ability(game, "d2", "g4", Ability.LASER)); play(game, "d2", "c1"); end(game);
        assertEquals(Success.SUCCESS, ability(game, "g5", "g4", Ability.SWALLOW));
        game = copy(game); assertTrue(game.getPieceState(ally).isBlinded()); assertEquals(PieceState.Status.SWALLOWED, game.getPieceState(ally).status());
        play(game, "h8", "h7"); end(game); assertFalse(game.getPieceState(ally).isBlinded()); copy(game);
    }
    @Test public void anotherPiecesCaptureDoesNotChargeLumeshard() {
        var board = board("a1", "h8"); put(board, Bishop.class, WHITE, "d2"); put(board, Rook.class, WHITE, "b2"); put(board, Pawn.class, BLACK, "b3");
        Game game = create(board, "d2"); play(game, "b2", "b3");
        assertEquals(0, game.getPieceState(pos("d2")).ultimateCharge()); assertEquals(1, game.getWhitePlayer().getPoints());
    }
}

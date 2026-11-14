package game.heroes.phoenix;

import game.core.Game;
import game.draft.Draft;
import game.draft.DraftRules;
import game.draft.DraftActionResult;
import game.heroes.AbilityRequest;
import game.heroes.HeroRules;
import game.heroes.HeroType;
import game.heroes.ginseng.GinsengAbility;
import game.heroes.ginseng.GinsengFixtures;
import game.heroes.pudge.PudgeAbility;
import game.heroes.moleminer.MoleminerAbility;
import game.heroes.supernova.SupernovaAbility;
import game.heroes.sunwukong.SunWukongAbility;
import game.heroes.sunwukong.SunWukongRules;
import game.heroes.sunwukong.SunWukongDefinition;
import game.heroes.sunwukong.SunWukongMechanic;
import game.heroes.strix.StrixAbility;
import game.mechanics.PieceHeroHistory;
import game.moves.MoveResult;
import game.moves.StatusRejection;
import game.moves.Success;
import game.persistence.GameSaveFile;
import game.state.CastlingRights;
import game.state.GamePosition;
import game.state.GameSnapshot;
import game.state.PieceState;
import game.state.SquareColor;
import java.util.HashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import model.Field;
import model.Figure;
import model.Position;
import model.figures.Archbishop;
import model.figures.Bishop;
import model.figures.Egg;
import model.figures.King;
import model.figures.Knight;
import model.figures.Pawn;
import model.figures.Rook;
import org.junit.Test;

import static game.core.Positions.pos;
import static model.FigureColor.WHITE;
import static model.FigureColor.BLACK;
import static org.junit.Assert.*;

public class PhoenixHeroTest {
    public static Map<Position, Figure> board() {
        var board = GinsengFixtures.board();
        GinsengFixtures.empty(board, "a1");
        GinsengFixtures.put(board, King.class, WHITE, "h1");
        GinsengFixtures.put(board, Pawn.class, WHITE, "h2");
        return board;
    }
    public static Game create(Map<Position, Figure> board, Map<Position, HeroType> other) {
        var heroes = new HashMap<>(other);
        heroes.put(pos("b1"), HeroType.PHOENIX);
        return Game.fromMobaPosition(new GamePosition(board, WHITE, CastlingRights.none(), null, 0),
                heroes, HeroRules.defaults(), () -> 0L);
    }
    public static Game basic() { return create(board(), Map.of()); }
    public static Game charge(Game game, String cell, int charge) {
        var s = game.getSnapshot();
        var pieces = new HashMap<>(s.pieces());
        var p = s.pieceAt(pos(cell));
        pieces.put(p.id(), p.withAbilityState(p.cooldown(), charge, p.storedPieceId()));
        return Game.fromSnapshot(new GameSnapshot(s.version(), s.mode(), s.field(), pieces, s.turn(), s.active(),
                s.castlingRights(), s.enPassantTarget(), s.halfmoveClock(), s.repetitions(), s.clock(), s.outcome(),
                s.moves(), s.events(), s.whitePoints(), s.blackPoints(), s.whiteCaptures(), s.blackCaptures(),
                s.drawOffer(), s.heroRules(), s.draft(), s.mechanics()), () -> 0L);
    }
    public static void ok(MoveResult result) { assertEquals(Success.SUCCESS, result); }
    public static void play(Game game, String from, String to) {
        ok(game.moveFigure(game.getCurrentPlayer(), pos(from), pos(to)));
        ok(game.endTurn(game.getCurrentPlayer()));
    }
    public static void rebirth(Game game, String from) {
        ok(game.useAbility(game.getCurrentPlayer(), pos(from), null, PhoenixAbility.REBIRTH));
    }
    public static Game restored(Game game) throws Exception {
        byte[] bytes = GameSaveFile.encode(game.getSnapshot());
        Game copy = Game.fromSnapshot(GameSaveFile.decode(bytes), () -> 0L);
        assertArrayEquals(bytes, GameSaveFile.encode(copy.getSnapshot()));
        return copy;
    }
    public static Game hatch(Game game) {
        game = charge(game, "b1", 2);
        rebirth(game, "b1");
        play(game, "h2", "h3"); play(game, "g8", "f6");
        play(game, "h3", "h4"); play(game, "f6", "g8");
        play(game, "h4", "h5"); play(game, "g8", "f6");
        return game;
    }
    public static Game supernova() { return hatch(basic()); }

    @Test public void pawnCaptureReturnsAndStillUsesTheMandatoryMoveAndAwardsGold() throws Exception {
        var board = board();
        GinsengFixtures.put(board, Pawn.class, BLACK, "a3");
        Game game = create(board, Map.of());
        long id = game.getPieceState(pos("b1")).id();
        ok(game.moveFigure(game.getCurrentPlayer(), pos("b1"), pos("a3")));
        assertEquals(id, game.getPieceState(pos("b1")).id());
        assertNull(game.getPieceState(pos("a3")));
        assertEquals(1, game.getPieceState(pos("b1")).ultimateCharge());
        assertEquals(2, game.getEconomy().whiteHalves());
        assertTrue(game.getTurnState().moveUsed());
        ok(game.endTurn(game.getCurrentPlayer()));
        restored(game);
    }
    @Test public void aPawnHeroAlsoTriggersThePassiveAndCostsTwoGold() throws Exception {
        var board = board(); GinsengFixtures.put(board, Pawn.class, BLACK, "a3");
        Game game = create(board, Map.of(pos("a3"), HeroType.BOUNTY_CROW));
        ok(game.moveFigure(game.getCurrentPlayer(), pos("b1"), pos("a3")));
        assertNotNull(game.getPieceState(pos("b1")));
        assertEquals(4, game.getEconomy().whiteHalves()); restored(game);
    }
    @Test public void captureOfNonPawnStaysOnTheCapturedSquare() throws Exception {
        var board = board(); GinsengFixtures.put(board, Bishop.class, BLACK, "a3");
        Game game = create(board, Map.of());
        ok(game.moveFigure(game.getCurrentPlayer(), pos("b1"), pos("a3")));
        assertNull(game.getPieceState(pos("b1")));
        assertEquals(HeroType.PHOENIX, game.getPieceState(pos("a3")).hero()); restored(game);
    }
    @Test public void eggHatchesAfterTwoFollowingOwnTurnsWithSameIdAndNoCharge() throws Exception {
        Game game = charge(basic(), "b1", 2);
        long id = game.getPieceState(pos("b1")).id();
        rebirth(game, "b1");
        assertEquals(Egg.class, game.getPieceState(pos("b1")).type());
        assertFalse(game.endTurn(game.getCurrentPlayer()).isSuccess());
        assertTrue(game.getLegalMoves(pos("b1")).getCanMove().isEmpty());
        game = restored(game);
        play(game, "h2", "h3"); play(game, "g8", "f6");
        assertEquals(Egg.class, game.getPieceState(pos("b1")).type());
        assertEquals(StatusRejection.IMMOBILE, game.getAbilityAvailability(pos("b1"), PhoenixAbility.REBIRTH));
        play(game, "h3", "h4"); play(game, "f6", "g8");
        assertEquals(Egg.class, game.getPieceState(pos("b1")).type());
        play(game, "h4", "h5");
        var nova = game.getPieceState(pos("b1"));
        assertEquals(id, nova.id()); assertEquals(HeroType.SUPERNOVA, nova.hero());
        assertEquals(Archbishop.class, nova.type()); assertEquals(0, nova.ultimateCharge());
        assertEquals(HeroType.PHOENIX, PieceHeroHistory.replay(game.getSnapshot()).initial(id));
        restored(game);
    }
    @Test public void eggCanBeCapturedForThreeGoldAndNeverHatches() throws Exception {
        var board = board(); GinsengFixtures.put(board, Rook.class, BLACK, "b4");
        Game game = charge(create(board, Map.of()), "b1", 2);
        long id = game.getPieceState(pos("b1")).id();
        rebirth(game, "b1"); play(game, "h2", "h3");
        ok(game.moveFigure(game.getCurrentPlayer(), pos("b4"), pos("b1")));
        assertEquals(6, game.getEconomy().blackHalves());
        assertEquals(PieceState.Status.CAPTURED, game.getSnapshot().pieces().get(id).status());
        ok(game.endTurn(game.getCurrentPlayer()));
        assertFalse(game.getSnapshot().mechanics().get(PhoenixMechanic.KEY).eggs().containsKey(id));
        restored(game);
    }
    @Test public void hookAndDisplaceCannotMoveEggAndFailedCastIsAtomic() throws Exception {
        var board = board(); GinsengFixtures.put(board, Rook.class, WHITE, "b4");
        Game game = charge(create(board, Map.of(pos("b4"), HeroType.PUDGE, pos("c2"), HeroType.GINSENG)), "b1", 2);
        rebirth(game, "b1"); play(game, "h2", "h3"); play(game, "g8", "f6");
        byte[] before = GameSaveFile.encode(game.getSnapshot());
        assertFalse(game.useAbility(game.getCurrentPlayer(), pos("b4"), pos("b1"), PudgeAbility.HOOK).isSuccess());
        assertFalse(game.useAbility(game.getCurrentPlayer(), new AbilityRequest(pos("c2"), GinsengAbility.DISPLACE,
                pos("b1"), pos("a2"), false)).isSuccess());
        assertArrayEquals(before, GameSaveFile.encode(game.getSnapshot())); restored(game);
    }
    @Test public void supernovaHasBothMovePatternsAndStillGetsThreeGoldWhenCaptured() throws Exception {
        var board = board(); GinsengFixtures.put(board, Rook.class, BLACK, "b4");
        Game game = hatch(create(board, Map.of()));
        var moves = game.getLegalMoves(pos("b1")).getCanMove();
        assertTrue(moves.contains(pos("a2"))); assertTrue(moves.contains(pos("d2")));
        assertFalse(moves.contains(pos("b2"))); assertFalse(moves.contains(pos("b3")));
        play(game, "h5", "h6");
        ok(game.moveFigure(game.getCurrentPlayer(), pos("b4"), pos("b1")));
        assertEquals(6, game.getEconomy().blackHalves()); restored(game);
    }
    @Test public void ginsengUsingCopiedRebirthBecomesTheRealSupernova() throws Exception {
        var b = board(); GinsengFixtures.put(b, Knight.class, BLACK, "b1");
        Game game = charge(charge(create(b, Map.of(pos("c2"), HeroType.GINSENG)), "b1", 2), "c2", 2);
        long id = game.getPieceState(pos("c2")).id();
        play(game, "h2", "h3"); rebirth(game, "b1"); play(game, "g8", "f6");
        ok(game.useAbility(game.getCurrentPlayer(), pos("c2"), pos("b1"), GinsengAbility.COPY_SPELL));
        play(game, "h3", "h4"); play(game, "f6", "g8");
        ok(game.useAbility(game.getCurrentPlayer(), new AbilityRequest(pos("c2"), PhoenixAbility.REBIRTH, null, null, true)));
        assertEquals(Egg.class, game.getPieceState(pos("c2")).type());
        assertFalse(game.getSpells().borrowed().containsKey(id));
        game = restored(game);
        play(game, "h4", "h5"); play(game, "g8", "f6");
        play(game, "h5", "h6"); play(game, "f6", "g8");
        play(game, "h6", "h7");
        var nova = game.getPieceState(pos("c2"));
        assertEquals(id, nova.id()); assertEquals(HeroType.SUPERNOVA, nova.hero());
        assertEquals(0, nova.ultimateCharge());
        assertEquals(HeroType.GINSENG, PieceHeroHistory.replay(game.getSnapshot()).initial(id));
        restored(game);
    }
    @Test public void draftAllowsPhoenixOnEitherKnightAndExcludesSupernova() throws Exception {
        var draft = new Draft(new DraftRules(0, 1));
        assertFalse(draft.getSnapshot().catalog().heroes().contains(HeroType.SUPERNOVA));
        assertTrue(draft.getPlacementTargets(WHITE, HeroType.PHOENIX).contains(pos("g1")));
        assertEquals(DraftActionResult.SUCCESS, draft.pick(WHITE, HeroType.PHOENIX, pos("b1")));
        draft.finishPicks(BLACK);
        Game game = Game.mobaFromDraft(draft.getSnapshot(), HeroRules.defaults(), 600, () -> 0L);
        String xml = new String(GameSaveFile.encode(game.getSnapshot()), StandardCharsets.UTF_8);
        assertTrue(xml.contains("formatVersion=\"20\""));
        assertEquals(draft.getSnapshot(), restored(game).getDraft());
    }
    @Test public void classicBoardRejectsExtendedBodies() {
        var board = board(); GinsengFixtures.put(board, Archbishop.class, WHITE, "b1");
        assertThrows(IllegalArgumentException.class, () -> Field.fromPosition(board));
        assertNotNull(Field.fromMobaPosition(board));
    }

    @Test public void pawnCaptureIntoAnEnemyMineKillsPhoenixBeforeHeCanReturn() throws Exception {
        var board = board(); GinsengFixtures.put(board, Pawn.class, BLACK, "a3");
        Game game = create(board, Map.of(pos("a3"), HeroType.MOLEMINER));
        play(game, "h2", "h3");
        ok(game.useAbility(game.getCurrentPlayer(), pos("a3"), null, MoleminerAbility.PLANT_MINE));
        play(game, "g8", "f6");
        long phoenix = game.getPieceState(pos("b1")).id();
        ok(game.moveFigure(game.getCurrentPlayer(), pos("b1"), pos("a3")));
        assertNull(game.getPieceState(pos("b1"))); assertNull(game.getPieceState(pos("a3")));
        assertTrue(game.getPieceState(phoenix).isRemoved());
        assertEquals(4, game.getEconomy().whiteHalves()); assertEquals(6, game.getEconomy().blackHalves());
        restored(game);
    }
    @Test public void massAbilitiesIgnoreTheEgg() throws Exception {
        var board = board(); GinsengFixtures.put(board, Knight.class, WHITE, "d2");
        Game game = create(board, Map.of(pos("d2"), HeroType.PHOENIX));
        // Два Phoenix здесь только для проверки взаимодействия; драфт этого не разрешает.
        game = hatch(game);
        game = charge(game, "d2", 2); rebirth(game, "d2");
        play(game, "h5", "h6"); play(game, "f6", "g8");
        long egg = game.getPieceState(pos("d2")).id();
        ok(game.useAbility(game.getCurrentPlayer(), pos("b1"), pos("c3"), SupernovaAbility.PULL));
        assertEquals(egg, game.getPieceState(pos("d2")).id());
        restored(game);
    }

    @Test public void ginsengHatchingKeepsTheOriginalDraftAndStartingColorValidation() throws Exception {
        var draft = new Draft(new DraftRules(0, 2));
        assertEquals(DraftActionResult.SUCCESS, draft.pick(WHITE, HeroType.GINSENG, pos("f1")));
        assertEquals(DraftActionResult.SUCCESS, draft.pick(BLACK, HeroType.PHOENIX, pos("b8")));
        draft.finishPicks(WHITE); draft.finishPicks(BLACK);
        Game game = charge(charge(Game.mobaFromDraft(draft.getSnapshot(), HeroRules.defaults(), 600, () -> 0L),
                "b8", 2), "f1", 2);
        play(game, "h2", "h3"); rebirth(game, "b8"); play(game, "g8", "f6");
        ok(game.useAbility(game.getCurrentPlayer(), pos("f1"), pos("b8"), GinsengAbility.COPY_SPELL));
        play(game, "h3", "h4"); play(game, "f6", "g8");
        ok(game.useAbility(game.getCurrentPlayer(), new AbilityRequest(pos("f1"), PhoenixAbility.REBIRTH, null, null, true)));
        play(game, "h4", "h5"); play(game, "g8", "f6");
        play(game, "a2", "a3"); play(game, "f6", "g8"); play(game, "a3", "a4");
        assertEquals(HeroType.SUPERNOVA, game.getPieceState(pos("f1")).hero());
        assertEquals(draft.getSnapshot(), restored(game).getDraft());
        var s = game.getSnapshot();
        var nova = s.pieceAt(pos("f1"));
        var pieces = new HashMap<>(s.pieces());
        pieces.put(nova.id(), nova.withStartingColor(SquareColor.DARK));
        var forged = new GameSnapshot(s.version(), s.mode(), s.field(), pieces, s.turn(), s.active(), s.castlingRights(),
                s.enPassantTarget(), s.halfmoveClock(), s.repetitions(), s.clock(), s.outcome(), s.moves(), s.events(),
                s.whitePoints(), s.blackPoints(), s.whiteCaptures(), s.blackCaptures(), s.drawOffer(), s.heroRules(), s.draft(), s.mechanics());
        assertThrows(IllegalArgumentException.class, () -> Game.fromSnapshot(forged, () -> 0L));
    }

    @Test public void strixCannotReturnEggToTheEarlierKnightPosition() throws Exception {
        var board = board(); GinsengFixtures.put(board, Pawn.class, WHITE, "c2");
        Game game = charge(create(board, Map.of(pos("c2"), HeroType.STRIX)), "b1", 2);
        play(game, "b1", "c3"); play(game, "g8", "f6");
        rebirth(game, "c3"); play(game, "h2", "h3"); play(game, "f6", "g8");
        byte[] before = GameSaveFile.encode(game.getSnapshot());
        assertEquals(StatusRejection.IMMOBILE,
                game.useAbility(game.getCurrentPlayer(), pos("c2"), pos("c3"), StrixAbility.REWIND));
        assertArrayEquals(before, GameSaveFile.encode(game.getSnapshot())); restored(game);
    }
    @Test public void olderTemporaryFormCannotOverwriteEggOrHatchedSupernova() throws Exception {
        var board = board();
        GinsengFixtures.put(board, Knight.class, BLACK, "b1");
        GinsengFixtures.put(board, Pawn.class, BLACK, "f2");
        GinsengFixtures.put(board, Pawn.class, WHITE, "a2");
        var rules = HeroRules.defaults().with(SunWukongDefinition.INSTANCE, new SunWukongRules(4, 6, 1));
        Game game = Game.fromMobaPosition(new GamePosition(board, WHITE, CastlingRights.none(), null, 0),
                Map.of(pos("b1"), HeroType.PHOENIX, pos("c2"), HeroType.GINSENG, pos("f2"), HeroType.SUN_WUKONG), rules, () -> 0L);
        game = charge(charge(game, "b1", 2), "c2", 2);
        long ginseng = game.getPieceState(pos("c2")).id();
        play(game, "h2", "h3");
        ok(game.useAbility(game.getCurrentPlayer(), pos("f2"), null, SunWukongAbility.SHAPESHIFT)); play(game, "g8", "f6");
        ok(game.useAbility(game.getCurrentPlayer(), pos("c2"), pos("f2"), GinsengAbility.COPY_SPELL));
        play(game, "h3", "h4"); play(game, "f6", "g8");
        ok(game.useAbility(game.getCurrentPlayer(), new AbilityRequest(pos("c2"), SunWukongAbility.SHAPESHIFT, null, null, true)));
        play(game, "h4", "h5"); play(game, "g8", "f6");
        play(game, "h5", "h6"); rebirth(game, "b1"); play(game, "f6", "g8");
        game = charge(game, "c2", 2);
        ok(game.useAbility(game.getCurrentPlayer(), pos("c2"), pos("b1"), GinsengAbility.COPY_SPELL));
        play(game, "h6", "h7"); play(game, "g8", "f6");
        ok(game.useAbility(game.getCurrentPlayer(), new AbilityRequest(pos("c2"), PhoenixAbility.REBIRTH, null, null, true)));
        play(game, "a2", "a3"); play(game, "f6", "g8");
        assertEquals(Egg.class, game.getPieceState(ginseng).type()); game = restored(game);
        play(game, "a3", "a4"); play(game, "g8", "f6");
        play(game, "a4", "a5"); play(game, "f6", "g8");
        assertEquals(HeroType.SUPERNOVA, game.getPieceState(ginseng).hero());
        play(game, "a5", "a6");
        assertEquals(Archbishop.class, game.getPieceState(ginseng).type());
        assertFalse(game.getSnapshot().mechanics().get(SunWukongMechanic.KEY).forms().containsKey(ginseng));
        restored(game);
    }
}

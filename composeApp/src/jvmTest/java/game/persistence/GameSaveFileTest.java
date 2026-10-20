package game.persistence;

import game.moves.Success;
import game.moves.TurnRejection;
import game.core.Game;
import game.heroes.Ability;
import game.heroes.HeroType;
import game.heroes.pudge.PudgeRules;
import game.moves.Promotion;
import game.scenarios.PudgeScenario;
import game.state.CastlingRights;
import game.state.GamePosition;
import game.state.PieceState;
import game.state.Termination;

import model.*;
import model.figures.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import static game.core.Positions.pos;
import static model.FigureColor.*;
import static org.junit.Assert.*;

public class GameSaveFileTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private Game scenario(PudgeScenario scenario) { return scenario.createGame(() -> 0L); }
    private Game roundtrip(Game game) throws Exception {
        byte[] bytes = GameSaveFile.encode(game.getSnapshot());
        Game restored = Game.fromSnapshot(GameSaveFile.decode(bytes), () -> 0L);
        assertArrayEquals(bytes, GameSaveFile.encode(restored.getSnapshot())); return restored;
    }
    private void move(Game game, String from, String to) { assertEquals(Success.SUCCESS, game.moveFigure(game.getCurrentPlayer(), pos(from), pos(to))); }
    private void ability(Game game, String from, String to, Ability ability) { assertEquals(Success.SUCCESS, game.useAbility(game.getCurrentPlayer(), pos(from), pos(to), ability)); }
    private void end(Game game) { assertEquals(Success.SUCCESS, game.endTurn(game.getCurrentPlayer())); }
    private Map<Position, Figure> board(String whiteKing, String blackKing) {
        Field factory = new Field(8); var board = new HashMap<Position, Figure>();
        for (int row = 0; row < 8; row++) for (int col = 0; col < 8; col++) {
            Position p = new Position(row, col); board.put(p, factory.createFigure(Empty.class, p, NONE));
        }
        put(board, King.class, WHITE, whiteKing); put(board, King.class, BLACK, blackKing); return board;
    }
    private void put(Map<Position, Figure> board, Class<? extends Figure> type, FigureColor side, String square) {
        board.put(pos(square), new Field(8).createFigure(type, pos(square), side));
    }
    private byte[] changed(Game game, Consumer<Document> change) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(GameSaveFile.encode(game.getSnapshot())));
        change.accept(doc); var out = new ByteArrayOutputStream();
        TransformerFactory.newInstance().newTransformer().transform(new DOMSource(doc), new StreamResult(out)); return out.toByteArray();
    }
    private Element first(Document doc, String name) { return (Element) doc.getElementsByTagName(name).item(0); }
    private void rejects(Game game, Consumer<Document> mutation) throws Exception {
        byte[] bytes = changed(game, mutation); assertThrows(IOException.class, () -> GameSaveFile.decode(bytes));
    }

    @Test public void everyInitialScenarioAndClassicGameRoundtripWithoutChangingIdsOrSettings() throws Exception {
        for (PudgeScenario scenario : PudgeScenario.values()) roundtrip(scenario(scenario));
        roundtrip(new Game(8, () -> 0L));
    }
    @Test public void skillOnlySaveKeepsItsBudgetAndStillRequiresNormalMovement() throws Exception {
        Game game = scenario(PudgeScenario.COUNTER_CAPTURE); long ally = game.getPieceState(pos("d3")).id();
        ability(game, "e2", "d3", Ability.SWALLOW); Game restored = roundtrip(game);
        assertEquals(PieceState.Status.SWALLOWED, restored.getPieceState(ally).status());
        assertEquals(TurnRejection.MOVE_REQUIRED, restored.endTurn(restored.getCurrentPlayer()));
        assertEquals(TurnRejection.ABILITY_ALREADY_USED, restored.useAbility(restored.getCurrentPlayer(), pos("e2"), pos("f8"), Ability.HOOK));
        move(restored, "e2", "f2"); end(restored);
        assertFalse(game.getTurnState().moveUsed()); // Restored player/Game are independent.
    }
    @Test public void moveOnlySaveDoesNotGrantASecondMovement() throws Exception {
        Game game = scenario(PudgeScenario.DIAGONAL_HOOK); move(game, "e2", "d2"); Game restored = roundtrip(game);
        assertEquals(TurnRejection.MOVE_ALREADY_USED, restored.moveFigure(restored.getCurrentPlayer(), pos("d2"), pos("d3")));
        assertFalse(restored.getTurnState().abilityUsed()); end(restored);
    }
    @Test public void loadedPudgesReactionAfterLoadingPreservesAllyIdAndBothCaptureLists() throws Exception {
        Game game = scenario(PudgeScenario.COUNTER_CAPTURE);
        long ally = game.getPieceState(pos("d3")).id(), pudge = game.getPieceState(pos("e2")).id();
        ability(game, "e2", "d3", Ability.SWALLOW); move(game, "e2", "f2"); end(game);
        Game restored = roundtrip(game); move(restored, "f8", "f2");
        assertEquals(ally, restored.getPieceState(pos("f2")).id()); assertEquals(PieceState.Status.CAPTURED, restored.getPieceState(pudge).status());
        assertEquals(1, restored.getWhitePlayer().getAttackedFigures().size()); assertEquals(1, restored.getBlackPlayer().getAttackedFigures().size());
        roundtrip(restored); end(restored); roundtrip(restored);
    }
    @Test public void nestedPassengersSurviveLoadingAndTheirSubsequentRelease() throws Exception {
        var board = board("a1", "h8"); put(board, Rook.class, WHITE, "e2"); put(board, Rook.class, WHITE, "g2");
        put(board, Pawn.class, WHITE, "g3"); put(board, Rook.class, BLACK, "f8");
        Game game = Game.fromMobaPosition(new GamePosition(board, WHITE, CastlingRights.none(), null, 0),
                Map.of(pos("e2"), HeroType.PUDGE_ROOK, pos("g2"), HeroType.PUDGE_ROOK), PudgeRules.sandbox(), () -> 0L);
        long inner = game.getPieceState(pos("g2")).id(), pawn = game.getPieceState(pos("g3")).id();
        ability(game, "g2", "g3", Ability.SWALLOW); move(game, "e2", "f2"); end(game);
        move(game, "h8", "h7"); end(game); ability(game, "f2", "g2", Ability.SWALLOW); move(game, "f2", "f3"); end(game);
        Game restored = roundtrip(game); move(restored, "f8", "f3");
        assertEquals(inner, restored.getPieceState(pos("f3")).id()); assertEquals(Long.valueOf(pawn), restored.getPieceState(inner).storedPieceId());
        roundtrip(restored);
    }
    @Test public void readyUltimateAndItsCaptureChargeArePreserved() throws Exception {
        Game game = scenario(PudgeScenario.ULTIMATE_CHARGE); move(game, "e2", "e3"); end(game); move(game, "h8", "g8"); end(game); move(game, "e3", "f3");
        Game restored = roundtrip(game); assertEquals(2, restored.getPieceState(pos("f3")).ultimateCharge());
        ability(restored, "f3", "g4", Ability.SWALLOW); roundtrip(restored);
    }
    @Test public void indirectCheckBeforeEndTurnIsAValidSavedMobaPosition() throws Exception {
        Game game = scenario(PudgeScenario.INDIRECT_CHECK); ability(game, "b2", "e2", Ability.HOOK);
        Game restored = roundtrip(game); assertEquals(WHITE, restored.getCurrentPlayer().color());
        move(restored, "b2", "b3"); roundtrip(restored); end(restored); assertTrue(restored.isInCheck()); roundtrip(restored);
    }
    @Test public void mobaDoublePawnMoveKeepsEnPassantBeforeAndAfterHandingOverTheTurn() throws Exception {
        Game initial = new Game(8, () -> 0L);
        Game game = Game.fromMobaPosition(initial.getPosition(), Map.of(pos("a1"), HeroType.PUDGE_ROOK), PudgeRules.prototype(), () -> 0L);
        move(game, "e2", "e4"); assertEquals(pos("e3"), roundtrip(game).getEnPassantTarget());
        end(game); assertEquals(pos("e3"), roundtrip(game).getEnPassantTarget());
    }
    @Test public void classicSpecialRightsEnPassantAndPromotionRemainPlayableAfterLoading() throws Exception {
        var board = board("a1", "h8"); put(board, Pawn.class, WHITE, "e2"); put(board, Pawn.class, BLACK, "d4");
        Game game = Game.fromPosition(new GamePosition(board, WHITE, CastlingRights.none(), null, 0), () -> 0L);
        move(game, "e2", "e4"); Game restored = roundtrip(game); move(restored, "d4", "e3"); roundtrip(restored);
        board = board("e1", "e8"); put(board, Rook.class, WHITE, "a1"); put(board, Rook.class, WHITE, "h1");
        game = Game.fromPosition(new GamePosition(board, WHITE, new CastlingRights(true, true, false, false), null, 0), () -> 0L);
        restored = roundtrip(game); move(restored, "e1", "g1"); roundtrip(restored);
        board = board("h1", "h8"); put(board, Pawn.class, WHITE, "a7");
        game = Game.fromPosition(new GamePosition(board, WHITE, CastlingRights.none(), null, 0), () -> 0L);
        long id = game.getPieceState(pos("a7")).id();
        assertEquals(Success.SUCCESS, game.moveFigure(game.getCurrentPlayer(), pos("a7"), pos("a8"), Promotion.QUEEN));
        assertEquals(id, roundtrip(game).getPieceState(pos("a8")).id());
    }
    @Test public void repetitionCountersAndDrawOfferPersistAndBothPlayersHistoriesAreRestored() throws Exception {
        Game game = new Game(8, () -> 0L);
        move(game, "g1", "f3"); move(game, "g8", "f6"); move(game, "f3", "g1"); move(game, "f6", "g8");
        assertEquals(Success.SUCCESS, game.offerDraw(game.getWhitePlayer()));
        Game restored = roundtrip(game); assertSame(restored.getWhitePlayer(), restored.getDrawOffer()); assertEquals(2, restored.getRepetitionCount());
        assertEquals(2, restored.getWhitePlayer().getMadeMoves().size()); assertEquals(2, restored.getBlackPlayer().getMadeMoves().size());
        assertEquals(Success.SUCCESS, restored.acceptDraw(restored.getBlackPlayer())); roundtrip(restored);
    }
    @Test public void localClockRestorationPausesOfflineTimeAndRetainsSubsecondAccuracy() throws Exception {
        AtomicLong now = new AtomicLong(); Game game = PudgeScenario.ULTIMATE_CHARGE.createGame(now::get); game.startClock(60);
        now.set(123_456_789L); byte[] bytes = GameSaveFile.encode(game.getSnapshot());
        long remaining = 60_000_000_000L - now.get(); AtomicLong loadTime = new AtomicLong(999_000_000_000L);
        Game restored = Game.fromSnapshot(GameSaveFile.decode(bytes), loadTime::get);
        assertEquals(remaining, restored.getClockSnapshot().whiteNanos()); assertEquals(60_000_000_000L, restored.getClockSnapshot().blackNanos());
        loadTime.addAndGet(50_000_001L); assertEquals(remaining - 50_000_001L, restored.getClockSnapshot().whiteNanos());
    }
    @Test public void finishedGameRemainsFinishedAndItsClockStaysStoppedAfterLoading() throws Exception {
        Game game = scenario(PudgeScenario.DIAGONAL_HOOK); game.startClock(10);
        assertEquals(Success.SUCCESS, game.resign(game.getWhitePlayer())); Game restored = roundtrip(game);
        assertFalse(restored.getClockSnapshot().running()); assertEquals(Termination.RESIGNATION, restored.getOutcome().reason());
        assertEquals(TurnRejection.GAME_OVER, restored.moveFigure(restored.getWhitePlayer(), pos("e2"), pos("e3")));
    }
    @Test public void unicodeFileNamesAndReplacementOfAnExistingSaveWork() throws Exception {
        var path = temporary.newFolder("Сохранения").toPath().resolve("Пудж.moba.xml");
        Game game = scenario(PudgeScenario.ULTIMATE_CHARGE); GameSaveFile.save(path, game); move(game, "e2", "e3"); GameSaveFile.save(path, game);
        Game restored = GameSaveFile.load(path, () -> 0L); assertTrue(restored.getTurnState().moveUsed());
        assertArrayEquals(GameSaveFile.encode(game.getSnapshot()), Files.readAllBytes(path));
        try (var files = Files.list(path.getParent())) { assertEquals(1, files.count()); }
    }
    @Test public void duplicateOrMissingPieceIdsAndOverlappingSquaresAreRejected() throws Exception {
        Game game = scenario(PudgeScenario.COUNTER_CAPTURE);
        rejects(game, doc -> first(doc, "pieces").appendChild(first(doc, "piece").cloneNode(true)));
        rejects(game, doc -> ((Element) doc.getElementsByTagName("piece").item(1)).setAttribute("square", first(doc, "piece").getAttribute("square")));
        ability(game, "e2", "d3", Ability.SWALLOW);
        rejects(game, doc -> {
            NodeList list = doc.getElementsByTagName("piece");
            for (int i = 0; i < list.getLength(); i++) if (((Element) list.item(i)).hasAttribute("storedPieceId"))
                ((Element) list.item(i)).setAttribute("storedPieceId", "99999");
        });
    }
    @Test public void corruptedBudgetsEventSequencesChargesAndScoresAreRejected() throws Exception {
        Game game = scenario(PudgeScenario.ULTIMATE_CHARGE); move(game, "e2", "e3");
        rejects(game, doc -> doc.getDocumentElement().setAttribute("moveUsed", "false"));
        rejects(game, doc -> first(doc, "event").setAttribute("sequence", "8"));
        rejects(game, doc -> first(doc, "whiteCaptures").setAttribute("points", "999"));
        rejects(game, doc -> {
            NodeList list = doc.getElementsByTagName("piece");
            for (int i = 0; i < list.getLength(); i++) if (((Element) list.item(i)).getAttribute("hero").equals("PUDGE_ROOK"))
                ((Element) list.item(i)).setAttribute("ultimateCharge", "3");
        });
    }
    @Test public void malformedUnsupportedAndExternalEntityFilesAreRejected() throws Exception {
        Game game = scenario(PudgeScenario.DIAGONAL_HOOK);
        rejects(game, doc -> doc.getDocumentElement().setAttribute("formatVersion", "99"));
        rejects(game, doc -> first(doc, "piece").setAttribute("type", "java.lang.Runtime"));
        rejects(game, doc -> first(doc, "clock").setAttribute("whiteNanos", "-1"));
        rejects(game, doc -> first(doc, "piece").removeChild(first(doc, "slot")));
        assertThrows(IOException.class, () -> GameSaveFile.decode("<moba-chess".getBytes(StandardCharsets.UTF_8)));
        assertThrows(IOException.class, () -> GameSaveFile.decode(("<!DOCTYPE moba-chess [<!ENTITY x SYSTEM 'file:///missing'>]><moba-chess>&x;</moba-chess>").getBytes(StandardCharsets.UTF_8)));
        assertThrows(IOException.class, () -> GameSaveFile.decode(new byte[GameSaveFile.MAX_BYTES + 1]));
    }
    @Test public void invalidFileDoesNotAlterTheExistingGame() throws Exception {
        Game game = scenario(PudgeScenario.COUNTER_CAPTURE); byte[] before = GameSaveFile.encode(game.getSnapshot());
        var file = temporary.newFile("bad.moba.xml").toPath(); Files.writeString(file, "broken");
        assertThrows(IOException.class, () -> GameSaveFile.load(file));
        assertArrayEquals(before, GameSaveFile.encode(game.getSnapshot()));
    }
    @Test public void orphanSharedAndCyclicPassengersAreRejected() throws Exception {
        var board = board("a1", "h8"); put(board, Rook.class, WHITE, "e2"); put(board, Rook.class, WHITE, "g2"); put(board, Pawn.class, WHITE, "g3");
        Game game = Game.fromMobaPosition(new GamePosition(board, WHITE, CastlingRights.none(), null, 0),
                Map.of(pos("e2"), HeroType.PUDGE_ROOK, pos("g2"), HeroType.PUDGE_ROOK), PudgeRules.sandbox(), () -> 0L);
        ability(game, "g2", "g3", Ability.SWALLOW);
        long pawn = game.getPieceState(pos("g2")).storedPieceId();
        rejects(game, doc -> {
            NodeList list = doc.getElementsByTagName("piece");
            for (int i = 0; i < list.getLength(); i++) ((Element) list.item(i)).removeAttribute("storedPieceId");
        });
        rejects(game, doc -> {
            NodeList list = doc.getElementsByTagName("piece");
            for (int i = 0; i < list.getLength(); i++) if (((Element) list.item(i)).getAttribute("hero").equals("PUDGE_ROOK"))
                ((Element) list.item(i)).setAttribute("storedPieceId", String.valueOf(pawn));
        });
        rejects(game, doc -> {
            var heroes = new ArrayList<Element>(); NodeList list = doc.getElementsByTagName("piece");
            for (int i = 0; i < list.getLength(); i++) if (((Element) list.item(i)).getAttribute("hero").equals("PUDGE_ROOK")) heroes.add((Element) list.item(i));
            for (int i = list.getLength() - 1; i >= 0; i--) if (((Element) list.item(i)).getAttribute("id").equals(String.valueOf(pawn)))
                list.item(i).getParentNode().removeChild(list.item(i));
            for (int i = 0; i < 2; i++) {
                heroes.get(i).removeAttribute("square"); heroes.get(i).setAttribute("status", "SWALLOWED");
                heroes.get(i).setAttribute("storedPieceId", heroes.get(1 - i).getAttribute("id"));
            }
            // The cycle must be rejected even though neither swallowed rook occupies the board.
        });
    }
    @Test public void fakeClassicOutcomesAndUnsupportedRootSectionsAreRejected() throws Exception {
        Game game = new Game(8, () -> 0L);
        rejects(game, doc -> { first(doc, "outcome").setAttribute("reason", "CHECKMATE"); first(doc, "outcome").setAttribute("winner", "BLACK"); });
        rejects(game, doc -> doc.getDocumentElement().appendChild(doc.createElement("futureState")));
    }
}

package game.persistence;

import game.core.Game;
import game.heroes.Ability;
import game.moves.MoveResult;
import game.scenarios.LumeshardScenario;
import game.state.SquareColor;
import org.junit.Test;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import static game.core.Positions.pos;
import static org.junit.Assert.*;

public class LumeshardSaveTest {
    private Game copy(Game game) throws Exception {
        byte[] bytes = GameSaveFile.encode(game.getSnapshot());
        assertTrue(new String(bytes, StandardCharsets.UTF_8).contains("formatVersion=\"3\""));
        Game restored = Game.fromSnapshot(GameSaveFile.decode(bytes), () -> 0L);
        assertArrayEquals(bytes, GameSaveFile.encode(restored.getSnapshot())); return restored;
    }
    private void reject(Game game, Consumer<Document> mutation) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(GameSaveFile.encode(game.getSnapshot())));
        mutation.accept(doc); var output = new ByteArrayOutputStream();
        TransformerFactory.newInstance().newTransformer().transform(new DOMSource(doc), new StreamResult(output));
        assertThrows(IOException.class, () -> GameSaveFile.decode(output.toByteArray()));
    }
    @Test public void allPreparedScenariosRestartAndRoundtripWithTheirActualStartingState() throws Exception {
        for (LumeshardScenario scenario : LumeshardScenario.values()) {
            Game initial = scenario.createGame(() -> 0L); Game restarted = scenario.createGame(() -> 0L);
            assertArrayEquals(GameSaveFile.encode(initial.getSnapshot()), GameSaveFile.encode(restarted.getSnapshot()));
            assertFalse(initial.getClockSnapshot().enabled()); assertFalse(initial.getTurnState().moveUsed());
            copy(initial);
        }
    }
    @Test public void blindEnemyAndLaserCooldownRemainEffectiveAfterLoadingThenExpireNormally() throws Exception {
        Game game = LumeshardScenario.LASER.createGame(() -> 0L);
        assertEquals(MoveResult.SUCCESS, game.useAbility(game.getCurrentPlayer(), pos("d2"), pos("g5"), Ability.LASER));
        assertEquals(MoveResult.SUCCESS, game.moveFigure(game.getCurrentPlayer(), pos("d2"), pos("c1")));
        assertEquals(MoveResult.SUCCESS, game.endTurn(game.getCurrentPlayer())); game = copy(game);
        assertTrue(game.getPieceState(pos("g5")).isBlinded());
        assertEquals(MoveResult.BLINDED_CANNOT_CAPTURE, game.moveFigure(game.getCurrentPlayer(), pos("g5"), pos("g2")));
        assertEquals(MoveResult.SUCCESS, game.moveFigure(game.getCurrentPlayer(), pos("g5"), pos("g4")));
        assertEquals(MoveResult.SUCCESS, game.endTurn(game.getCurrentPlayer()));
        assertFalse(game.getPieceState(pos("g4")).isBlinded()); assertEquals(2, game.getPieceState(pos("c1")).cooldown()); copy(game);
    }
    @Test public void invalidNewFieldsAndDowngradedExtendedFilesAreRejected() throws Exception {
        Game game = LumeshardScenario.TELEPORT.createGame(() -> 0L);
        reject(game, doc -> doc.getDocumentElement().setAttribute("formatVersion", "1"));
        reject(game, doc -> {
            NodeList pieces = doc.getElementsByTagName("piece");
            for (int i = 0; i < pieces.getLength(); i++) ((Element) pieces.item(i)).removeAttribute("startingColor");
        });
        reject(game, doc -> ((Element) doc.getElementsByTagName("piece").item(0)).setAttribute("blindTurns", "-1"));
        reject(game, doc -> ((Element) doc.getElementsByTagName("lumeshardRules").item(0)).setAttribute("blindnessTurns", "0"));
    }
}

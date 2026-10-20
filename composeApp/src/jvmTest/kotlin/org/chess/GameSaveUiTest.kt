package org.chess

import game.heroes.pudge.PudgeAbility

import game.moves.Success
import game.core.Game
import game.heroes.Abilities
import game.persistence.GameSaveFile
import game.scenarios.PudgeScenario

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.use
import org.junit.Test
import org.junit.Rule
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder
import model.Position
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities

class GameSaveUiTest {
    @get:Rule val temporary = TemporaryFolder()
    @Test fun renderFileControlsAndLoadEntryPoint() {
        SwingUtilities.invokeAndWait {
            ImageComposeScene(1200, 800) { MaterialTheme { ChessApp(Game(8), GameFilePicker { null }) } }.use { save(it, "save-menu") }
            val game = PudgeScenario.ULTIMATE_CHARGE.createGame()
            ImageComposeScene(1200, 800) {
                MaterialTheme { Surface(Modifier.fillMaxSize()) { ChessBoard(game, onSave = {}, onLoad = {}) } }
            }.use { save(it, "save-controls") }
        }
    }
    @Test fun loadFromMenuAndSaveTheSameMidTurnGameThroughTheRealApp() {
        val source = temporary.newFile("source.moba.xml").toPath()
        val output = source.resolveSibling("result.moba.xml")
        val broken = temporary.newFile("broken.moba.xml").toPath()
        Files.writeString(broken, "broken")
        val game = PudgeScenario.COUNTER_CAPTURE.createGame()
        assertEquals(Success.SUCCESS, game.useAbility(game.currentPlayer, Position(1, 4), Position(2, 3), PudgeAbility.SWALLOW))
        assertEquals(Success.SUCCESS, game.moveFigure(game.currentPlayer, Position(1, 4), Position(1, 5)))
        GameSaveFile.save(source, game)
        var loads = 0
        var saves = 0
        val picker = GameFilePicker { saving ->
            if (saving) { saves++; if (saves == 1) null else output }
            else { loads++; when (loads) { 1 -> null; 2 -> broken; else -> source } }
        }
        SwingUtilities.invokeAndWait {
            ImageComposeScene(1200, 800) {
                MaterialTheme { Surface(Modifier.fillMaxSize()) { ChessApp(Game(8), picker) } }
            }.use { scene ->
                scene.render().close()
                click(scene, 600f, 526f) // Load in the inspected main menu; cancel first.
                assertEquals(1, loads)
                click(scene, 600f, 526f) // An invalid file leaves the menu usable.
                assertEquals(2, loads); save(scene, "save-load-error")
                click(scene, 600f, 526f) // Load the actual mid-turn game.
                assertEquals(3, loads); save(scene, "save-loaded")
                click(scene, 688f, 744f) // Save, observed at the bottom of the right panel.
                assertEquals(1, saves); assertFalse(Files.exists(output))
                click(scene, 688f, 744f)
                assertEquals(2, saves); assertTrue(Files.exists(output))
                assertArrayEquals(Files.readAllBytes(source), Files.readAllBytes(output))
                val restored = GameSaveFile.load(output)
                assertTrue(restored.turnState.moveUsed()); assertTrue(restored.turnState.abilityUsed())
                assertNotNull(restored.getPieceState(Position(1, 5)).storedPieceId())
                save(scene, "save-saved")
            }
        }
    }
    private fun click(scene: ImageComposeScene, x: Float, y: Float) {
        scene.sendPointerEvent(PointerEventType.Press, Offset(x, y), button = PointerButton.Primary)
        scene.sendPointerEvent(PointerEventType.Release, Offset(x, y), button = PointerButton.Primary)
        scene.render().close()
    }
    private fun save(scene: ImageComposeScene, name: String) {
        val destination = Path.of("build", "ui-previews"); Files.createDirectories(destination)
        scene.render().use { image ->
            image.encodeToData()!!.use { data -> Files.write(destination.resolve("$name.png"), data.bytes) }
        }
    }
}

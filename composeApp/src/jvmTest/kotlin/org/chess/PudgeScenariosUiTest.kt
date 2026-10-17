package org.chess

import game.scenarios.PudgeScenario

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.use
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerButton
import model.Position
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities

class PudgeScenariosUiTest {
    @Test fun renderScenarioMenuAndAllFiveTeachingPositions() {
        SwingUtilities.invokeAndWait {
            var selected: PudgeScenario? = null
            ImageComposeScene(1200, 800) { MaterialTheme { PudgeScenariosMenu({ selected = it }, {}) } }.use { scene ->
                save(scene, "scenarios-menu")
                click(scene, 440f, 144f) // First scenario button in the inspected menu.
                assertEquals(PudgeScenario.DIAGONAL_HOOK, selected)
            }
            for (scenario in PudgeScenario.values()) {
                val game = scenario.createGame()
                ImageComposeScene(1200, 800) {
                    MaterialTheme { Surface(Modifier.fillMaxSize()) { ChessBoard(game, scenario) } }
                }.use { scene -> save(scene, "scenario-${scenario.name.lowercase()}") }
            }
        }
    }
    @Test fun restartWorksDuringAnUnfinishedTurnAndReturnButtonIsReachable() {
        SwingUtilities.invokeAndWait {
            val scenario = PudgeScenario.ULTIMATE_CHARGE
            var game by mutableStateOf(scenario.createGame())
            val original = game
            var restarts = 0
            var returns = 0
            ImageComposeScene(1200, 800) {
                MaterialTheme { Surface(Modifier.fillMaxSize()) {
                    key(game) { ChessBoard(game, scenario,
                        onRestart = { game = scenario.createGame(); restarts++ },
                        onNewGame = { returns++ }) }
                } }
            }.use { scene ->
                scene.render().close()
                click(scene, 346f, 522f) // Select e2.
                save(scene, "scenario-ultimate-selected")
                click(scene, 346f, 462f) // Capture the pawn e3, before ending the turn.
                assertTrue(game.turnState.moveUsed())
                assertEquals(1, game.getPieceState(Position(2, 4)).ultimateCharge())
                click(scene, 676f, 184f) // Restart, as observed above the turn controls.
                assertEquals(1, restarts); assertNotSame(original, game)
                assertTrue(game.moves.isEmpty()); assertTrue(game.events.isEmpty())
                assertFalse(game.turnState.moveUsed())
                assertEquals(0, game.getPieceState(Position(1, 4)).ultimateCharge())
                save(scene, "scenario-restarted")
                click(scene, 776f, 184f)
                assertEquals(1, returns)
            }
        }
    }
    private fun click(scene: ImageComposeScene, x: Float, y: Float) {
        scene.sendPointerEvent(PointerEventType.Press, Offset(x, y), button = PointerButton.Primary)
        scene.sendPointerEvent(PointerEventType.Release, Offset(x, y), button = PointerButton.Primary)
        scene.render().close()
    }
    private fun save(scene: ImageComposeScene, name: String) {
        val destination = Path.of("build", "ui-previews")
        Files.createDirectories(destination)
        scene.render().use { image ->
            image.encodeToData()!!.use { data -> Files.write(destination.resolve("$name.png"), data.bytes) }
        }
    }
}

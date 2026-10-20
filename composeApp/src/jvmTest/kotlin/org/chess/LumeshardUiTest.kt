package org.chess

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.use
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerButton
import game.scenarios.LumeshardScenario
import game.heroes.Abilities
import model.Position
import model.FigureColor
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities

class LumeshardUiTest {
    @Test fun laserAndBlindMovementRestrictionsWorkThroughTheRealInterface() {
        SwingUtilities.invokeAndWait {
            val game = LumeshardScenario.LASER.createGame()
            ImageComposeScene(1200, 800) {
                MaterialTheme { Surface(Modifier.fillMaxSize()) { ChessBoard(game, LumeshardScenario.LASER) } }
            }.use { scene ->
                scene.render().close()
                click(scene, 286f, 522f) // Lumeshard d2.
                click(scene, 672f, 504f) // Laser in the inspected selected panel.
                click(scene, 466f, 342f) // Enemy rook g5, behind the pawn e3.
                assertTrue(game.getPieceState(Position(4, 6)).isBlinded)
                assertTrue(game.turnState.abilityUsed()); assertFalse(game.turnState.moveUsed())
                save(scene, "lumeshard-laser-applied")
                click(scene, 226f, 582f) // Ordinary d2-c1.
                click(scene, 704f, 280f) // End turn.
                assertEquals(FigureColor.BLACK, game.currentPlayer.color())
                click(scene, 466f, 342f) // Select the blinded rook.
                click(scene, 466f, 522f) // White knight g2 must not be a capture destination.
                assertFalse(game.turnState.moveUsed())
                click(scene, 466f, 342f)
                click(scene, 466f, 402f) // Quiet g5-g4 remains available.
                assertTrue(game.turnState.moveUsed()); assertTrue(game.getPieceState(Position(3, 6)).isBlinded)
                click(scene, 704f, 280f)
                assertFalse(game.getPieceState(Position(3, 6)).isBlinded)
            }
        }
    }
    @Test fun teleportSelectsAnEmptyCellAndDoesNotSatisfyTheMandatoryMovement() {
        SwingUtilities.invokeAndWait {
            val game = LumeshardScenario.TELEPORT.createGame()
            val id = game.getPieceState(Position(3, 5)).id()
            ImageComposeScene(1200, 800) {
                MaterialTheme { Surface(Modifier.fillMaxSize()) { ChessBoard(game, LumeshardScenario.TELEPORT) } }
            }.use { scene ->
                scene.render().close(); click(scene, 406f, 402f) // f4.
                click(scene, 770f, 504f) // Teleport.
                save(scene, "lumeshard-teleport-targets")
                click(scene, 106f, 462f) // Empty dark a3.
                assertEquals(id, game.getPieceState(Position(2, 0)).id())
                assertEquals(0, game.getPieceState(id).ultimateCharge())
                assertTrue(game.turnState.abilityUsed()); assertFalse(game.turnState.moveUsed())
                click(scene, 704f, 280f) // Disabled until an ordinary movement.
                assertEquals(FigureColor.WHITE, game.currentPlayer.color())
                click(scene, 166f, 402f) // Same Lumeshard a3-b4.
                assertTrue(game.turnState.moveUsed())
                click(scene, 704f, 280f)
                assertEquals(FigureColor.BLACK, game.currentPlayer.color())
            }
        }
    }
    @Test fun renderAllLumeshardScenariosAndTheirSelectedPanels() {
        SwingUtilities.invokeAndWait {
            ImageComposeScene(1200, 800) {
                MaterialTheme { TrainingScenariosMenu("Тестовые позиции Lumeshard", LumeshardScenario.values().toList(), {}, {}) }
            }.use { save(it, "lumeshard-menu") }
            for (scenario in LumeshardScenario.values()) {
                val game = scenario.createGame()
                ImageComposeScene(1200, 800) {
                    MaterialTheme { Surface(Modifier.fillMaxSize()) { ChessBoard(game, scenario) } }
                }.use { scene ->
                    save(scene, "lumeshard-${scenario.name.lowercase()}")
                    click(scene, if (scenario == LumeshardScenario.LASER) 286f else 406f,
                        if (scenario == LumeshardScenario.LASER) 522f else 402f)
                    save(scene, "lumeshard-${scenario.name.lowercase()}-selected")
                }
            }
        }
    }
    private fun click(scene: ImageComposeScene, x: Float, y: Float) {
        scene.sendPointerEvent(PointerEventType.Press, Offset(x, y), button = PointerButton.Primary)
        scene.sendPointerEvent(PointerEventType.Release, Offset(x, y), button = PointerButton.Primary)
        scene.render().close()
    }
    private fun save(scene: ImageComposeScene, name: String) {
        val folder = Path.of("build", "ui-previews"); Files.createDirectories(folder)
        scene.render().use { image -> image.encodeToData()!!.use { data -> Files.write(folder.resolve("$name.png"), data.bytes) } }
    }
}

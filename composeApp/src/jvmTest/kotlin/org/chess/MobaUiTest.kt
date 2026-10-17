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
import game.core.Game
import game.heroes.pudge.PudgeRules
import game.state.PieceState
import model.Position
import model.FigureColor
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities

/** Offscreen rendering uses the real composables and requires no visible app window. */
class MobaUiTest {
    @Test fun swallowMoveAndEndTurnThroughRealInterface() {
        SwingUtilities.invokeAndWait {
            val destination = Path.of("build", "ui-previews")
            Files.createDirectories(destination)
            ImageComposeScene(1200, 800) {
                MaterialTheme { MainMenu({}) }
            }.use { scene ->
                scene.render().use { image ->
                    image.encodeToData()!!.use { data -> Files.write(destination.resolve("menu.png"), data.bytes) }
                }
            }
            val game = Game.mobaPrototype(PudgeRules(0, 0, true, false), 600)
            val pudgeId = game.getPieceState(Position(0, 0)).id()
            val allyId = game.getPieceState(Position(1, 0)).id()
            ImageComposeScene(1200, 800) {
                MaterialTheme { Surface(Modifier.fillMaxSize()) { ChessBoard(game) } }
            }.use { scene ->
                scene.render().use { image ->
                    image.encodeToData()!!.use { data -> Files.write(destination.resolve("moba.png"), data.bytes) }
                }
                click(scene, 704f, 112f) // Disabled end-turn button before any movement.
                assertFalse(game.turnState.moveUsed())
                assertTrue(game.events.isEmpty())
                click(scene, 106f, 582f) // a1, observed in the rendered board.
                scene.render().use { image ->
                    image.encodeToData()!!.use { data -> Files.write(destination.resolve("moba-selected.png"), data.bytes) }
                }
                click(scene, 766f, 312f) // "Проглотить", observed in the selected-Pudge panel.
                click(scene, 106f, 522f) // a2: swallow the allied pawn.
                assertEquals(PieceState.Status.SWALLOWED, game.getPieceState(allyId).status())
                assertTrue(game.turnState.abilityUsed())
                assertFalse(game.turnState.moveUsed())
                click(scene, 106f, 522f) // Move that same Pudge from a1 to the newly empty a2.
                assertEquals(pudgeId, game.getPieceState(Position(1, 0)).id())
                assertTrue(game.turnState.moveUsed())
                assertEquals(FigureColor.WHITE, game.clockSnapshot.activeColor())
                click(scene, 704f, 112f) // Explicitly hand over the turn.
                assertEquals(FigureColor.BLACK, game.currentPlayer.color())
                assertEquals(FigureColor.BLACK, game.clockSnapshot.activeColor())
                assertFalse(game.turnState.moveUsed())
                scene.render().use { image ->
                    image.encodeToData()!!.use { data -> Files.write(destination.resolve("moba-after-turn.png"), data.bytes) }
                }
            }
        }
    }
    private fun click(scene: ImageComposeScene, x: Float, y: Float) {
        scene.sendPointerEvent(PointerEventType.Press, Offset(x, y), button = PointerButton.Primary)
        scene.sendPointerEvent(PointerEventType.Release, Offset(x, y), button = PointerButton.Primary)
        scene.render().close()
    }
}

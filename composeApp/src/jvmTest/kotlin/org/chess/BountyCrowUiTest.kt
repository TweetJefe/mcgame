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
import game.scenarios.BountyCrowScenario
import model.FigureColor
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities

class BountyCrowUiTest {
    @Test fun bountyAndPassiveRewardsWorkThroughBoardClicks() {
        SwingUtilities.invokeAndWait {
            val game = BountyCrowScenario.PASSIVE.createGame()
            ImageComposeScene(1200, 800) {
                MaterialTheme { Surface(Modifier.fillMaxSize()) { ChessBoard(game, BountyCrowScenario.PASSIVE) } }
            }.use { scene ->
                scene.render().close()
                click(scene, 286f, 402f) // d4.
                click(scene, 672f, 480f) // Bounty, inspected in the selected Crow panel.
                click(scene, 346f, 342f) // Mark pawn e5.
                assertEquals(1, game.economy.marks().size)
                save(scene, "crow-mark-applied")
                click(scene, 346f, 342f) // Same Crow captures e5.
                assertEquals(6L, game.economy.whiteHalves())
                assertTrue(game.economy.marks().isEmpty())
                click(scene, 704f, 256f)
                assertEquals(FigureColor.BLACK, game.currentPlayer.color())
            }
        }
    }
    @Test fun targetlessUltimateTransfersGoldButDoesNotReplaceMovement() {
        SwingUtilities.invokeAndWait {
            val game = BountyCrowScenario.STEAL.createGame()
            ImageComposeScene(1200, 800) {
                MaterialTheme { Surface(Modifier.fillMaxSize()) { ChessBoard(game, BountyCrowScenario.STEAL) } }
            }.use { scene ->
                scene.render().close(); click(scene, 406f, 282f) // f6.
                click(scene, 780f, 504f) // Steal applies directly, without selecting a square.
                assertEquals(10L, game.economy.whiteHalves()); assertEquals(2L, game.economy.blackHalves())
                assertTrue(game.turnState.abilityUsed()); assertFalse(game.turnState.moveUsed())
                click(scene, 704f, 280f); assertEquals(FigureColor.WHITE, game.currentPlayer.color())
                click(scene, 406f, 222f) // Ordinary f6-f7.
                click(scene, 704f, 280f); assertEquals(FigureColor.BLACK, game.currentPlayer.color())
                save(scene, "crow-theft-completed")
            }
        }
    }
    @Test fun renderCrowExercisesAndSelectedHeroPanels() {
        SwingUtilities.invokeAndWait {
            ImageComposeScene(1200, 800) {
                MaterialTheme { TrainingScenariosMenu("Тестовые позиции Bounty Crow", BountyCrowScenario.values().toList(), {}, {}) }
            }.use { save(it, "crow-menu") }
            for (scenario in BountyCrowScenario.values()) {
                val game = scenario.createGame()
                ImageComposeScene(1200, 800) {
                    MaterialTheme { Surface(Modifier.fillMaxSize()) { ChessBoard(game, scenario) } }
                }.use { scene ->
                    save(scene, "crow-${scenario.name.lowercase()}")
                    click(scene, if (scenario == BountyCrowScenario.BOUNTY) 166f else if (scenario == BountyCrowScenario.PASSIVE) 286f else 406f,
                        if (scenario == BountyCrowScenario.BOUNTY) 522f else if (scenario == BountyCrowScenario.PASSIVE) 402f else 282f)
                    save(scene, "crow-${scenario.name.lowercase()}-selected")
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

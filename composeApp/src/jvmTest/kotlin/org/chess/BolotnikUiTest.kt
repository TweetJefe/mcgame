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
import game.scenarios.BolotnikScenario
import game.state.PieceState
import game.heroes.HeroType
import model.Position
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities

class BolotnikUiTest {
    @Test fun mudUnderAnAllyTriggersWhenAnEnemyCapturesItThroughTheInterface() {
        SwingUtilities.invokeAndWait {
            val game=BolotnikScenario.MUD.createGame()
            ImageComposeScene(1200,800) { MaterialTheme { Surface(Modifier.fillMaxSize()) { ChessBoard(game,BolotnikScenario.MUD) } } }.use { scene ->
                scene.render().close();click(scene,286f,402f);click(scene,672f,480f);click(scene,406f,402f)
                assertEquals(1,game.terrain.cells().size);save(scene,"bolotnik-mud-applied")
                click(scene,106f,582f);click(scene,166f,582f);click(scene,704f,256f)
                click(scene,406f,162f);click(scene,406f,402f)
                assertEquals(HeroType.BOLOTNIK,game.getPieceState(Position(3,5)).hero())
                assertTrue(game.terrain.cells().isEmpty());assertEquals(10L,game.economy.whiteHalves());assertEquals(2L,game.economy.blackHalves())
                save(scene,"bolotnik-mud-triggered")
            }
        }
    }
    @Test fun ultimateHidesTheHeroAndEnemyEntryReleasesIt() {
        SwingUtilities.invokeAndWait {
            val game=BolotnikScenario.AMBUSH.createGame();val id=game.getPieceState(Position(4,5)).id()
            ImageComposeScene(1200,800) { MaterialTheme { Surface(Modifier.fillMaxSize()) { ChessBoard(game,BolotnikScenario.AMBUSH) } } }.use { scene ->
                scene.render().close();click(scene,406f,342f);click(scene,750f,504f)
                assertEquals(PieceState.Status.HIDDEN_BOG,game.getPieceState(id).status());assertEquals(3,game.terrain.cells().size)
                save(scene,"bolotnik-hidden")
                click(scene,106f,582f);click(scene,166f,582f);click(scene,704f,280f)
                click(scene,346f,162f);click(scene,346f,342f)
                assertEquals(id,game.getPieceState(Position(4,4)).id());assertTrue(game.terrain.cells().isEmpty())
                save(scene,"bolotnik-released")
            }
        }
    }
    @Test fun immediateUltimateCaptureUsesTheLeftEnemyWithoutChoosingATargetCell() {
        SwingUtilities.invokeAndWait {
            val game=BolotnikScenario.LEFTMOST.createGame();val id=game.getPieceState(Position(4,5)).id()
            ImageComposeScene(1200,800) { MaterialTheme { Surface(Modifier.fillMaxSize()) { ChessBoard(game,BolotnikScenario.LEFTMOST) } } }.use { scene ->
                scene.render().close();click(scene,406f,342f);click(scene,750f,504f)
                assertEquals(id,game.getPieceState(Position(4,4)).id());assertEquals(model.figures.Pawn::class.java,game.getPieceState(Position(4,6)).type())
                assertTrue(game.terrain.cells().isEmpty());assertFalse(game.turnState.moveUsed());assertTrue(game.turnState.abilityUsed())
                save(scene,"bolotnik-leftmost-resolved")
            }
        }
    }
    @Test fun renderAllExercisesAndSelectedPanels() {
        SwingUtilities.invokeAndWait {
            ImageComposeScene(1200,800) { MaterialTheme { TrainingScenariosMenu("Тестовые позиции Bolotnik",BolotnikScenario.values().toList(),{},{}) } }.use { save(it,"bolotnik-menu") }
            for(scenario in BolotnikScenario.values()) {
                val game=scenario.createGame()
                ImageComposeScene(1200,800) { MaterialTheme { Surface(Modifier.fillMaxSize()) { ChessBoard(game,scenario) } } }.use { scene ->
                    save(scene,"bolotnik-${scenario.name.lowercase()}")
                    val charged=scenario==BolotnikScenario.AMBUSH || scenario==BolotnikScenario.LEFTMOST
                    click(scene,if(charged) 406f else 286f,if(charged) 342f else 402f)
                    save(scene,"bolotnik-${scenario.name.lowercase()}-selected")
                }
            }
        }
    }
    private fun click(scene:ImageComposeScene,x:Float,y:Float) {
        scene.sendPointerEvent(PointerEventType.Press,Offset(x,y),button=PointerButton.Primary)
        scene.sendPointerEvent(PointerEventType.Release,Offset(x,y),button=PointerButton.Primary)
        scene.render().close()
    }
    private fun save(scene:ImageComposeScene,name:String) {
        val folder=Path.of("build","ui-previews");Files.createDirectories(folder)
        scene.render().use { image -> image.encodeToData()!!.use { data -> Files.write(folder.resolve("$name.png"),data.bytes) } }
    }
}

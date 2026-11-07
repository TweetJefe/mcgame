package org.chess

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.use
import game.core.Positions.pos
import game.draft.Draft
import game.heroes.HeroType
import game.heroes.strix.StrixHeroTest
import model.FigureColor
import model.figures.Knight
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities

class StrixUiTest {
    @Test fun rewindButtonUsesHistoricalDestination() {
        SwingUtilities.invokeAndWait {
            val game = StrixHeroTest.rewindReady()
            ImageComposeScene(1200, 800) { MaterialTheme { Surface(Modifier.fillMaxSize()) { ChessBoard(game) } } }.use { scene ->
                scene.render().close()
                click(scene, 226f, 522f)
                save(scene, "strix-controls")
                click(scene, 725f, 340f)
                click(scene, 106f, 462f)
                assertEquals(Knight::class.java, game.getPieceState(pos("b1")).type())
                assertEquals(4, game.getPieceState(pos("c2")).cooldown())
                assertFalse(game.turnState.moveUsed())
                save(scene, "strix-rewind")
            }
        }
    }
    @Test fun ultimateButtonAndEndTurnGiveTheSamePlayerAnotherTurn() {
        SwingUtilities.invokeAndWait {
            val game = StrixHeroTest.ready()
            ImageComposeScene(1200, 800) { MaterialTheme { Surface(Modifier.fillMaxSize()) { ChessBoard(game) } } }.use { scene ->
                scene.render().close()
                click(scene, 226f, 522f)
                click(scene, 900f, 340f)
                assertTrue(game.turnState.abilityUsed())
                click(scene, 166f, 582f)
                click(scene, 106f, 462f)
                click(scene, 710f, 110f)
                assertEquals(FigureColor.WHITE, game.currentPlayer.color())
                assertEquals(3L, game.turnState.number())
                assertFalse(game.turnState.moveUsed())
                save(scene, "strix-extra-turn")
            }
        }
    }
    @Test fun fifteenthHeroCanBePickedIntoAPawnHome() {
        SwingUtilities.invokeAndWait {
            val roster = game.draft.HeroCatalog.heroes(10) + listOf(HeroType.SUN_WUKONG, HeroType.STRIX)
            val draft = Draft(game.draft.DraftRules.prototype(), game.draft.DraftCatalog.restore(roster, game.heroes.HeroDefinitions.all()))
            draft.ban(FigureColor.WHITE, HeroType.PUDGE)
            draft.ban(FigureColor.BLACK, HeroType.IZBA)
            ImageComposeScene(1200, 800) { MaterialTheme { DraftScreen(draft, {}, {}) } }.use { scene ->
                scene.render().close()
                scene.scrollDraftToLastHero()
                save(scene, "strix-draft")
                click(scene, 150f, 580f)
                click(scene, 912f, 399f)
                click(scene, 100f, 677f)
                assertEquals(HeroType.STRIX, draft.snapshot.assignments()[pos("c2")])
                save(scene, "strix-picked")
            }
        }
    }
    private fun click(scene: ImageComposeScene, x: Float, y: Float) {
        scene.sendPointerEvent(PointerEventType.Press, Offset(x, y), button = PointerButton.Primary)
        scene.sendPointerEvent(PointerEventType.Release, Offset(x, y), button = PointerButton.Primary)
        scene.render().close()
    }
    private fun save(scene: ImageComposeScene, name: String) {
        val folder = Path.of("build", "ui-previews")
        Files.createDirectories(folder)
        scene.render().use { it.encodeToData()!!.use { data -> Files.write(folder.resolve("$name.png"), data.bytes) } }
    }
}
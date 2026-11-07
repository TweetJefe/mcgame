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
import game.heroes.sunwukong.SunWukongHeroTest
import model.FigureColor
import model.figures.Pawn
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities

class SunWukongUiTest {
    @Test fun selfCastUsesSharedPanelWithoutSelectingATarget() {
        SwingUtilities.invokeAndWait {
            val game = SunWukongHeroTest.basic()
            ImageComposeScene(1200, 800) {
                MaterialTheme { Surface(Modifier.fillMaxSize()) { ChessBoard(game) } }
            }.use { scene ->
                scene.render().close()
                click(scene, 226f, 522f)
                save(scene, "wukong-controls")
                click(scene, 735f, 340f)
                assertNotEquals(Pawn::class.java, game.getPieceState(pos("c2")).type())
                assertTrue(game.turnState.abilityUsed())
                assertFalse(game.turnState.moveUsed())
                save(scene, "wukong-shift")
            }
        }
    }

    @Test fun ultimateUsesTheSharedTargetSelectionAndCreatesAFullHero() {
        SwingUtilities.invokeAndWait {
            val game = SunWukongHeroTest.ready()
            ImageComposeScene(1200, 800) {
                MaterialTheme { Surface(Modifier.fillMaxSize()) { ChessBoard(game) } }
            }.use { scene ->
                scene.render().close()
                click(scene, 226f, 522f)
                click(scene, 920f, 340f)
                save(scene, "wukong-targets")
                click(scene, 406f, 522f)
                assertEquals(HeroType.SUN_WUKONG, game.getPieceState(pos("f2")).hero())
                assertFalse(game.turnState.moveUsed())
                save(scene, "wukong-clone")
            }
        }
    }

    @Test fun heroCanBeScrolledAndPickedIntoAnOrdinaryPawnHome() {
        SwingUtilities.invokeAndWait {
            val draft = Draft(game.draft.DraftRules(1, 6), game.draft.DraftCatalog.restore(
                game.draft.HeroCatalog.heroes(10) + HeroType.SUN_WUKONG, game.heroes.HeroDefinitions.all()))
            draft.ban(FigureColor.WHITE, HeroType.PUDGE)
            draft.ban(FigureColor.BLACK, HeroType.IZBA)
            ImageComposeScene(1200, 800) { MaterialTheme { DraftScreen(draft, {}, {}) } }.use { scene ->
                scene.render().close()
                scene.scrollDraftToLastHero()
                save(scene, "wukong-draft")
                click(scene, 150f, 580f)
                click(scene, 912f, 399f)
                click(scene, 100f, 677f)
                assertEquals(HeroType.SUN_WUKONG, draft.snapshot.assignments()[pos("c2")])
                save(scene, "wukong-picked")
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
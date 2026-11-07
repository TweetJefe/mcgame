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
import game.heroes.leshy.LeshyHeroTest
import game.heroes.leshy.LeshyMechanic
import model.FigureColor
import model.figures.Egg
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities

class LeshyUiTest {
    @Test fun vineButtonDrawsTheLaneAndKeepsTheMoveMandatory() {
        SwingUtilities.invokeAndWait {
            val game = LeshyHeroTest.basic()
            ImageComposeScene(1200, 800) { MaterialTheme { Surface(Modifier.fillMaxSize()) { ChessBoard(game) } } }.use { scene ->
                scene.render().close()
                val emptyCellBefore = pixel(scene, 245, 350)
                click(scene, 226f, 522f)
                save(scene, "leshy-controls")
                click(scene, 675f, 340f)
                assertEquals(3, game.snapshot.mechanics().get(LeshyMechanic.KEY).cells().size)
                assertFalse(game.turnState.moveUsed())
                assertNotEquals("Лоза должна быть видна на пустой клетке c5", emptyCellBefore, pixel(scene, 245, 350))
                save(scene, "leshy-vine")
            }
        }
    }
    @Test fun forestButtonDrawsBothTrees() {
        SwingUtilities.invokeAndWait {
            val game = LeshyHeroTest.forestReady()
            ImageComposeScene(1200, 800) { MaterialTheme { Surface(Modifier.fillMaxSize()) { ChessBoard(game) } } }.use { scene ->
                scene.render().close()
                click(scene, 226f, 522f)
                click(scene, 850f, 340f)
                assertEquals(2, game.snapshot.mechanics().get(LeshyMechanic.KEY).trees().size)
                save(scene, "leshy-trees")
            }
        }
    }
    @Test fun leshyCanBePickedIntoAPawnHome() {
        SwingUtilities.invokeAndWait {
            val heroes = java.util.ArrayList(game.draft.HeroCatalog.heroes(10))
            heroes.addAll(listOf(HeroType.SUN_WUKONG, HeroType.STRIX, HeroType.PHOENIX, HeroType.LESHY))
            val draft = Draft(game.draft.DraftRules.prototype(),
                game.draft.DraftCatalog.restore(heroes, game.heroes.HeroDefinitions.all()))
            draft.ban(FigureColor.WHITE, HeroType.PUDGE)
            draft.ban(FigureColor.BLACK, HeroType.IZBA)
            ImageComposeScene(1200, 800) { MaterialTheme { DraftScreen(draft, {}, {}) } }.use { scene ->
                scene.render().close()
                scene.scrollDraftToLastHero()
                click(scene, 150f, 580f)
                click(scene, 912f, 399f)
                click(scene, 100f, 677f)
                assertEquals(HeroType.LESHY, draft.snapshot.assignments()[pos("c2")])
                save(scene, "leshy-picked")
            }
        }
    }
    private fun pixel(scene: ImageComposeScene, x: Int, y: Int): Int = scene.render().use { picture ->
        picture.encodeToData()!!.use { data ->
            javax.imageio.ImageIO.read(java.io.ByteArrayInputStream(data.bytes)).getRGB(x, y)
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

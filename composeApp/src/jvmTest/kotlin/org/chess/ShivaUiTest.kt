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
import game.creation.CreationMechanic
import game.draft.Draft
import game.heroes.HeroType
import game.heroes.shiva.ShivaHeroTest
import model.FigureColor
import model.figures.Ward
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities

class ShivaUiTest {
    @Test fun lotusButtonSelectsEmptySquareAndDrawsTheFlower() {
        SwingUtilities.invokeAndWait {
            val game = ShivaHeroTest.basic()
            ImageComposeScene(1200, 800) { MaterialTheme { Surface(Modifier.fillMaxSize()) { ChessBoard(game) } } }.use { scene ->
                scene.render().close()
                val before = pixel(scene, 226, 462)
                click(scene, 226f, 522f)
                save(scene, "shiva-controls")
                click(scene, 675f, 340f)
                click(scene, 226f, 462f)
                assertEquals(Ward::class.java, game.getPieceState(pos("c3")).type())
                assertFalse(game.turnState.moveUsed())
                assertNotEquals(before, pixel(scene, 226, 462))
                save(scene, "shiva-lotus")
            }
        }
    }
    @Test fun resurrectionButtonCreatesNewLives() {
        SwingUtilities.invokeAndWait {
            val game = ShivaHeroTest.revivalReady()
            ImageComposeScene(1200, 800) { MaterialTheme { Surface(Modifier.fillMaxSize()) { ChessBoard(game) } } }.use { scene ->
                scene.render().close()
                click(scene, 226f, 462f)
                click(scene, 850f, 340f)
                assertEquals(2, game.snapshot.mechanics().get(CreationMechanic.KEY).births().size)
                save(scene, "shiva-resurrection")
            }
        }
    }
    @Test fun shivaCanBePickedIntoARookHome() {
        SwingUtilities.invokeAndWait {
            val draft = Draft()
            draft.ban(FigureColor.WHITE, HeroType.PUDGE)
            draft.ban(FigureColor.BLACK, HeroType.IZBA)
            ImageComposeScene(1200, 800) { MaterialTheme { DraftScreen(draft, {}, {}) } }.use { scene ->
                scene.render().close()
                scene.scrollDraftToLastHero()
                click(scene, 150f, 580f)
                click(scene, 840f, 435f)
                click(scene, 100f, 677f)
                assertEquals(HeroType.SHIVA, draft.snapshot.assignments()[pos("a1")])
                save(scene, "shiva-picked")
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

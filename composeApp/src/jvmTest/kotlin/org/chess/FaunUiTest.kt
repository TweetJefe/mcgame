package org.chess
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.use
import game.heroes.faun.FaunTest
import game.heroes.faun.FaunView
import game.draft.*
import game.heroes.*
import game.core.Positions.pos
import org.junit.Test
import org.junit.Assert.*
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities
class FaunUiTest {
    @Test fun cloudAndInvisibilityControlsWorkAndEnemyBoardHidesTheKnight() {
        SwingUtilities.invokeAndWait {
            val cloud=FaunTest.basic()
            ImageComposeScene(1200,800){MaterialTheme{Surface(Modifier.fillMaxSize()){ChessBoard(cloud)}}}.use {
                it.render().close();click(it,226f,522f);save(it,"faun-controls")
                click(it,700f,360f);click(it,406f,402f)
                assertTrue(cloud.faun.silenced(pos("f4")));save(it,"faun-cloud")
            }
            val invisible=FaunTest.charged()
            ImageComposeScene(1200,800){MaterialTheme{Surface(Modifier.fillMaxSize()){ChessBoard(invisible)}}}.use {
                it.render().close();click(it,226f,522f);click(it,865f,360f)
                assertFalse(invisible.faun.invisible().isEmpty());save(it,"faun-own-invisible")
                click(it,226f,522f);click(it,286f,402f);click(it,704f,112f)
                assertEquals(model.FigureColor.BLACK,invisible.snapshot.active())
                assertFalse(FaunView.visible(invisible.snapshot,invisible.getPieceState(pos("d4"))));save(it,"faun-enemy-hidden")
            }
        }
    }
    @Test fun seventhHeroCanBePickedOnAnAlternateKnightHome() {
        SwingUtilities.invokeAndWait {
            val draft=Draft(DraftRules(1,6,4));draft.ban(model.FigureColor.WHITE,HeroType.PUDGE);draft.ban(model.FigureColor.BLACK,HeroType.IZBA)
            ImageComposeScene(1200,800){MaterialTheme{DraftScreen(draft,{}, {})}}.use {
                save(it,"faun-catalog");click(it,150f,600f);click(it,1056f,435f);click(it,100f,701f)
                assertEquals(HeroType.FAUN,draft.snapshot.assignments()[pos("g1")]);save(it,"faun-picked")
            }
        }
    }
    private fun click(s:ImageComposeScene,x:Float,y:Float){s.sendPointerEvent(PointerEventType.Press,Offset(x,y),button=PointerButton.Primary);s.sendPointerEvent(PointerEventType.Release,Offset(x,y),button=PointerButton.Primary);s.render().close()}
    private fun save(s:ImageComposeScene,name:String){val d=Path.of("build","ui-previews");Files.createDirectories(d);s.render().use{i->i.encodeToData()!!.use{Files.write(d.resolve("$name.png"),it.bytes)}}}
}

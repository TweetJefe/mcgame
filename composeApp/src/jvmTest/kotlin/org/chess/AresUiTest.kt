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
import game.heroes.ares.AresTest
import game.draft.*
import game.heroes.*
import game.core.Positions.pos
import org.junit.Test
import org.junit.Assert.*
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities
class AresUiTest {
    @Test fun spearDirectionAndStunAreControlledThroughBoardClicks() {
        SwingUtilities.invokeAndWait {
            val game=AresTest.collision()
            ImageComposeScene(1200,800){MaterialTheme{Surface(Modifier.fillMaxSize()){ChessBoard(game)}}}.use {
                it.render().close();click(it,226f,522f);save(it,"ares-controls")
                click(it,710f,360f);save(it,"ares-directions");click(it,406f,522f)
                assertTrue(game.getPieceState(pos("e2")).isStunned());assertTrue(game.getPieceState(pos("f2")).isStunned());assertFalse(game.turnState.moveUsed());save(it,"ares-collision")
            }
        }
    }
    @Test fun eighthHeroCanBeScrolledIntoViewAndPickedWhileButtonsStayVisible() {
        SwingUtilities.invokeAndWait {
            val draft=Draft(DraftRules(1,6,5));draft.ban(model.FigureColor.WHITE,HeroType.PUDGE_ROOK);draft.ban(model.FigureColor.BLACK,HeroType.IZBA_ROOK)
            ImageComposeScene(1200,800){MaterialTheme{DraftScreen(draft,{}, {})}}.use {
                save(it,"ares-catalog")
                it.sendPointerEvent(PointerEventType.Scroll,Offset(300f,500f),scrollDelta=Offset(0f,4f))
                val scrollTime=System.nanoTime();repeat(40){frame->it.render(scrollTime+frame*16_000_000L).close()};save(it,"ares-catalog-scrolled")
                click(it,150f,580f);click(it,1020f,435f);click(it,100f,677f)
                assertEquals(HeroType.ARES_BISHOP,draft.snapshot.assignments()[pos("f1")]);save(it,"ares-picked")
            }
        }
    }
    private fun click(s:ImageComposeScene,x:Float,y:Float){s.sendPointerEvent(PointerEventType.Press,Offset(x,y),button=PointerButton.Primary);s.sendPointerEvent(PointerEventType.Release,Offset(x,y),button=PointerButton.Primary);s.render().close()}
    private fun save(s:ImageComposeScene,name:String){val d=Path.of("build","ui-previews");Files.createDirectories(d);s.render().use{i->i.encodeToData()!!.use{Files.write(d.resolve("$name.png"),it.bytes)}}}
}

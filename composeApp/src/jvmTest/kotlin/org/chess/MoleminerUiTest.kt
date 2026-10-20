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
import game.heroes.moleminer.MoleminerTest
import game.heroes.faun.FaunView
import game.draft.*
import game.heroes.*
import game.core.Positions.pos
import org.junit.Test
import org.junit.Assert.*
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities
class MoleminerUiTest{
    @Test fun mineButtonPlantsUnderTheHeroAndEnemyViewHidesIt(){
        SwingUtilities.invokeAndWait{
            val g=MoleminerTest.basic()
            ImageComposeScene(1200,800){MaterialTheme{Surface(Modifier.fillMaxSize()){ChessBoard(g)}}}.use{
                it.render().close();click(it,226f,522f);save(it,"moleminer-controls");click(it,720f,340f)
                assertTrue(g.snapshot.mines().cells().containsKey(pos("c2")));assertFalse(g.turnState.moveUsed());save(it,"moleminer-mine")
            }
            MoleminerTest.move(g,"c2","c3");MoleminerTest.end(g);assertEquals("Противник заложил мину",FaunView.message(g.snapshot,g.events.first()))
            ImageComposeScene(1200,800){MaterialTheme{Surface(Modifier.fillMaxSize()){ChessBoard(g)}}}.use{save(it,"moleminer-enemy-view")}
        }
    }
    @Test fun ultimateButtonSelectsLandingAndLeavesMandatoryMoveUnused(){
        SwingUtilities.invokeAndWait{
            val g=MoleminerTest.ready()
            ImageComposeScene(1200,800){MaterialTheme{Surface(Modifier.fillMaxSize()){ChessBoard(g)}}}.use{
                it.render().close();click(it,226f,522f);click(it,900f,340f);save(it,"moleminer-targets");click(it,346f,402f)
                assertEquals(1,g.snapshot.mines().blasts().size);assertNull(g.getPieceState(pos("c2")));assertFalse(g.turnState.moveUsed());save(it,"moleminer-explosion")
            }
        }
    }
    @Test fun tenthHeroCanBeScrolledAndPickedIntoPawnSlot(){
        SwingUtilities.invokeAndWait{
            val d=Draft(DraftRules(1,6,7));d.ban(model.FigureColor.WHITE,HeroType.PUDGE_ROOK);d.ban(model.FigureColor.BLACK,HeroType.IZBA_ROOK)
            ImageComposeScene(1200,800){MaterialTheme{DraftScreen(d,{}, {})}}.use{
                it.render().close();it.sendPointerEvent(PointerEventType.Scroll,Offset(300f,500f),scrollDelta=Offset(0f,10f));val t=System.nanoTime();repeat(40){f->it.render(t+f*16_000_000L).close()};save(it,"moleminer-draft")
                click(it,150f,580f);click(it,840f,399f);click(it,100f,677f);assertEquals(HeroType.MOLEMINER_PAWN,d.snapshot.assignments()[pos("a2")]);save(it,"moleminer-picked")
            }
        }
    }
    private fun click(s:ImageComposeScene,x:Float,y:Float){s.sendPointerEvent(PointerEventType.Press,Offset(x,y),button=PointerButton.Primary);s.sendPointerEvent(PointerEventType.Release,Offset(x,y),button=PointerButton.Primary);s.render().close()}
    private fun save(s:ImageComposeScene,name:String){val d=Path.of("build","ui-previews");Files.createDirectories(d);s.render().use{i->i.encodeToData()!!.use{Files.write(d.resolve("$name.png"),it.bytes)}}}
}

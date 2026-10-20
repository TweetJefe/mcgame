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
import game.heroes.zeus.ZeusTest
import game.heroes.ares.AresTest
import game.draft.*
import game.heroes.*
import game.core.Positions.pos
import org.junit.Test
import org.junit.Assert.*
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities
class ZeusUiTest {
    @Test fun lightningButtonSelectsTargetAndShowsStacks(){SwingUtilities.invokeAndWait{
        val g=ZeusTest.basic()
        ImageComposeScene(1200,800){MaterialTheme{Surface(Modifier.fillMaxSize()){ChessBoard(g)}}}.use{
            it.render().close();click(it,346f,522f);save(it,"zeus-controls");click(it,740f,340f);save(it,"zeus-targets");click(it,346f,402f)
            assertEquals(1,ZeusTest.count(g,"e2","e4"));assertFalse(g.turnState.moveUsed());save(it,"zeus-charge")
        }
    }}
    @Test fun ultimateButtonDischargesWithoutAskingForATarget(){SwingUtilities.invokeAndWait{
        val g=AresTest.charge(ZeusTest.basic(),"e2",2);ZeusTest.chain(g,"e4");ZeusTest.cycle(g)
        ImageComposeScene(1200,800){MaterialTheme{Surface(Modifier.fillMaxSize()){ChessBoard(g)}}}.use{
            it.render().close();click(it,346f,522f);click(it,835f,340f);assertNull(g.getPieceState(pos("e4")));assertFalse(g.turnState.moveUsed());save(it,"zeus-discharge")
        }
    }}
    @Test fun zeusCanBePickedIntoARookHome(){SwingUtilities.invokeAndWait{
        val d=Draft(DraftRules(1,6,8));d.ban(model.FigureColor.WHITE,HeroType.PUDGE_ROOK);d.ban(model.FigureColor.BLACK,HeroType.IZBA_ROOK)
        ImageComposeScene(1200,800){MaterialTheme{DraftScreen(d,{}, {})}}.use{
            it.render().close();it.sendPointerEvent(PointerEventType.Scroll,Offset(300f,500f),scrollDelta=Offset(0f,12f));val t=System.nanoTime();repeat(40){f->it.render(t+f*16_000_000L).close()};save(it,"zeus-draft");click(it,150f,580f);click(it,840f,435f);click(it,100f,677f);assertEquals(HeroType.ZEUS_ROOK,d.snapshot.assignments()[pos("a1")]);save(it,"zeus-picked")
        }
    }}
    private fun click(s:ImageComposeScene,x:Float,y:Float){s.sendPointerEvent(PointerEventType.Press,Offset(x,y),button=PointerButton.Primary);s.sendPointerEvent(PointerEventType.Release,Offset(x,y),button=PointerButton.Primary);s.render().close()}
    private fun save(s:ImageComposeScene,name:String){val d=Path.of("build","ui-previews");Files.createDirectories(d);s.render().use{i->i.encodeToData()!!.use{Files.write(d.resolve("$name.png"),it.bytes)}}}
}

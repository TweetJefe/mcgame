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
import game.heroes.kikimora.KikimoraTest
import game.draft.*
import game.heroes.*
import game.core.Positions.pos
import org.junit.Test
import org.junit.Assert.*
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities
class KikimoraUiTest{
    @Test fun yarnButtonTargetsAndStunsEnemy(){SwingUtilities.invokeAndWait{
        val g=KikimoraTest.basic()
        ImageComposeScene(1200,800){MaterialTheme{Surface(Modifier.fillMaxSize()){ChessBoard(g)}}}.use{
            it.render().close();click(it,226f,522f);save(it,"kikimora-controls");click(it,715f,340f);save(it,"kikimora-yarn-targets");click(it,286f,462f)
            assertTrue(g.getPieceState(pos("d3")).isStunned);assertFalse(g.turnState.moveUsed());save(it,"kikimora-stun")
        }
    }}
    @Test fun potButtonCreatesVisibleObstacle(){SwingUtilities.invokeAndWait{
        val g=KikimoraTest.ready()
        ImageComposeScene(1200,800){MaterialTheme{Surface(Modifier.fillMaxSize()){ChessBoard(g)}}}.use{
            it.render().close();click(it,226f,522f);click(it,900f,340f);click(it,286f,462f);assertTrue(g.getPieceState(pos("d3")).isPotted);assertFalse(g.turnState.moveUsed());save(it,"kikimora-pot")
        }
    }}
    @Test fun thirteenthHeroCanBePickedIntoABishopHome(){SwingUtilities.invokeAndWait{
        val d=Draft();d.ban(model.FigureColor.WHITE,HeroType.PUDGE_ROOK);d.ban(model.FigureColor.BLACK,HeroType.IZBA_ROOK)
        ImageComposeScene(1200,800){MaterialTheme{DraftScreen(d,{}, {})}}.use{
            it.render().close();it.sendPointerEvent(PointerEventType.Scroll,Offset(300f,500f),scrollDelta=Offset(0f,28f));val t=System.nanoTime();repeat(40){f->it.render(t+f*16_000_000L).close()};save(it,"kikimora-draft");click(it,150f,580f);click(it,912f,435f);click(it,100f,677f);assertEquals(HeroType.KIKIMORA_BISHOP,d.snapshot.assignments()[pos("c1")]);save(it,"kikimora-picked")
        }
    }}
    private fun click(s:ImageComposeScene,x:Float,y:Float){s.sendPointerEvent(PointerEventType.Press,Offset(x,y),button=PointerButton.Primary);s.sendPointerEvent(PointerEventType.Release,Offset(x,y),button=PointerButton.Primary);s.render().close()}
    private fun save(s:ImageComposeScene,name:String){val d=Path.of("build","ui-previews");Files.createDirectories(d);s.render().use{i->i.encodeToData()!!.use{Files.write(d.resolve("$name.png"),it.bytes)}}}
}

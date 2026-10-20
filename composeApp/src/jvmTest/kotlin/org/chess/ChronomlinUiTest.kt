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
import game.heroes.chronomlin.ChronomlinTest
import game.draft.*
import game.heroes.*
import game.core.Positions.pos
import model.figures.Bishop
import org.junit.Test
import org.junit.Assert.*
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities
class ChronomlinUiTest {
    @Test fun grappleButtonTargetsTheAnchorAndLandsBeforeIt(){SwingUtilities.invokeAndWait{
        val g=ChronomlinTest.basic()
        ImageComposeScene(1200,800){MaterialTheme{Surface(Modifier.fillMaxSize()){ChessBoard(g)}}}.use{
            it.render().close();click(it,226f,522f);save(it,"chronomlin-controls");click(it,700f,340f);save(it,"chronomlin-targets");click(it,226f,282f)
            assertEquals(HeroType.CHRONOMLIN_PAWN,g.getPieceState(pos("c5")).hero());assertFalse(g.turnState.moveUsed());save(it,"chronomlin-grapple")
        }
    }}
    @Test fun evolutionButtonChangesThePieceWithoutAskingForACell(){SwingUtilities.invokeAndWait{
        val g=ChronomlinTest.ready()
        ImageComposeScene(1200,800){MaterialTheme{Surface(Modifier.fillMaxSize()){ChessBoard(g)}}}.use{
            it.render().close();click(it,226f,522f);click(it,835f,340f);assertEquals(Bishop::class.java,g.getPieceState(pos("c2")).type());assertFalse(g.turnState.moveUsed());save(it,"chronomlin-evolution")
        }
    }}
    @Test fun twelfthHeroCanBeScrolledAndPickedIntoAPawnHome(){SwingUtilities.invokeAndWait{
        val d=Draft(DraftRules(1,6,9));d.ban(model.FigureColor.WHITE,HeroType.PUDGE_ROOK);d.ban(model.FigureColor.BLACK,HeroType.IZBA_ROOK)
        ImageComposeScene(1200,800){MaterialTheme{DraftScreen(d,{}, {})}}.use{
            it.render().close();it.scrollDraftToLastHero();save(it,"chronomlin-draft");click(it,150f,580f);click(it,840f,399f);click(it,100f,677f);assertEquals(HeroType.CHRONOMLIN_PAWN,d.snapshot.assignments()[pos("a2")]);save(it,"chronomlin-picked")
        }
    }}
    private fun click(s:ImageComposeScene,x:Float,y:Float){s.sendPointerEvent(PointerEventType.Press,Offset(x,y),button=PointerButton.Primary);s.sendPointerEvent(PointerEventType.Release,Offset(x,y),button=PointerButton.Primary);s.render().close()}
    private fun save(s:ImageComposeScene,name:String){val d=Path.of("build","ui-previews");Files.createDirectories(d);s.render().use{i->i.encodeToData()!!.use{Files.write(d.resolve("$name.png"),it.bytes)}}}
}

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
import game.heroes.clownganger.ClowngangerTest
import game.heroes.clownganger.ClownView
import game.draft.*
import game.heroes.*
import game.core.Positions.pos
import org.junit.Test
import org.junit.Assert.*
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities
class ClowngangerUiTest {
    @Test fun illusionButtonAndBothBodiesCanBeMovedThroughTheBoard(){
        SwingUtilities.invokeAndWait{
            val game=game.heroes.ares.AresTest.charge(ClowngangerTest.basic(),"c2",1)
            ImageComposeScene(1200,800){MaterialTheme{Surface(Modifier.fillMaxSize()){ChessBoard(game)}}}.use{
                it.render().close();click(it,226f,522f);click(it,915f,340f);click(it,286f,522f)
                assertEquals(HeroType.ILLUSION,game.getPieceState(pos("d2")).hero())
                click(it,346f,462f);click(it,286f,522f);click(it,406f,462f)
                assertEquals(2,game.moves.size);assertEquals(HeroType.CLOWNGANGER_KNIGHT,game.getPieceState(pos("e3")).hero());assertEquals(HeroType.ILLUSION,game.getPieceState(pos("f3")).hero());save(it,"clown-pair-moved")
            }
        }
    }
    @Test fun boxControlsAndProtectedKingRenderThroughBoardClicks(){
        SwingUtilities.invokeAndWait{
            val game=ClowngangerTest.basic()
            ImageComposeScene(1200,800){MaterialTheme{Surface(Modifier.fillMaxSize()){ChessBoard(game)}}}.use{
                it.render().close();click(it,226f,522f);save(it,"clown-controls")
                click(it,735f,340f);save(it,"clown-box-targets");click(it,106f,582f)
                assertTrue(game.getPieceState(pos("a1")).isBoxed);save(it,"clown-box")
            }
        }
    }
    @Test fun enemySeesMatchingHeroesAndHistoryConcealsCreation(){
        SwingUtilities.invokeAndWait{
            val game=ClowngangerTest.withIllusion();ClowngangerTest.move(game,"b1","a3");ClowngangerTest.end(game)
            val s=game.snapshot;val dummy=s.pieceAt(pos("d2"));assertEquals(HeroType.CLOWNGANGER_KNIGHT,ClownView.displayed(s,dummy).hero());assertTrue(ClownView.concealed(s,s.events().first()))
            ImageComposeScene(1200,800){MaterialTheme{Surface(Modifier.fillMaxSize()){ChessBoard(game)}}}.use{save(it,"clown-enemy-view")}
        }
    }
    @Test fun ninthHeroCanBeScrolledAndPickedIntoKnightSlot(){
        SwingUtilities.invokeAndWait{
            val d=Draft(DraftRules(1,6,6));d.ban(model.FigureColor.WHITE,HeroType.PUDGE_ROOK);d.ban(model.FigureColor.BLACK,HeroType.IZBA_ROOK)
            ImageComposeScene(1200,800){MaterialTheme{DraftScreen(d,{}, {})}}.use{
                it.render().close();it.sendPointerEvent(PointerEventType.Scroll,Offset(300f,500f),scrollDelta=Offset(0f,8f))
                val t=System.nanoTime();repeat(40){f->it.render(t+f*16_000_000L).close()};save(it,"clown-draft-scroll")
                click(it,150f,580f);click(it,1056f,435f);click(it,100f,677f)
                assertEquals(HeroType.CLOWNGANGER_KNIGHT,d.snapshot.assignments()[pos("g1")]);save(it,"clown-draft-picked")
            }
        }
    }
    private fun click(s:ImageComposeScene,x:Float,y:Float){s.sendPointerEvent(PointerEventType.Press,Offset(x,y),button=PointerButton.Primary);s.sendPointerEvent(PointerEventType.Release,Offset(x,y),button=PointerButton.Primary);s.render().close()}
    private fun save(s:ImageComposeScene,name:String){val d=Path.of("build","ui-previews");Files.createDirectories(d);s.render().use{i->i.encodeToData()!!.use{Files.write(d.resolve("$name.png"),it.bytes)}}}
}

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
import game.core.Game
import game.draft.Draft
import game.heroes.HeroType
import game.heroes.izba.IzbaFixtures
import game.state.PieceState
import game.moves.MoveResult
import model.FigureColor
import model.Position
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities

class IzbaUiTest {
    @Test fun summonThroughButtonShowBabaLifetimeAndStillRequireAnOrdinaryMove() {
        SwingUtilities.invokeAndWait {
            ImageComposeScene(1200, 800) { MaterialTheme { DraftScreen(Draft(), {}, {}) } }.use { save(it,"izba-catalog") }
            val game=IzbaFixtures.standard(5L)
            ImageComposeScene(1200, 800) { MaterialTheme { Surface(Modifier.fillMaxSize()) { ChessBoard(game) } } }.use {
                it.render().close();click(it,346f,522f);save(it,"izba-ready")
                click(it,720f,360f)
                val baby=game.snapshot.pieces().values.single { p->p.hero()==HeroType.BABA_YAGA }
                assertEquals(PieceState.Status.BOARD,baby.status());assertTrue(game.turnState.abilityUsed());assertFalse(game.turnState.moveUsed())
                assertEquals(0,game.getPieceState(Position(1,4)).ultimateCharge())
                click(it,106f+60f*baby.position().column,582f-60f*baby.position().row);save(it,"izba-yaga-counter")
                click(it,166f,582f);click(it,226f,462f);assertTrue(game.turnState.moveUsed())
                click(it,704f,112f);assertEquals(FigureColor.BLACK,game.snapshot.active())
            }
        }
    }
    @Test fun pickIzbaOnTheSecondRookSquareThroughTheExpandedDraft() {
        SwingUtilities.invokeAndWait {
            val draft=Draft(game.draft.DraftRules(1,6,2));draft.ban(FigureColor.WHITE,HeroType.PUDGE);draft.ban(FigureColor.BLACK,HeroType.LUMESHARD)
            ImageComposeScene(1200,800) { MaterialTheme { DraftScreen(draft,{}, {}) } }.use {
                it.render().close();click(it,150f,555f);click(it,1092f,435f);click(it,95f,679f)
                assertEquals(HeroType.IZBA,draft.snapshot.picksFor(FigureColor.WHITE).single().hero())
                assertEquals(Position(0,7),draft.snapshot.picksFor(FigureColor.WHITE).single().square())
                assertFalse(draft.getAvailableHeroes(FigureColor.BLACK).contains(HeroType.IZBA));save(it,"izba-draft-picked")
            }
        }
    }
    private fun click(scene:ImageComposeScene,x:Float,y:Float) {
        scene.sendPointerEvent(PointerEventType.Press,Offset(x,y),button=PointerButton.Primary)
        scene.sendPointerEvent(PointerEventType.Release,Offset(x,y),button=PointerButton.Primary)
        scene.render().close()
    }
    private fun save(scene:ImageComposeScene,name:String) {
        val folder=Path.of("build","ui-previews");Files.createDirectories(folder)
        scene.render().use { image->image.encodeToData()!!.use { Files.write(folder.resolve("$name.png"),it.bytes) } }
    }
}

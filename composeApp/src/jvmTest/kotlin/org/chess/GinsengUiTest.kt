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
import game.draft.Draft
import game.draft.DraftPhase
import game.heroes.Ability
import game.heroes.HeroType
import game.core.Positions.pos
import game.heroes.ginseng.GinsengFixtures
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities

class GinsengUiTest {
    @Test fun renderSixHeroCatalogAndGinsengControls() {
        SwingUtilities.invokeAndWait {
            val draft=Draft(game.draft.DraftRules(1,6,3))
            ImageComposeScene(1200,800) { MaterialTheme { DraftScreen(draft,{}, {}) } }.use {
                save(it,"ginseng-catalog")
                click(it,150f,150f);click(it,100f,649f) // Pudge banned by white.
                click(it,150f,480f);click(it,100f,649f) // Izba banned by black.
                assertEquals(DraftPhase.PICKS,draft.snapshot.phase())
                click(it,150f,565f);click(it,1020f,435f);click(it,100f,673f)
                assertEquals(HeroType.GINSENG_BISHOP,draft.snapshot.assignments()[pos("f1")])
                save(it,"ginseng-picked")
            }
            val game=GinsengFixtures.hook()
            ImageComposeScene(1200,800) { MaterialTheme { Surface(Modifier.fillMaxSize()) { ChessBoard(game) } } }.use {
                it.render().close();click(it,226f,522f);save(it,"ginseng-native")
                val target=game.getPieceState(pos("e2")).id()
                click(it,720f,360f);click(it,346f,522f);save(it,"ginseng-destination")
                click(it,346f,462f)
                assertEquals(target,game.getPieceState(pos("e3")).id())
                assertTrue(game.snapshot.turn().abilityUsed())
                assertFalse(game.snapshot.turn().moveUsed())
            }
            val copying=GinsengFixtures.hook()
            assertTrue(copying.useAbility(copying.currentPlayer,pos("e2"),pos("e5"),Ability.HOOK).isSuccess)
            assertTrue(copying.moveFigure(copying.currentPlayer,pos("b1"),pos("a3")).isSuccess)
            assertTrue(copying.endTurn(copying.currentPlayer).isSuccess)
            assertTrue(copying.moveFigure(copying.currentPlayer,pos("g8"),pos("f6")).isSuccess)
            assertTrue(copying.endTurn(copying.currentPlayer).isSuccess)
            ImageComposeScene(1200,800) { MaterialTheme { Surface(Modifier.fillMaxSize()) { ChessBoard(copying) } } }.use {
                it.render().close();click(it,226f,522f);click(it,930f,360f);click(it,346f,522f)
                val actor=copying.getPieceState(pos("c2"))
                val copy=copying.spells.borrowed()[actor.id()]!!
                assertEquals(Ability.HOOK,copy.ability())
                assertFalse(copy.isReady(copying.snapshot.turn().number()))
                assertEquals(0,actor.ultimateCharge())
                save(it,"ginseng-copy-waiting")
            }
            val borrowed=GinsengFixtures.readyHook()
            ImageComposeScene(1200,800) { MaterialTheme { Surface(Modifier.fillMaxSize()) { ChessBoard(borrowed) } } }.use {
                it.render().close();click(it,226f,522f);save(it,"ginseng-ready-copy")
                val target=borrowed.getPieceState(pos("a4")).id()
                click(it,720f,432f);click(it,106f,402f)
                assertEquals(target,borrowed.getPieceState(pos("b3")).id())
                assertTrue(borrowed.spells.borrowed().isEmpty())
                assertEquals(Ability.HOOK,borrowed.spells.lastUsed()[borrowed.getPieceState(pos("c2")).id()])
                save(it,"ginseng-hook-used")
            }
        }
    }
    private fun click(scene:ImageComposeScene,x:Float,y:Float) {
        scene.sendPointerEvent(PointerEventType.Press,Offset(x,y),button=PointerButton.Primary)
        scene.sendPointerEvent(PointerEventType.Release,Offset(x,y),button=PointerButton.Primary);scene.render().close()
    }
    private fun save(scene:ImageComposeScene,name:String) {
        val folder=Path.of("build","ui-previews");Files.createDirectories(folder)
        scene.render().use { image->image.encodeToData()!!.use { Files.write(folder.resolve("$name.png"),it.bytes) } }
    }
}

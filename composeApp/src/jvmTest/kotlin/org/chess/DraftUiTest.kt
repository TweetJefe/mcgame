package org.chess

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.use
import game.core.Game
import game.draft.*
import game.heroes.HeroRules
import game.heroes.HeroType
import game.persistence.GameSaveFile
import model.FigureColor
import model.Position
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities

class DraftUiTest {
    @get:Rule val temporary = TemporaryFolder()
    @Test fun renderDraftStagesAndChosenRosters() {
        SwingUtilities.invokeAndWait {
            val draft = Draft(DraftRules(1, 6, 1))
            ImageComposeScene(1200, 800) { MaterialTheme { DraftScreen(draft, {}, {}) } }.use { save(it, "draft-bans") }
            draft.ban(FigureColor.WHITE, HeroType.BOUNTY_CROW_PAWN)
            draft.ban(FigureColor.BLACK, HeroType.BOLOTNIK_KNIGHT)
            ImageComposeScene(1200, 800) { MaterialTheme { DraftScreen(draft, {}, {}) } }.use { save(it, "draft-picks") }
            draft.pick(FigureColor.WHITE, HeroType.PUDGE_ROOK, Position(0, 7))
            draft.pick(FigureColor.BLACK, HeroType.LUMESHARD_BISHOP, Position(7, 5))
            ImageComposeScene(1200, 800) { MaterialTheme { DraftScreen(draft, {}, {}) } }.use { save(it, "draft-ready") }
            val game = Game.mobaFromDraft(draft.snapshot, HeroRules.prototype(), 600) { 0L }
            ImageComposeScene(1200, 800) { MaterialTheme { Surface(Modifier.fillMaxSize()) { ChessBoard(game) } } }.use {
                save(it, "draft-game"); click(it, 700f, 64f); save(it, "draft-roster-dialog")
            }
        }
    }
    @Test fun completeDraftThroughRealButtonsAndAlternativeHomeSquares() {
        SwingUtilities.invokeAndWait {
            val draft = Draft(DraftRules(1, 6, 1)); var started: Game? = null; var starts = 0
            ImageComposeScene(1200, 800) { MaterialTheme { DraftScreen(draft, {
                starts++; started = Game.mobaFromDraft(it, HeroRules.prototype(), 600) { 0L }
            }, {}) } }.use { scene ->
                scene.render().close()
                click(scene, 150f, 355f); click(scene, 95f, 553f) // White bans Crow.
                assertEquals(FigureColor.BLACK, draft.snapshot.active())
                click(scene, 150f, 455f); click(scene, 95f, 553f) // Black bans Bolotnik.
                assertEquals(DraftPhase.PICKS, draft.snapshot.phase())
                assertEquals(0, starts)
                click(scene, 150f, 155f) // Pudge: a1 and h1 highlighted.
                save(scene, "draft-placement")
                click(scene, 1020f, 435f) // f1 isn't a rook square; selection stays empty.
                click(scene, 95f, 577f); assertTrue(draft.snapshot.picksFor(FigureColor.WHITE).isEmpty())
                click(scene, 1092f, 435f); click(scene, 95f, 577f) // Pick h1.
                assertEquals(Position(0, 7), draft.snapshot.picksFor(FigureColor.WHITE).single().square())
                click(scene, 150f, 155f) // Opponent cannot select already picked Pudge.
                click(scene, 150f, 255f); click(scene, 1020f, 183f); click(scene, 95f, 577f)
                assertTrue(draft.snapshot.isComplete); assertNull(started)
                click(scene, 95f, 553f)
                assertEquals(1, starts); val game = started!!
                assertEquals(HeroType.PUDGE_ROOK, game.getPieceState(Position(0, 7)).hero())
                assertEquals(HeroType.LUMESHARD_BISHOP, game.getPieceState(Position(7, 5)).hero())
                assertEquals(600_000L, game.clockSnapshot.whiteMillis)
            }
        }
    }
    @Test fun finishEarlyAndReturnToMenuDoNotStartAClock() {
        SwingUtilities.invokeAndWait {
            val draft = Draft(DraftRules(1, 6, 1)); draft.ban(FigureColor.WHITE, HeroType.PUDGE_ROOK); draft.ban(FigureColor.BLACK, HeroType.LUMESHARD_BISHOP)
            var starts = 0; var backs = 0
            ImageComposeScene(1200, 800) { MaterialTheme { DraftScreen(draft, { starts++ }, { backs++ }) } }.use { scene ->
                scene.render().close(); click(scene, 250f, 577f)
                assertEquals(FigureColor.BLACK, draft.snapshot.active()); assertTrue(draft.snapshot.finished().contains(FigureColor.WHITE))
                click(scene, 250f, 577f); assertTrue(draft.snapshot.isComplete); assertEquals(0, starts)
                click(scene, 90f, 750f); assertEquals(1, backs); assertEquals(0, starts)
            }
        }
    }
    @Test fun mainMobaMenuEntersDraftAndBackResetsIt() {
        val output = temporary.root.toPath().resolve("draft.moba.xml")
        var saves = 0
        SwingUtilities.invokeAndWait {
            val unusedGame = Game(8)
            ImageComposeScene(1200, 800) { MaterialTheme { Surface(Modifier.fillMaxSize()) { ChessApp(unusedGame, GameFilePicker {
                if (it) { saves++; output } else null
            }) } } }.use { scene ->
                save(scene, "draft-main-menu")
                click(scene, 600f, 126f); save(scene, "draft-from-menu")
                click(scene, 150f, 300f); click(scene, 95f, 677f)
                click(scene, 90f, 750f); click(scene, 600f, 126f)
                save(scene, "draft-restarted")
                assertFalse(unusedGame.clockSnapshot.isEnabled)
                click(scene, 150f, 300f); click(scene, 95f, 677f)
                click(scene, 150f, 380f); click(scene, 95f, 677f)
                click(scene, 150f, 155f); click(scene, 1092f, 435f); click(scene, 95f, 677f)
                click(scene, 150f, 230f); click(scene, 1020f, 183f); click(scene, 95f, 677f)
                click(scene, 250f, 677f); click(scene, 250f, 677f)
                click(scene, 95f, 677f); save(scene, "draft-app-game")
                click(scene, 688f, 744f); assertEquals(1, saves); assertTrue(Files.exists(output))
                val restored = GameSaveFile.load(output) { 0L }
                assertTrue(restored.draft.isComplete)
                assertEquals(HeroType.PUDGE_ROOK, restored.getPieceState(Position(0, 7)).hero())
                assertEquals(HeroType.LUMESHARD_BISHOP, restored.getPieceState(Position(7, 5)).hero())
                assertEquals(6, restored.draft.events().size)
                assertTrue(restored.clockSnapshot.isRunning)
            }
        }
    }
    private fun click(scene: ImageComposeScene, x: Float, y: Float) {
        scene.sendPointerEvent(PointerEventType.Press, Offset(x, y), button = PointerButton.Primary)
        scene.sendPointerEvent(PointerEventType.Release, Offset(x, y), button = PointerButton.Primary)
        scene.render().close()
    }
    private fun save(scene: ImageComposeScene, name: String) {
        val folder = Path.of("build", "ui-previews"); Files.createDirectories(folder)
        scene.render().use { image -> image.encodeToData()!!.use { Files.write(folder.resolve("$name.png"), it.bytes) } }
    }
}

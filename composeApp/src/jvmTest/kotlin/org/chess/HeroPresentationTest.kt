package org.chess

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.sp
import androidx.compose.ui.use
import game.core.Positions.pos
import game.draft.Draft
import game.draft.DraftCatalog
import game.draft.DraftCatalogTest
import game.draft.DraftRules
import game.heroes.HeroDefinitions
import game.heroes.HeroType
import game.heroes.UiExtensionFixtures
import model.FigureColor.WHITE
import org.chess.heroes.BaseHeroPresentation
import org.chess.heroes.HeroPresentationRegistry
import org.chess.heroes.HeroPresentations
import org.chess.heroes.HeroViewContext
import org.junit.Assert.assertSame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities

class HeroPresentationTest {
    @Test fun missingAndDuplicateUiRegistrationsFailWithoutChangingTheEngineRegistry() {
        assertThrows(IllegalArgumentException::class.java) {
            HeroPresentationRegistry(emptyList()).validate(HeroDefinitions.all())
        }
        assertThrows(IllegalArgumentException::class.java) {
            HeroPresentationRegistry(listOf(HeroPresentations.all.first(), HeroPresentations.all.first()))
        }
        val mutable = HeroPresentations.all.toMutableList()
        val registry = HeroPresentationRegistry(mutable)
        mutable.clear()
        registry.validate(HeroDefinitions.all())
        assertTrue(UiExtensionFixtures.game().snapshot.pieces().isNotEmpty())
    }

    @Test fun aNewRegisteredAbilityUsesTheSharedPanelAndCommandPath() {
        SwingUtilities.invokeAndWait {
            for ((spell, buttonY) in listOf(UiExtensionFixtures.Spell.CAST to 312f,
                                           UiExtensionFixtures.Spell.THIRD to 360f)) {
                val game = UiExtensionFixtures.game()
                val presentation = object : BaseHeroPresentation(HeroType.PUDGE, "Extension", "E",
                    "Независимое отображение", beforeInventory = true) {
                    @Composable override fun Details(context: HeroViewContext) {
                        assertSame(game.heroRegistry, context.heroes)
                        Text("Состояние нового героя", fontSize = 13.sp)
                        Text("Общая кнопка способности", fontSize = 13.sp)
                    }
                }
                val registry = HeroPresentationRegistry(HeroPresentations.all.filter { it.type != HeroType.PUDGE } + presentation)
                ImageComposeScene(1200, 800) {
                    MaterialTheme { Surface(Modifier.fillMaxSize()) { ChessBoard(game, presentations = registry) } }
                }.use { scene ->
                    scene.render().close()
                    click(scene, 106f, 582f)
                    save(scene, "extension-selected")
                    click(scene, 700f, buttonY)
                    assertTrue(game.turnState.abilityUsed())
                    assertEquals(2, game.getPieceState(pos("a1")).cooldown())
                    assertTrue(game.getPieceState(pos("a1")).isBlinded)
                    assertEquals(spell, game.events.last().ability())
                }
            }
        }
    }

    @Test fun aNewDraftProfileAndPresentationCanBePickedWithoutChangingDraftScreen() {
        SwingUtilities.invokeAndWait {
            val rules = DraftCatalogTest.extendedRules()
            val catalog = DraftCatalog.restore(listOf(HeroType.ILLUSION, HeroType.PUDGE), rules.definitions())
            val draft = Draft(DraftRules(0, 1), catalog)
            val presentation = object : BaseHeroPresentation(HeroType.ILLUSION, "Extension", "E", "Новый участник каталога") {}
            val registry = HeroPresentationRegistry(HeroPresentations.all.filter { it.type != HeroType.ILLUSION } + presentation)
            ImageComposeScene(1200, 800) { MaterialTheme { DraftScreen(draft, {}, {}, registry) } }.use { scene ->
                scene.render().close()
                click(scene, 150f, 150f)
                click(scene, 948f, 435f)
                save(scene, "extension-draft-target")
                click(scene, 100f, 675f)
                assertEquals(HeroType.ILLUSION, draft.snapshot.assignments()[pos("d1")])
                assertEquals(1, draft.snapshot.picksFor(WHITE).size)
            }
        }
    }

    private fun click(scene: ImageComposeScene, x: Float, y: Float) {
        scene.sendPointerEvent(PointerEventType.Press, Offset(x, y), button = PointerButton.Primary)
        scene.sendPointerEvent(PointerEventType.Release, Offset(x, y), button = PointerButton.Primary)
        scene.render().close()
    }
    private fun save(scene: ImageComposeScene, name: String) {
        val folder = Path.of("build", "ui-previews")
        Files.createDirectories(folder)
        scene.render().use { it.encodeToData()!!.use { data -> Files.write(folder.resolve("$name.png"), data.bytes) } }
    }
}

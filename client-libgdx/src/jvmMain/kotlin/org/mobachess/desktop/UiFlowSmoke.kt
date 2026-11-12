package org.mobachess.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import game.draft.MatchDraft
import game.heroes.HeroType
import model.FigureColor
import org.mobachess.screen.*
import org.mobachess.presentation.HeroGuide
import org.mobachess.presentation.HeroCatalog
import java.util.function.LongSupplier

/** Проверка переходов настоящим вводом Scene2D; часы ускоряются только в этом режиме запуска. */
class UiFlowSmoke {
    private var now = System.nanoTime()
    val clock = LongSupplier { now }
    private var step = 0
    private var frames = 0
    private var confirming = false
    private var previousDraftStep = -1
    private var whiteHero: HeroType? = null
    private var blackHero: HeroType? = null

    fun update(screen: Screen) {
        if (++frames < 12) return
        if (step == 2 && screen is HeroesScreen && screen.detailOpen && frames < 45) {
            if (frames == 12) capture("hero-transition")
            return
        }
        fun next() { step++; frames = 0 }
        when (step) {
            0 -> {
                check(screen is MainMenuScreen)
                check(screen.stage.root.findActor<TextButton>("play").isDisabled)
                capture("menu")
                click(screen, "heroes"); next()
            }
            1 -> {
                check(screen is HeroesScreen)
                val guide = HeroGuide()
                HeroCatalog().draft.heroes().forEach { hero ->
                    val text = guide.entry(hero).paragraphs
                    check(text.any { it.startsWith("Скилл") } && text.any { it.startsWith("Ульта") }) {
                        "Incomplete hero guide: $hero"
                    }
                }
                listOf("PAWN", "KNIGHT", "BISHOP", "ROOK").forEach {
                    check(screen.stage.root.findActor<Actor>("group-$it") != null)
                }
                click(screen, "hero-PUDGE"); next()
            }
            2 -> {
                check(screen is HeroesScreen)
                if (screen.detailOpen) {
                    check(screen.stage.root.findActor<Actor>("hero-description") != null)
                    capture("hero-detail"); click(screen, "back"); frames = 0
                } else { capture("heroes"); click(screen, "back"); next() }
            }
            3 -> { check(screen is MainMenuScreen); click(screen, "local"); next() }
            4 -> {
                check(screen is DraftScreen)
                val s = screen.session.snapshot()
                if (s.phase() == MatchDraft.Phase.PREPARATION) { next(); return }
                if (s.step() == 0 && !confirming) capture("draft")
                if (!confirming) {
                    if (previousDraftStep >= 0) check(s.step() == previousDraftStep + 1)
                    previousDraftStep = s.step()
                    click(screen, "hero-${screen.session.available().first().name}")
                    confirming = true
                } else { click(screen, "confirm"); confirming = false }
                frames = 0
            }
            5 -> {
                check(screen is DraftScreen)
                check(screen.session.snapshot().step() == 16)
                whiteHero = screen.session.snapshot().rosters().getValue(FigureColor.WHITE).first()
                click(screen, "placement-${whiteHero!!.name}"); next()
            }
            6 -> {
                check(screen is DraftScreen)
                capture("preparation-selected")
                val square = screen.session.placementTargets(FigureColor.WHITE, whiteHero!!).first()
                click(screen, "cell-${square.row}-${square.column}"); next()
            }
            7 -> {
                check(screen is DraftScreen)
                capture("preparation-after-click")
                check(whiteHero in screen.session.snapshot().assignments().values) { "White placement failed: ${screen.session.snapshot().assignments()}" }
                click(screen, "side-BLACK"); next()
            }
            8 -> {
                check(screen is DraftScreen)
                blackHero = screen.session.snapshot().rosters().getValue(FigureColor.BLACK).first()
                click(screen, "placement-${blackHero!!.name}"); next()
            }
            9 -> {
                check(screen is DraftScreen)
                val square = screen.session.placementTargets(FigureColor.BLACK, blackHero!!).first()
                click(screen, "cell-${square.row}-${square.column}"); next()
            }
            10 -> {
                check(screen is DraftScreen)
                check(blackHero in screen.session.snapshot().assignments().values)
                capture("preparation")
                now += 16_000_000_000L
                next()
            }
            11 -> {
                check(screen is MatchScreen)
                capture("match")
                Gdx.app.log("MobaChess", "UI_FLOW_OK menu, grouped heroes, 16 draft actions, both placements, auto-fill, match")
                Gdx.app.exit(); next()
            }
        }
    }

    private fun click(screen: SimpleScreen, name: String) {
        val actor = checkNotNull(screen.stage.root.findActor<Actor>(name)) { "Missing UI actor: $name" }
        val point = actor.localToStageCoordinates(Vector2(actor.width / 2, actor.height / 2))
        screen.viewport.project(point)
        val x = point.x.toInt(); val y = Gdx.graphics.height - point.y.toInt()
        Gdx.input.inputProcessor.touchDown(x, y, 0, Input.Buttons.LEFT)
        Gdx.input.inputProcessor.touchUp(x, y, 0, Input.Buttons.LEFT)
    }

    private fun capture(name: String) {
        val path = System.getProperty("mobachess.capture") ?: return
        if (Gdx.graphics.backBufferWidth <= 0 || Gdx.graphics.backBufferHeight <= 0) return
        val pixmap = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.backBufferWidth, Gdx.graphics.backBufferHeight)
        PixmapIO.writePNG(Gdx.files.absolute(path).parent().child("$name.png"), pixmap, 6, true)
        pixmap.dispose()
    }
}

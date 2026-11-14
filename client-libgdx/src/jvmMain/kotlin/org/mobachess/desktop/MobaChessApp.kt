package org.mobachess.desktop

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import org.mobachess.assets.GameAssets
import org.mobachess.presentation.HeroCatalog
import org.mobachess.screen.*
import org.mobachess.session.GameSession
import org.mobachess.session.LocalDraftSession
import org.mobachess.session.LocalSandboxSession

/** Навигация владеет общими ассетами; завершённые экраны освобождают свои ресурсы. */
class MobaChessApp : Game(), ScreenNavigation {
    private lateinit var assets: GameAssets
    private val catalog = HeroCatalog()
    private val flow = if (System.getProperty("mobachess.flowSmoke") == "true") UiFlowSmoke() else null
    private var frames = 0
    private var captured = false

    override fun create() {
        assets = GameAssets()
        if (System.getProperty("mobachess.smoke") == "true") setScreen(MatchScreen())
        else if (System.getProperty("mobachess.sandboxSmoke")=="true") setScreen(MatchScreen(LocalSandboxSession(),assets,false,this))
        else setScreen(MainMenuScreen(assets, this, catalog))
    }

    private fun navigate(factory: () -> Screen) {
        Gdx.app.postRunnable {
            val previous = screen
            setScreen(factory())
            previous?.dispose()
        }
    }
    override fun menu() = navigate { MainMenuScreen(assets, this, catalog) }
    override fun heroes() = navigate { HeroesScreen(assets, this, catalog) }
    override fun sandbox() = navigate { MatchScreen(LocalSandboxSession(), assets, false, this) }
    override fun localMatch() = navigate {
        val session = flow?.let { LocalDraftSession(it.clock) } ?: LocalDraftSession()
        DraftScreen(assets, this, catalog, session)
    }
    override fun match(session: GameSession) = navigate { MatchScreen(session, assets, false, this) }
    override fun render() {
        super.render()
        flow?.update(screen)
        frames++
        val path = System.getProperty("mobachess.capture")
        if (flow == null && screen !is MatchScreen && path != null && !captured && frames >= 20 &&
            Gdx.graphics.backBufferWidth > 0 && Gdx.graphics.backBufferHeight > 0) {
            val image = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.backBufferWidth, Gdx.graphics.backBufferHeight)
            PixmapIO.writePNG(Gdx.files.absolute(path), image, 6, true); image.dispose(); captured = true
        }
    }
    override fun dispose() { screen?.dispose(); assets.dispose() }
}

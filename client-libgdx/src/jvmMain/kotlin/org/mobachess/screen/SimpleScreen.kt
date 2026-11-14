package org.mobachess.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.viewport.FitViewport
import org.mobachess.assets.GameAssets
import org.mobachess.session.GameSession

interface ScreenNavigation {
    fun menu()
    fun heroes()
    fun localMatch()
    fun sandbox()
    fun match(session: GameSession)
}

/** Общая простая оболочка экранов, без правил матча и драфта. */
abstract class SimpleScreen(protected val assets: GameAssets, protected val navigation: ScreenNavigation) : ScreenAdapter() {
    val viewport = FitViewport(1600f, 900f)
    val stage = Stage(viewport)
    protected val batch = SpriteBatch()
    protected val skin = Skin()
    protected val root = Table()
    protected val gold = Color.valueOf("E5C88B")
    protected val muted = Color.valueOf("ABB2B5")

    init {
        skin.add("default", Label.LabelStyle(assets.font, Color.valueOf("EEE9DF")))
        skin.add("small", Label.LabelStyle(assets.small, Color.valueOf("EEE9DF")))
        skin.add("default", TextButton.TextButtonStyle().apply {
            font = assets.font; fontColor = Color.valueOf("EEE9DF")
            up = background("283039"); over = background("3B4854"); down = background("665534")
            checked = background("665534"); disabled = background("171C22")
            disabledFontColor = Color.valueOf("646C72")
        })
        skin.add("card", Button.ButtonStyle().apply {
            up = background("252D34"); over = background("3A4651"); down = background("695A3E")
            checked = background("695A3E"); disabled = background("181D22")
        })
        root.setFillParent(true)
        root.pad(40f)
        stage.addActor(root)
    }

    protected fun background(hex: String) = TextureRegionDrawable(TextureRegion(assets.pixel)).tint(Color.valueOf(hex))
    protected fun label(text: String, small: Boolean = false): Label = Label(text, skin, if (small) "small" else "default")
    protected fun button(text: String, name: String = text, action: () -> Unit): TextButton = TextButton(text, skin).apply {
        this.name = name
        setProgrammaticChangeEvents(false)
        label.setFontScale(.84f)
        addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) { if (!isDisabled) action() }
        })
    }

    protected open fun update(delta: Float) {}
    protected open fun back() { navigation.menu() }
    override fun show() {
        Gdx.input.inputProcessor = InputMultiplexer(stage, object : InputAdapter() {
            override fun keyDown(keycode: Int): Boolean {
                if (keycode != Input.Keys.ESCAPE) return false
                back(); return true
            }
        })
    }
    override fun render(delta: Float) {
        update(delta)
        viewport.apply()
        Gdx.gl.glClearColor(.025f, .035f, .045f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        batch.projectionMatrix = viewport.camera.combined
        batch.begin()
        batch.color = Color.WHITE
        batch.draw(assets.texture(assets.backgroundPath), 0f, 0f, 1600f, 900f)
        batch.color = Color(.025f, .04f, .055f, .91f)
        batch.draw(assets.pixel, 0f, 0f, 1600f, 900f)
        batch.color = Color.WHITE
        batch.end()
        stage.act(delta.coerceAtMost(.1f)); stage.draw()
    }
    override fun resize(width: Int, height: Int) { viewport.update(width, height, true) }
    override fun hide() { Gdx.input.inputProcessor = null }
    override fun dispose() { stage.dispose(); skin.dispose(); batch.dispose() }
}

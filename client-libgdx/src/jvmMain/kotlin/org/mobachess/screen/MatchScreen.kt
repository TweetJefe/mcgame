package org.mobachess.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.FitViewport
import game.heroes.HeroRegistry
import game.moves.Promotion
import model.FigureColor
import model.Position
import org.mobachess.assets.GameAssets
import org.mobachess.presentation.MatchController
import org.mobachess.presentation.PieceDragState
import org.mobachess.render.Point
import org.mobachess.render.BoardGeometry
import org.mobachess.render.MatchRenderer
import org.mobachess.session.LocalGameSession
import org.mobachess.session.GameSession

/** Экран управляет жизненным циклом графики, не содержит игровых правил. */
class MatchScreen(private val initialSession: GameSession = LocalGameSession.preview(),
                  private val assets: GameAssets = GameAssets(),
                  private val ownsAssets: Boolean = true,
                  private val navigation: ScreenNavigation? = null) : ScreenAdapter() {
    private val batch=SpriteBatch()
    private val geometry=BoardGeometry()
    private val renderer=MatchRenderer(assets,geometry)
    private val drag=PieceDragState()
    private val viewport=FitViewport(1600f,900f)
    private val stage=Stage(viewport)
    private val skin=Skin()
    private val controls=Table()
    private lateinit var heading: Label
    private lateinit var status: Label
    private lateinit var message: Label
    private var controller=MatchController(initialSession)
    private val skillHud=SkillHud(assets,skin,renderer.hudLayout) { choice -> drag.cancel(); controller.choose(choice) }
    private var signature=""
    private var frames=0
    private var captured=false

    init {
        val white=TextureRegionDrawable(TextureRegion(assets.pixel))
        skin.add("default",Label.LabelStyle(assets.font,Color.valueOf("F1DDB6")))
        skin.add("default",TextButton.TextButtonStyle().apply {
            font=assets.font; fontColor=Color.valueOf("F4E5C7")
            up=white.tint(Color.valueOf("453426")); over=white.tint(Color.valueOf("6D5130"))
            down=white.tint(Color.valueOf("8B6537")); disabled=white.tint(Color.valueOf("292728"))
            disabledFontColor=Color.valueOf("77716A")
        })
        val panel=Table().apply {
            setBounds(1320f,0f,280f,900f)
            background=white.tint(Color(.055f,.042f,.035f,.94f))
            pad(20f); top()
        }
        heading=Label("",skin).apply { setAlignment(Align.center); setWrap(true) }
        status=Label("",skin).apply { setWrap(true); setFontScale(.80f) }
        message=Label("",skin).apply { setWrap(true); setFontScale(.75f) }
        panel.add(Label("MOBA CHESS",skin)).padBottom(20f).row()
        panel.add(heading).width(240f).padBottom(16f).row()
        panel.add(status).width(240f).padBottom(14f).row()
        panel.add(button("Закончить ход",{controller.endTurn()})).width(240f).height(52f).padBottom(14f).row()
        panel.add(controls).width(240f).padBottom(16f).row()
        panel.add(message).width(240f).minHeight(76f).padBottom(14f).row()
        panel.add(button("Повернуть доску",{geometry.flipped=!geometry.flipped})).width(240f).height(42f).padBottom(8f).row()
        panel.add(button("Новая партия",{
            if(navigation!=null) navigation.localMatch()
            else { controller=MatchController(LocalGameSession.preview());signature="" }
        })).width(240f).height(42f).padBottom(8f).row()
        if(navigation!=null) panel.add(button("Главное меню",navigation::menu)).width(240f).height(42f).padBottom(14f).row()
        panel.add(Label("Локально • за обе стороны\nEnter — закончить ход\nEsc / ПКМ — отмена\nF — повернуть доску",skin).apply {
            setFontScale(.62f); setWrap(true)
        }).width(240f).row()
        stage.addActor(panel)
        stage.addActor(skillHud)
    }

    private fun button(text: String, action: () -> Unit): TextButton = TextButton(text,skin).apply {
        label.setFontScale(.78f)
        addListener(object:ChangeListener() { override fun changed(event: ChangeEvent?, actor: Actor?) { drag.cancel(); action() } })
    }

    override fun show() {
        Gdx.input.inputProcessor=InputMultiplexer(stage,object:InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                if(button==Input.Buttons.RIGHT) { drag.cancel(); controller.clear(); return true }
                if(button!=Input.Buttons.LEFT || pointer!=0) return false
                val p=viewport.unproject(Vector2(screenX.toFloat(),screenY.toFloat()))
                geometry.cell(p.x,p.y)?.let { square ->
                    drag.cancel()
                    val selecting = controller.spell == null && controller.promotion == null
                    controller.click(square)
                    if (selecting && controller.selected == square && controller.highlights.isNotEmpty()) {
                        val state=controller.session.snapshot()
                        state.pieceAt(square)?.takeIf { it.color()==state.active() }?.let {
                            drag.begin(it.id(),Point(p.x,p.y),geometry.anchor(square))
                        }
                    }
                    return true
                }
                return false
            }
            override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
                if(pointer!=0 || drag.pieceId==null) return false
                val p=viewport.unproject(Vector2(screenX.toFloat(),screenY.toFloat()))
                drag.move(Point(p.x,p.y))
                return true
            }
            override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                if(pointer!=0 || button!=Input.Buttons.LEFT || drag.pieceId==null) return false
                val p=viewport.unproject(Vector2(screenX.toFloat(),screenY.toFloat()))
                drag.move(Point(p.x,p.y))
                if(drag.release()) {
                    val destination=geometry.cell(p.x,p.y)
                    if(destination!=null && destination!=controller.selected && destination in controller.highlights) {
                        controller.click(destination)
                    }
                }
                return true
            }
            override fun keyDown(keycode: Int): Boolean = when(keycode) {
                Input.Keys.ENTER -> {drag.cancel();controller.endTurn();true}
                Input.Keys.ESCAPE -> {drag.cancel();controller.clear();true}
                Input.Keys.F -> {drag.cancel();geometry.flipped=!geometry.flipped;true}
                else -> false
            }
        })
    }

    private fun refresh() {
        val state=controller.session.snapshot()
        heading.setText(if(state.outcome().isFinished) "Партия завершена\n${state.outcome().reason()}" else
            "Ход ${state.turn().number()}\n${if(state.active()==FigureColor.WHITE) "Белые" else "Чёрные"}")
        fun gold(value: Long) = if(value%2L==0L) "${value/2}" else "${value/2},5"
        fun time(nanos: Long): String { val s=(nanos/1_000_000_000L).coerceAtLeast(0); return "%d:%02d".format(s/60,s%60) }
        status.setText("Время: ${time(state.clock().whiteNanos())} / ${time(state.clock().blackNanos())}\nЗолото: ${gold(state.economy().whiteHalves())} / ${gold(state.economy().blackHalves())}\n"+
            "Ход фигурой: ${if(state.turn().moveUsed()) "сделан" else "обязателен"}\nСпособность: ${if(state.turn().abilityUsed()) "использована" else "необязательна"}")
        message.setText(controller.message)
        val newSignature="${state.version()}:${controller.selected}:${controller.promotion}:${controller.spell}"
        if(signature==newSignature) return
        signature=newSignature; controls.clearChildren()
        val selected=controller.selected?.let(state::pieceAt)
        selected?.let { piece ->
            val name=HeroRegistry.fromRules(state.heroRules()).get(piece.hero()).definition().displayName()
            controls.add(Label(name,skin).apply { setWrap(true);setFontScale(.84f) }).width(240f).padBottom(10f).row()
        }
        if(controller.promotion!=null) {
            controls.add(Label("Превращение",skin)).row()
            val names=mapOf(Promotion.QUEEN to "Ферзь",Promotion.ROOK to "Ладья",Promotion.BISHOP to "Слон",Promotion.KNIGHT to "Конь")
            Promotion.values().forEach { p -> controls.add(button(names.getValue(p),{controller.promote(p)})).width(240f).height(38f).padBottom(5f).row() }
        } else controller.choices().forEachIndexed { index, choice ->
            val ready=controller.selected?.let { controller.session.availability(it,choice.ability,choice.borrowed).isSuccess } ?: false
            controls.add(button("${index+1}. ${if(choice.borrowed) "Копия: " else ""}${choice.ability.displayName}",{controller.choose(choice)}).apply {
                isDisabled=!ready || state.turn().abilityUsed()
            }).width(240f).height(46f).padBottom(6f).row()
        }
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(.025f,.018f,.012f,1f); Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        viewport.apply()
        Gdx.gl.glEnable(GL20.GL_BLEND); Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA)
        refresh()
        skillHud.refresh(controller)
        val draggedPiece=drag.pieceId?.let { controller.session.snapshot().pieces()[it] }
        drag.update(delta,draggedPiece?.position()?.let(geometry::anchor))
        renderer.draw(batch,viewport.camera.combined,controller,drag)
        stage.act(delta.coerceAtMost(.1f)); stage.draw()
        frames++
        if(frames==7 && System.getProperty("mobachess.smoke")=="true") smoke()
        val capture=System.getProperty("mobachess.capture")
        if(frames>=10 && capture!=null && !captured) {
            val image=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.backBufferWidth,Gdx.graphics.backBufferHeight)
            PixmapIO.writePNG(Gdx.files.absolute(capture),image,6,true); image.dispose(); captured=true
            if(System.getProperty("mobachess.smoke")=="true") Gdx.app.exit()
        }
    }

    private fun smoke() {
        fun click(square: Position) {
            val anchor=geometry.anchor(square)
            val point=viewport.project(Vector2(anchor.x,anchor.y))
            val sx=point.x.toInt(); val sy=Gdx.graphics.height-point.y.toInt()
            Gdx.input.inputProcessor.touchDown(sx,sy,0,Input.Buttons.LEFT)
            Gdx.input.inputProcessor.touchUp(sx,sy,0,Input.Buttons.LEFT)
        }
        click(Position(1,0)); click(Position(2,0)); Gdx.input.inputProcessor.keyDown(Input.Keys.ENTER)
        click(Position(6,0)); click(Position(5,0)); Gdx.input.inputProcessor.keyDown(Input.Keys.ENTER)
        check(controller.session.snapshot().turn().number()==3L) { "Local input smoke failed: ${controller.message}" }
        Gdx.app.log("MobaChess","LOCAL_SMOKE_OK turn=3")

        fun screenPoint(square: Position): Vector2 {
            val anchor=geometry.anchor(square)
            val point=viewport.project(Vector2(anchor.x,anchor.y))
            return Vector2(point.x,Gdx.graphics.height-point.y)
        }
        fun press(square: Position) {
            val p=screenPoint(square)
            Gdx.input.inputProcessor.touchDown(p.x.toInt(),p.y.toInt(),0,Input.Buttons.LEFT)
        }
        fun movePointer(square: Position) {
            val p=screenPoint(square)
            Gdx.input.inputProcessor.touchDragged(p.x.toInt(),p.y.toInt(),0)
        }
        fun release(square: Position) {
            val p=screenPoint(square)
            Gdx.input.inputProcessor.touchUp(p.x.toInt(),p.y.toInt(),0,Input.Buttons.LEFT)
        }
        val from=Position(2,0)
        val to=Position(3,0)
        val id=controller.session.snapshot().pieceAt(from).id()
        press(from)
        repeat(4) { drag.update(.05f,geometry.anchor(from)) }
        check(drag.isFloating(id)) { "Holding did not lift the piece" }
        movePointer(Position(4,2))
        release(Position(4,2))
        check(controller.session.snapshot().pieceAt(from)?.id()==id) { "Illegal drag changed the board" }
        repeat(7) { drag.update(.05f,geometry.anchor(from)) }
        check(drag.pieceId==null) { "Return animation did not finish" }
        press(from)
        movePointer(to)
        release(to)
        check(controller.session.snapshot().pieceAt(to)?.id()==id && controller.session.snapshot().turn().moveUsed()) {
            "Legal drag failed: ${controller.message}"
        }
        Gdx.input.inputProcessor.keyDown(Input.Keys.ENTER)
        val preview=Position(5,0)
        press(preview)
        movePointer(Position(4,0))
        repeat(6) { drag.update(.05f,geometry.anchor(preview)) }
        Gdx.app.log("MobaChess","DRAG_SMOKE_OK hold, invalid return, legal move")
        Gdx.input.inputProcessor.keyDown(Input.Keys.ESCAPE)
        val iconPaths=org.mobachess.assets.SkillIconCatalog.paths.filter { Gdx.files.internal(it).exists() }
        check(iconPaths.size==26 && iconPaths.all { assets.skillIcon(it)!=null }) { "Skill icons were not loaded" }
        check(org.mobachess.assets.SkillIconCatalog.slots(game.heroes.HeroType.IZBA)[0].passive)
        check(org.mobachess.assets.SkillIconCatalog.slots(game.heroes.HeroType.ARES)[1].passive)
        click(Position(7,5))
        skillHud.refresh(controller)
        val slot=renderer.hudLayout.skillSlot(0)
        val iconPoint=viewport.project(Vector2(slot.x+slot.width/2,slot.y+slot.height/2))
        val ix=iconPoint.x.toInt(); val iy=Gdx.graphics.height-iconPoint.y.toInt()
        Gdx.input.inputProcessor.touchDown(ix,iy,0,Input.Buttons.LEFT)
        Gdx.input.inputProcessor.touchUp(ix,iy,0,Input.Buttons.LEFT)
        check(controller.spell?.ability==org.mobachess.assets.SkillIconCatalog.slots(game.heroes.HeroType.GINSENG)[0].ability) {
            "HUD skill click failed: ${controller.message}"
        }
        Gdx.app.log("MobaChess","SKILL_HUD_OK 26 icons, passive slots, active HUD click")
    }

    override fun resize(width: Int, height: Int) { drag.cancel(); viewport.update(width,height,true) }
    override fun pause() { drag.cancel() }
    override fun hide() { drag.cancel(); Gdx.input.inputProcessor=null }
    override fun dispose() { stage.dispose(); skin.dispose(); renderer.dispose(); batch.dispose(); if(ownsAssets) assets.dispose() }
}

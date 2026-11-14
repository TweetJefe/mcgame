package org.mobachess.screen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Scaling
import game.heroes.HeroType
import game.moves.Promotion
import model.FigureColor
import model.Position
import org.mobachess.assets.GameAssets
import org.mobachess.presentation.*
import org.mobachess.session.SandboxSession

/** Панель редактора компонуется с тем же игровым экраном и HUD. */
class SandboxPanel(private val session: SandboxSession, private val assets: GameAssets, private val skin: Skin,
                   private val resetSelection: () -> Unit, private val cancelDrag: () -> Unit,
                   onMenu: () -> Unit) : Table() {
    private val palette = SandboxPalette(session.snapshot().heroRules())
    private val choices = Table()
    private val abilities = Table()
    private val status = Label("",skin).apply { setFontScale(.68f); setWrap(true) }
    private val info = Label("Выберите фигуру справа и нажмите на пустую клетку.",skin).apply { setFontScale(.65f); setWrap(true) }
    private val mode = TextButton("Выбор / ход",skin)
    private var color = FigureColor.WHITE
    private var group = SandboxGroup.FIGURES
    private var tool: SandboxPiece? = null
    private var signature = ""

    init {
        name="sandbox-panel"
        setBounds(1320f,0f,280f,900f)
        background=TextureRegionDrawable(TextureRegion(assets.pixel)).tint(Color(.055f,.042f,.035f,.97f))
        pad(12f); top()
        add(Label("ПЕСОЧНИЦА",skin)).padBottom(10f).row()
        add(status).width(250f).padBottom(8f).row()
        add(button("Следующий ход","sandbox-next") { cancelDrag(); session.endTurn().let { info.setText(it.message) }; resetSelection() })
            .width(250f).height(34f).padBottom(6f).row()
        add(button("Очистить доску","sandbox-clear") {
            session.clearBoard(); tool=null; resetSelection(); signature=""; info.setText("Доска и все эффекты очищены.")
        }).width(250f).height(34f).padBottom(6f).row()
        add(button("Главное меню","sandbox-menu",onMenu)).width(250f).height(32f).padBottom(10f).row()
        val colors=Table()
        listOf(FigureColor.WHITE,FigureColor.BLACK).forEach { side ->
            colors.add(button(if(side==FigureColor.WHITE) "Белые" else "Чёрные","sandbox-color-${side.name}") {
                color=side; resetSelection(); rebuildPalette()
            }).width(122f).height(30f).pad(2f)
        }
        add(colors).padBottom(5f).row()
        mode.name="sandbox-play"
        mode.label.setFontScale(.72f)
        mode.setProgrammaticChangeEvents(false)
        mode.addListener(object:ChangeListener() {
            override fun changed(event:ChangeEvent?,actor:Actor?) { cancelPlacement(); resetSelection() }
        })
        add(mode).width(250f).height(30f).padBottom(7f).row()
        val tabs=Table()
        SandboxGroup.entries.forEachIndexed { index,value ->
            tabs.add(button(value.title,"sandbox-group-${value.name}") { group=value; rebuildPalette() })
                .width(80f).height(28f).pad(2f)
            if(index%3==2) tabs.row()
        }
        add(tabs).padBottom(6f).row()
        val scroll=ScrollPane(choices,ScrollPane.ScrollPaneStyle()).apply { setScrollingDisabled(true,false) }
        add(scroll).width(250f).growY().minHeight(130f).padBottom(8f).row()
        add(abilities).width(250f).padBottom(6f).row()
        add(info).width(250f).minHeight(55f)
        rebuildPalette()
    }

    private fun button(text:String,name:String,action:()->Unit):TextButton = TextButton(text,skin).apply {
        this.name=name; label.setFontScale(.68f); setProgrammaticChangeEvents(false)
        addListener(object:ChangeListener() { override fun changed(event:ChangeEvent?,actor:Actor?) { action() } })
    }
    private fun rebuildPalette() {
        choices.clearChildren(); choices.top()
        palette.pieces(group).forEach { piece ->
            val card=Button(skin.get(TextButton.TextButtonStyle::class.java)).apply {
                name=if(piece.hero==HeroType.NONE) "sandbox-piece-${piece.body.simpleName}" else "sandbox-hero-${piece.hero.name}"
                pad(3f); setProgrammaticChangeEvents(false); isChecked=piece==tool
            }
            val region=if(piece.hero==HeroType.NONE) assets.regularSprite(piece.body.simpleName.lowercase(),color) else assets.heroSprite(piece.hero)
            if(region!=null) card.add(Image(TextureRegionDrawable(region)).apply { setScaling(Scaling.fit) }).size(37f)
            card.add(Label(piece.title,skin).apply { setFontScale(.63f); setWrap(true) }).width(183f).left().padLeft(6f)
            card.addListener(object:ChangeListener() {
                override fun changed(event:ChangeEvent?,actor:Actor?) {
                    tool=piece; resetSelection(); info.setText("${piece.title}: выберите пустую клетку."); rebuildPalette()
                }
            })
            choices.add(card).width(242f).height(46f).padBottom(4f).row()
        }
    }
    fun cancelPlacement() { tool=null; info.setText("Выберите свою фигуру на доске. Для смены стороны нажмите «Следующий ход»."); rebuildPalette() }
    fun handleCell(square:Position):Boolean {
        val selected=tool ?: return false
        cancelDrag()
        val result=session.place(selected.body,selected.hero,color,square)
        info.setText(if(result.isSuccess) "${selected.title} размещён. Можно поставить ещё." else result.message)
        resetSelection(); signature=""
        return true
    }
    fun refresh(controller:MatchController) {
        val state=session.snapshot()
        status.setText("Ход ${state.turn().number()}: ${if(state.active()==FigureColor.WHITE) "белые" else "чёрные"}\nКД и цена ульт: 0 • без лимита кастов\nОбычный ход: ${if(state.turn().moveUsed()) "сделан" else "доступен"}")
        if(tool==null) info.setText(controller.message)
        mode.isChecked=tool==null
        val key="${state.version()}:${controller.selected}:${controller.promotion}:${controller.spell}"
        if(signature==key) return
        signature=key; abilities.clearChildren()
        if(controller.promotion!=null) {
            listOf(Promotion.QUEEN to "Ферзь",Promotion.ROOK to "Ладья",Promotion.BISHOP to "Слон",Promotion.KNIGHT to "Конь").forEach { (promotion,title) ->
                abilities.add(button(title,"sandbox-promote-${promotion.name}") { cancelDrag(); controller.promote(promotion) })
                    .width(240f).height(28f).padBottom(3f).row()
            }
        } else controller.choices().forEach { choice ->
            val selected=controller.selected ?: return@forEach
            val ready=session.availability(selected,choice.ability,choice.borrowed).isSuccess
            abilities.add(button((if(choice.borrowed) "Копия: " else "")+choice.ability.displayName,"sandbox-ability-${choice.ability.id()}") {
                cancelDrag(); controller.choose(choice)
            }.apply { isDisabled=!ready }).width(242f).height(32f).padBottom(4f).row()
        }
    }
}

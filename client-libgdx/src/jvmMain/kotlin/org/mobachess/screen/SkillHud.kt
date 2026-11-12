package org.mobachess.screen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import org.mobachess.assets.GameAssets
import org.mobachess.assets.SkillIconCatalog
import org.mobachess.presentation.AbilityChoice
import org.mobachess.presentation.MatchController
import org.mobachess.render.HudLayout

/** Кнопки HUD используют тот же контроллер и доступность, что и правая панель. */
class SkillHud(private val assets: GameAssets, private val skin: Skin, private val layout: HudLayout,
               private val choose: (AbilityChoice) -> Unit) : Group() {
    private var signature = ""

    init { touchable = Touchable.childrenOnly }

    fun refresh(controller: MatchController) {
        val state = controller.session.snapshot()
        val piece = controller.selected?.let(state::pieceAt)
        val key = "${state.version()}:${piece?.id()}:${piece?.hero()}:${piece?.cooldown()}:${piece?.ultimateCharge()}:${state.turn().abilityUsed()}:${controller.spell}:${controller.promotion}"
        if (signature == key) return
        signature = key
        clearChildren()
        if (piece == null) return
        SkillIconCatalog.slots(piece.hero()).forEachIndexed { index, slot ->
            val rect = layout.skillSlot(index)
            val choice = slot.ability?.let { AbilityChoice(it) }
            val ready = choice != null && controller.promotion == null && !state.turn().abilityUsed() &&
                !state.outcome().isFinished && controller.session.availability(piece.position(), choice.ability, false).isSuccess
            val region = slot.iconPath?.let(assets::skillIcon)
            val button: Button = if (region != null) {
                val image = TextureRegionDrawable(region)
                val border = TextureRegionDrawable(TextureRegion(assets.pixel))
                ImageButton(ImageButton.ImageButtonStyle().apply {
                    up = border.tint(Color(.04f, .04f, .04f, 1f))
                    over = border.tint(Color.valueOf("D3B672")); checked = over
                    imageUp = image
                    imageDisabled = if (slot.passive) image else image.tint(Color(.35f, .35f, .35f, 1f))
                }).apply { imageCell.size(rect.width - 3f, rect.height - 3f) }
            } else TextButton(if (index == 0) "СКИЛЛ" else "УЛЬТА", skin).apply { label.setFontScale(.52f) }
            button.name = "hud-skill-$index"
            button.setBounds(rect.x, rect.y, rect.width, rect.height)
            button.setProgrammaticChangeEvents(false)
            button.isDisabled = !ready
            button.isChecked = choice != null && controller.spell == choice
            button.addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) { if (ready) choose(checkNotNull(choice)) }
            })
            val text = Label(slot.title + if (slot.passive) "" else if (ready) "" else "\nСейчас недоступно", skin).apply {
                setFontScale(.72f); setWrap(true)
            }
            button.addListener(Tooltip(text).apply {
                container.width(285f).pad(10f)
                container.background = TextureRegionDrawable(TextureRegion(assets.pixel)).tint(Color.valueOf("17212B"))
            })
            addActor(button)
        }
    }
}

package org.mobachess.screen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Scaling
import game.heroes.HeroType
import org.mobachess.assets.GameAssets
import org.mobachess.presentation.HeroCatalog

data class HeroCardState(val enabled: Boolean = true, val selected: Boolean = false, val caption: String = "")

/** Общая сетка каталога и выбора: четыре группы по шахматному телу. */
class HeroGrid(assets: GameAssets, catalog: HeroCatalog, skin: Skin,
               state: (HeroType) -> HeroCardState, choose: (HeroType) -> Unit) : Table() {
    init {
        catalog.groups.forEach { role ->
            val column = Table().top().pad(10f)
            column.name = "group-${role.name}"
            column.add(Label(catalog.groupName(role), skin).apply { color = Color.valueOf("E5C88B") })
                .colspan(2).left().padBottom(18f).row()
            catalog.heroes(role).forEachIndexed { index, hero ->
                val view = state(hero)
                val card = Button(skin, "card").apply {
                    name = "hero-${hero.name}"
                    isDisabled = !view.enabled
                    isChecked = view.selected
                    pad(6f)
                }
                val region = assets.heroSprite(hero)
                if (region != null) card.add(Image(TextureRegionDrawable(region)).apply {
                    setScaling(Scaling.fit)
                    if (!view.enabled) color.a = .28f
                }).width(128f).height(84f).row()
                else card.add(Label("?", skin)).height(84f).row()
                card.add(Label(catalog.name(hero), skin, "small").apply {
                    setFontScale(.94f)
                    if (!view.enabled) color = Color.valueOf("737C83")
                }).padTop(4f).row()
                card.add(Label(view.caption.ifBlank { " " }, skin, "small").apply { setFontScale(.78f) })
                    .padTop(3f).row()
                card.addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent?, actor: Actor?) { if (!card.isDisabled) choose(hero) }
                })
                column.add(card).width(156f).height(136f).pad(4f)
                if (index % 2 == 1) column.row()
            }
            if (catalog.heroes(role).size % 2 == 1) column.add().width(156f)
            add(column).expandX().top()
        }
    }
}

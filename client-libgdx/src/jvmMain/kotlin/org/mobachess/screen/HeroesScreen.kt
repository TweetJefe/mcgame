package org.mobachess.screen

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.*
import game.heroes.HeroType
import org.mobachess.assets.GameAssets
import org.mobachess.presentation.HeroCatalog
import org.mobachess.presentation.HeroGuide

class HeroesScreen(assets: GameAssets, navigation: ScreenNavigation, private val catalog: HeroCatalog) : SimpleScreen(assets, navigation) {
    private val guide = HeroGuide()
    private val detailLayer = Group()
    private var selected: HeroType? = null
    val detailOpen: Boolean get() = selected != null
    private val backButton = button("В меню", "back", ::back)

    init {
        val header = Table()
        header.add(backButton).width(160f).height(44f)
        header.add(label("ГЕРОИ").apply { setFontScale(1.5f); color = gold }).expandX()
        header.add(button("Локальный матч", "local", navigation::localMatch)).width(230f).height(44f)
        root.add(header).growX().padBottom(20f).row()
        root.add(label("Выберите портрет героя", true).apply { color = muted }).left().padBottom(18f).row()
        val grid = Table().top()
        catalog.groups.forEach { role ->
            val column = Table().top().pad(10f)
            column.name = "group-${role.name}"
            column.add(label(catalog.groupName(role)).apply { color = gold }).colspan(2).left().padBottom(18f).row()
            catalog.heroes(role).forEachIndexed { index, hero ->
                val region = assets.heroSprite(hero)
                if (region != null) {
                    val card = HeroPortraitCard(region, skin) { reveal(hero, it) }
                    card.name = "hero-${hero.name}"
                    column.add(card).width(130f).height(184f).pad(6f)
                } else {
                    column.add(button("?", "hero-${hero.name}") {}).width(130f).height(184f).pad(6f)
                }
                if (index % 2 == 1) column.row()
            }
            grid.add(column).expandX().top()
        }
        root.add(grid).grow().top().row()
        root.add(label("Баба-яга и Supernova появляются через способности других героев и не участвуют в драфте.", true)
            .apply { color = muted }).left().padTop(10f)
        detailLayer.setBounds(0f, 0f, 1600f, 790f)
        detailLayer.isVisible = false
        stage.addActor(detailLayer)
    }

    private fun reveal(hero: HeroType, card: HeroPortraitCard) {
        if (detailOpen) return
        selected = hero
        backButton.setText("К героям")
        detailLayer.clearChildren(); detailLayer.clearActions()
        detailLayer.color.a = 0f
        detailLayer.isVisible = true
        val backdrop = Table().apply {
            background = this@HeroesScreen.background("0C1218")
            touchable = Touchable.enabled
            setBounds(20f, 25f, 1560f, 765f)
        }
        detailLayer.addActor(backdrop)
        val source = card.localToStageCoordinates(Vector2(2f, 2f))
        val region = card.fullRegion
        val fit = minOf(570f / region.regionWidth, 650f / region.regionHeight)
        val width = region.regionWidth * fit
        val height = region.regionHeight * fit
        val target = Rectangle(365f - width / 2f, 90f + (650f - height) / 2f, width, height)
        detailLayer.addActor(HeroRevealActor(card.portraitRegion, region,
            Rectangle(source.x, source.y, card.width - 4f, card.height - 4f), target))

        val entry = guide.entry(hero)
        val panel = Table().top().left().pad(26f)
        panel.add(label(entry.name).apply { setFontScale(1.65f); color = gold }).left().padBottom(28f).row()
        entry.paragraphs.forEach { paragraph ->
            val heading = paragraph.substringBefore(" — ")
            val description = paragraph.substringAfter(" — ", paragraph)
            panel.add(label(heading.uppercase()).apply { color = gold; setFontScale(.86f) }).left().padBottom(10f).row()
            panel.add(label(description).apply { setWrap(true); setFontScale(.88f) })
                .width(650f).left().padBottom(25f).row()
        }
        panel.add(label("КД — собственные ходы. Стоимость — собственные взятия, включая пешек.\nХод применения не входит в срок эффекта.", true)
            .apply { setWrap(true); color = muted }).width(650f).left().padTop(8f).row()
        val scroll = ScrollPane(panel, ScrollPane.ScrollPaneStyle()).apply {
            name = "hero-description"
            setBounds(760f, 65f, 730f, 680f)
            setScrollingDisabled(true, false)
            setFadeScrollBars(false)
            color.a = 0f
            addAction(Actions.sequence(Actions.delay(.18f), Actions.fadeIn(.32f)))
        }
        detailLayer.addActor(scroll)
        stage.scrollFocus = scroll
        detailLayer.addAction(Actions.fadeIn(.18f))
    }

    override fun back() {
        if (!detailOpen) { navigation.menu(); return }
        selected = null
        backButton.setText("В меню")
        stage.scrollFocus = null
        detailLayer.clearActions()
        detailLayer.addAction(Actions.sequence(Actions.fadeOut(.18f), Actions.run {
            detailLayer.isVisible = false
            detailLayer.clearChildren()
        }))
    }
}

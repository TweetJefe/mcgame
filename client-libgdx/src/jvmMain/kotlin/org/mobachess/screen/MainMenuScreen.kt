package org.mobachess.screen

import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Scaling
import org.mobachess.assets.GameAssets
import org.mobachess.presentation.HeroCatalog

class MainMenuScreen(assets: GameAssets, navigation: ScreenNavigation, catalog: HeroCatalog) : SimpleScreen(assets, navigation) {
    init {
        val menu = Table().left()
        menu.add(label("MOBA CHESS").apply { setFontScale(2.2f); color = gold }).left().padBottom(18f).row()
        menu.add(label("Шахматы с героями", true).apply { color = muted }).left().padBottom(48f).row()
        menu.add(button("Играть — скоро", "play") {}).width(360f).height(64f).left().padBottom(14f).row()
        menu.findActor<com.badlogic.gdx.scenes.scene2d.ui.TextButton>("play").isDisabled = true
        menu.add(button("Герои", "heroes", navigation::heroes)).width(360f).height(64f).left().padBottom(14f).row()
        menu.add(button("Локальный матч", "local", navigation::localMatch)).width(360f).height(64f).left().padBottom(14f).row()
        menu.add(button("Песочница", "sandbox", navigation::sandbox)).width(360f).height(64f).left().padBottom(26f).row()
        menu.add(label("Драфт → расстановка → игра за обе стороны", true).apply { color = muted }).left().row()
        val art = Table()
        catalog.groups.take(3).forEach { role -> catalog.heroes(role).firstOrNull()?.let { hero ->
            assets.heroSprite(hero)?.let {
                art.add(Image(TextureRegionDrawable(it)).apply { setScaling(Scaling.fit) }).width(225f).height(340f).pad(4f)
            }
        } }
        root.add(menu).width(580f).left().padLeft(60f)
        root.add(art).expand().fill()
    }
}

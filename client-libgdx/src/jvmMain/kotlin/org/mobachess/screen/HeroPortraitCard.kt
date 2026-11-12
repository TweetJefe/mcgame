package org.mobachess.screen

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Scaling

/** Портрет ограничен UV-рамкой: модель приближена и не выходит в соседнюю карточку. */
class HeroPortraitCard(val fullRegion: TextureRegion, skin: Skin, choose: (HeroPortraitCard) -> Unit) : Button(skin, "card") {
    val portraitRegion: TextureRegion

    init {
        val cropHeight = (fullRegion.regionHeight * .62f).toInt().coerceAtLeast(1)
        val cropWidth = (cropHeight * 130f / 184f).toInt().coerceIn(1, fullRegion.regionWidth)
        val left = (fullRegion.regionWidth - cropWidth) / 2
        val top = (fullRegion.regionHeight * .035f).toInt()
        portraitRegion = TextureRegion(fullRegion.texture, fullRegion.regionX + left, fullRegion.regionY + top,
            cropWidth, cropHeight.coerceAtMost(fullRegion.regionHeight - top))
        pad(2f)
        add(Image(TextureRegionDrawable(portraitRegion)).apply { setScaling(Scaling.fill) }).grow()
        addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) { choose(this@HeroPortraitCard) }
        })
    }
}

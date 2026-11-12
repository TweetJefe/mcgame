package org.mobachess.render

import com.badlogic.gdx.math.Rectangle

/** Общая геометрия картинки HUD и интерактивных ячеек в координатах исходного PNG. */
class HudLayout(sourceWidth: Int, sourceHeight: Int) {
    val bounds = Rectangle(310f, -95f, 760f, 760f * sourceHeight / sourceWidth)
    private val scale = bounds.width / sourceWidth

    fun skillSlot(index: Int): Rectangle {
        require(index in 0..1)
        val left = if (index == 0) 76f else 219f
        return Rectangle(bounds.x + left * scale, bounds.y + bounds.height - 336f * scale, 94f * scale, 92f * scale)
    }
}

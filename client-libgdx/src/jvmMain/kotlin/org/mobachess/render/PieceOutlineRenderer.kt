package org.mobachess.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion

/** Тонкий контур по альфа-каналу: смещённые силуэты рисуются под исходным спрайтом. */
class PieceOutlineRenderer {
    private val color = Color(0f, 0f, 0f, .92f)

    fun draw(batch: SpriteBatch, region: TextureRegion, x: Float, y: Float,
             width: Float, height: Float, thickness: Float) {
        val previousColor = batch.packedColor
        batch.color = color
        for (dy in -1..1) for (dx in -1..1) {
            if (dx == 0 && dy == 0) continue
            val distance = if (dx != 0 && dy != 0) thickness * .7071f else thickness
            batch.draw(region, x + dx * distance, y + dy * distance, width, height)
        }
        batch.setPackedColor(previousColor)
    }
}

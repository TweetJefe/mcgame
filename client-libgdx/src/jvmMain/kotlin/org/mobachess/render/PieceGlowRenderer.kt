package org.mobachess.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import model.FigureColor
import kotlin.math.roundToInt

/** Кеш мягких силуэтов: исходные PNG не меняются, игровые данные не хранятся. */
class PieceGlowRenderer : Disposable {
    private data class Glow(val texture: Texture, val paddingX: Float, val paddingY: Float)

    private val cache = mutableMapOf<TextureRegion, Glow>()

    fun draw(batch: SpriteBatch, region: TextureRegion, side: FigureColor,
             x: Float, y: Float, width: Float, height: Float) {
        val glow = cache.getOrPut(region) { create(region) }
        val padX = width * glow.paddingX
        val padY = height * glow.paddingY
        val previousColor = batch.packedColor
        batch.color = if (side == FigureColor.WHITE) Color(1f, 1f, 1f, .72f)
                      else Color(0f, 0f, 0f, .80f)
        batch.draw(glow.texture, x - padX, y - padY, width + 2 * padX, height + 2 * padY)
        batch.setPackedColor(previousColor)
    }

    private fun create(region: TextureRegion): Glow {
        // Ореол рассчитывается один раз в разрешении, близком к экранному размеру фигуры.
        val scale = 128f / maxOf(region.regionWidth, region.regionHeight)
        val bodyWidth = (region.regionWidth * scale).roundToInt().coerceAtLeast(1)
        val bodyHeight = (region.regionHeight * scale).roundToInt().coerceAtLeast(1)
        val padding = 7
        val width = bodyWidth + padding * 2
        val height = bodyHeight + padding * 2
        val alpha = FloatArray(width * height)
        val data = region.texture.textureData
        if (!data.isPrepared) data.prepare()
        val source = data.consumePixmap()
        try {
            for (y in 0 until bodyHeight) for (x in 0 until bodyWidth) {
                val sourceX = region.regionX + (x * region.regionWidth / bodyWidth)
                val sourceY = region.regionY + (y * region.regionHeight / bodyHeight)
                alpha[(y + padding) * width + x + padding] = (source.getPixel(sourceX, sourceY) and 255) / 255f
            }
        } finally {
            if (data.disposePixmap()) source.dispose()
        }

        val expanded = FloatArray(alpha.size)
        for (y in 0 until height) for (x in 0 until width) {
            var value = 0f
            for (dy in -2..2) for (dx in -2..2) {
                val px = x + dx; val py = y + dy
                if (px in 0 until width && py in 0 until height) {
                    value = maxOf(value, alpha[py * width + px])
                }
            }
            expanded[y * width + x] = value
        }
        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
        pixmap.setBlending(Pixmap.Blending.None)
        for (y in 0 until height) for (x in 0 until width) {
            var sum = 0f
            for (dy in -3..3) for (dx in -3..3) {
                val px = x + dx; val py = y + dy
                if (px in 0 until width && py in 0 until height) sum += expanded[py * width + px]
            }
            pixmap.setColor(1f, 1f, 1f, sum / 49f)
            pixmap.drawPixel(x, y)
        }
        val texture = Texture(pixmap)
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        pixmap.dispose()
        return Glow(texture, padding.toFloat() / bodyWidth, padding.toFloat() / bodyHeight)
    }

    override fun dispose() {
        cache.values.forEach { it.texture.dispose() }
        cache.clear()
    }
}

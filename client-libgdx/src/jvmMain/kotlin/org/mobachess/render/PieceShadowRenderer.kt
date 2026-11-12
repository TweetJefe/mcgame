package org.mobachess.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import game.state.PieceState
import model.figures.Pawn

/** Мягкая контактная тень: один общий ресурс, размер учитывает глубину клетки. */
class PieceShadowRenderer : Disposable {
    private val texture: Texture

    init {
        val pixmap = Pixmap(96, 48, Pixmap.Format.RGBA8888)
        pixmap.setBlending(Pixmap.Blending.None)
        for (y in 0 until pixmap.height) for (x in 0 until pixmap.width) {
            val dx = (x + .5f - pixmap.width / 2f) / (pixmap.width / 2f)
            val dy = (y + .5f - pixmap.height / 2f) / (pixmap.height / 2f)
            val falloff = (1f - dx * dx - dy * dy).coerceIn(0f, 1f)
            pixmap.setColor(0f, 0f, 0f, .30f * falloff * falloff)
            pixmap.drawPixel(x, y)
        }
        texture = Texture(pixmap)
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        pixmap.dispose()
    }

    fun draw(batch: SpriteBatch, piece: PieceState, anchor: Point, figureWidth: Float, depthScale: Float) {
        // Пешечные герои исключаются так же, как обычные пешки; после смены тела правило меняется автоматически.
        if (piece.type() == Pawn::class.java) return
        val width = figureWidth * .82f
        val height = 19f * depthScale
        val previousColor = batch.packedColor
        batch.color = Color.WHITE
        batch.draw(texture, anchor.x - width / 2f, anchor.y - height / 2f - 2f * depthScale, width, height)
        batch.setPackedColor(previousColor)
    }

    override fun dispose() = texture.dispose()
}

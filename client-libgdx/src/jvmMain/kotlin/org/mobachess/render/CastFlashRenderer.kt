package org.mobachess.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.Disposable
import game.heroes.faun.FaunView
import game.state.GameSnapshot
import org.mobachess.presentation.CastFlash
import org.mobachess.presentation.HeroVisuals

/** Общий мягкий ореол за фигурами: расширение и затухание без изменения спрайтов. */
class CastFlashRenderer : Disposable {
    private val texture: Texture
    private val colors = mutableMapOf<String, Color>()

    init {
        val pixmap = Pixmap(128, 128, Pixmap.Format.RGBA8888)
        pixmap.setBlending(Pixmap.Blending.None)
        for (y in 0 until 128) for (x in 0 until 128) {
            val dx = (x + .5f - 64f) / 64f
            val dy = (y + .5f - 64f) / 64f
            val falloff = (1f - dx * dx - dy * dy).coerceIn(0f, 1f)
            pixmap.setColor(1f, 1f, 1f, falloff * falloff)
            pixmap.drawPixel(x, y)
        }
        texture = Texture(pixmap)
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        pixmap.dispose()
    }

    fun draw(batch: SpriteBatch, flashes: List<CastFlash>, state: GameSnapshot, geometry: BoardGeometry) {
        if (flashes.isEmpty()) return
        val previous = batch.packedColor
        flashes.forEach { flash ->
            val piece = state.pieces()[flash.actorId]
            if (piece != null && !FaunView.visible(state, piece)) return@forEach
            val square = piece?.position() ?: flash.origin
            val anchor = geometry.anchor(square)
            val depth = 1f - geometry.display(square).row * .027f
            val progress = (flash.age / HeroVisuals.CAST_FLASH_SECONDS).coerceIn(0f, 1f)
            val size = 138f * depth * (1f + .28f * progress)
            val hex = HeroVisuals.castColor(flash.hero)
            val color = colors.getOrPut(hex) { Color.valueOf(hex) }
            val fade = ((progress - .15f) / .85f).coerceIn(0f, 1f)
            val opacity = 1f - fade * fade * (3f - 2f * fade)
            batch.setColor(color.r, color.g, color.b, .85f * opacity)
            batch.draw(texture, anchor.x - size / 2f, anchor.y + 40f * depth - size / 2f, size, size)
        }
        batch.setPackedColor(previous)
    }

    override fun dispose() { texture.dispose(); colors.clear() }
}

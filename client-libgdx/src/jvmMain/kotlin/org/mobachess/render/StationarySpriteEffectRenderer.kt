package org.mobachess.render

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import game.state.GameSnapshot
import game.state.PieceState
import org.mobachess.assets.GameAssets
import org.mobachess.presentation.animation.SpriteEffectFactory
import org.mobachess.presentation.animation.SpriteEffectTimeline

/** Однократное проигрывание спрайтшита поверх фигуры, с учётом перспективы доски. */
class StationarySpriteEffectRenderer(
    assets: GameAssets,
    path: String,
    factory: SpriteEffectFactory,
    columns: Int = 4,
    rows: Int = 4
) {
    private val timeline = SpriteEffectTimeline(factory)
    private val texture = assets.texture(path)
    private val frames = TextureRegion.split(texture, texture.width / columns, texture.height / rows)
        .flatMap { it.toList() }

    fun update(session: Any, snapshot: GameSnapshot, delta: Float) = timeline.update(session, snapshot, delta)

    fun modelFor(piece: PieceState): PieceState = timeline.modelFor(piece, frames.size)

    fun draw(batch: SpriteBatch, geometry: BoardGeometry) {
        val previousColor = batch.packedColor
        batch.setColor(1f, 1f, 1f, 1f)
        timeline.active.forEach { playing ->
            val effect = playing.effect
            val anchor = geometry.anchor(effect.square)
            val scale = 1f - geometry.display(effect.square).row * .027f
            val size = effect.size * scale
            val frame = playing.frameIndex(frames.size)
            batch.draw(frames[frame], anchor.x - size / 2, anchor.y + 52f * scale - size / 2, size, size)
        }
        batch.setPackedColor(previousColor)
    }
}

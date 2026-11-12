package org.mobachess.screen

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.Actor

/** Портрет плавно раскрывается в полную модель одновременно с перемещением. */
class HeroRevealActor(private val portrait: TextureRegion, private val full: TextureRegion,
                      private val origin: Rectangle, private val destination: Rectangle) : Actor() {
    private val frame = TextureRegion(full)
    private var elapsed = 0f
    private var progress = 0f

    init { setBounds(origin.x, origin.y, origin.width, origin.height) }

    override fun act(delta: Float) {
        super.act(delta)
        elapsed += delta
        progress = Interpolation.pow3Out.apply((elapsed / .55f).coerceIn(0f, 1f))
        setBounds(MathUtils.lerp(origin.x, destination.x, progress), MathUtils.lerp(origin.y, destination.y, progress),
            MathUtils.lerp(origin.width, destination.width, progress), MathUtils.lerp(origin.height, destination.height, progress))
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        frame.setRegion(MathUtils.lerp(portrait.u, full.u, progress), MathUtils.lerp(portrait.v, full.v, progress),
            MathUtils.lerp(portrait.u2, full.u2, progress), MathUtils.lerp(portrait.v2, full.v2, progress))
        val previous = batch.packedColor
        batch.setColor(1f, 1f, 1f, color.a * parentAlpha)
        batch.draw(frame, x, y, width, height)
        batch.setPackedColor(previous)
    }
}

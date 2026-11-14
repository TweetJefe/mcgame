package org.mobachess.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Interpolation
import game.heroes.ginseng.GinsengAbility
import game.state.GameSnapshot
import org.mobachess.assets.GameAssets
import kotlin.math.atan2

/** Спрайтшит анимирует энергию; положение и поворот рассчитываются независимо от кадров. */
class GinsengTheftRenderer(assets: GameAssets) {
    private data class Flight(val from: model.Position, val to: model.Position, val duration: Float, var age: Float = 0f)
    private val texture = assets.texture("art/animations/ginseng/ginseng_ult1.png")
    private val frames = TextureRegion.split(texture, texture.width / 4, texture.height / 4).flatMap { it.toList() }
    private var source: Any? = null
    private var previous: GameSnapshot? = null
    private val flights = mutableListOf<Flight>()

    fun update(session: Any, state: GameSnapshot, delta: Float) {
        val before = previous
        if (source !== session || before == null || state.events().size < before.events().size) {
            source = session; previous = state; flights.clear(); return
        }
        flights.forEach { it.age += delta.coerceAtLeast(0f) }
        flights.removeAll { it.age >= it.duration }
        state.events().drop(before.events().size).filter { it.isAbility(GinsengAbility.COPY_SPELL) && it.copiedAbility() != null }.forEach { event ->
            val from = before.pieces()[event.targetId()]?.position()
            val to = state.pieces()[event.actorId()]?.position()
            if (from != null && to != null) {
                val distance = maxOf(kotlin.math.abs(from.row-to.row),kotlin.math.abs(from.column-to.column))
                flights.add(Flight(from,to,(.4f+distance*.07f).coerceAtMost(.9f)))
            }
        }
        previous = state
    }

    fun draw(batch: SpriteBatch, geometry: BoardGeometry) {
        val old = batch.packedColor
        flights.forEach { flight ->
            val start=geometry.anchor(flight.from); val end=geometry.anchor(flight.to)
            val t=Interpolation.smooth.apply((flight.age/flight.duration).coerceIn(0f,1f))
            val x=MathUtils.lerp(start.x,end.x,t); val y=MathUtils.lerp(start.y,end.y,t)+42f
            val angle=atan2(end.y-start.y,end.x-start.x)*MathUtils.radiansToDegrees
            val frame=frames[(flight.age*24).toInt()%frames.size]
            val alpha=((flight.duration-flight.age)/.12f).coerceIn(0f,1f)
            batch.setColor(1f,1f,1f,alpha)
            // Ядро исходного рисунка находится справа от центра кадра.
            batch.draw(frame,x-74f,y-48f,74f,48f,96f,96f,1f,1f,angle)
        }
        batch.setPackedColor(old)
    }
}

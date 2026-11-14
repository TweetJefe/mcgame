package org.mobachess.presentation.animation

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import game.heroes.ginseng.GinsengAbility
import game.history.GameEvent
import game.state.GameSnapshot
import model.Position
import org.mobachess.render.BoardGeometry
import org.mobachess.render.Point

/** Две фазы: подъём на месте и ускоренное мягкое приземление. */
class GinsengDisplaceAnimation(override val pieceId: Long, private val origin: Position,
                               override val destination: Position) : PieceAnimation {
    override val duration = .68f
    override fun pose(age: Float, geometry: BoardGeometry): PiecePose {
        val from = geometry.anchor(origin)
        val to = geometry.anchor(destination)
        val lift = Interpolation.sineOut.apply((age / .30f).coerceIn(0f, 1f))
        val travel = Interpolation.smooth.apply(((age - .30f) / .38f).coerceIn(0f, 1f))
        val ground = Point(MathUtils.lerp(from.x, to.x, travel), MathUtils.lerp(from.y, to.y, travel))
        val depth = MathUtils.lerp(1f - geometry.display(origin).row * .027f,
            1f - geometry.display(destination).row * .027f, travel)
        val direction = if (to.x < from.x) -1f else 1f
        return PiecePose(Point(ground.x, ground.y + 42f * depth * lift * (1f - travel)), ground,
            direction * 9f * lift * (1f - travel), depth)
    }
}

class GinsengDisplaceAnimationFactory : AbilityAnimationFactory {
    override fun create(event: GameEvent, before: GameSnapshot, after: GameSnapshot): PieceAnimation? {
        if (!event.isAbility(GinsengAbility.DISPLACE)) return null
        val id = event.targetId() ?: return null
        val from = before.pieces()[id]?.position() ?: return null
        val to = event.to() ?: return null
        if (from == to || after.pieces()[id]?.position() != to) return null
        return GinsengDisplaceAnimation(id, from, to)
    }
}

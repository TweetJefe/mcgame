package org.mobachess.presentation.animation

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import model.Position
import org.mobachess.render.BoardGeometry
import org.mobachess.render.Point

/** Общая траектория плавного движения вдоль доски; не зависит от героя или способности. */
class BoardSlideAnimation(override val pieceId: Long, private val origin: Position,
                        override val destination: Position) : PieceAnimation {
    override val duration = .45f
    override fun pose(age: Float, geometry: BoardGeometry): PiecePose {
        val progress = Interpolation.smooth.apply((age / duration).coerceIn(0f, 1f))
        val from = geometry.anchor(origin)
        val to = geometry.anchor(destination)
        val position = Point(MathUtils.lerp(from.x, to.x, progress), MathUtils.lerp(from.y, to.y, progress))
        val depth = MathUtils.lerp(1f - geometry.display(origin).row * .027f,
            1f - geometry.display(destination).row * .027f, progress)
        return PiecePose(position, position, 0f, depth)
    }
}

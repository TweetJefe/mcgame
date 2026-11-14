package org.mobachess.presentation.animation

import game.heroes.ares.AresAbility
import game.history.GameEvent
import game.state.GameSnapshot
import model.Position
import model.figures.King
import org.mobachess.render.BoardGeometry
import org.mobachess.render.Point
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Interpolation

data class SpearPlan(val origin: Position, val end: Position, val carriedId: Long?,
                     val contact: Position?, val landing: Position?) {
    val duration = .55f
    companion object {
        fun create(event: GameEvent, before: GameSnapshot, after: GameSnapshot): SpearPlan? {
            if (!event.isAbility(AresAbility.SPEAR)) return null
            val origin = event.from() ?: return null
            val direction = event.to() ?: return null
            val dr = (direction.row-origin.row).compareTo(0)
            val dc = (direction.column-origin.column).compareTo(0)
            if (dr==0 && dc==0) return null
            val range = if(dr==0 || dc==0) 3 else 2
            var end = origin
            var caught: game.state.PieceState? = null
            for (step in 1..range) {
                val cell = Position(origin.row+dr*step,origin.column+dc*step)
                if(cell.row !in 0..7 || cell.column !in 0..7) break
                end = cell
                val hit=before.pieceAt(cell)
                if(hit!=null) {
                    if(caught!=null || hit.color()==event.color() || hit.type()==King::class.java || hit.isBoxed) break
                    caught=hit
                }
                // Реальное перемещение уже завершено движком; не продлеваем полёт за сработавшую ловушку.
                if(caught!=null && before.terrain().hostileSquares(before.pieces(),caught.color()).contains(cell)) break
            }
            if(end==origin) return null
            val landing=caught?.let { after.pieces()[it.id()]?.position() }
            return SpearPlan(origin,end,caught?.id(),caught?.position(),landing)
        }
    }
}

class SpearCarryAnimation(private val plan: SpearPlan, override val pieceId: Long,
                           private val origin: Position, override val destination: Position) : PieceAnimation {
    override val duration = plan.duration
    override fun pose(age: Float, geometry: BoardGeometry): PiecePose {
        fun distance(a:Position,b:Position)=maxOf(kotlin.math.abs(a.row-b.row),kotlin.math.abs(a.column-b.column)).toFloat()
        val contact=distance(plan.origin,origin)/distance(plan.origin,plan.end)
        val flight=Interpolation.smooth.apply((age/duration).coerceIn(0f,1f))
        val t=if(contact>=1f) 1f else ((flight-contact)/(1f-contact)).coerceIn(0f,1f)
        val from=geometry.anchor(origin);val to=geometry.anchor(destination)
        val point=Point(MathUtils.lerp(from.x,to.x,t),MathUtils.lerp(from.y,to.y,t))
        return PiecePose(point,point,0f,MathUtils.lerp(1f-geometry.display(origin).row*.027f,1f-geometry.display(destination).row*.027f,t))
    }
}

class AresCarryAnimationFactory : AbilityAnimationFactory {
    override fun create(event: GameEvent,before: GameSnapshot,after: GameSnapshot): PieceAnimation? {
        val plan=SpearPlan.create(event,before,after) ?: return null
        val id=plan.carriedId ?: return null
        val from=plan.contact ?: return null
        val to=plan.landing ?: return null
        return if(from==to) null else SpearCarryAnimation(plan,id,from,to)
    }
}

class AresSpearFlightFactory : org.mobachess.render.SpriteFlightFactory {
    override fun create(event:GameEvent,before:GameSnapshot,after:GameSnapshot):org.mobachess.render.SpriteFlight? =
        SpearPlan.create(event,before,after)?.let { org.mobachess.render.SpriteFlight(it.origin,it.end,it.duration) }
}

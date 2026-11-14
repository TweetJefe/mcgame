package org.mobachess.render

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Interpolation
import game.history.GameEvent
import game.state.GameSnapshot
import model.Position
import org.mobachess.assets.GameAssets
import kotlin.math.atan2

data class SpriteFlight(val from:Position,val to:Position,val duration:Float)
fun interface SpriteFlightFactory { fun create(event:GameEvent,before:GameSnapshot,after:GameSnapshot):SpriteFlight? }

/** Общая отрисовка летящего спрайтшита. Геометрию конкретного скилла задаёт фабрика. */
class SpriteProjectileRenderer(assets:GameAssets,path:String,private val factory:SpriteFlightFactory) {
    private data class Playing(val flight:SpriteFlight,var age:Float=0f)
    private val texture=assets.texture(path)
    private val frames=TextureRegion.split(texture,texture.width/4,texture.height/4).flatMap { it.toList() }
    private var source:Any?=null
    private var previous:GameSnapshot?=null
    private val active=mutableListOf<Playing>()
    fun update(session:Any,state:GameSnapshot,delta:Float) {
        val before=previous
        if(source!==session || before==null || state.events().size<before.events().size) {
            source=session;previous=state;active.clear();return
        }
        active.forEach { it.age+=delta.coerceAtLeast(0f) }
        active.removeAll { it.age>=it.flight.duration }
        state.events().drop(before.events().size).forEach { event -> factory.create(event,before,state)?.let { active.add(Playing(it)) } }
        previous=state
    }
    fun draw(batch:SpriteBatch,geometry:BoardGeometry) {
        val old=batch.packedColor
        active.forEach { playing ->
            val f=playing.flight;val from=geometry.anchor(f.from);val to=geometry.anchor(f.to)
            val t=Interpolation.smooth.apply((playing.age/f.duration).coerceIn(0f,1f))
            val x=MathUtils.lerp(from.x,to.x,t);val y=MathUtils.lerp(from.y,to.y,t)+38f
            val angle=atan2(to.y-from.y,to.x-from.x)*MathUtils.radiansToDegrees
            batch.setColor(1f,1f,1f,1f)
            batch.draw(frames[(playing.age*24).toInt()%frames.size],x-88f,y-48f,88f,48f,96f,96f,1f,1f,angle)
        }
        batch.setPackedColor(old)
    }
}

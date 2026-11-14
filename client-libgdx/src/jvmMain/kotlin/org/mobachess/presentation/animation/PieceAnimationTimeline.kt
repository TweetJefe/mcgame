package org.mobachess.presentation.animation

import game.history.GameEvent
import game.state.GameSnapshot
import org.mobachess.render.BoardGeometry

/** Наблюдение за подтверждёнными событиями; рендеру неизвестны конкретные способности. */
class PieceAnimationTimeline(private val factories: List<AbilityAnimationFactory> = listOf(GinsengDisplaceAnimationFactory(), HookPullAnimationFactory(), ChronomlinGrappleAnimationFactory(), AresCarryAnimationFactory())) {
    private data class Playing(val animation: PieceAnimation, val age: Float)
    private var source: Any? = null
    private var previous: GameSnapshot? = null
    private val playing = mutableMapOf<Long, Playing>()

    fun update(session: Any, snapshot: GameSnapshot, delta: Float) {
        val before = previous
        if (source !== session || before == null || snapshot.events().size < before.events().size) {
            source = session; previous = snapshot; playing.clear(); return
        }
        playing.replaceAll { _, value -> value.copy(age = value.age + delta.coerceAtLeast(0f)) }
        playing.entries.removeAll { (_, p) -> p.age >= p.animation.duration ||
            snapshot.pieces()[p.animation.pieceId]?.position() != p.animation.destination }
        snapshot.events().drop(before.events().size).filter { it.kind() == GameEvent.Kind.ABILITY_USED }.forEach { event ->
            factories.firstNotNullOfOrNull { it.create(event, before, snapshot) }?.let {
                playing[it.pieceId] = Playing(it, 0f)
            }
        }
        previous = snapshot
    }

    fun isPlaying(id: Long): Boolean = id in playing
    fun pose(id: Long, geometry: BoardGeometry): PiecePose? = playing[id]?.let { it.animation.pose(it.age, geometry) }
}

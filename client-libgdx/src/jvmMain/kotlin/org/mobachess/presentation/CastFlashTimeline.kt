package org.mobachess.presentation

import game.heroes.HeroType
import game.history.GameEvent
import game.state.GameSnapshot
import game.state.PieceState
import model.Position

data class CastFlash(val actorId: Long, val hero: HeroType, val origin: Position, val age: Float = 0f)

/** Читает только новые подтверждённые касты. Загруженная история не проигрывается заново. */
class CastFlashTimeline {
    private var source: Any? = null
    private var observed = 0
    private var previousPieces: Map<Long, PieceState> = emptyMap()
    var active: List<CastFlash> = emptyList(); private set

    fun update(session: Any, snapshot: GameSnapshot, delta: Float) {
        if (source !== session || snapshot.events().size < observed) {
            source = session
            observed = snapshot.events().size
            previousPieces = snapshot.pieces()
            active = emptyList()
            return
        }
        advance(delta)
        val added = snapshot.events().drop(observed).mapNotNull { event ->
            if (event.kind() != GameEvent.Kind.ABILITY_USED) return@mapNotNull null
            val id = event.actorId() ?: return@mapNotNull null
            val actor = previousPieces[id] ?: snapshot.pieces()[id] ?: return@mapNotNull null
            val origin = event.from() ?: actor.position() ?: return@mapNotNull null
            if (actor.hero() == HeroType.NONE) return@mapNotNull null
            CastFlash(id, actor.hero(), origin)
        }
        if (added.isNotEmpty()) active = active + added
        observed = snapshot.events().size
        previousPieces = snapshot.pieces()
    }

    fun advance(delta: Float) {
        if (active.isEmpty()) return
        active = active.map { it.copy(age = it.age + delta.coerceAtLeast(0f)) }
            .filter { it.age < HeroVisuals.CAST_FLASH_SECONDS }
    }
}

package org.mobachess.presentation.animation

import game.history.GameEvent
import game.state.GameSnapshot
import game.state.PieceState
import model.Position

/** Изменяет только выбор рисунка; актуальное игровое состояние остаётся в снимке партии. */
data class SpriteModelChange(val before: PieceState, val after: PieceState, val revealFrame: Int) {
    init {
        require(before.id() == after.id() && revealFrame >= 1)
    }
}

data class SpriteEffect(
    val square: Position,
    val duration: Float,
    val size: Float = 192f,
    val modelChange: SpriteModelChange? = null
)

data class PlayingSpriteEffect(val effect: SpriteEffect, val age: Float = 0f) {
    fun frameIndex(frameCount: Int): Int =
        ((age / effect.duration) * frameCount).toInt().coerceIn(0, frameCount - 1)
}

fun interface SpriteEffectFactory {
    fun create(event: GameEvent, before: GameSnapshot, after: GameSnapshot): SpriteEffect?
}

/** Только подтверждённые новые события; загрузка, смена сессии и очистка не повторяют историю. */
class SpriteEffectTimeline(private val factory: SpriteEffectFactory) {
    private var source: Any? = null
    private var previous: GameSnapshot? = null
    var active: List<PlayingSpriteEffect> = emptyList()
        private set

    /** Старый рисунок используется до начала заданного кадра (номер кадра считается с единицы). */
    fun modelFor(piece: PieceState, frameCount: Int): PieceState {
        var model = piece
        for (playing in active.asReversed()) {
            val change = playing.effect.modelChange ?: continue
            // Проверяем цепочку форм: новый эффект не отменяет незавершённую смену предыдущего.
            if (model.id() != change.after.id() || model.hero() != change.after.hero()
                || model.type() != change.after.type() || model.color() != change.after.color()) continue
            if (playing.frameIndex(frameCount) + 1 < change.revealFrame) model = change.before
        }
        return model
    }

    fun update(session: Any, snapshot: GameSnapshot, delta: Float) {
        val before = previous
        if (source !== session || before == null || snapshot.events().size < before.events().size) {
            source = session
            previous = snapshot
            active = emptyList()
            return
        }
        active = active.map { it.copy(age = it.age + delta.coerceAtLeast(0f)) }
            .filter { it.age < it.effect.duration } + snapshot.events().drop(before.events().size)
            .mapNotNull { factory.create(it, before, snapshot)?.let(::PlayingSpriteEffect) }
        previous = snapshot
    }
}

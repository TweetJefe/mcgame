package org.mobachess.presentation.animation

import game.heroes.pudge.PudgeAbility
import game.history.GameEvent
import game.state.GameSnapshot

class HookPullAnimationFactory : AbilityAnimationFactory {
    override fun create(event: GameEvent, before: GameSnapshot, after: GameSnapshot): PieceAnimation? {
        if (!event.isAbility(PudgeAbility.HOOK)) return null
        val id = event.targetId() ?: return null
        val from = before.pieces()[id]?.position() ?: return null
        // Событие хука указывает выбранную цель; конечная клетка берётся из подтверждённого состояния.
        val to = after.pieces()[id]?.position() ?: return null
        if (from == to) return null
        return BoardSlideAnimation(id, from, to)
    }
}

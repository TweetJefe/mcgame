package org.mobachess.presentation.animation

import game.heroes.chronomlin.ChronomlinAbility
import game.history.GameEvent
import game.state.GameSnapshot

/** Притягивается исполнитель; выбранная фигура остаётся неподвижной. */
class ChronomlinGrappleAnimationFactory : AbilityAnimationFactory {
    override fun create(event: GameEvent, before: GameSnapshot, after: GameSnapshot): PieceAnimation? {
        if (!event.isAbility(ChronomlinAbility.GRAPPLE)) return null
        val id = event.actorId() ?: return null
        val from = before.pieces()[id]?.position() ?: return null
        val to = after.pieces()[id]?.position() ?: return null
        if (from == to) return null
        return BoardSlideAnimation(id, from, to)
    }
}

package org.mobachess.presentation.animation

import game.heroes.sunwukong.SunWukongAbility
import game.history.GameEvent
import game.state.GameSnapshot

/** Копия способности использует ту же анимацию, независимо от личности исполнителя. */
class WukongCloudEffectFactory : SpriteEffectFactory {
    override fun create(event: GameEvent, before: GameSnapshot, after: GameSnapshot): SpriteEffect? {
        val square = when {
            event.isAbility(SunWukongAbility.SHAPESHIFT) -> event.from()
            event.isAbility(SunWukongAbility.CLONE) -> event.to()
                ?: before.pieces()[event.targetId()]?.position()
            else -> null
        } ?: return null
        val affectedId = (if (event.isAbility(SunWukongAbility.SHAPESHIFT)) event.actorId() else event.targetId())
            ?: return null
        val previousModel = before.pieces()[affectedId]
        val currentModel = after.pieces()[affectedId]
        val modelChange = if (previousModel != null && currentModel != null)
            SpriteModelChange(previousModel, currentModel, REVEAL_FRAME) else null
        return SpriteEffect(square, DURATION, modelChange = modelChange)
    }

    companion object {
        const val PATH = "art/animations/wukong/sprites/cloud_wukong.png"
        const val DURATION = 1f
        const val REVEAL_FRAME = 8
    }
}

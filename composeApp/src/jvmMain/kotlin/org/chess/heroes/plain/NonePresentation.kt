package org.chess.heroes.plain

import game.heroes.HeroType
import org.chess.heroes.BaseHeroPresentation
import org.chess.heroes.HeroViewContext

object NonePresentation : BaseHeroPresentation(HeroType.NONE, "Обычная фигура", null,
    "Обычная шахматная фигура", beforeInventory = true) {
    override fun title(context: HeroViewContext) = context.piece.type().simpleName
}

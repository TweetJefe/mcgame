package org.chess.heroes.lumeshard

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import game.heroes.HeroRules
import game.heroes.HeroType
import game.state.SquareColor
import org.chess.heroes.BaseHeroPresentation
import org.chess.heroes.HeroViewContext
import game.heroes.lumeshard.LumeshardAbility

object LumeshardPresentation : BaseHeroPresentation(HeroType.LUMESHARD, "Lumeshard", "L",
    "Слон • Лазер: ослепление, радиус 3, КД 3. Ульта: телепорт на исходный цвет.",
    mapOf(LumeshardAbility.LASER to "Лазер", LumeshardAbility.TELEPORT to "Телепорт"),
    beforeInventory = true, showRejections = true, hideBudgetRejection = true) {
    override fun ultimateCaptures(rules: HeroRules) = rules.lumeshard().ultimateCaptures()
    @Composable override fun Details(context: HeroViewContext) {
        val selected = context.piece
        val snapshot = context.snapshot
        val threshold = snapshot.heroRules().lumeshard().ultimateCaptures()
        Text("Лазер: ${if (selected.cooldown() == 0) "готов" else "КД ${selected.cooldown()}"} • радиус 3", fontSize = 13.sp)
        Text("Ульта: ${if (threshold == 0) "без заряда" else "${selected.ultimateCharge()} / $threshold"} • свободная клетка своего цвета", fontSize = 13.sp)
        Text("Исходный цвет: ${if (selected.startingColor() == SquareColor.DARK) "чёрный" else "белый"}", fontSize = 13.sp)
        selected.storedPieceId()?.let { passenger ->
            Text("Внутри: ${snapshot.pieces()[passenger]?.type()?.simpleName}", color = Color(0xFF6A5ACD))
        }
    }
    @Composable override fun AfterActions(context: HeroViewContext) {
        if (context.selectedAbility != null) Text("Выберите подсвеченную цель", color = Color(0xFF6A5ACD))
    }
}

package org.chess.heroes.moleminer

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import game.heroes.HeroRules
import game.heroes.HeroType
import org.chess.heroes.BaseHeroPresentation
import org.chess.heroes.HeroViewContext
import game.heroes.moleminer.MoleminerAbility

object MoleminerPresentation : BaseHeroPresentation(HeroType.MOLEMINER, "Moleminer", "M",
    "Пешка • Мина под собой: КД 3, скрыта и бессрочна. Ульта: прыжок 3 и взрыв 2×2 за 2 взятия.", mapOf(MoleminerAbility.PLANT_MINE to "Заложить мину", MoleminerAbility.DETONATE to "Прыжок и взрыв")) {
    override fun ultimateCaptures(rules: HeroRules) = rules.moleminer().ultimateCaptures()
    @Composable override fun Details(context: HeroViewContext) {
        val selected = context.piece
        val snapshot = context.snapshot

        Text("Мина: КД ${selected.cooldown()}. Ульта: ${selected.ultimateCharge()} / ${snapshot.heroRules().moleminer().ultimateCaptures()}", fontSize = 13.sp)
    }
    @Composable override fun AfterActions(context: HeroViewContext) {
        val selectedAbility = context.selectedAbility

        if (selectedAbility==MoleminerAbility.DETONATE) Text("Клетка в радиусе 3. Случайный квадрат 2×2: погибнут герой и все внутри, кроме королей и фигур в коробках.", fontSize = 13.sp)
    }
}

package org.chess.heroes.clownganger

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import game.heroes.HeroRules
import game.heroes.HeroType
import org.chess.heroes.BaseHeroPresentation
import org.chess.heroes.HeroViewContext
import game.heroes.clownganger.ClowngangerAbility

object ClowngangerPresentation : BaseHeroPresentation(HeroType.CLOWNGANGER, "Clownganger", "Кл",
    "Конь • Коробка: радиус 3, КД 3, защита на 1 ход. Ульта: двойник на 2 хода за 1 взятие.", mapOf(ClowngangerAbility.BOX to "Спрятать в коробку", ClowngangerAbility.ILLUSION to "Создать иллюзию")) {
    override fun ultimateCaptures(rules: HeroRules) = rules.clownganger().ultimateCaptures()
    @Composable override fun Details(context: HeroViewContext) {
        val selected = context.piece
        val snapshot = context.snapshot

        Text("Коробка: радиус 3, КД ${selected.cooldown()}. Ульта: ${selected.ultimateCharge()} / ${snapshot.heroRules().clownganger().ultimateCaptures()}", fontSize = 13.sp)
    }
    @Composable override fun AfterActions(context: HeroViewContext) {
        val selectedAbility = context.selectedAbility

        if (selectedAbility==ClowngangerAbility.BOX) Text("Выберите союзника, себя или иллюзию в радиусе 3", fontSize = 13.sp)
        if (selectedAbility==ClowngangerAbility.ILLUSION) Text("Выберите свободную соседнюю клетку", fontSize = 13.sp)
    }
}

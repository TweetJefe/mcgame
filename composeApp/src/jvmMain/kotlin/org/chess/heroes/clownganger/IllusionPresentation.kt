package org.chess.heroes.clownganger

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import game.heroes.HeroType
import org.chess.heroes.BaseHeroPresentation
import org.chess.heroes.HeroViewContext

object IllusionPresentation : BaseHeroPresentation(HeroType.ILLUSION, "Иллюзия", "Коп",
    "Пустышка: блокирует проход, не берёт фигуры и не имеет способностей.", beforeInventory = false) {
    @Composable override fun Details(context: HeroViewContext) {
        Text("Иллюзия: не берёт фигуры. Может сходить вместе с оригиналом.", fontSize = 13.sp)
    }
}

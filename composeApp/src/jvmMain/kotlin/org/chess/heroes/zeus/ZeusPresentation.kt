package org.chess.heroes.zeus

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import game.heroes.HeroRules
import game.heroes.HeroType
import org.chess.heroes.BaseHeroPresentation
import org.chess.heroes.HeroViewContext
import game.heroes.zeus.ZeusAbility

object ZeusPresentation : BaseHeroPresentation(HeroType.ZEUS, "Zeus", "Z",
    "Ладья • Цепь до 3 врагов: радиус 3, КД 1; третий заряд убивает. Ульта: случайный заряженный враг за 2 взятия.", mapOf(ZeusAbility.CHAIN_LIGHTNING to "Цепная молния", ZeusAbility.THUNDERSTORM to "Разряд")) {
    override fun ultimateCaptures(rules: HeroRules) = rules.zeus().ultimateCaptures()
    @Composable override fun Details(context: HeroViewContext) {
        val selected = context.piece
        val snapshot = context.snapshot

        Text("Молния: КД ${selected.cooldown()}. Ульта: ${selected.ultimateCharge()} / ${snapshot.heroRules().zeus().ultimateCaptures()}", fontSize = 13.sp)
    }
    @Composable override fun AfterActions(context: HeroViewContext) {
        val selectedAbility = context.selectedAbility

        Text("Третий заряд убивает. Разряд выбирает случайного врага с зарядом этого героя. Z и G — разные заряды.", fontSize = 13.sp)
        if (selectedAbility==ZeusAbility.CHAIN_LIGHTNING) Text("Выберите врага в радиусе 3; цепь поразит до 3 целей.", fontSize = 13.sp)
    }
}

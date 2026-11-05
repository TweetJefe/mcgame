package org.chess.heroes.kikimora

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import game.heroes.HeroRules
import game.heroes.HeroType
import org.chess.heroes.BaseHeroPresentation
import org.chess.heroes.HeroViewContext
import game.heroes.kikimora.KikimoraAbility

object KikimoraPresentation : BaseHeroPresentation(HeroType.KIKIMORA, "Kikimora", "Ки",
    "Слон • Пряжа: радиус 2, стан 1 или 2 хода, КД 3. Ульта за 2 взятия: неуязвимый подвижный горшок рядом.", mapOf(KikimoraAbility.YARN to "Связать пряжей", KikimoraAbility.CLAY_POT to "Глиняный горшок")) {
    override fun ultimateCaptures(rules: HeroRules) = rules.kikimora().ultimateCaptures()
    @Composable override fun Details(context: HeroViewContext) {
        val selected = context.piece
        val snapshot = context.snapshot

        Text("Пряжа: КД ${selected.cooldown()}. Горшок: ${selected.ultimateCharge()} / ${snapshot.heroRules().kikimora().ultimateCaptures()}", fontSize = 13.sp)
    }
    @Composable override fun AfterActions(context: HeroViewContext) {
        val selectedAbility = context.selectedAbility

        if (selectedAbility==KikimoraAbility.YARN) Text("Враг в радиусе 2. Другая фигура за целью по направлению броска усиливает стан до 2 ходов.", fontSize = 13.sp)
        if (selectedAbility==KikimoraAbility.CLAY_POT) Text("Любая соседняя фигура, кроме короля: вечное неуязвимое препятствие. Можно сдвинуть способностью.", fontSize = 13.sp)
    }
}

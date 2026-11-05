package org.chess.heroes.izba

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import game.heroes.HeroRules
import game.heroes.HeroType
import org.chess.heroes.BaseHeroPresentation
import org.chess.heroes.HeroViewContext
import game.heroes.izba.IzbaAbility

object IzbaPresentation : BaseHeroPresentation(HeroType.IZBA, "Изба", "И",
    "Ладья • При гибели призывает Бабу-ягу рядом. Ульта: призыв на свои первые две линии.", mapOf(IzbaAbility.SUMMON_YAGA to "Призвать Бабу-ягу"), showRejections = true) {
    override fun ultimateCaptures(rules: HeroRules) = rules.izba().ultimateCaptures()
    @Composable override fun Details(context: HeroViewContext) {
        val selected = context.piece
        val snapshot = context.snapshot

        Text("Пассивка: при взятии призовёт Бабу-ягу на свободную клетку рядом", fontSize = 13.sp)
        Text("Ульта: ${selected.ultimateCharge()} / ${snapshot.heroRules().izba().ultimateCaptures()}", fontSize = 13.sp)
    }
}

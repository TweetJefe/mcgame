package org.chess.heroes.ares

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import game.heroes.HeroRules
import game.heroes.HeroType
import org.chess.heroes.BaseHeroPresentation
import org.chess.heroes.HeroViewContext
import game.heroes.ares.AresAbility

object AresPresentation : BaseHeroPresentation(HeroType.ARES, "Ares", "A",
    "Слон • Копьё: 3 прямо / 2 по диагонали, столкновение оглушает, КД 3. Пассивка: месть за 1 взятие.", mapOf(AresAbility.SPEAR to "Бросить копьё"), showRejections = true) {
    override fun boardBadge(context: HeroViewContext) = if (context.piece.ultimateCharge() >= context.snapshot.heroRules().ares().revengeCaptures()) "A●" else badge
    override fun ultimateCaptures(rules: HeroRules) = rules.ares().revengeCaptures()
    @Composable override fun Details(context: HeroViewContext) {
        val selected = context.piece
        val snapshot = context.snapshot

        Text("Копьё: 3 прямо / 2 по диагонали • КД ${selected.cooldown()}", fontSize = 13.sp)
        Text("Пассивная ульта: ${selected.ultimateCharge()} / ${snapshot.heroRules().ares().revengeCaptures()} • заберёт взявшую его фигуру", fontSize = 13.sp)
    }
    @Composable override fun AfterActions(context: HeroViewContext) {
        val selectedAbility = context.selectedAbility

        if (selectedAbility==AresAbility.SPEAR) Text("Выберите подсвеченное направление: первая фигура летит до конца, столкновение оглушает обе", fontSize = 13.sp)
    }
}

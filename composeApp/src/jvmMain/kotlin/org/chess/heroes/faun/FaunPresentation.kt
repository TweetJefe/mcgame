package org.chess.heroes.faun

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import game.heroes.HeroRules
import game.heroes.HeroType
import org.chess.heroes.BaseHeroPresentation
import org.chess.heroes.HeroViewContext
import game.heroes.faun.FaunAbility

object FaunPresentation : BaseHeroPresentation(HeroType.FAUN, "Faun", "F",
    "Конь • Туча 3×3: дальность 4, КД 3. Ульта: невидимость на 2 хода.", mapOf(FaunAbility.CLOUD to "Бросить тучу", FaunAbility.INVISIBILITY to "Невидимость")) {
    override fun ultimateCaptures(rules: HeroRules) = rules.faun().ultimateCaptures()
    @Composable override fun Details(context: HeroViewContext) {
        val selected = context.piece
        val snapshot = context.snapshot

        Text("Туча 3×3 • дальность 4 • КД ${selected.cooldown()}", fontSize = 13.sp)
        Text("Невидимость: ${selected.ultimateCharge()} / ${snapshot.heroRules().faun().ultimateCaptures()} • 2 следующих своих хода", fontSize = 13.sp)
    }
    @Composable override fun AfterActions(context: HeroViewContext) {
        val selected = context.piece
        val snapshot = context.snapshot
        val selectedPos = selected.position()

        if (snapshot.faun().silenced(selectedPos)) Text("В туче: скилл и ульта недоступны", fontSize = 12.sp, color = Color.Gray)
    }
}

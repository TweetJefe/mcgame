package org.chess.heroes.chronomlin

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import game.heroes.HeroRules
import game.heroes.HeroType
import org.chess.heroes.BaseHeroPresentation
import org.chess.heroes.HeroViewContext
import game.heroes.chronomlin.ChronomlinAbility

object ChronomlinPresentation : BaseHeroPresentation(HeroType.CHRONOMLIN, "Chronomlin", "Ch",
    "Пешка • Притяжение без дальности, КД 3. Ульта за 1 взятие: пешка → слон → конь → ладья.", mapOf(ChronomlinAbility.GRAPPLE to "Притянуться", ChronomlinAbility.EVOLVE to "Улучшиться")) {
    override fun ultimateCaptures(rules: HeroRules) = rules.chronomlin().ultimateCaptures()
    @Composable override fun Details(context: HeroViewContext) {
        val selected = context.piece
        val snapshot = context.snapshot

        Text("Притяжение: КД ${selected.cooldown()}. Улучшение: ${selected.ultimateCharge()} / ${snapshot.heroRules().chronomlin().ultimateCaptures()}", fontSize = 13.sp)
    }
    @Composable override fun AfterActions(context: HeroViewContext) {
        val selectedAbility = context.selectedAbility

        Text("Пешка → слон → конь → ладья. Способности сохраняются.", fontSize = 13.sp)
        if (selectedAbility==ChronomlinAbility.GRAPPLE) Text("Выберите первую фигуру по прямой, включая короля. Приземление перед ней, без взятия.", fontSize = 13.sp)
    }
}

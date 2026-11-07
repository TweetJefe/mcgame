package org.chess.heroes.supernova

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import game.heroes.HeroRules
import game.heroes.HeroType
import game.heroes.supernova.SupernovaAbility
import game.heroes.supernova.SupernovaDefinition
import org.chess.heroes.BaseHeroPresentation
import org.chess.heroes.HeroViewContext

object SupernovaPresentation : BaseHeroPresentation(HeroType.SUPERNOVA, "Supernova", "Sn",
    "Конь + слон • Центр: радиус 2. Область: радиус 3. Стягивание: КД 2. Перемешивание: 1 взятие.", mapOf(SupernovaAbility.PULL to "Стянуть фигуры", SupernovaAbility.SHUFFLE to "Перемешать фигуры"),
    showRejections = true, hideBudgetRejection = true) {
    override fun ultimateCaptures(rules: HeroRules) = rules.get(SupernovaDefinition.INSTANCE).ultimateCaptures()

    override fun draftSummary(rules: HeroRules): String {
        val p = rules.get(SupernovaDefinition.INSTANCE)
        return "Конь + слон • Центр: радиус ${p.castRadius()}. Область: ${p.effectRadius()}. Стягивание: КД ${p.pullCooldown()}. Ульта: ${p.ultimateCaptures()} взят."
    }

    @Composable override fun Details(context: HeroViewContext) {
        Text("КД: ${context.piece.cooldown()}. Ульта: ${context.piece.ultimateCharge()} / ${ultimateCaptures(context.snapshot.heroRules())}", fontSize = 13.sp)
        
    }
}

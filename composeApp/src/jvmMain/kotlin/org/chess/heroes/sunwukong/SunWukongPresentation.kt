package org.chess.heroes.sunwukong

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import game.heroes.HeroRules
import game.heroes.HeroType
import game.heroes.sunwukong.SunWukongAbility
import game.heroes.sunwukong.SunWukongDefinition
import org.chess.heroes.BaseHeroPresentation
import org.chess.heroes.HeroViewContext

object SunWukongPresentation : BaseHeroPresentation(HeroType.SUN_WUKONG, "Sun Wukong", "SW",
    "Пешка • Случайная форма на 1 ход, общий КД 4. Ульта за 1 взятие: союзная пешка становится Wukong.",
    mapOf(SunWukongAbility.SHAPESHIFT to "Случайная форма", SunWukongAbility.CLONE to "Создать клона"),
    showRejections = true, hideBudgetRejection = true) {
    override fun ultimateCaptures(rules: HeroRules) = rules.get(SunWukongDefinition.INSTANCE).ultimateCaptures()

    override fun draftSummary(rules: HeroRules): String {
        val parameters = rules.get(SunWukongDefinition.INSTANCE)
        return "Пешка • Случайная форма: ${parameters.shapeshiftTurns()} ход., общий КД ${parameters.shapeshiftCooldown()}. Клон: ${parameters.ultimateCaptures()} взят."
    }

    @Composable override fun Details(context: HeroViewContext) {
        val parameters = context.snapshot.heroRules().get(SunWukongDefinition.INSTANCE)
        Text("Общий КД: ${context.piece.cooldown()}. Ульта: ${context.piece.ultimateCharge()} / ${parameters.ultimateCaptures()}", fontSize = 13.sp)
    }

    @Composable override fun AfterActions(context: HeroViewContext) {
        when (context.selectedAbility) {
            SunWukongAbility.CLONE -> Text("Выберите обычную союзную пешку на доске.", fontSize = 13.sp)
            SunWukongAbility.SHAPESHIFT -> Text("Случайно: конь, слон или ладья. Затем вернётся прежняя форма.", fontSize = 13.sp)
            else -> Unit
        }
    }
}

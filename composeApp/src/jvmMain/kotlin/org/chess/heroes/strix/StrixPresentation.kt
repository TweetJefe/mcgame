package org.chess.heroes.strix
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import game.heroes.HeroRules
import game.heroes.HeroType
import game.heroes.strix.StrixAbility
import game.heroes.strix.StrixDefinition
import org.chess.heroes.BaseHeroPresentation
import org.chess.heroes.HeroViewContext

object StrixPresentation : BaseHeroPresentation(HeroType.STRIX, "Strix", "St",
    "Пешка • Возврат позиции: радиус 2, КД 4. Ульта за 2 взятия: пропуск хода врага.",
    mapOf(StrixAbility.REWIND to "Вернуть позицию", StrixAbility.TIME_SKIP to "Пропустить ход врага"),
    showRejections = true, hideBudgetRejection = true) {
    override fun ultimateCaptures(rules: HeroRules) = rules.get(StrixDefinition.INSTANCE).ultimateCaptures()
    override fun draftSummary(rules: HeroRules): String {
        val p = rules.get(StrixDefinition.INSTANCE)
        return "Пешка • Позиция 2 своих хода назад: радиус 2, КД ${p.rewindCooldown()}. Пропуск хода врага: ${p.ultimateCaptures()} взят."
    }
    @Composable override fun Details(context: HeroViewContext) {
        val p = context.snapshot.heroRules().get(StrixDefinition.INSTANCE)
        Text("Возврат: КД ${context.piece.cooldown()}. Ульта: ${context.piece.ultimateCharge()} / ${p.ultimateCaptures()}", fontSize = 13.sp)
    }
    @Composable override fun AfterActions(context: HeroViewContext) {
        if (context.selectedAbility == StrixAbility.REWIND)
            Text("Выберите фигуру рядом. Прежняя клетка должна быть свободна.", fontSize = 13.sp)
    }
}

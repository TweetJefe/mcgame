package org.chess.heroes.phoenix

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import game.heroes.HeroRules
import game.heroes.HeroType
import game.heroes.phoenix.PhoenixAbility
import game.heroes.phoenix.PhoenixDefinition
import org.chess.heroes.BaseHeroPresentation
import org.chess.heroes.HeroViewContext

object PhoenixPresentation : BaseHeroPresentation(HeroType.PHOENIX, "Phoenix", "Ph",
    "Конь • После взятия пешки возвращается. Ульта за 2 взятия: яйцо → Supernova.", mapOf(PhoenixAbility.REBIRTH to "Стать яйцом"),
    showRejections = true, hideBudgetRejection = true) {
    override fun ultimateCaptures(rules: HeroRules) = rules.get(PhoenixDefinition.INSTANCE).ultimateCaptures()

    override fun draftSummary(rules: HeroRules): String {
        val p = rules.get(PhoenixDefinition.INSTANCE)
        return "Конь • Возврат после взятия пешки. Яйцо: ${p.ultimateCaptures()} взят., вылупление через ${p.eggTurns()} своих ход."
    }
    override fun boardBadge(context: HeroViewContext): String =
        if (context.piece.type() == model.figures.Egg::class.java) "Я" else "Ph"
    override fun title(context: HeroViewContext): String =
        if (context.piece.type() == model.figures.Egg::class.java) "Яйцо Phoenix" else "Phoenix"

    @Composable override fun Details(context: HeroViewContext) {
        Text("КД: ${context.piece.cooldown()}. Ульта: ${context.piece.ultimateCharge()} / ${ultimateCaptures(context.snapshot.heroRules())}", fontSize = 13.sp)
        
        if (context.piece.type() == model.figures.Egg::class.java) {
            val record = context.snapshot.mechanics().get(game.heroes.phoenix.PhoenixMechanic.KEY).eggs()[context.piece.id()]
            Text("Яйцо неподвижно. Вылупление: конец хода ${record?.until() ?: "—"}", fontSize = 13.sp)
        }

    }
}

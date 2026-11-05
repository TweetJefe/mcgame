package org.chess.heroes.bolotnik

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import game.heroes.HeroRules
import game.heroes.HeroType
import org.chess.heroes.BaseHeroPresentation
import org.chess.heroes.HeroViewContext
import game.heroes.bolotnik.BolotnikAbility

object BolotnikPresentation : BaseHeroPresentation(HeroType.BOLOTNIK, "Bolotnik", "Б",
    "Конь • Грязь: радиус 4, КД 4. Ульта: скрыться в болоте и съесть вошедшего врага.",
    mapOf(BolotnikAbility.MUD to "Грязь", BolotnikAbility.AMBUSH to "Засада"),
    beforeInventory = true, showRejections = true, hideBudgetRejection = true, retained = setOf(BolotnikAbility.AMBUSH)) {
    override fun ultimateCaptures(rules: HeroRules) = rules.bolotnik().ultimateCaptures()
    @Composable override fun Details(context: HeroViewContext) {
        val selected = context.piece
        val snapshot = context.snapshot
        val threshold = snapshot.heroRules().bolotnik().ultimateCaptures()
        Text("Грязь: ${if (selected.cooldown() == 0) "готов" else "КД ${selected.cooldown()}"} • радиус 4", fontSize = 13.sp)
        Text("Ульта: ${if (threshold == 0) "без заряда" else "${selected.ultimateCharge()} / $threshold"} • клетка и соседи слева/справа", fontSize = 13.sp)
        Text("Враг входит в болото — Bolotnik выходит и берёт его", fontSize = 12.sp)
        selected.storedPieceId()?.let { passenger ->
            Text("Внутри: ${snapshot.pieces()[passenger]?.type()?.simpleName}", color = Color(0xFF6A5ACD))
        }
    }
    @Composable override fun AfterActions(context: HeroViewContext) {
        if (context.selectedAbility != null) Text("Выберите подсвеченную цель", color = Color(0xFF6A5ACD))
    }
}

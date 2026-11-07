package org.chess.heroes.shiva

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import game.heroes.HeroRules
import game.heroes.HeroType
import game.heroes.shiva.ShivaAbility
import game.heroes.shiva.ShivaDefinition
import game.heroes.shiva.ShivaMechanic
import game.mechanics.PieceRestriction
import game.visibility.VisibilityRules
import org.chess.heroes.BaseHeroPresentation
import org.chess.heroes.BoardEffectContext
import org.chess.heroes.HeroViewContext

object ShivaPresentation : BaseHeroPresentation(HeroType.SHIVA, "Shiva", "Шв",
    "Ладья • Лотус: радиус 2, обнаружение 3, срок 2 хода, КД 3. Воскрешение: 2 взятия.",
    mapOf(ShivaAbility.LOTUS to "Лотус", ShivaAbility.REBIRTH to "Воскрешение"),
    showRejections = true, hideBudgetRejection = true) {
    override fun ultimateCaptures(rules: HeroRules) = rules.get(ShivaDefinition.INSTANCE).ultimateCaptures()
    override fun draftSummary(rules: HeroRules): String {
        val p = rules.get(ShivaDefinition.INSTANCE)
        return "Ладья • Лотус: радиус ${p.castRadius()}, обнаружение ${p.revealRadius()}, ${p.lotusTurns()} своих хода, КД ${p.lotusCooldown()}. Воскрешение: ${p.ultimateCaptures()} взят."
    }
    @Composable override fun Details(context: HeroViewContext) {
        Text("КД: ${context.piece.cooldown()}. Ульта: ${context.piece.ultimateCharge()} / ${ultimateCaptures(context.snapshot.heroRules())}",fontSize=13.sp)
    }
    @Composable override fun BoardDecoration(context: BoardEffectContext) {
        val piece = context.piece ?: return
        if (piece.isRemoved || piece.isPotted || context.snapshot.mechanics().get(ShivaMechanic.KEY).lotuses()[piece.id()] == null) return
        if (VisibilityRules.context(context.snapshot).blocks(piece, PieceRestriction.TRANSFORMATION)) return
        val background = if ((context.square.row+context.square.column)%2==0) Color(0xFF769656) else Color(0xFFEEEED2)
        Box(Modifier.fillMaxSize().background(background),contentAlignment=Alignment.Center) {
            Text("✿",fontSize=43.sp,color=Color(0xFFAA317A))
            Text("Лотус",fontSize=10.sp,color=Color(0xFF702653),modifier=Modifier.align(Alignment.BottomCenter))
        }
    }
}

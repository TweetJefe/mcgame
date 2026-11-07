package org.chess.heroes.leshy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import game.heroes.HeroRules
import game.heroes.HeroType
import game.heroes.leshy.LeshyAbility
import game.heroes.leshy.LeshyDefinition
import game.heroes.leshy.LeshyMechanic
import org.chess.heroes.BaseHeroPresentation
import org.chess.heroes.BoardEffectContext
import org.chess.heroes.HeroViewContext

object LeshyPresentation : BaseHeroPresentation(HeroType.LESHY, "Leshy", "Лш",
    "Пешка • Лоза: 3 клетки вперёд, 3 хода, КД 4. Деревья справа/слева: 1 взятие.",
    mapOf(LeshyAbility.VINE to "Лоза", LeshyAbility.FOREST to "Превратить в деревья"),
    showRejections = true, hideBudgetRejection = true) {
    override fun ultimateCaptures(rules: HeroRules) = rules.get(LeshyDefinition.INSTANCE).ultimateCaptures()
    override fun draftSummary(rules: HeroRules): String {
        val p = rules.get(LeshyDefinition.INSTANCE)
        return "Пешка • Лоза: ${p.vineLength()} клетки, ${p.vineTurns()} своих хода, КД ${p.vineCooldown()}. Деревья: ${p.ultimateCaptures()} взят.; свои ${p.treeTurns()} хода, враги навсегда."
    }
    @Composable override fun Details(context: HeroViewContext) {
        Text("КД: ${context.piece.cooldown()}. Ульта: ${context.piece.ultimateCharge()} / ${ultimateCaptures(context.snapshot.heroRules())}",fontSize=13.sp)
    }
    @Composable override fun BoardDecoration(context: BoardEffectContext) {
        val state = context.snapshot.mechanics().get(LeshyMechanic.KEY)
        if (state.cells().contains(context.square)) {
            Box(Modifier.fillMaxSize().background(Color(0xFF315D28).copy(alpha=.23f))) {
                Text("≋",fontSize=19.sp,color=Color(0xFF164616),modifier=Modifier.align(Alignment.TopStart).padding(2.dp))
            }
        }
        val tree = state.trees().values.firstOrNull { it.square() == context.square }
        if (tree != null) {
            val background = if ((context.square.row+context.square.column)%2==0) Color(0xFF769656) else Color(0xFFEEEED2)
            Box(Modifier.fillMaxSize().background(background),contentAlignment=Alignment.Center) {
                Text("♣",fontSize=42.sp,color=Color(0xFF245D2C))
                Text(if (tree.until()==0L) "Дерево ∞" else "Дерево",fontSize=10.sp,
                    modifier=Modifier.align(Alignment.BottomCenter),color=Color(0xFF18391D))
            }
        }
    }
}

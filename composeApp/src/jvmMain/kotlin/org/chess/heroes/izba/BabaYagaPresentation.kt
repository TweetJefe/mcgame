package org.chess.heroes.izba

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import game.heroes.HeroType
import org.chess.heroes.BaseHeroPresentation
import org.chess.heroes.HeroViewContext

object BabaYagaPresentation : BaseHeroPresentation(HeroType.BABA_YAGA, "Баба-яга", "Я",
    "Пешка • Через 3 собственных хода становится Избой. Только призыв, без пика.", beforeInventory = true) {
    @Composable override fun Details(context: HeroViewContext) {
        val snapshot = context.snapshot
        val selected = context.piece
        val origin = snapshot.summons().records()[selected.id()]!!
        val remaining = if (origin.bornTurn() == snapshot.turn().number() && selected.color() == snapshot.active())
            snapshot.heroRules().izba().lifetimeTurns().toLong()
            else ((origin.transformsAfterTurn() - snapshot.turn().number()) / 2 + 1).coerceAtLeast(1)
        Text("Станет Избой: осталось завершить своих ходов — $remaining", fontSize = 13.sp)
    }
}

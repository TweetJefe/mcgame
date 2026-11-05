package org.chess.heroes.bountycrow

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import game.heroes.HeroRules
import game.heroes.HeroType
import org.chess.heroes.BaseHeroPresentation
import org.chess.heroes.HeroViewContext
import game.heroes.bountycrow.BountyCrowAbility

object BountyCrowPresentation : BaseHeroPresentation(HeroType.BOUNTY_CROW, "Crow", "C",
    "Пешка • Награда +1,5, КД 3. Пассивка +0,5 за взятие. Ульта: украсть до 2 золота.",
    mapOf(BountyCrowAbility.BOUNTY to "Награда", BountyCrowAbility.STEAL to "Украсть"),
    beforeInventory = true, showRejections = true, hideBudgetRejection = true, retained = setOf(BountyCrowAbility.STEAL)) {
    override fun ultimateCaptures(rules: HeroRules) = rules.crow().ultimateCaptures()
    @Composable override fun Details(context: HeroViewContext) {
        val selected = context.piece
        val snapshot = context.snapshot
        val threshold = snapshot.heroRules().crow().ultimateCaptures()
        Text("Награда: ${if (selected.cooldown() == 0) "готов" else "КД ${selected.cooldown()}"} • вся доска, +1,5 золота", fontSize = 13.sp)
        Text("Ульта: ${if (threshold == 0) "без заряда" else "${selected.ultimateCharge()} / $threshold"} • украсть до 2 золота", fontSize = 13.sp)
        Text("Пассивка: +0,5 золота за собственное взятие", fontSize = 13.sp)
        selected.storedPieceId()?.let { passenger ->
            Text("Внутри: ${snapshot.pieces()[passenger]?.type()?.simpleName}", color = Color(0xFF6A5ACD))
        }
    }
    @Composable override fun AfterActions(context: HeroViewContext) {
        if (context.selectedAbility != null) Text("Выберите подсвеченную цель", color = Color(0xFF6A5ACD))
    }
}

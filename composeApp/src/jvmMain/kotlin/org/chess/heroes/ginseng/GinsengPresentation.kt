package org.chess.heroes.ginseng

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import game.heroes.HeroRules
import game.heroes.HeroType
import game.heroes.ginseng.GinsengAbility
import org.chess.heroes.BaseHeroPresentation
import org.chess.heroes.HeroPanelControls
import org.chess.heroes.HeroViewContext

object GinsengPresentation : BaseHeroPresentation(HeroType.GINSENG, "Ginseng", "G",
    "Слон • Сдвиг: радиус 3, соседняя клетка, КД 3. Ульта: копия последнего скилла героя.",
    mapOf(GinsengAbility.DISPLACE to "Переместить фигуру", GinsengAbility.COPY_SPELL to "Копировать последнее")) {
    override fun boardBadge(context: HeroViewContext) = if (context.piece.storedPieceId() != null) "G●" else badge
    override fun ultimateCaptures(rules: HeroRules) = rules.ginseng().ultimateCaptures()
    @Composable override fun Details(context: HeroViewContext) {
        val selected = context.piece
        Text("Сдвиг: радиус 3 • КД ${selected.cooldown()} • новая клетка рядом с целью", fontSize = 13.sp)
        Text("Ульта: ${selected.ultimateCharge()} / ${context.snapshot.heroRules().ginseng().ultimateCaptures()} • последняя активная способность цели", fontSize = 13.sp)
    }
    @Composable override fun Extra(context: HeroViewContext, controls: HeroPanelControls) {
        val copy = context.snapshot.spells().borrowed()[context.piece.id()]
        if (copy == null) Text("Украденной способности нет", fontSize = 12.sp, color = Color.Gray)
        else {
            Text("Копия: ${copy.ability().displayName} • одно применение", fontSize = 13.sp)
            controls.AbilityButton(copy.ability(), "Использовать: ${copy.ability().displayName}", true)
            controls.Rejection(copy.ability(), true)
        }
        if (context.selectedAbility == GinsengAbility.COPY_SPELL)
            Text("Выберите героя: скопируется последняя успешно применённая им способность", fontSize = 13.sp)
    }
}

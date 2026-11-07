package org.chess.heroes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import game.core.Game
import game.heroes.Abilities
import game.heroes.HeroRegistry
import game.heroes.HeroRules
import game.heroes.HeroType
import game.moves.TurnRejection
import game.state.GameSnapshot
import game.state.PieceState
import model.Position

/** Только данные отображения: контекст не предоставляет изменяемую партию. */
data class HeroViewContext(val snapshot: GameSnapshot, val piece: PieceState,
                           val selectedAbility: Abilities?, val heroes: HeroRegistry)

data class BoardEffectContext(val snapshot: GameSnapshot, val square: Position,
                              val piece: PieceState?, val heroes: HeroRegistry)

interface HeroPresentation {
    val type: HeroType
    val shortName: String
    val summary: String
    val badge: String?
    fun draftSummary(rules: HeroRules): String = summary
    val beforeInventory: Boolean get() = false
    val showRejections: Boolean get() = false
    val hideBudgetRejection: Boolean get() = false
    fun label(ability: Abilities): String = ability.displayName
    fun title(context: HeroViewContext): String = context.heroes.get(type).definition().displayName()
    fun retainSelection(ability: Abilities): Boolean = false
    fun boardBadge(context: HeroViewContext): String? = badge
    fun ultimateCaptures(rules: HeroRules): Int? = null
    @Composable fun Details(context: HeroViewContext) {}
    @Composable fun AfterActions(context: HeroViewContext) {}
    @Composable fun Extra(context: HeroViewContext, controls: HeroPanelControls) {}
    /** Необязательное локальное изображение/декорация поверх клетки. */
    @Composable fun CellDecoration(context: HeroViewContext) {}
    @Composable fun BoardDecoration(context: BoardEffectContext) {}
    /** Необязательное изображение карточки; текстовый значок доступен независимо. */
    @Composable fun Portrait() {}
}

open class BaseHeroPresentation(
    override val type: HeroType,
    override val shortName: String,
    override val badge: String?,
    override val summary: String,
    labels: Map<Abilities, String> = emptyMap(),
    override val beforeInventory: Boolean = false,
    override val showRejections: Boolean = false,
    override val hideBudgetRejection: Boolean = false,
    retained: Set<Abilities> = emptySet()
) : HeroPresentation {
    private val labels = java.util.Map.copyOf(labels)
    private val retained = java.util.Set.copyOf(retained)
    override fun label(ability: Abilities): String = labels[ability] ?: ability.displayName
    override fun retainSelection(ability: Abilities): Boolean = ability in retained
}

/** Все кнопки, включая локальные виджеты, используют одну доступность и выбор запроса. */
class HeroPanelControls(private val game: Game, private val position: Position,
                        private val choose: (Abilities, Boolean) -> Unit) {
    @Composable fun AbilityButton(ability: Abilities, label: String, borrowed: Boolean = false) {
        val ready = game.getAbilityAvailability(position, ability, borrowed)
        Button(enabled = ready.isSuccess, onClick = { choose(ability, borrowed) }) { Text(label) }
    }
    @Composable fun Rejection(ability: Abilities, borrowed: Boolean = false, hideBudget: Boolean = false) {
        val ready = game.getAbilityAvailability(position, ability, borrowed)
        if (!ready.isSuccess && (!hideBudget || ready != TurnRejection.ABILITY_ALREADY_USED))
            Text(ready.message, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun HeroPanel(presentation: HeroPresentation, context: HeroViewContext, controls: HeroPanelControls) {
    presentation.Details(context)
    val abilities = context.heroes.get(context.piece.hero()).abilities()
    abilities.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { controls.AbilityButton(it, presentation.label(it)) }
        }
    }
    presentation.AfterActions(context)
    if (presentation.showRejections)
        abilities.forEach { controls.Rejection(it, hideBudget = presentation.hideBudgetRejection) }
    presentation.Extra(context, controls)
}

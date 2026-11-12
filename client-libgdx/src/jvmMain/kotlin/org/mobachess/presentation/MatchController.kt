package org.mobachess.presentation

import game.heroes.Abilities
import game.heroes.AbilityRequest
import game.heroes.HeroRegistry
import game.moves.MoveResult
import game.moves.Promotion
import model.Position
import model.figures.Pawn
import org.mobachess.session.GameSession

data class AbilityChoice(val ability: Abilities, val borrowed: Boolean = false)

/** Состояние выбора принадлежит клиенту. Законность и бюджет — исключительно движку. */
class MatchController(val session: GameSession) {
    var selected: Position? = null; private set
    var spell: AbilityChoice? = null; private set
    var target: Position? = null; private set
    var promotion: Pair<Position, Position>? = null; private set
    var highlights: Set<Position> = emptySet(); private set
    var message = "Выбери фигуру. Ты управляешь обеими сторонами."; private set

    fun choices(): List<AbilityChoice> {
        val s = session.snapshot()
        val piece = selected?.let(s::pieceAt) ?: return emptyList()
        val own = HeroRegistry.fromRules(s.heroRules()).get(piece.hero()).abilities().map { AbilityChoice(it) }
        val copy = s.spells().borrowed()[piece.id()]?.let { listOf(AbilityChoice(it.ability(), true)) } ?: emptyList()
        return own + copy
    }

    fun clear() { selected = null; spell = null; target = null; promotion = null; highlights = emptySet() }
    fun choose(choice: AbilityChoice) {
        val from = selected ?: return
        val ready = session.availability(from, choice.ability, choice.borrowed)
        if (!ready.isSuccess) { message = ready.message; return }
        spell = choice; target = null
        if (!choice.ability.targeting().requiresTarget()) {
            finish(session.cast(AbilityRequest(from, choice.ability, null, null, choice.borrowed)))
        } else {
            highlights = session.targets(from, choice.ability, choice.borrowed)
            message = "Выбери цель: ${choice.ability.displayName}"
        }
    }

    fun click(square: Position) {
        if (promotion != null) return
        val from = selected
        val choice = spell
        if (from != null && choice != null) {
            if (square !in highlights) { message = "Эта клетка недоступна для способности. Esc — отмена."; return }
            if (choice.ability.targeting().requiresDestination() && target == null) {
                target = square
                highlights = session.destinations(from, choice.ability, square, choice.borrowed)
                message = "Теперь выбери клетку назначения"
            } else finish(session.cast(AbilityRequest(from, choice.ability, target ?: square,
                if (target != null) square else null, choice.borrowed)))
            return
        }
        val snapshot = session.snapshot()
        val piece = snapshot.pieceAt(square)
        if (piece != null && piece.color() == snapshot.active()) {
            selected = square; highlights = session.moves(square); spell = null; target = null
            message = "Выбрано: ${square}. Сделай ход или примени способность."
        } else if (from != null && square in highlights) {
            val actor = snapshot.pieceAt(from)
            if (actor?.type() == Pawn::class.java && square.row in listOf(0, 7)) {
                promotion = from to square; message = "Выбери фигуру для превращения"
            } else finish(session.move(from, square))
        } else message = "Выбери свою фигуру или подсвеченную клетку"
    }

    fun promote(value: Promotion) {
        val pending = promotion ?: return
        finish(session.move(pending.first, pending.second, value))
    }

    fun endTurn() { finish(session.endTurn()) }
    private fun finish(result: MoveResult) {
        message = if (result.isSuccess) "Действие выполнено" else result.message
        if (result.isSuccess) clear()
    }
}

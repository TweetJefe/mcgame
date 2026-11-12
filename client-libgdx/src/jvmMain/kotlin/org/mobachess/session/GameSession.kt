package org.mobachess.session

import game.core.Game
import game.draft.Draft
import game.draft.DraftRules
import game.heroes.Abilities
import game.heroes.AbilityRequest
import game.heroes.HeroRules
import game.heroes.HeroType
import game.moves.MoveResult
import game.moves.Promotion
import game.state.GameSnapshot
import model.FigureColor
import model.Position

private fun pos(cell: String) = Position(cell[1] - '1', cell[0] - 'a')

/** Контракт клиента: локальный движок сейчас, транспорт Go-сервера в будущем. */
interface GameSession {
    fun snapshot(): GameSnapshot
    fun moves(from: Position): Set<Position>
    fun targets(from: Position, ability: Abilities, borrowed: Boolean): Set<Position>
    fun destinations(from: Position, ability: Abilities, target: Position, borrowed: Boolean): Set<Position>
    fun availability(from: Position, ability: Abilities, borrowed: Boolean): MoveResult
    fun move(from: Position, to: Position, promotion: Promotion? = null): MoveResult
    fun cast(request: AbilityRequest): MoveResult
    fun endTurn(): MoveResult
}

class LocalGameSession(private val game: Game) : GameSession {
    override fun snapshot() = game.snapshot
    override fun moves(from: Position): Set<Position> = game.getLegalMoves(from).let { (it.canMove + it.canAttack).toSet() }
    override fun targets(from: Position, ability: Abilities, borrowed: Boolean) = game.getAbilityTargets(from, ability, borrowed).toSet()
    override fun destinations(from: Position, ability: Abilities, target: Position, borrowed: Boolean) =
        game.getAbilityDestinations(from, ability, target, borrowed).toSet()
    override fun availability(from: Position, ability: Abilities, borrowed: Boolean) = game.getAbilityAvailability(from, ability, borrowed)
    override fun move(from: Position, to: Position, promotion: Promotion?) = game.moveFigure(game.currentPlayer, from, to, promotion)
    override fun cast(request: AbilityRequest) = game.useAbility(game.currentPlayer, request)
    override fun endTurn() = game.endTurn(game.currentPlayer)

    companion object {
        /** Фиксированный полноценный состав 2П+С+К+Л, без ожидания драфта для визуального прототипа. */
        fun preview(): LocalGameSession {
            val draft = Draft(DraftRules(0, 5))
            val white = listOf(HeroType.PUDGE to "a1", HeroType.LUMESHARD to "c1", HeroType.BOLOTNIK to "g1",
                HeroType.BOUNTY_CROW to "b2", HeroType.LESHY to "c2")
            val black = listOf(HeroType.SHIVA to "h8", HeroType.GINSENG to "f8", HeroType.FAUN to "b8",
                HeroType.CHRONOMLIN to "f7", HeroType.STRIX to "g7")
            white.indices.forEach { i ->
                check(draft.pick(FigureColor.WHITE, white[i].first, pos(white[i].second)).isSuccess)
                check(draft.pick(FigureColor.BLACK, black[i].first, pos(black[i].second)).isSuccess)
            }
            return LocalGameSession(Game.mobaFromDraft(draft.snapshot, HeroRules.defaults(), 600))
        }
    }
}

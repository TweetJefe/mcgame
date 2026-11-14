package org.mobachess.session

import game.core.Game
import game.heroes.Abilities
import game.heroes.AbilityRequest
import game.heroes.HeroType
import game.moves.MoveResult
import game.moves.Promotion
import model.Figure
import model.FigureColor
import model.Position

interface SandboxSession : GameSession {
    fun place(body: Class<out Figure>, hero: HeroType, color: FigureColor, square: Position): MoveResult
    fun clearBoard()
}

/** Редактирование и сброс принадлежат отдельной сессии; игровые команды делегируются обычному адаптеру. */
class LocalSandboxSession : SandboxSession {
    private var game = Game.sandbox()
    private var play = LocalGameSession(game)
    override fun snapshot() = play.snapshot()
    override fun moves(from: Position) = play.moves(from)
    override fun targets(from: Position, ability: Abilities, borrowed: Boolean) = play.targets(from, ability, borrowed)
    override fun destinations(from: Position, ability: Abilities, target: Position, borrowed: Boolean) = play.destinations(from, ability, target, borrowed)
    override fun availability(from: Position, ability: Abilities, borrowed: Boolean) = play.availability(from, ability, borrowed)
    override fun move(from: Position, to: Position, promotion: Promotion?) = play.move(from, to, promotion)
    override fun cast(request: AbilityRequest) = play.cast(request)
    override fun endTurn() = play.endTurn()
    override fun place(body: Class<out Figure>, hero: HeroType, color: FigureColor, square: Position) =
        game.placeEditorPiece(body, hero, color, square)
    override fun clearBoard() { game = Game.sandbox(); play = LocalGameSession(game) }
}

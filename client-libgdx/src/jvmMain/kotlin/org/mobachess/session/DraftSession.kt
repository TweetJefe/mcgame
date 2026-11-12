package org.mobachess.session

import game.core.Game
import game.draft.MatchDraft
import game.heroes.HeroRules
import game.heroes.HeroType
import model.FigureColor
import model.Position
import java.util.function.LongSupplier

/** UI драфта использует сессию; локальная реализация делегирует правила движку. */
interface DraftSession {
    fun tick(): MatchDraft.Snapshot
    fun snapshot(): MatchDraft.Snapshot
    fun available(): List<HeroType>
    fun choose(hero: HeroType): MatchDraft.Result
    fun placementTargets(side: FigureColor, hero: HeroType): List<Position>
    fun place(side: FigureColor, hero: HeroType, square: Position): MatchDraft.Result
    fun startMatch(): GameSession
}

class LocalDraftSession(clock: LongSupplier = LongSupplier { System.nanoTime() }) : DraftSession {
    private val draft = MatchDraft(HeroRules.defaults(), clock)
    override fun tick() = draft.tick()
    override fun snapshot() = draft.getSnapshot()
    override fun available() = draft.availableHeroes()
    override fun choose(hero: HeroType): MatchDraft.Result {
        val action = snapshot().expected() ?: return MatchDraft.Result.WRONG_PHASE
        return if (action.kind() == MatchDraft.Kind.BAN) draft.ban(action.side(), hero) else draft.pick(action.side(), hero)
    }
    override fun placementTargets(side: FigureColor, hero: HeroType) = draft.placementTargets(side, hero)
    override fun place(side: FigureColor, hero: HeroType, square: Position) = draft.place(side, hero, square)
    override fun startMatch(): GameSession = LocalGameSession(Game.mobaFromDraft(draft, 600, System::nanoTime))
}

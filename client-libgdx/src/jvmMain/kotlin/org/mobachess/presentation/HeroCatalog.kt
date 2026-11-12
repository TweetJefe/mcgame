package org.mobachess.presentation

import game.draft.DraftCatalog
import game.draft.DraftRole
import game.heroes.HeroDefinition
import game.heroes.HeroRegistry
import game.heroes.HeroRules
import game.heroes.HeroType

/** Группы берутся из определений движка, а не из списка имён в интерфейсе. */
class HeroCatalog {
    val rules = HeroRules.defaults()
    val registry = HeroRegistry.fromRules(rules)
    val draft = DraftCatalog.current(rules.definitions())
    val groups = listOf(DraftRole.PAWN, DraftRole.KNIGHT, DraftRole.BISHOP, DraftRole.ROOK)

    fun heroes(role: DraftRole): List<HeroType> = draft.heroes().filter { DraftRole.of(draft.definition(it)) == role }
    fun definition(hero: HeroType): HeroDefinition<*> = registry.get(hero).definition()
    fun name(hero: HeroType): String = definition(hero).displayName()
    fun abilities(hero: HeroType): String = registry.get(hero).abilities().joinToString(" • ") { it.displayName }
    fun groupName(role: DraftRole): String = when (role) {
        DraftRole.PAWN -> "ПЕШКИ"
        DraftRole.KNIGHT -> "КОНИ"
        DraftRole.BISHOP -> "СЛОНЫ"
        DraftRole.ROOK -> "ЛАДЬИ"
    }
}

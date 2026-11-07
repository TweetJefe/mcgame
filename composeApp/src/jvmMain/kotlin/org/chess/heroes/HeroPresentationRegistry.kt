package org.chess.heroes

import game.heroes.HeroDefinition
import game.heroes.HeroRegistry
import game.heroes.HeroType

/** Состав UI проверяется отдельно от создания и проверки игрового движка. */
class HeroPresentationRegistry(presentations: List<HeroPresentation>) {
    private val entries: Map<HeroType, HeroPresentation>
    init {
        require(presentations.map { it.type }.distinct().size == presentations.size) { "Duplicate hero presentation" }
        entries = java.util.Map.copyOf(presentations.associateBy { it.type })
    }
    operator fun get(type: HeroType): HeroPresentation = requireNotNull(entries[type]) { "Missing HeroPresentation: $type" }
    @androidx.compose.runtime.Composable
    fun BoardDecorations(context: BoardEffectContext) {
        context.heroes.rules().definitions().forEach { entries[it.type()]?.BoardDecoration(context) }
    }
    fun validate(definitions: List<HeroDefinition<*>>) {
        definitions.forEach {
            val presentation = get(it.type())
            require(presentation.shortName.isNotBlank() && presentation.summary.isNotBlank()) { "Incomplete hero presentation: ${it.type()}" }
        }
    }
    fun validate(heroes: HeroRegistry, definitions: List<HeroDefinition<*>>) {
        validate(definitions)
        definitions.forEach { definition ->
            val presentation = get(definition.type())
            require(presentation.shortName.isNotBlank() && presentation.summary.isNotBlank()) { "Incomplete hero presentation" }
            heroes.get(definition.type()).abilities().forEach { require(presentation.label(it).isNotBlank()) }
        }
    }
}

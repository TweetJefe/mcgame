package org.mobachess.presentation

import com.badlogic.gdx.Gdx
import game.heroes.HeroType
import java.util.Locale

data class HeroGuideEntry(val name: String, val paragraphs: List<String>)

/** Тексты поставляются из единственного документа docs/heroes_abilities.txt при сборке. */
class HeroGuide {
    private val entries: Map<String, HeroGuideEntry> by lazy {
        val result = linkedMapOf<String, HeroGuideEntry>()
        var title: String? = null
        var paragraphs = mutableListOf<String>()
        fun commit() {
            title?.let { result[key(it)] = HeroGuideEntry(it, paragraphs.toList()) }
        }
        Gdx.files.internal("guides/heroes_abilities.txt").readString("UTF-8").lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.endsWith(":") && line.contains(" (")) {
                commit()
                title = line.substringBefore(" (")
                paragraphs = mutableListOf()
            } else if (title != null && line.isNotBlank()) paragraphs.add(line)
        }
        commit()
        result.toMap()
    }

    fun entry(hero: HeroType): HeroGuideEntry = entries[key(hero.name)]
        ?: HeroGuideEntry(hero.name, listOf("Описание пока не добавлено."))

    private fun key(value: String): String = value.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }
}

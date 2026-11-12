package org.mobachess.presentation

import game.heroes.HeroType

/** Палитра клиента, независимая от правил и цвета шахматной стороны. */
object HeroVisuals {
    private val castColors = mapOf(
        HeroType.PUDGE to "F3B64B", HeroType.LUMESHARD to "46CCFF",
        HeroType.BOUNTY_CROW to "53DD70", HeroType.BOLOTNIK to "829A3C",
        HeroType.IZBA to "C0A269", HeroType.BABA_YAGA to "B9D16B",
        HeroType.GINSENG to "ED354A", HeroType.FAUN to "B774EB",
        HeroType.ARES to "EF603A", HeroType.CLOWNGANGER to "E84FC7",
        HeroType.ILLUSION to "E84FC7", HeroType.MOLEMINER to "F3A743",
        HeroType.ZEUS to "73CFFF", HeroType.CHRONOMLIN to "43DED3",
        HeroType.KIKIMORA to "71D4B1", HeroType.SUN_WUKONG to "F7CD58",
        HeroType.STRIX to "44DCAE", HeroType.PHOENIX to "FF7B30",
        HeroType.SUPERNOVA to "FFD66B", HeroType.LESHY to "7EA851",
        HeroType.SHIVA to "6BA9F2"
    )

    fun castColor(hero: HeroType): String = castColors[hero] ?: "E5C88B"
    const val CAST_FLASH_SECONDS = .8f
}

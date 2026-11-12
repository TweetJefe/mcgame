package org.mobachess.assets

import game.heroes.Abilities
import game.heroes.HeroType
import game.heroes.ares.AresAbility
import game.heroes.bolotnik.BolotnikAbility
import game.heroes.bountycrow.BountyCrowAbility
import game.heroes.chronomlin.ChronomlinAbility
import game.heroes.clownganger.ClowngangerAbility
import game.heroes.faun.FaunAbility
import game.heroes.ginseng.GinsengAbility
import game.heroes.izba.IzbaAbility
import game.heroes.kikimora.KikimoraAbility
import game.heroes.leshy.LeshyAbility
import game.heroes.lumeshard.LumeshardAbility
import game.heroes.moleminer.MoleminerAbility
import game.heroes.phoenix.PhoenixAbility
import game.heroes.pudge.PudgeAbility
import game.heroes.shiva.ShivaAbility
import game.heroes.strix.StrixAbility
import game.heroes.sunwukong.SunWukongAbility
import game.heroes.supernova.SupernovaAbility
import game.heroes.zeus.ZeusAbility

data class HudSkill(val ability: Abilities?, val iconPath: String?, val title: String) {
    val passive: Boolean get() = ability == null
}

/** Только представление: слот не определяется порядком активных способностей в движке. */
object SkillIconCatalog {
    private fun slots(folder: String, stem: String, skill: Abilities?, ultimate: Abilities?,
                      skillTitle: String = "Пассивная способность", ultTitle: String = "Пассивная ульта") = listOf(
        HudSkill(skill, "art/skill_icons/$folder/${stem}_skill.png", skill?.displayName ?: skillTitle),
        HudSkill(ultimate, "art/skill_icons/$folder/${stem}_ult.png", ultimate?.displayName ?: ultTitle)
    )

    private val heroes = mapOf(
        HeroType.ARES to slots("ares", "ares", AresAbility.SPEAR, null, ultTitle = "Забрать с собой (пассивная ульта)"),
        HeroType.BOLOTNIK to slots("bolotnik", "bolotnik", BolotnikAbility.MUD, BolotnikAbility.AMBUSH),
        HeroType.BOUNTY_CROW to slots("bountycrow", "bountycrow", BountyCrowAbility.BOUNTY, BountyCrowAbility.STEAL),
        HeroType.CHRONOMLIN to slots("chronomlin", "chronomlin", ChronomlinAbility.GRAPPLE, ChronomlinAbility.EVOLVE),
        HeroType.CLOWNGANGER to slots("clopwnganger", "clownganger", ClowngangerAbility.BOX, ClowngangerAbility.ILLUSION),
        HeroType.FAUN to slots("faun", "faun", FaunAbility.CLOUD, FaunAbility.INVISIBILITY),
        HeroType.GINSENG to slots("ginseng", "ginseng", GinsengAbility.DISPLACE, GinsengAbility.COPY_SPELL),
        HeroType.IZBA to slots("izba", "izba", null, IzbaAbility.SUMMON_YAGA, skillTitle = "Баба-яга после гибели (пассивная способность)"),
        HeroType.KIKIMORA to slots("kikimora", "kikimora", KikimoraAbility.YARN, KikimoraAbility.CLAY_POT),
        HeroType.MOLEMINER to slots("moleminer", "moleminer", MoleminerAbility.PLANT_MINE, MoleminerAbility.DETONATE),
        HeroType.STRIX to slots("strix", "strix", StrixAbility.REWIND, StrixAbility.TIME_SKIP),
        HeroType.SUN_WUKONG to slots("wukong", "wukong", SunWukongAbility.SHAPESHIFT, SunWukongAbility.CLONE),
        HeroType.ZEUS to slots("zeus", "zeus", ZeusAbility.CHAIN_LIGHTNING, ZeusAbility.THUNDERSTORM),
        HeroType.PUDGE to slots("pudge", "pudge", PudgeAbility.HOOK, PudgeAbility.SWALLOW),
        HeroType.LUMESHARD to slots("lumeshard", "lumeshard", LumeshardAbility.LASER, LumeshardAbility.TELEPORT),
        HeroType.LESHY to slots("leshy", "leshy", LeshyAbility.VINE, LeshyAbility.FOREST),
        HeroType.SHIVA to slots("shiva", "shiva", ShivaAbility.LOTUS, ShivaAbility.REBIRTH),
        HeroType.PHOENIX to slots("phoenix", "phoenix", null, PhoenixAbility.REBIRTH),
        HeroType.SUPERNOVA to slots("supernova", "supernova", SupernovaAbility.PULL, SupernovaAbility.SHUFFLE)
    )

    val paths: List<String> = heroes.values.flatten().mapNotNull { it.iconPath }.distinct()
    fun slots(hero: HeroType): List<HudSkill> = heroes[hero].orEmpty()
}

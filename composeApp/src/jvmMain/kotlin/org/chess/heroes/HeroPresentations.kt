package org.chess.heroes

import org.chess.heroes.pudge.PudgePresentation
import org.chess.heroes.lumeshard.LumeshardPresentation
import org.chess.heroes.bountycrow.BountyCrowPresentation
import org.chess.heroes.bolotnik.BolotnikPresentation
import org.chess.heroes.izba.IzbaPresentation
import org.chess.heroes.ginseng.GinsengPresentation
import org.chess.heroes.faun.FaunPresentation
import org.chess.heroes.ares.AresPresentation
import org.chess.heroes.clownganger.ClowngangerPresentation
import org.chess.heroes.moleminer.MoleminerPresentation
import org.chess.heroes.zeus.ZeusPresentation
import org.chess.heroes.chronomlin.ChronomlinPresentation
import org.chess.heroes.kikimora.KikimoraPresentation
import org.chess.heroes.plain.NonePresentation
import org.chess.heroes.clownganger.IllusionPresentation
import org.chess.heroes.izba.BabaYagaPresentation

/** Единственная регистрация UI; игровые описания регистрируются независимо. */
object HeroPresentations {
    val all: List<HeroPresentation> = java.util.List.copyOf(listOf(
        PudgePresentation,
        LumeshardPresentation,
        BountyCrowPresentation,
        BolotnikPresentation,
        IzbaPresentation,
        GinsengPresentation,
        FaunPresentation,
        AresPresentation,
        ClowngangerPresentation,
        MoleminerPresentation,
        ZeusPresentation,
        ChronomlinPresentation,
        KikimoraPresentation,
        NonePresentation,
        IllusionPresentation,
        BabaYagaPresentation))
    val standard = HeroPresentationRegistry(all)
}

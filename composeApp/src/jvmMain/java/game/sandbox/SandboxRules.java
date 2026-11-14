package game.sandbox;

import game.heroes.HeroRules;
import game.heroes.pudge.PudgeDefinition;
import game.heroes.pudge.PudgeRules;
import game.heroes.lumeshard.LumeshardDefinition;
import game.heroes.lumeshard.LumeshardRules;
import game.heroes.bountycrow.BountyCrowDefinition;
import game.heroes.bountycrow.BountyCrowRules;
import game.heroes.bolotnik.BolotnikDefinition;
import game.heroes.bolotnik.BolotnikRules;
import game.heroes.izba.IzbaDefinition;
import game.heroes.izba.IzbaRules;
import game.heroes.ginseng.GinsengDefinition;
import game.heroes.ginseng.GinsengRules;
import game.heroes.faun.FaunDefinition;
import game.heroes.faun.FaunRules;
import game.heroes.ares.AresDefinition;
import game.heroes.ares.AresRules;
import game.heroes.clownganger.ClowngangerDefinition;
import game.heroes.clownganger.ClowngangerRules;
import game.heroes.moleminer.MoleminerDefinition;
import game.heroes.moleminer.MoleminerRules;
import game.heroes.zeus.ZeusDefinition;
import game.heroes.zeus.ZeusRules;
import game.heroes.chronomlin.ChronomlinDefinition;
import game.heroes.chronomlin.ChronomlinRules;
import game.heroes.kikimora.KikimoraDefinition;
import game.heroes.kikimora.KikimoraRules;
import game.heroes.sunwukong.SunWukongDefinition;
import game.heroes.sunwukong.SunWukongRules;
import game.heroes.strix.StrixDefinition;
import game.heroes.strix.StrixRules;
import game.heroes.phoenix.PhoenixDefinition;
import game.heroes.phoenix.PhoenixRules;
import game.heroes.supernova.SupernovaDefinition;
import game.heroes.supernova.SupernovaRules;
import game.heroes.leshy.LeshyDefinition;
import game.heroes.leshy.LeshyRules;
import game.heroes.shiva.ShivaDefinition;
import game.heroes.shiva.ShivaRules;

/** Отдельная конфигурация: геометрия и сроки остаются стандартными, КД и цена равны нулю. */
public final class SandboxRules {
    private SandboxRules() {}
    public static HeroRules create() {
        return HeroRules.defaults()
                .with(PudgeDefinition.INSTANCE, PudgeRules.sandbox())
                .with(LumeshardDefinition.INSTANCE, LumeshardRules.sandbox())
                .with(BountyCrowDefinition.INSTANCE, BountyCrowRules.sandbox())
                .with(BolotnikDefinition.INSTANCE, BolotnikRules.sandbox())
                .with(IzbaDefinition.INSTANCE, IzbaRules.sandbox())
                .with(GinsengDefinition.INSTANCE, GinsengRules.sandbox())
                .with(FaunDefinition.INSTANCE, FaunRules.sandbox())
                .with(AresDefinition.INSTANCE, AresRules.sandbox())
                .with(ClowngangerDefinition.INSTANCE, ClowngangerRules.sandbox())
                .with(MoleminerDefinition.INSTANCE, MoleminerRules.sandbox())
                .with(ZeusDefinition.INSTANCE, ZeusRules.sandbox())
                .with(ChronomlinDefinition.INSTANCE, ChronomlinRules.sandbox())
                .with(KikimoraDefinition.INSTANCE, KikimoraRules.sandbox())
                .with(SunWukongDefinition.INSTANCE, SunWukongRules.sandbox())
                .with(StrixDefinition.INSTANCE, StrixRules.sandbox())
                .with(PhoenixDefinition.INSTANCE, new PhoenixRules(0, 2))
                .with(SupernovaDefinition.INSTANCE, new SupernovaRules(0, 0, 2, 3))
                .with(LeshyDefinition.INSTANCE, new LeshyRules(3, 3, 0, 2, 0))
                .with(ShivaDefinition.INSTANCE, new ShivaRules(2, 3, 2, 0, 0));
    }
}

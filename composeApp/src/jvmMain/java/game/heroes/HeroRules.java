package game.heroes;

import game.heroes.pudge.PudgeRules;
import game.heroes.faun.FaunRules;
import game.heroes.ares.AresRules;
import game.heroes.izba.IzbaRules;
import game.heroes.ginseng.GinsengRules;
import game.heroes.lumeshard.LumeshardRules;
import java.util.Objects;
import game.heroes.bolotnik.BolotnikRules;
import game.heroes.bountycrow.BountyCrowRules;
import game.economy.GoldRules;

/** Configuration shared by dispatch and snapshots, with independent settings for each hero. */
public record HeroRules(PudgeRules pudge, LumeshardRules lumeshard, BountyCrowRules crow, GoldRules gold, BolotnikRules bolotnik, IzbaRules izba, GinsengRules ginseng, FaunRules faun, AresRules ares, game.heroes.clownganger.ClowngangerRules clownganger, game.heroes.moleminer.MoleminerRules moleminer, game.heroes.zeus.ZeusRules zeus, game.heroes.chronomlin.ChronomlinRules chronomlin, game.heroes.kikimora.KikimoraRules kikimora) {
    public HeroRules { Objects.requireNonNull(pudge); Objects.requireNonNull(lumeshard); Objects.requireNonNull(crow); Objects.requireNonNull(gold); Objects.requireNonNull(bolotnik); Objects.requireNonNull(izba); Objects.requireNonNull(ginseng); Objects.requireNonNull(faun); Objects.requireNonNull(ares); Objects.requireNonNull(clownganger); Objects.requireNonNull(moleminer); Objects.requireNonNull(zeus); Objects.requireNonNull(chronomlin); Objects.requireNonNull(kikimora); }
    public HeroRules(PudgeRules pudge, LumeshardRules lumeshard, BountyCrowRules crow, GoldRules gold, BolotnikRules bolotnik, IzbaRules izba, GinsengRules ginseng, FaunRules faun, AresRules ares, game.heroes.clownganger.ClowngangerRules clownganger, game.heroes.moleminer.MoleminerRules moleminer, game.heroes.zeus.ZeusRules zeus, game.heroes.chronomlin.ChronomlinRules chronomlin){this(pudge,lumeshard,crow,gold,bolotnik,izba,ginseng,faun,ares,clownganger,moleminer,zeus,chronomlin,game.heroes.kikimora.KikimoraRules.prototype());}
    public HeroRules(PudgeRules pudge, LumeshardRules lumeshard, BountyCrowRules crow, GoldRules gold, BolotnikRules bolotnik, IzbaRules izba, GinsengRules ginseng, FaunRules faun, AresRules ares, game.heroes.clownganger.ClowngangerRules clownganger, game.heroes.moleminer.MoleminerRules moleminer, game.heroes.zeus.ZeusRules zeus){this(pudge,lumeshard,crow,gold,bolotnik,izba,ginseng,faun,ares,clownganger,moleminer,zeus,game.heroes.chronomlin.ChronomlinRules.prototype());}
    public HeroRules(PudgeRules pudge,LumeshardRules lumeshard,BountyCrowRules crow,GoldRules gold,BolotnikRules bolotnik,IzbaRules izba,GinsengRules ginseng,FaunRules faun,AresRules ares,game.heroes.clownganger.ClowngangerRules clownganger,game.heroes.moleminer.MoleminerRules moleminer){this(pudge,lumeshard,crow,gold,bolotnik,izba,ginseng,faun,ares,clownganger,moleminer,game.heroes.zeus.ZeusRules.prototype());}
    public HeroRules(PudgeRules pudge,LumeshardRules lumeshard,BountyCrowRules crow,GoldRules gold,BolotnikRules bolotnik,IzbaRules izba,GinsengRules ginseng,FaunRules faun,AresRules ares,game.heroes.clownganger.ClowngangerRules clownganger){this(pudge,lumeshard,crow,gold,bolotnik,izba,ginseng,faun,ares,clownganger,game.heroes.moleminer.MoleminerRules.prototype());}
    public HeroRules(PudgeRules pudge,LumeshardRules lumeshard,BountyCrowRules crow,GoldRules gold,BolotnikRules bolotnik,IzbaRules izba,GinsengRules ginseng,FaunRules faun,AresRules ares) {this(pudge,lumeshard,crow,gold,bolotnik,izba,ginseng,faun,ares,game.heroes.clownganger.ClowngangerRules.prototype());}
    public HeroRules(PudgeRules pudge,LumeshardRules lumeshard,BountyCrowRules crow,GoldRules gold,BolotnikRules bolotnik,IzbaRules izba,GinsengRules ginseng,FaunRules faun) {this(pudge,lumeshard,crow,gold,bolotnik,izba,ginseng,faun,AresRules.prototype());}
    public HeroRules(PudgeRules pudge, LumeshardRules lumeshard, BountyCrowRules crow, GoldRules gold, BolotnikRules bolotnik, IzbaRules izba, GinsengRules ginseng) { this(pudge,lumeshard,crow,gold,bolotnik,izba,ginseng,FaunRules.prototype()); }
    public HeroRules(PudgeRules pudge, LumeshardRules lumeshard, BountyCrowRules crow, GoldRules gold, BolotnikRules bolotnik, IzbaRules izba) { this(pudge,lumeshard,crow,gold,bolotnik,izba,GinsengRules.prototype()); }
    public HeroRules(PudgeRules pudge, LumeshardRules lumeshard, BountyCrowRules crow, GoldRules gold, BolotnikRules bolotnik) { this(pudge,lumeshard,crow,gold,bolotnik,IzbaRules.prototype()); }
    public HeroRules(PudgeRules pudge, LumeshardRules lumeshard, BountyCrowRules crow, GoldRules gold) { this(pudge, lumeshard, crow, gold, BolotnikRules.prototype()); }
    public HeroRules(PudgeRules pudge, LumeshardRules lumeshard) { this(pudge, lumeshard, BountyCrowRules.prototype(), GoldRules.prototype()); }
    public static HeroRules prototype() { return new HeroRules(PudgeRules.prototype(), LumeshardRules.prototype()); }
    public static HeroRules sandbox() { return new HeroRules(PudgeRules.sandbox(), LumeshardRules.sandbox(), BountyCrowRules.sandbox(), GoldRules.prototype(), BolotnikRules.sandbox()); }
    public static HeroRules withPudge(PudgeRules pudge) { return new HeroRules(pudge, LumeshardRules.prototype()); }
}

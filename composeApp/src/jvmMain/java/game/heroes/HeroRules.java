package game.heroes;

import game.economy.GoldRules;
import game.heroes.pudge.PudgeDefinition;
import game.heroes.pudge.PudgeRules;
import game.heroes.lumeshard.LumeshardDefinition;
import game.heroes.lumeshard.LumeshardRules;
import game.heroes.bountycrow.BountyCrowDefinition;
import game.heroes.bountycrow.BountyCrowRules;
import game.heroes.bolotnik.BolotnikDefinition;
import game.heroes.bolotnik.BolotnikRules;
import game.heroes.kikimora.KikimoraDefinition;
import game.heroes.kikimora.KikimoraRules;
import game.heroes.chronomlin.ChronomlinDefinition;
import game.heroes.chronomlin.ChronomlinRules;
import game.heroes.zeus.ZeusDefinition;
import game.heroes.zeus.ZeusRules;
import game.heroes.moleminer.MoleminerDefinition;
import game.heroes.moleminer.MoleminerRules;
import game.heroes.clownganger.ClowngangerDefinition;
import game.heroes.clownganger.ClowngangerRules;
import game.heroes.ares.AresDefinition;
import game.heroes.ares.AresRules;
import game.heroes.faun.FaunDefinition;
import game.heroes.faun.FaunRules;
import game.heroes.ginseng.GinsengDefinition;
import game.heroes.ginseng.GinsengRules;
import game.heroes.izba.IzbaDefinition;
import game.heroes.izba.IzbaRules;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable typed parameters for a game's registered hero definitions. */
public final class HeroRules {
    private record Entry(HeroDefinition<?> definition, Object parameters) {}
    private final Map<HeroType, Entry> entries;
    private final List<HeroDefinition<?>> definitions;
    private final GoldRules gold;

    private HeroRules(Map<HeroType, Entry> entries, GoldRules gold) {
        this.entries = Map.copyOf(entries);
        this.definitions = List.copyOf(entries.values().stream().map(Entry::definition).toList());
        this.gold = Objects.requireNonNull(gold, "gold");
    }

    private HeroRules(HeroRules rules) { this(rules.orderedEntries(), rules.gold); }

    public static HeroRules defaults() { return defaults(HeroDefinitions.all()); }

    public static HeroRules defaults(List<HeroDefinition<?>> definitions) {
        var entries = new LinkedHashMap<HeroType, Entry>();
        var sections = new java.util.HashSet<String>();
        for (HeroDefinition<?> definition : definitions) {
            Objects.requireNonNull(definition, "definition");
            if (entries.putIfAbsent(definition.type(), new Entry(definition, definition.defaultRules())) != null)
                throw new IllegalArgumentException("Duplicate hero definition: " + definition.type());
            definition.rulesCodec().ifPresent(codec -> {
                if (!sections.add(codec.section())) throw new IllegalArgumentException("Duplicate rules section: " + codec.section());
            });
        }
        return new HeroRules(entries, GoldRules.prototype());
    }

    public List<HeroDefinition<?>> definitions() { return definitions; }

    public <R> R get(HeroDefinition<R> definition) {
        return definition.checkedRules(entry(definition).parameters());
    }

    public <R> HeroRules with(HeroDefinition<R> definition, R parameters) {
        entry(definition);
        R checked = definition.checkedRules(parameters);
        var next = orderedEntries();
        next.put(definition.type(), new Entry(definition, checked));
        return new HeroRules(next, gold);
    }

    private Entry entry(HeroDefinition<?> definition) {
        Objects.requireNonNull(definition, "definition");
        Entry entry = entries.get(definition.type());
        if (entry == null || entry.definition() != definition)
            throw new IllegalArgumentException("Unregistered hero definition: " + definition.type());
        return entry;
    }

    private LinkedHashMap<HeroType, Entry> orderedEntries() {
        var result = new LinkedHashMap<HeroType, Entry>();
        for (HeroDefinition<?> definition : definitions) result.put(definition.type(), entries.get(definition.type()));
        return result;
    }

    public GoldRules gold() { return gold; }
    public HeroRules withGold(GoldRules gold) { return new HeroRules(orderedEntries(), gold); }

    @Override public boolean equals(Object value) {
        return value instanceof HeroRules other && entries.equals(other.entries) && gold.equals(other.gold);
    }
    @Override public int hashCode() { return Objects.hash(entries, gold); }

    public static HeroRules prototype() { return defaults(); }
    public static HeroRules withPudge(PudgeRules rules) { return defaults().with(PudgeDefinition.INSTANCE, rules); }

    /** Preserves the original sandbox profile; other heroes keep their prototype parameters. */
    public static HeroRules sandbox() {
        return defaults().with(PudgeDefinition.INSTANCE, PudgeRules.sandbox())
                .with(LumeshardDefinition.INSTANCE, LumeshardRules.sandbox())
                .with(BountyCrowDefinition.INSTANCE, BountyCrowRules.sandbox())
                .with(BolotnikDefinition.INSTANCE, BolotnikRules.sandbox());
    }

    // Compatibility API for existing callers. New definitions need no additions here.
    public PudgeRules pudge() { return get(PudgeDefinition.INSTANCE); }
    public LumeshardRules lumeshard() { return get(LumeshardDefinition.INSTANCE); }
    public BountyCrowRules crow() { return get(BountyCrowDefinition.INSTANCE); }
    public BolotnikRules bolotnik() { return get(BolotnikDefinition.INSTANCE); }
    public IzbaRules izba() { return get(IzbaDefinition.INSTANCE); }
    public GinsengRules ginseng() { return get(GinsengDefinition.INSTANCE); }
    public FaunRules faun() { return get(FaunDefinition.INSTANCE); }
    public AresRules ares() { return get(AresDefinition.INSTANCE); }
    public ClowngangerRules clownganger() { return get(ClowngangerDefinition.INSTANCE); }
    public MoleminerRules moleminer() { return get(MoleminerDefinition.INSTANCE); }
    public ZeusRules zeus() { return get(ZeusDefinition.INSTANCE); }
    public ChronomlinRules chronomlin() { return get(ChronomlinDefinition.INSTANCE); }
    public KikimoraRules kikimora() { return get(KikimoraDefinition.INSTANCE); }

    public HeroRules(PudgeRules pudge, LumeshardRules lumeshard, BountyCrowRules crow, GoldRules gold,
                        BolotnikRules bolotnik, IzbaRules izba, GinsengRules ginseng, FaunRules faun, AresRules ares, ClowngangerRules clownganger, MoleminerRules moleminer, ZeusRules zeus, ChronomlinRules chronomlin, KikimoraRules kikimora) {
        this(defaults()
                .with(PudgeDefinition.INSTANCE, pudge)
                .with(LumeshardDefinition.INSTANCE, lumeshard)
                .with(BountyCrowDefinition.INSTANCE, crow)
                .with(BolotnikDefinition.INSTANCE, bolotnik)
                .with(IzbaDefinition.INSTANCE, izba)
                .with(GinsengDefinition.INSTANCE, ginseng)
                .with(FaunDefinition.INSTANCE, faun)
                .with(AresDefinition.INSTANCE, ares)
                .with(ClowngangerDefinition.INSTANCE, clownganger)
                .with(MoleminerDefinition.INSTANCE, moleminer)
                .with(ZeusDefinition.INSTANCE, zeus)
                .with(ChronomlinDefinition.INSTANCE, chronomlin)
                .with(KikimoraDefinition.INSTANCE, kikimora)
                .withGold(gold));
    }
    public HeroRules(PudgeRules pudge, LumeshardRules lumeshard, BountyCrowRules crow, GoldRules gold,
                     BolotnikRules bolotnik, IzbaRules izba, GinsengRules ginseng, FaunRules faun,
                     AresRules ares, ClowngangerRules clownganger, MoleminerRules moleminer, ZeusRules zeus,
                     ChronomlinRules chronomlin) {
        this(pudge, lumeshard, crow, gold, bolotnik, izba,
                ginseng, faun, ares, clownganger, moleminer, zeus,
                chronomlin, KikimoraRules.prototype());
    }

    public HeroRules(PudgeRules pudge, LumeshardRules lumeshard, BountyCrowRules crow, GoldRules gold,
                     BolotnikRules bolotnik, IzbaRules izba, GinsengRules ginseng, FaunRules faun,
                     AresRules ares, ClowngangerRules clownganger, MoleminerRules moleminer, ZeusRules zeus) {
        this(pudge, lumeshard, crow, gold, bolotnik, izba,
                ginseng, faun, ares, clownganger, moleminer, zeus,
                ChronomlinRules.prototype());
    }

    public HeroRules(PudgeRules pudge, LumeshardRules lumeshard, BountyCrowRules crow, GoldRules gold,
                     BolotnikRules bolotnik, IzbaRules izba, GinsengRules ginseng, FaunRules faun,
                     AresRules ares, ClowngangerRules clownganger, MoleminerRules moleminer) {
        this(pudge, lumeshard, crow, gold, bolotnik, izba,
                ginseng, faun, ares, clownganger, moleminer, ZeusRules.prototype());
    }

    public HeroRules(PudgeRules pudge, LumeshardRules lumeshard, BountyCrowRules crow, GoldRules gold,
                     BolotnikRules bolotnik, IzbaRules izba, GinsengRules ginseng, FaunRules faun,
                     AresRules ares, ClowngangerRules clownganger) {
        this(pudge, lumeshard, crow, gold, bolotnik, izba,
                ginseng, faun, ares, clownganger, MoleminerRules.prototype());
    }

    public HeroRules(PudgeRules pudge, LumeshardRules lumeshard, BountyCrowRules crow, GoldRules gold,
                     BolotnikRules bolotnik, IzbaRules izba, GinsengRules ginseng, FaunRules faun,
                     AresRules ares) {
        this(pudge, lumeshard, crow, gold, bolotnik, izba,
                ginseng, faun, ares, ClowngangerRules.prototype());
    }

    public HeroRules(PudgeRules pudge, LumeshardRules lumeshard, BountyCrowRules crow, GoldRules gold,
                     BolotnikRules bolotnik, IzbaRules izba, GinsengRules ginseng, FaunRules faun) {
        this(pudge, lumeshard, crow, gold, bolotnik, izba,
                ginseng, faun, AresRules.prototype());
    }

    public HeroRules(PudgeRules pudge, LumeshardRules lumeshard, BountyCrowRules crow, GoldRules gold,
                     BolotnikRules bolotnik, IzbaRules izba, GinsengRules ginseng) {
        this(pudge, lumeshard, crow, gold, bolotnik, izba,
                ginseng, FaunRules.prototype());
    }

    public HeroRules(PudgeRules pudge, LumeshardRules lumeshard, BountyCrowRules crow, GoldRules gold,
                     BolotnikRules bolotnik, IzbaRules izba) {
        this(pudge, lumeshard, crow, gold, bolotnik, izba,
                GinsengRules.prototype());
    }

    public HeroRules(PudgeRules pudge, LumeshardRules lumeshard, BountyCrowRules crow, GoldRules gold,
                     BolotnikRules bolotnik) {
        this(pudge, lumeshard, crow, gold, bolotnik, IzbaRules.prototype());
    }

    public HeroRules(PudgeRules pudge, LumeshardRules lumeshard, BountyCrowRules crow, GoldRules gold) {
        this(pudge, lumeshard, crow, gold, BolotnikRules.prototype());
    }

    public HeroRules(PudgeRules pudge, LumeshardRules lumeshard) {
        this(pudge, lumeshard, BountyCrowRules.prototype(), GoldRules.prototype());
    }
}

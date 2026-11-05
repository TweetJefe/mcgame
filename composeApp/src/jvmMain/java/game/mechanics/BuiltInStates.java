package game.mechanics;

import game.economy.EconomyState;
import game.heroes.clownganger.ClownState;
import game.heroes.faun.FaunState;
import game.heroes.ginseng.SpellState;
import game.heroes.moleminer.MineState;
import game.heroes.zeus.LightningState;
import game.summoning.SummonState;
import game.terrain.TerrainState;

/** Ключи прежних публичных состояний; новые механики объявляют ключ в своём модуле. */
public final class BuiltInStates {
    private BuiltInStates() {}
    public static final MechanicKey<EconomyState> ECONOMY = new MechanicKey<>("economy", EconomyState.class, EconomyState.disabled());
    public static final MechanicKey<TerrainState> TERRAIN = new MechanicKey<>("terrain", TerrainState.class, TerrainState.empty());
    public static final MechanicKey<SummonState> SUMMONS = new MechanicKey<>("summons", SummonState.class, SummonState.empty());
    public static final MechanicKey<SpellState> SPELLS = new MechanicKey<>("spells", SpellState.class, SpellState.empty());
    public static final MechanicKey<FaunState> FAUN = new MechanicKey<>("faun", FaunState.class, FaunState.empty());
    public static final MechanicKey<ClownState> CLOWN = new MechanicKey<>("clown", ClownState.class, ClownState.empty());
    public static final MechanicKey<MineState> MINES = new MechanicKey<>("mines", MineState.class, MineState.empty());
    public static final MechanicKey<LightningState> LIGHTNING = new MechanicKey<>("lightning", LightningState.class, LightningState.empty());

    public static MechanicStates states(EconomyState economy, TerrainState terrain, SummonState summons,
            SpellState spells, FaunState faun, ClownState clown, MineState mines, LightningState lightning) {
        return MechanicStates.empty().with(ECONOMY, economy).with(TERRAIN, terrain).with(SUMMONS, summons)
                .with(SPELLS, spells).with(FAUN, faun).with(CLOWN, clown).with(MINES, mines).with(LIGHTNING, lightning);
    }
}

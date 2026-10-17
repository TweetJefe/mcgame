package game.summoning;

import java.util.Map;

/** Immutable PRNG state: previews and rejected commands never consume live randomness. */
public record SummonState(boolean enabled, long randomState, Map<Long,SummonRecord> records) {
    public static final int MAX_IDENTITIES = 100_000;
    public SummonState {
        records=Map.copyOf(records);
        if (records.size()>MAX_IDENTITIES || (!enabled && (randomState!=0 || !records.isEmpty()))
                || records.entrySet().stream().anyMatch(e->e.getKey()!=e.getValue().id()))
            throw new IllegalArgumentException("Invalid summon state");
    }
    public static SummonState empty() { return new SummonState(false,0,Map.of()); }
    public static SummonState initial(long seed) { return new SummonState(true,seed,Map.of()); }
    public record Choice(int index, long nextState) {}
    public Choice choose(int count) {
        if (!enabled || count<1) throw new IllegalArgumentException("No summon choices");
        long next=randomState+0x9e3779b97f4a7c15L;
        long value=next;value=(value^(value>>>30))*0xbf58476d1ce4e5b9L;
        value=(value^(value>>>27))*0x94d049bb133111ebL;value^=value>>>31;
        return new Choice((int)Long.remainderUnsigned(value,count),next);
    }
}

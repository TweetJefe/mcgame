package game.heroes.leshy;

import game.heroes.HeroRuleAttributes;
import game.heroes.HeroRulesCodec;
import java.util.Map;

public final class LeshyRulesCodec implements HeroRulesCodec<LeshyRules> {
    @Override public String section() { return "leshyRules"; }
    @Override public int sinceVersion() { return 21; }
    @Override public Map<String, String> encode(LeshyRules r) {
        return Map.of("vineLength", Integer.toString(r.vineLength()), "vineTurns", Integer.toString(r.vineTurns()),
                "vineCooldown", Integer.toString(r.vineCooldown()), "treeTurns", Integer.toString(r.treeTurns()),
                "ultimateCaptures", Integer.toString(r.ultimateCaptures()));
    }
    @Override public LeshyRules decode(HeroRuleAttributes a) {
        return new LeshyRules(a.integer("vineLength"), a.integer("vineTurns"), a.integer("vineCooldown"),
                a.integer("treeTurns"), a.integer("ultimateCaptures"));
    }
}

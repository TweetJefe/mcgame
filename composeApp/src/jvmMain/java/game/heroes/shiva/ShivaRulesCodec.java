package game.heroes.shiva;

import game.heroes.HeroRuleAttributes;
import game.heroes.HeroRulesCodec;
import java.util.Map;

public final class ShivaRulesCodec implements HeroRulesCodec<ShivaRules> {
    @Override public String section() { return "shivaRules"; }
    @Override public int sinceVersion() { return 22; }
    @Override public Map<String, String> encode(ShivaRules r) {
        return Map.of("castRadius", Integer.toString(r.castRadius()), "revealRadius", Integer.toString(r.revealRadius()),
                "lotusTurns", Integer.toString(r.lotusTurns()), "lotusCooldown", Integer.toString(r.lotusCooldown()),
                "ultimateCaptures", Integer.toString(r.ultimateCaptures()));
    }
    @Override public ShivaRules decode(HeroRuleAttributes a) {
        return new ShivaRules(a.integer("castRadius"), a.integer("revealRadius"), a.integer("lotusTurns"),
                a.integer("lotusCooldown"), a.integer("ultimateCaptures"));
    }
}

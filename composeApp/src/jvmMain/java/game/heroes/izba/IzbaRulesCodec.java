package game.heroes.izba;

import game.heroes.HeroRuleAttributes;
import game.heroes.HeroRulesCodec;

import java.util.Map;

/** Explicit legacy XML attributes for Izba's balance parameters. */
public final class IzbaRulesCodec implements HeroRulesCodec<IzbaRules> {
    @Override public String section() { return "izbaRules"; }
    @Override public int sinceVersion() { return 6; }
    @Override public String beforeSection() { return "summons"; }

    @Override public Map<String, String> encode(IzbaRules rules) {
        return Map.of("ultimateCaptures", String.valueOf(rules.ultimateCaptures()),
                "lifetimeTurns", String.valueOf(rules.lifetimeTurns()));
    }

    @Override public IzbaRules decode(HeroRuleAttributes attributes) {
        return new IzbaRules(attributes.integer("ultimateCaptures"),
                attributes.integer("lifetimeTurns"));
    }
}

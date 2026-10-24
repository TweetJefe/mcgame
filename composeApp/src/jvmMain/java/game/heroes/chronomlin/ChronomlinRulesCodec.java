package game.heroes.chronomlin;

import game.heroes.HeroRuleAttributes;
import game.heroes.HeroRulesCodec;

import java.util.Map;

/** Explicit legacy XML attributes for Chronomlin's balance parameters. */
public final class ChronomlinRulesCodec implements HeroRulesCodec<ChronomlinRules> {
    @Override public String section() { return "chronomlinRules"; }
    @Override public int sinceVersion() { return 13; }
    @Override public String beforeSection() { return "summons"; }

    @Override public Map<String, String> encode(ChronomlinRules rules) {
        return Map.of("grappleCooldown", String.valueOf(rules.grappleCooldown()),
                "ultimateCaptures", String.valueOf(rules.ultimateCaptures()));
    }

    @Override public ChronomlinRules decode(HeroRuleAttributes attributes) {
        return new ChronomlinRules(attributes.integer("grappleCooldown"),
                attributes.integer("ultimateCaptures"));
    }
}

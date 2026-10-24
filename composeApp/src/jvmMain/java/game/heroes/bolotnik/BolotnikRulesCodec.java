package game.heroes.bolotnik;

import game.heroes.HeroRuleAttributes;
import game.heroes.HeroRulesCodec;

import java.util.Map;

/** Explicit legacy XML attributes for Bolotnik's balance parameters. */
public final class BolotnikRulesCodec implements HeroRulesCodec<BolotnikRules> {
    @Override public String section() { return "bolotnikRules"; }
    @Override public int sinceVersion() { return 4; }
    @Override public String beforeSection() { return "terrain"; }

    @Override public Map<String, String> encode(BolotnikRules rules) {
        return Map.of("mudCooldown", String.valueOf(rules.mudCooldown()),
                "mudTurns", String.valueOf(rules.mudTurns()),
                "ultimateCaptures", String.valueOf(rules.ultimateCaptures()));
    }

    @Override public BolotnikRules decode(HeroRuleAttributes attributes) {
        return new BolotnikRules(attributes.integer("mudCooldown"),
                attributes.integer("mudTurns"),
                attributes.integer("ultimateCaptures"));
    }
}

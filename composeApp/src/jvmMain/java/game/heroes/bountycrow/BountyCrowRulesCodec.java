package game.heroes.bountycrow;

import game.heroes.HeroRuleAttributes;
import game.heroes.HeroRulesCodec;

import java.util.Map;

/** Explicit legacy XML attributes for BountyCrow's balance parameters. */
public final class BountyCrowRulesCodec implements HeroRulesCodec<BountyCrowRules> {
    @Override public String section() { return "crowRules"; }
    @Override public int sinceVersion() { return 3; }
    @Override public String beforeSection() { return "goldRules"; }

    @Override public Map<String, String> encode(BountyCrowRules rules) {
        return Map.of("bountyCooldown", String.valueOf(rules.bountyCooldown()),
                "bountyTurns", String.valueOf(rules.bountyTurns()),
                "ultimateCaptures", String.valueOf(rules.ultimateCaptures()));
    }

    @Override public BountyCrowRules decode(HeroRuleAttributes attributes) {
        return new BountyCrowRules(attributes.integer("bountyCooldown"),
                attributes.integer("bountyTurns"),
                attributes.integer("ultimateCaptures"));
    }
}

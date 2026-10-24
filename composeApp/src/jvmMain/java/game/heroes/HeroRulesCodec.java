package game.heroes;

import java.util.Map;

/** Explicit wire schema for one hero's immutable rules. No class names or reflection in saves. */
public interface HeroRulesCodec<R> {
    String section();
    int sinceVersion();
    Map<String, String> encode(R rules);
    R decode(HeroRuleAttributes attributes);

    /** Stable placement for compatibility with historical XML; new sections precede castling. */
    default String beforeSection() { return "castling"; }
}

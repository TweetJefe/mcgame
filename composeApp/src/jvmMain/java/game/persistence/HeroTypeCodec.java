package game.persistence;

import game.heroes.HeroType;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Frozen aliases for save formats that included a chess body in the hero identifier. */
final class HeroTypeCodec {
    private HeroTypeCodec() {}

    private static final Map<String, HeroType> LEGACY_IDS = Map.ofEntries(
            Map.entry("KIKIMORA_BISHOP", HeroType.KIKIMORA),
            Map.entry("CHRONOMLIN_PAWN", HeroType.CHRONOMLIN),
            Map.entry("CLOWNGANGER_KNIGHT", HeroType.CLOWNGANGER),
            Map.entry("MOLEMINER_PAWN", HeroType.MOLEMINER),
            Map.entry("ZEUS_ROOK", HeroType.ZEUS),
            Map.entry("PUDGE_ROOK", HeroType.PUDGE),
            Map.entry("LUMESHARD_BISHOP", HeroType.LUMESHARD),
            Map.entry("BOUNTY_CROW_PAWN", HeroType.BOUNTY_CROW),
            Map.entry("BOLOTNIK_KNIGHT", HeroType.BOLOTNIK),
            Map.entry("IZBA_ROOK", HeroType.IZBA),
            Map.entry("BABA_YAGA_PAWN", HeroType.BABA_YAGA),
            Map.entry("GINSENG_BISHOP", HeroType.GINSENG),
            Map.entry("FAUN_KNIGHT", HeroType.FAUN),
            Map.entry("ARES_BISHOP", HeroType.ARES));
    private static final Map<HeroType, String> LEGACY_NAMES = LEGACY_IDS.entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getValue, Map.Entry::getKey));

    static String encode(HeroType hero) {
        return LEGACY_NAMES.getOrDefault(hero, hero.name());
    }

    static HeroType decode(String id) {
        HeroType legacy = LEGACY_IDS.get(id);
        return legacy == null ? HeroType.valueOf(id) : legacy;
    }
}

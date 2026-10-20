package game.heroes;

import game.heroes.pudge.PudgeHero;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Immutable registry of reusable strategies, built once for a game's rules. */
public final class HeroRegistry {
    private final Map<HeroType, Hero> heroes;

    public HeroRegistry() {
        this(Map.of());
    }

    private HeroRegistry(Map<HeroType, Hero> heroes) {
        this.heroes = Map.copyOf(heroes);
    }

    public Hero get(HeroType type) {
        Hero hero = heroes.get(Objects.requireNonNull(type, "type"));
        if (hero == null) throw new IllegalArgumentException("Unregistered hero: " + type);
        return hero;
    }

    /** Returns a new registry; existing registries and running games stay unchanged. */
    public HeroRegistry register(Hero hero) {
        Objects.requireNonNull(hero, "hero");
        var next = new EnumMap<HeroType, Hero>(HeroType.class);
        next.putAll(heroes);
        if (next.putIfAbsent(hero.type(), hero) != null)
            throw new IllegalArgumentException("Duplicate hero: " + hero.type());
        for (Hero existing : heroes.values())
            for (Ability ability : hero.abilities())
                if (existing.abilities().contains(ability))
                    throw new IllegalArgumentException("Duplicate ability owner: " + ability);
        return new HeroRegistry(next);
    }

    /** Source strategy for a copied spell's targeting and candidate requests. */
    public Hero ownerOf(Ability ability) {
        Objects.requireNonNull(ability, "ability");
        return heroes.values().stream().filter(hero -> hero.abilities().contains(ability))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Unregistered ability: " + ability));
    }

    public static HeroRegistry fromRules(HeroRules rules) {
        Objects.requireNonNull(rules, "rules");
        Hero pudge = new PudgeHero(rules.pudge());
        HeroRegistry registry = new HeroRegistry().register(pudge);
        // Temporary adapters are removed individually as each hero gets its own strategy.
        for (HeroType type : HeroType.values())
            if (!registry.heroes.containsKey(type)) registry = registry.register(new LegacyHero(type, rules, pudge));
        return registry;
    }
}

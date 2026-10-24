package game.heroes;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable registry of reusable strategies, built once for a game's rules. */
public final class HeroRegistry {
    private final Map<HeroType, Hero> heroes;
    private final Map<String, Abilities> abilitiesById;
    private final Map<String, Hero> ownersById;

    public HeroRegistry() {
        this(Map.of());
    }

    private HeroRegistry(Map<HeroType, Hero> heroes) {
        this.heroes = Map.copyOf(heroes);
        var abilities = new HashMap<String, Abilities>();
        var owners = new HashMap<String, Hero>();
        for (Hero hero : heroes.values())
            for (Abilities ability : hero.abilities()) {
                String id = Objects.requireNonNull(ability.id(), "ability.id");
                if (id.isBlank()) throw new IllegalArgumentException("Blank ability ID");
                if (abilities.putIfAbsent(id, ability) != null)
                    throw new IllegalArgumentException("Duplicate ability ID: " + id);
                owners.put(id, hero);
            }
        abilitiesById = Map.copyOf(abilities);
        ownersById = Map.copyOf(owners);
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
        return new HeroRegistry(next);
    }

    /** Source strategy for a copied spell's targeting and candidate requests. */
    public Hero ownerOf(Abilities ability) {
        Objects.requireNonNull(ability, "ability");
        if (abilitiesById.get(ability.id()) != ability)
            throw new IllegalArgumentException("Unregistered ability: " + ability.id());
        return ownersById.get(ability.id());
    }

    /** Resolves persistence IDs to the exact registered enum instances. */
    public Abilities abilityById(String id) {
        Abilities ability = findAbility(id);
        if (ability == null) throw new IllegalArgumentException("Unregistered ability ID: " + id);
        return ability;
    }

    /** Non-ability history events have no registered ability and return null. */
    public Abilities findAbility(String id) {
        return abilitiesById.get(Objects.requireNonNull(id, "id"));
    }

    /** Shared metadata from the same registration factory as running games. */
    public static HeroRegistry standard() {
        return StandardMetadata.INSTANCE;
    }

    private static final class StandardMetadata {
        private static final HeroRegistry INSTANCE = fromRules(HeroRules.prototype());
    }

    public static HeroRegistry fromRules(HeroRules rules) {
        Objects.requireNonNull(rules, "rules");
        HeroRegistry registry = new HeroRegistry();
        for (HeroDefinition<?> definition : rules.definitions())
            registry = registry.register(definition.create(rules));
        return registry;
    }
}

package game.heroes;

import model.Figure;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/** Immutable identity, initial bodies, typed parameters, wire codec and strategy factory. */
public final class HeroDefinition<R> {
    private final HeroType type;
    private final String displayName;
    private final Set<Class<? extends Figure>> bodies;
    private final Class<R> rulesType;
    private final R defaults;
    private final HeroRulesCodec<R> codec;
    private final BiFunction<R, HeroRules, Hero> factory;

    public HeroDefinition(HeroType type, String displayName, Set<Class<? extends Figure>> bodies,
                          Class<R> rulesType, R defaults, HeroRulesCodec<R> codec, Function<R, Hero> factory) {
        this(type, displayName, bodies, rulesType, defaults, codec,
                adapt(Objects.requireNonNull(factory, "factory")));
    }

    /** The whole configuration is supplied only for bridges to existing multi-hero dispatch. */
    public HeroDefinition(HeroType type, String displayName, Set<Class<? extends Figure>> bodies,
                          Class<R> rulesType, R defaults, HeroRulesCodec<R> codec,
                          BiFunction<R, HeroRules, Hero> factory) {
        this.type = Objects.requireNonNull(type, "type");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        if (displayName.isBlank()) throw new IllegalArgumentException("Blank hero name");
        this.bodies = Set.copyOf(bodies);
        this.rulesType = Objects.requireNonNull(rulesType, "rulesType");
        this.defaults = checkedRules(defaults);
        if (codec == null && rulesType != NoHeroRules.class)
            throw new IllegalArgumentException("Configurable heroes require a rules codec");
        if (codec != null && (codec.section() == null || !codec.section().matches("[A-Za-z][A-Za-z0-9_-]*")
                || codec.sinceVersion() < 1)) throw new IllegalArgumentException("Invalid rules wire schema");
        this.codec = codec;
        this.factory = Objects.requireNonNull(factory, "factory");
    }

    private static <R> BiFunction<R, HeroRules, Hero> adapt(Function<R, Hero> factory) {
        return (parameters, configuration) -> factory.apply(parameters);
    }

    public HeroType type() { return type; }
    public String displayName() { return displayName; }
    public Set<Class<? extends Figure>> supportedBodies() { return bodies; }
    public boolean supports(Class<? extends Figure> body) { return body != null && bodies.contains(body); }
    public Class<R> rulesType() { return rulesType; }
    public R defaultRules() { return defaults; }
    public Optional<HeroRulesCodec<R>> rulesCodec() { return Optional.ofNullable(codec); }

    R checkedRules(Object value) {
        Objects.requireNonNull(value, "rules");
        if (!rulesType.isInstance(value)) throw new IllegalArgumentException("Wrong rules type for " + type);
        return rulesType.cast(value);
    }

    public Hero create(HeroRules configuration) {
        Hero hero = Objects.requireNonNull(factory.apply(configuration.get(this), configuration), "hero");
        if (hero.type() != type) throw new IllegalArgumentException("Factory returned a different hero: " + type);
        if (hero.definition() != this) throw new IllegalArgumentException("Factory returned a different definition: " + type);
        return hero;
    }
}

package game.heroes;

import game.mechanics.MechanicDefinition;
import game.state.PieceState;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import model.Figure;
import model.Position;

/** Immutable identity, initial bodies, typed parameters, wire codec and strategy factory. */
public final class HeroDefinition<R> {
    private final List<MechanicDefinition<?>> mechanics;
    private final DraftProfile draftProfile;
    private final HeroStatePolicy<R> statePolicy;
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
        this(type, displayName, bodies, rulesType, defaults, codec, factory, List.of());
    }

    public HeroDefinition(HeroType type, String displayName, Set<Class<? extends Figure>> bodies,
                          Class<R> rulesType, R defaults, HeroRulesCodec<R> codec, Function<R, Hero> factory,
                          List<MechanicDefinition<?>> mechanics) {
        this(type, displayName, bodies, rulesType, defaults, codec, adapt(factory), mechanics);
    }

    public HeroDefinition(HeroType type, String displayName, Set<Class<? extends Figure>> bodies,
                          Class<R> rulesType, R defaults, HeroRulesCodec<R> codec, BiFunction<R, HeroRules, Hero> factory,
                          List<MechanicDefinition<?>> mechanics) {
        this(type, displayName, bodies, rulesType, defaults, codec, factory, mechanics, DraftProfile.EXCLUDED, HeroStatePolicy.basic());
    }

    private HeroDefinition(HeroType type, String displayName, Set<Class<? extends Figure>> bodies,
                           Class<R> rulesType, R defaults, HeroRulesCodec<R> codec, BiFunction<R, HeroRules, Hero> factory,
                           List<MechanicDefinition<?>> mechanics, DraftProfile draftProfile, HeroStatePolicy<R> statePolicy) {
        this.statePolicy = Objects.requireNonNull(statePolicy, "statePolicy");
        this.draftProfile = Objects.requireNonNull(draftProfile, "draftProfile");
        this.mechanics = List.copyOf(mechanics);
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

    public List<MechanicDefinition<?>> mechanics() { return mechanics; }

    public DraftProfile draftProfile() { return draftProfile; }

    public HeroDefinition<R> withDraftProfile(DraftProfile profile) {
        return new HeroDefinition<>(type, displayName, bodies, rulesType, defaults, codec, factory, mechanics, profile, statePolicy);
    }

    public HeroStatePolicy<R> statePolicy() { return statePolicy; }

    public HeroDefinition<R> withStatePolicy(HeroStatePolicy<R> policy) {
        return new HeroDefinition<>(type, displayName, bodies, rulesType, defaults, codec, factory, mechanics,
                draftProfile, policy);
    }

    PieceState initializeState(PieceState base, PieceCreationContext origin, HeroRules configuration) {
        return Objects.requireNonNull(statePolicy.initialize(base, origin, configuration.get(this)), "initial state");
    }

    void validateState(PieceState piece, HeroRules configuration) {
        statePolicy.validate(piece, configuration.get(this));
    }

    void validateOrigin(PieceState piece, Position origin, HeroRules configuration) {
        statePolicy.validateOrigin(piece, origin, configuration.get(this));
    }

    void validateCreation(PieceState piece, PieceCreationContext origin, HeroRules configuration) {
        statePolicy.validateCreation(piece, origin, configuration.get(this));
    }
    RevivalPlan revival(PieceState corpse, HeroActionContext context, HeroRules configuration) {
        return statePolicy.revival(corpse, context, configuration, configuration.get(this));
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

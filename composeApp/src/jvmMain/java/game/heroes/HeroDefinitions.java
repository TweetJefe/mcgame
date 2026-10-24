package game.heroes;

import game.heroes.pudge.PudgeDefinition;
import game.heroes.lumeshard.LumeshardDefinition;
import game.heroes.bountycrow.BountyCrowDefinition;
import game.heroes.bolotnik.BolotnikDefinition;
import game.heroes.kikimora.KikimoraDefinition;
import game.heroes.chronomlin.ChronomlinDefinition;
import game.heroes.zeus.ZeusDefinition;
import game.heroes.moleminer.MoleminerDefinition;
import game.heroes.clownganger.ClowngangerDefinition;
import game.heroes.ares.AresDefinition;
import game.heroes.faun.FaunDefinition;
import game.heroes.ginseng.GinsengDefinition;
import game.heroes.izba.IzbaDefinition;
import game.heroes.plain.NoneDefinition;
import game.heroes.clownganger.IllusionDefinition;
import game.heroes.izba.BabaYagaDefinition;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Single explicit registration list; does not instantiate strategies. */
public final class HeroDefinitions {
    private HeroDefinitions() {}

    private static final class BuiltIns {
        private static final List<HeroDefinition<?>> ALL = List.of(
                PudgeDefinition.INSTANCE,
                LumeshardDefinition.INSTANCE,
                BountyCrowDefinition.INSTANCE,
                BolotnikDefinition.INSTANCE,
                KikimoraDefinition.INSTANCE,
                ChronomlinDefinition.INSTANCE,
                ZeusDefinition.INSTANCE,
                MoleminerDefinition.INSTANCE,
                ClowngangerDefinition.INSTANCE,
                AresDefinition.INSTANCE,
                FaunDefinition.INSTANCE,
                GinsengDefinition.INSTANCE,
                IzbaDefinition.INSTANCE,
                NoneDefinition.INSTANCE,
                IllusionDefinition.INSTANCE,
                BabaYagaDefinition.INSTANCE);
        private static final Map<HeroType, HeroDefinition<?>> BY_TYPE = ALL.stream()
                .collect(Collectors.toUnmodifiableMap(HeroDefinition::type, Function.identity()));
    }

    public static List<HeroDefinition<?>> all() { return BuiltIns.ALL; }

    public static HeroDefinition<?> get(HeroType type) {
        HeroDefinition<?> definition = BuiltIns.BY_TYPE.get(Objects.requireNonNull(type, "type"));
        if (definition == null) throw new IllegalArgumentException("Unregistered hero definition: " + type);
        return definition;
    }
}

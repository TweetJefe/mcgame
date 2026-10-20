package game.heroes;

import java.util.List;

/** Public ability identity and presentation, implemented by each hero's own enum. */
public interface Abilities {
    /** Creates an immutable enum catalog in declaration order; cache it in the implementing enum. */
    static <E extends Enum<E> & Abilities> List<Abilities> getAbilities(E[] values) {
        return List.of(values);
    }

    /** Stable persistence ID, independent of the enum's class, constant name or display text. */
    String id();

    String getDisplayName();

    /** Targeting follows a copied ability as well as its native owner. */
    default HeroTargeting targeting() {
        return HeroTargeting.square(HeroTargeting.Permission.ILLUSION);
    }
}

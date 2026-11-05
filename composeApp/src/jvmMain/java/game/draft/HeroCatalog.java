package game.draft;

import game.heroes.HeroDefinitions;
import game.heroes.HeroType;
import model.FigureColor;
import model.Position;
import java.util.List;

/** Совместимый фасад; новый драфт использует собственный DraftCatalog. */
public final class HeroCatalog {
    private HeroCatalog() {}
    public static List<HeroType> heroes() { return DraftCatalog.current(HeroDefinitions.all()).heroes(); }
    public static List<HeroType> heroes(int version) { return catalog(version).heroes(); }
    public static DraftCatalog catalog(int version) {
        return version == DraftRules.CURRENT_CATALOG ? DraftCatalog.current(HeroDefinitions.all())
                : DraftCatalog.restore(LegacyHeroCatalog.heroes(version), HeroDefinitions.all());
    }
    public static List<Position> startingSquares(HeroType hero, FigureColor side) {
        return DraftCatalog.current(HeroDefinitions.all()).startingSquares(hero, side);
    }
    public static String summary(HeroType hero) {
        String old = LegacyHeroCatalog.summary(hero);
        return old == null ? HeroDefinitions.get(hero).displayName() : old;
    }
}

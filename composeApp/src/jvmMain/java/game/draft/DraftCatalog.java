package game.draft;

import game.heroes.HeroDefinition;
import game.heroes.HeroType;
import model.Field;
import model.FigureColor;
import model.Position;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Зафиксированный состав и порядок; описания привязываются из реестра партии. */
public final class DraftCatalog {
    private final List<HeroDefinition<?>> definitions;
    private final Map<HeroType, HeroDefinition<?>> byType;
    private final List<HeroType> heroes;
    private final Map<FigureColor, Map<HeroType, List<Position>>> squares;

    public DraftCatalog(List<HeroDefinition<?>> definitions) {
        this.definitions = List.copyOf(definitions);
        var index = new HashMap<HeroType, HeroDefinition<?>>();
        for (var definition : definitions)
            if (index.putIfAbsent(definition.type(), definition) != null)
                throw new IllegalArgumentException("Duplicate draft hero: " + definition.type());
        byType = Map.copyOf(index);
        heroes = definitions.stream().map(HeroDefinition::type).toList();
        var board = new Field(8);
        var sides = new HashMap<FigureColor, Map<HeroType, List<Position>>>();
        for (FigureColor side : List.of(FigureColor.WHITE, FigureColor.BLACK)) {
            var placements = new HashMap<HeroType, List<Position>>();
            for (var definition : definitions) {
                var cells = new ArrayList<Position>();
                for (int row = 0; row < 8; row++) for (int column = 0; column < 8; column++) {
                    var square = new Position(row, column);
                    var figure = board.getFigureByPosition(square);
                    if (figure.getColor() == side && definition.supports(figure.getClass())) cells.add(square);
                }
                placements.put(definition.type(), List.copyOf(cells));
            }
            sides.put(side, Map.copyOf(placements));
        }
        squares = Map.copyOf(sides);
    }
    public static DraftCatalog current(List<HeroDefinition<?>> definitions) {
        return new DraftCatalog(definitions.stream().filter(d -> d.draftProfile().pickable())
                .sorted(Comparator.<HeroDefinition<?>>comparingInt(d -> d.draftProfile().order())
                        .thenComparing(d -> d.type().name())).toList());
    }
    public static DraftCatalog restore(List<HeroType> ids, List<HeroDefinition<?>> definitions) {
        var index = new HashMap<HeroType, HeroDefinition<?>>();
        for (var definition : definitions) index.put(definition.type(), definition);
        var selected = new ArrayList<HeroDefinition<?>>();
        for (HeroType id : ids) {
            HeroDefinition<?> definition = index.get(id);
            if (definition == null) throw new IllegalArgumentException("Unregistered draft hero: " + id);
            selected.add(definition);
        }
        return new DraftCatalog(selected);
    }
    public List<HeroDefinition<?>> definitions() { return definitions; }
    public List<HeroType> heroes() { return heroes; }
    public HeroDefinition<?> definition(HeroType hero) {
        return Objects.requireNonNull(byType.get(hero), "Hero outside draft catalog: " + hero);
    }
    public List<Position> startingSquares(HeroType hero, FigureColor side) {
        if (hero == null || side != FigureColor.WHITE && side != FigureColor.BLACK) return List.of();
        return squares.get(side).getOrDefault(hero, List.of());
    }
    @Override public boolean equals(Object other) {
        return other instanceof DraftCatalog catalog && definitions.equals(catalog.definitions);
    }
    @Override public int hashCode() { return definitions.hashCode(); }
}

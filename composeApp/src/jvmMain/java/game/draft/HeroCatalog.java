package game.draft;

import game.heroes.HeroType;
import model.FigureColor;
import model.Position;

import java.util.List;
import java.util.ArrayList;

/**
 * The draft uses the same hero identities as the engine and only compatible initial squares.
 */
public final class HeroCatalog {
    private HeroCatalog() {
    }

    public static List<HeroType> heroes() {
        return heroes(10);
    }

    public static List<HeroType> heroes(int version) {
        if(version==10){var heroes=new ArrayList<>(heroes(9));heroes.add(HeroType.KIKIMORA);return List.copyOf(heroes);}
        if(version==9){var heroes=new ArrayList<>(heroes(8));heroes.add(HeroType.CHRONOMLIN);return List.copyOf(heroes);}
        if(version==8){var heroes=new ArrayList<>(heroes(7));heroes.add(HeroType.ZEUS);return List.copyOf(heroes);}
        if(version==7){var heroes=new ArrayList<>(heroes(6));heroes.add(HeroType.MOLEMINER);return List.copyOf(heroes);}
        if(version==6){var heroes=new ArrayList<>(heroes(5));heroes.add(HeroType.CLOWNGANGER);return List.copyOf(heroes);}
        if (version == 5)
            return List.of(HeroType.PUDGE, HeroType.LUMESHARD, HeroType.BOUNTY_CROW, HeroType.BOLOTNIK, HeroType.IZBA, HeroType.GINSENG, HeroType.FAUN, HeroType.ARES);
        if (version == 4)
            return List.of(HeroType.PUDGE, HeroType.LUMESHARD, HeroType.BOUNTY_CROW, HeroType.BOLOTNIK, HeroType.IZBA, HeroType.GINSENG, HeroType.FAUN);
        if (version == 3)
            return List.of(HeroType.PUDGE, HeroType.LUMESHARD, HeroType.BOUNTY_CROW, HeroType.BOLOTNIK, HeroType.IZBA, HeroType.GINSENG);
        if (version == 2)
            return List.of(HeroType.PUDGE, HeroType.LUMESHARD, HeroType.BOUNTY_CROW, HeroType.BOLOTNIK, HeroType.IZBA);
        if (version != 1) throw new IllegalArgumentException("Unknown hero catalog version");
        return List.of(HeroType.PUDGE, HeroType.LUMESHARD, HeroType.BOUNTY_CROW, HeroType.BOLOTNIK);
    }

    public static List<Position> startingSquares(HeroType hero, FigureColor side) {
        if (hero == null || !heroes().contains(hero) || (side != FigureColor.WHITE && side != FigureColor.BLACK))
            return List.of();
        int row = side == FigureColor.WHITE ? 0 : 7;
        return switch (hero) {
            case ZEUS, PUDGE, IZBA -> List.of(new Position(row, 0), new Position(row, 7));
            case KIKIMORA, LUMESHARD, GINSENG, ARES -> List.of(new Position(row, 2), new Position(row, 5));
            case CLOWNGANGER, BOLOTNIK, FAUN -> List.of(new Position(row, 1), new Position(row, 6));
            case CHRONOMLIN, MOLEMINER, BOUNTY_CROW -> {
                var squares = new ArrayList<Position>();
                for (int col = 0; col < 8; col++) squares.add(new Position(side == FigureColor.WHITE ? 1 : 6, col));
                yield List.copyOf(squares);
            }
            case ILLUSION, NONE, BABA_YAGA -> List.of();
        };
    }

    public static String summary(HeroType hero) {
        return switch (hero) {
            case PUDGE -> "Ладья • Хук: 3 клетки, КД 2. Ульта: проглотить союзника, ответить на взятие.";
            case LUMESHARD -> "Слон • Лазер: ослепление, радиус 3, КД 3. Ульта: телепорт на исходный цвет.";
            case BOUNTY_CROW -> "Пешка • Награда +1,5, КД 3. Пассивка +0,5 за взятие. Ульта: украсть до 2 золота.";
            case BOLOTNIK -> "Конь • Грязь: радиус 4, КД 4. Ульта: скрыться в болоте и съесть вошедшего врага.";
            case IZBA -> "Ладья • При гибели призывает Бабу-ягу рядом. Ульта: призыв на свои первые две линии.";
            case BABA_YAGA -> "Пешка • Через 3 собственных хода становится Избой. Только призыв, без пика.";
            case ARES ->
                    "Слон • Копьё: 3 прямо / 2 по диагонали, столкновение оглушает, КД 3. Пассивка: месть за 1 взятие.";
            case FAUN -> "Конь • Туча 3×3: дальность 4, КД 3. Ульта: невидимость на 2 хода.";
            case GINSENG ->
                    "Слон • Сдвиг: радиус 3, соседняя клетка, КД 3. Ульта: копия последнего скилла героя.";
            case CLOWNGANGER -> "Конь • Коробка: радиус 3, КД 3, защита на 1 ход. Ульта: двойник на 2 хода за 1 взятие.";
            case ILLUSION -> "Пустышка: блокирует проход, не берёт фигуры и не имеет способностей.";
            case MOLEMINER -> "Пешка • Мина под собой: КД 3, скрыта и бессрочна. Ульта: прыжок 3 и взрыв 2×2 за 2 взятия.";
            case ZEUS -> "Ладья • Цепь до 3 врагов: радиус 3, КД 1; третий заряд убивает. Ульта: случайный заряженный враг за 2 взятия.";
            case CHRONOMLIN -> "Пешка • Притяжение без дальности, КД 3. Ульта за 1 взятие: пешка → слон → конь → ладья.";
            case KIKIMORA -> "Слон • Пряжа: радиус 2, стан 1 или 2 хода, КД 3. Ульта за 2 взятия: неуязвимый подвижный горшок рядом.";
            case NONE -> "Обычная шахматная фигура";
        };
    }
}

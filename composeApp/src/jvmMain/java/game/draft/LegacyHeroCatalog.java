package game.draft;

import game.heroes.HeroType;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/** Замороженные каталоги 1–10 и прежние описания для публичного фасада совместимости. */
final class LegacyHeroCatalog {
    private LegacyHeroCatalog() {
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

    private static final Map<HeroType, String> SUMMARIES = Map.ofEntries(
            Map.entry(HeroType.PUDGE, "Ладья • Хук: 3 клетки, КД 2. Ульта: проглотить союзника, ответить на взятие."),
            Map.entry(HeroType.LUMESHARD, "Слон • Лазер: ослепление, радиус 3, КД 3. Ульта: телепорт на исходный цвет."),
            Map.entry(HeroType.BOUNTY_CROW, "Пешка • Награда +1,5, КД 3. Пассивка +0,5 за взятие. Ульта: украсть до 2 золота."),
            Map.entry(HeroType.BOLOTNIK, "Конь • Грязь: радиус 4, КД 4. Ульта: скрыться в болоте и съесть вошедшего врага."),
            Map.entry(HeroType.IZBA, "Ладья • При гибели призывает Бабу-ягу рядом. Ульта: призыв на свои первые две линии."),
            Map.entry(HeroType.BABA_YAGA, "Пешка • Через 3 собственных хода становится Избой. Только призыв, без пика."),
            Map.entry(HeroType.ARES, "Слон • Копьё: 3 прямо / 2 по диагонали, столкновение оглушает, КД 3. Пассивка: месть за 1 взятие."),
            Map.entry(HeroType.FAUN, "Конь • Туча 3×3: дальность 4, КД 3. Ульта: невидимость на 2 хода."),
            Map.entry(HeroType.GINSENG, "Слон • Сдвиг: радиус 3, соседняя клетка, КД 3. Ульта: копия последнего скилла героя."),
            Map.entry(HeroType.CLOWNGANGER, "Конь • Коробка: радиус 3, КД 3, защита на 1 ход. Ульта: двойник на 2 хода за 1 взятие."),
            Map.entry(HeroType.ILLUSION, "Пустышка: блокирует проход, не берёт фигуры и не имеет способностей."),
            Map.entry(HeroType.MOLEMINER, "Пешка • Мина под собой: КД 3, скрыта и бессрочна. Ульта: прыжок 3 и взрыв 2×2 за 2 взятия."),
            Map.entry(HeroType.ZEUS, "Ладья • Цепь до 3 врагов: радиус 3, КД 1; третий заряд убивает. Ульта: случайный заряженный враг за 2 взятия."),
            Map.entry(HeroType.CHRONOMLIN, "Пешка • Притяжение без дальности, КД 3. Ульта за 1 взятие: пешка → слон → конь → ладья."),
            Map.entry(HeroType.KIKIMORA, "Слон • Пряжа: радиус 2, стан 1 или 2 хода, КД 3. Ульта за 2 взятия: неуязвимый подвижный горшок рядом."),
            Map.entry(HeroType.NONE, "Обычная шахматная фигура"));

    static String summary(HeroType hero) { return SUMMARIES.get(hero); }
}

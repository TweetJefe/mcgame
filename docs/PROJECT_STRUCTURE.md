# Структура проекта

Структура обновлена 16 октября 2026 года при добавлении Ginseng. Java-движок находится в `composeApp/src/jvmMain/java`, Compose-интерфейс — в `composeApp/src/jvmMain/kotlin/org/chess`.

## Пакеты движка

| Пакет | Назначение и классы |
| --- | --- |
| `game.core` | Управление партией: `Game`, `Player`; внутренние `ActionEngine` и `ChessClock`; проверка полного состояния `SnapshotValidator` |
| `game.draft` | Подготовка матча: `Draft`, `DraftRules`, `DraftPhase`, `DraftActionResult`, `HeroCatalog`, `HeroPick`, `DraftEvent`, `DraftSnapshot`, `DraftValidator` |
| `game.summoning` | Создание и превращение фигур: `SummonState`, `SummonRecord`, `SummonResolver`, `SummonValidator`; случайный выбор без изменения живого состояния в preview |
| `game.state` | Неизменяемые данные: `GameSnapshot`, `GamePosition`, `PieceState`, `TurnState`, `GameMode`, `GameOutcome`, `Termination`, `CastlingRights`, `ClockSnapshot`, `ItemSlot`, `SquareColor` |
| `game.moves` | Запросы/результаты перемещений: `AvailableMoves`, `MoveResult`, `MoveKind`, `Promotion`; описание подготовленного взятия `Capture` |
| `game.history` | История: `MadeMove`, `GameEvent` |
| `game.heroes` | Общие типы `Ability`, `HeroType`, конфигурация `HeroRules`; выбор реализации `HeroAbilities`; неизменяемое предложение изменений `HeroEffect` |
| `game.heroes.pudge` | Параметры `PudgeRules` и реализация `PudgeAbilities`: хук, проглатывание, заряд и ответные взятия |
| `game.heroes.lumeshard` | `LumeshardRules` и `LumeshardAbilities`: лазер, ослепление, телепорт и заряд |
| `game.heroes.bountycrow` | `BountyCrowRules`, `BountyCrowAbilities`: метка, заряд и кража |
| `game.heroes.bolotnik` | `BolotnikRules`, `BolotnikAbilities`: грязь, укрытие и заряд |
| `game.terrain` | `MudCell`, `TerrainState`, `TerrainResolver`, `TerrainValidator`: клетки болота, срабатывания и проверка скрытых героев |
| `game.economy` | `GoldRules`, `EconomyState`, `BountyMark`, `GoldChange`, `GoldEngine`, `EconomyValidator`: цены, кошельки, метки и финансовая история |
| `rules.moba` | `AttackRules`, `TerrainGeometry`: ослепление и вражеское болото в обычных атаках/перемещениях |
| `game.persistence` | `GameSaveFile`: XML, ограничения чтения, подготовка файла и замена назначения |
| `game.scenarios` | `TrainingScenario`, `PudgeScenario`, `LumeshardScenario`, `BountyCrowScenario`, `BolotnikScenario`: учебные позиции и подсказки |
| `model`, `model.figures` | Геометрия доски, координаты и обычные шахматные фигуры; расположение сохранено |
| `rules.standard` | Легальность шахматных перемещений, структура позиции, материал |
| `rules.field_filling` | Начальные расстановки; активна 8×8, остальные размеры остаются заглушками |

## Как проходит действие

`ChessBoard` вызывает публичную команду `game.core.Game`. `Game` проверяет очередь и лимиты действий. Внутренний `ActionEngine` готовит результат на отдельных копиях данных.

Для способности `HeroAbilities` выбирает реализацию по `HeroType`. `PudgeAbilities` рассчитывает геометрию, стоимость и изменения состояний, возвращая `HeroEffect`. При обычном взятии тот же слой определяет заряд и пассивную реакцию героя.

`HeroEffect` содержит неизменяемые изменения фигур и список взятий. Он не меняет живую `Game`, очки, часы или историю. `ActionEngine` применяет предложение к временной доске, обновляет специальные права и проверяет короля. Только успешный результат публикуется через `Game` вместе с очками, бюджетом и событиями.

## Доступ и границы

- `Game` остаётся точкой изменения партии. `Player.recordMove` и `recordCapture` доступны только внутри `core`; прямой внешний записи очков и истории нет.
- `ActionEngine` и `ChessClock` не стали публичными при переносе.
- `PieceState` предоставляет публичные операции, возвращающие новый неизменяемый объект: размещение, скрытие, изменение заряда. Они нужны ядру и реализациям героев, но не изменяют исходную фигуру.
- `SnapshotValidator.validate` публичен и не изменяет игру. Проверка остаётся в ядре: восстановление из памяти не зависит от XML. `GameSaveFile` обращается к ядру, а ядро не вызывает файловый слой.
- `HeroAbilities` содержит текущую диспетчеризацию; параметры героев объединены в `HeroRules`, содержащий независимые `PudgeRules`, `LumeshardRules`, `BountyCrowRules`, `BolotnikRules` и `GoldRules`, а также `IzbaRules` и `GinsengRules`.

## Куда добавлять изменения

Нового героя размещать в `game.heroes.<имя>` рядом с его параметрами. Подключить его в `HeroType`, `Ability` и `HeroAbilities`, затем согласовать конфигурацию, состояние, отображение и формат сохранений. Бюджеты хода, часы и финальная проверка короля остаются ответственностью ядра.

Новый способ сохранения относится к `persistence`; учебная расстановка — к `scenarios`; данные партии — к `state`. Поле и шахматная геометрия остаются в `model`/`rules`.

Перенос пакетов не заменяет правила движения, шаха и бюджета и не вводит новых героев или предметов.

## Тесты и совместимость

Java-тесты повторяют основные границы: `game.core`, `game.draft`, `game.persistence`, `game.scenarios`, `game.heroes.<имя>`. Общий помощник `Positions` остаётся в тестовом `game.core`. Kotlin-тесты интерфейса остаются в `org.chess`.

В `composeApp/src/jvmTest/resources/saves/v1` находятся четыре XML-файла, записанные прежним движком до переноса. `LegacySaveCompatibilityTest` проверяет точное сохранение данных при повторной записи и продолжение партии. Формат XML версии 1 использует значения и ID, поэтому перенос Java-пакетов не требует миграции этих файлов.

Проверка: `:composeApp:jvmTest`, всего **370 тест**. Git не изменяется по указанию пользователя.

## Подготовка матча

Пакет `game.draft` содержит независимую от интерфейса модель банов, пиков, размещения и исходных составов. `DraftScreen.kt` отображает её в Compose; `ChessApp` вызывает `Game.mobaFromDraft` только после завершения выбора. `DraftValidator` проверяет связь сохраняемой партии с первоначальным составом. Правила и API: [DRAFT.md](DRAFT.md).

## Призыв Избы

`game.heroes.izba` содержит параметры и ульту. Создание новых ID, происхождение, срок жизни и PRNG вынесены в `game.summoning`, поскольку они затрагивают подготовку действий, реакции, конец хода, драфт, экономику и сохранение. Данные передаются через `ActionEngine.Context` и `GameSnapshot`; XML 6 хранит значения и ID. Новых героев, которые создают фигуры, следует подключать к этому слою, сохраняя явное различие взятия и замены. Подробнее: [IZBA.md](IZBA.md).

## Ginseng и копии способностей

`game.heroes.ginseng` содержит `GinsengRules`, `GinsengAbilities`, `BorrowedEffects`, `BorrowedAbility`, `SpellState`, `SpellValidator`. Сдвиг и копирование отделены от общей обработки реакции, болота, золота и призыва. `BorrowedEffects` переиспользует исходные способности через представление исполнителя, сохраняя реальную идентичность слона и его собственные ресурсы.

`game.heroes.AbilityRequest` передаёт исполнителя, способность, цель, отдельную клетку назначения и признак использования копии. `GameSnapshot.spells` и `ActionEngine.Context.spells` хранят неизменяемую память успешных активаций и копий. Источник истины при восстановлении — метаданные `GameEvent`; XML 7 сохраняет их и `GinsengRules`. Подсветка, команды и поиск доступного составного хода проверяют оба канала: собственные способности и сохранённую копию. Подробнее: [GINSENG.md](GINSENG.md).

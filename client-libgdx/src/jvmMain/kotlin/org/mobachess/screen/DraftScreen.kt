package org.mobachess.screen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Scaling
import game.draft.MatchDraft
import game.heroes.HeroType
import model.Field
import model.FigureColor
import model.Position
import org.mobachess.assets.GameAssets
import org.mobachess.presentation.HeroCatalog
import org.mobachess.session.DraftSession

/** Экран обеих сторон локального драфта. Все допустимые действия сообщает сессия. */
class DraftScreen(assets: GameAssets, navigation: ScreenNavigation, private val catalog: HeroCatalog,
                  val session: DraftSession) : SimpleScreen(assets, navigation) {
    private val title = label("")
    private val clocks = label("", true)
    private val content = Table()
    private val rosters = label("", true)
    private val footer = Table()
    private val order = Table()
    private var selected: HeroType? = null
    private var side = FigureColor.WHITE
    private var signature = ""
    private var openingMatch = false
    private var notice = ""
    private val initialBoard = Field(8)

    init {
        val header = Table()
        header.add(button("В меню", "back", navigation::menu)).width(130f).height(44f)
        header.add(title.apply { color = gold; setFontScale(1.15f) }).expandX()
        header.add(label("ЛОКАЛЬНО • ОБЕ СТОРОНЫ", true).apply { color = muted })
        root.add(header).growX().padBottom(12f).row()
        root.add(clocks).left().padBottom(12f).row()
        root.add(order).growX().padBottom(12f).row()
        root.add(rosters.apply { setWrap(true) }).growX().height(62f).padBottom(8f).row()
        root.add(content).grow().top().row()
        root.add(footer).growX().height(76f)
        refresh(session.snapshot())
    }

    private fun sideName(value: FigureColor) = if (value == FigureColor.WHITE) "Белые" else "Чёрные"
    private fun seconds(nanos: Long) = ((nanos + 999_999_999L) / 1_000_000_000L).coerceAtLeast(0)
    private fun rosterText(s: MatchDraft.Snapshot, value: FigureColor): String =
        "${sideName(value)}: " + s.rosters().getValue(value).joinToString(", ", transform = catalog::name).ifBlank { "—" }

    override fun update(delta: Float) {
        val s = session.tick()
        if (s.phase() == MatchDraft.Phase.COMPLETE) {
            if (!openingMatch) { openingMatch = true; navigation.match(session.startMatch()) }
            return
        }
        refresh(s)
    }

    private fun refresh(s: MatchDraft.Snapshot) {
        val action = s.expected()
        title.setText(if (s.phase() == MatchDraft.Phase.PREPARATION) "РАССТАНОВКА ГЕРОЕВ" else
            "${sideName(action.side()).uppercase()} • ${if (action.kind() == MatchDraft.Kind.BAN) "БАН" else "ПИК"} • ${s.step() + 1}/16")
        clocks.setText(if (s.phase() == MatchDraft.Phase.PREPARATION)
            "Осталось ${seconds(s.preparationNanos())} с • выберите героя и подсвеченную клетку. Остальных расставим автоматически."
        else "На действие: ${seconds(s.actionNanos())} с   |   Запас белых: ${seconds(s.reserveNanos().getValue(FigureColor.WHITE))} с   |   Запас чёрных: ${seconds(s.reserveNanos().getValue(FigureColor.BLACK))} с")
        rosters.setText(rosterText(s, FigureColor.WHITE) + "\n" + rosterText(s, FigureColor.BLACK))
        val key = "${s.phase()}:${s.step()}:${s.assignments()}:$side:$selected:$notice"
        if (signature == key) return
        signature = key
        order.clearChildren()
        MatchDraft.order().forEachIndexed { index, step ->
            order.add(label("${if (step.side() == FigureColor.WHITE) 1 else 2} ${if (step.kind() == MatchDraft.Kind.BAN) "БАН" else "ПИК"}", true).apply {
                setFontScale(.82f)
                color = when { index < s.step() -> muted; index == s.step() -> gold; else -> Color.valueOf("57636D") }
            }).expandX().pad(5f)
        }
        content.clearChildren(); footer.clearChildren()
        if (s.phase() == MatchDraft.Phase.PREPARATION) preparation(s) else selection(s)
    }

    private fun invalidate() { signature = "" }

    private fun selection(s: MatchDraft.Snapshot) {
        val available = session.available().toSet()
        content.add(HeroGrid(assets, catalog, skin, { hero ->
            val owner = s.rosters().entries.firstOrNull { hero in it.value }?.key
            HeroCardState(hero in available, selected == hero, when {
                hero in s.bans() -> "БАН"
                owner != null -> sideName(owner)
                hero !in available -> "Нужен для состава"
                else -> " "
            })
        }) { hero -> selected = hero; notice = ""; invalidate() }).growX().top()
        val hero = selected?.takeIf { it in available }
        footer.add(label(hero?.let(catalog::name) ?: "Состав каждого: 2 пешки • 1 конь • 1 слон • 1 ладья", true)
            .apply { color = gold }).expandX().left()
        footer.add(button(if (s.expected().kind() == MatchDraft.Kind.BAN) "Забанить" else "Выбрать", "confirm") {
            selected?.let { choice ->
                val result = session.choose(choice)
                notice = if (result == MatchDraft.Result.SUCCESS) "" else "Выбор уже изменился — выберите доступного героя"
                selected = null; invalidate()
            }
        }.apply { isDisabled = hero == null }).width(230f).height(48f)
    }

    private fun preparation(s: MatchDraft.Snapshot) {
        // Смена стороны доступна независимо: подготовка у обоих игроков идёт одновременно.
        if (selected?.let { it !in s.rosters().getValue(side) } == true) selected = null
        val targets = selected?.let { session.placementTargets(side, it).toSet() } ?: emptySet()
        val board = Table()
        for (row in 7 downTo 0) {
            board.add(label("${row + 1}", true)).width(22f)
            for (col in 0..7) {
                val square = Position(row, col)
                val hero = s.assignments()[square]
                val figure = initialBoard.getFigureByPosition(square)
                val region = if (hero != null) assets.heroSprite(hero) else assets.regularSprite(figure.javaClass.simpleName.lowercase(), figure.color)
                val cell = Button(skin, "card").apply {
                    name = "cell-$row-$col"
                    color = when {
                        square in targets -> Color(.55f, .85f, .55f, 1f)
                        (row + col) % 2 == 0 -> Color(.46f, .52f, .57f, 1f)
                        else -> Color(.85f, .82f, .72f, 1f)
                    }
                }
                if (region != null) cell.add(Image(TextureRegionDrawable(region)).apply { setScaling(Scaling.fit) }).size(48f)
                cell.addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent?, actor: Actor?) {
                        val choice = selected ?: return
                        if (square !in targets) return
                        val result = session.place(side, choice, square)
                        if (result == MatchDraft.Result.SUCCESS) { notice = "${catalog.name(choice)}: ${('a' + col)}${row + 1}"; selected = null }
                        else notice = "Расстановка уже завершилась"
                        invalidate()
                    }
                })
                board.add(cell).size(58f).pad(1f)
            }
            board.row()
        }
        board.add()
        for (col in 0..7) board.add(label(('a' + col).toString(), true)).height(24f)
        content.add(board).top().padRight(60f)
        val panel = Table().top().left()
        val sides = Table()
        listOf(FigureColor.WHITE, FigureColor.BLACK).forEach { value ->
            sides.add(button(sideName(value), "side-${value.name}") {
                side = value; selected = null; notice = ""; invalidate()
            }.apply { isChecked = side == value }).width(210f).height(42f).padRight(10f)
        }
        panel.add(sides).left().padBottom(18f).row()
        s.rosters().getValue(side).forEach { hero ->
            val position = s.assignments().entries.firstOrNull { it.value == hero }?.key
            val place = position?.let { "${('a' + it.column)}${it.row + 1}" } ?: "не расставлен"
            panel.add(button("${catalog.name(hero)} — $place", "placement-${hero.name}") {
                selected = hero; notice = ""; invalidate()
            }.apply { isChecked = selected == hero }).width(430f).height(48f).left().padBottom(9f).row()
        }
        panel.add(label("Выберите сторону, затем героя и его стартовую клетку.\nМожно переставлять уже размещённых героев.", true)
            .apply { setWrap(true); color = muted }).width(430f).left().padTop(16f).row()
        content.add(panel).top().left().expandX()
        footer.add(label(notice.ifBlank { "Обычные фигуры останутся на остальных стартовых клетках." }, true)).expandX().left()
    }
}

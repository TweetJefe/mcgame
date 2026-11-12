package org.mobachess.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import game.heroes.HeroRegistry
import game.heroes.clownganger.ClownView
import game.heroes.faun.FaunView
import game.heroes.leshy.LeshyMechanic
import game.heroes.shiva.ShivaMechanic
import game.state.GameSnapshot
import game.visibility.VisibilityRules
import model.FigureColor
import model.Position
import org.mobachess.assets.GameAssets
import org.mobachess.presentation.MatchController
import org.mobachess.presentation.PieceDragState

/** Отрисовка читает модель; игровые позиции не зависят от экранных координат спрайтов. */
class MatchRenderer(private val assets: GameAssets, val geometry: BoardGeometry) {
    val hudLayout = HudLayout(assets.texture(assets.hudPath).width, assets.texture(assets.hudPath).height)
    private val shapes = ShapeRenderer()
    private val shadows = PieceShadowRenderer()
    private val glows = PieceGlowRenderer()
    private val outlines = PieceOutlineRenderer()
    private val cream = Color.valueOf("F4D9A0")

    fun draw(batch: SpriteBatch, matrix: Matrix4, controller: MatchController, drag: PieceDragState) {
        val state = controller.session.snapshot()
        batch.projectionMatrix = matrix; shapes.projectionMatrix = matrix
        batch.begin()
        batch.color = Color.WHITE
        batch.draw(assets.texture(assets.backgroundPath), 0f, 0f, 1600f, 900f)
        batch.draw(assets.texture(assets.boardPath), geometry.x, geometry.y, geometry.width, geometry.height)
        batch.end()
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        controller.highlights.forEach { cell(it, Color(.35f, .95f, .64f, .30f)) }
        controller.selected?.let { cell(it, Color(1f, .72f, .18f, .30f)) }
        state.terrain().cells().keys.forEach { cell(it, Color(.12f, .28f, .08f, .60f)) }
        state.mechanics().get(LeshyMechanic.KEY).cells().forEach { cell(it, Color(.15f, .42f, .12f, .52f)) }
        state.pieces().values.filter { it.position() != null }.forEach { p ->
            if (state.faun().clouds().any { it.contains(p.position()) }) cell(p.position(), Color(.42f,.34f,.58f,.27f))
        }
        shapes.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)
        batch.begin()
        for (rank in 0..7) {
            val p = geometry.point(-.35f, rank + .5f)
            assets.small.color = cream
            assets.small.draw(batch, if (geometry.flipped) "${8-rank}" else "${rank+1}", p.x, p.y)
        }
        for (file in 0..7) {
            val p = geometry.point(file + .48f, -.30f)
            assets.small.draw(batch, ('a' + if (geometry.flipped) 7-file else file).toString(), p.x, p.y)
        }
        val trees = state.mechanics().get(LeshyMechanic.KEY).trees()
        state.pieces().values.filter { it.position() != null && FaunView.visible(state, it) }
            .sortedWith(compareBy<game.state.PieceState> { if (drag.isFloating(it.id())) 1 else 0 }
                .thenByDescending { geometry.display(it.position()).row }.thenBy { it.position().column })
            .forEach { actual ->
                val piece = ClownView.displayed(state, actual)
                val square = piece.position()
                val home = geometry.anchor(square)
                val anchor = drag.anchor(actual.id(), home)
                val scale = 1f - geometry.display(square).row * .027f
                val width = 85f * scale
                val region = assets.sprite(piece)
                shadows.draw(batch, piece, home, width, scale)
                val previousTransform = if (drag.isFloating(actual.id())) Matrix4(batch.transformMatrix) else null
                if (previousTransform != null) {
                    batch.transformMatrix = Matrix4(previousTransform).translate(anchor.x, anchor.y, 0f)
                        .rotate(0f, 0f, 1f, drag.rotation(actual.id())).translate(-anchor.x, -anchor.y, 0f)
                }
                batch.color = Color.WHITE
                when {
                    trees.containsKey(piece.id()) -> label(batch, "ДЕРЕВО", anchor, scale)
                    piece.isPotted -> label(batch, "ГОРШОК", anchor, scale)
                    state.mechanics().get(ShivaMechanic.KEY).lotuses().containsKey(piece.id()) -> label(batch, "ЛОТУС", anchor, scale)
                    region != null -> {
                        val h = 104f * scale * assets.boardHeightRatio(piece)
                        val fit = minOf(width / region.regionWidth, h / region.regionHeight)
                        val spriteX = anchor.x - region.regionWidth * fit * assets.spriteAnchorX(region)
                        if (piece.hero() != game.heroes.HeroType.NONE) glows.draw(batch, region, piece.color(), spriteX,
                            anchor.y, region.regionWidth*fit, region.regionHeight*fit)
                        if (piece.hero() == game.heroes.HeroType.NONE) {
                            outlines.draw(batch, region, spriteX, anchor.y,
                                region.regionWidth * fit, region.regionHeight * fit, 1.25f * scale)
                        }
                        batch.draw(region, spriteX, anchor.y, region.regionWidth*fit, region.regionHeight*fit)
                    }
                    else -> label(batch, piece.type().simpleName, anchor, scale)
                }
                val status = listOfNotNull(if(piece.isBlinded) "СЛЕП" else null, if(piece.isStunned) "СТАН" else null,
                    if(piece.isBoxed) "КОРОБКА" else null).joinToString(" ")
                if(status.isNotEmpty()) label(batch,status,anchor,scale)
                if (previousTransform != null) batch.transformMatrix = previousTransform
            }
        state.mines().cells().keys.filter { VisibilityRules.mineVisible(state,it) }.forEach {
            val p=geometry.anchor(it); assets.small.color=Color.ORANGE; assets.small.draw(batch,"МИНА",p.x-20,p.y)
        }
        batch.color = Color.WHITE
        val hud = hudLayout.bounds
        batch.draw(assets.texture(assets.hudPath), hud.x, hud.y, hud.width, hud.height)
        val selected = controller.selected?.let(state::pieceAt)
        selected?.let { piece -> assets.sprite(piece)?.let { region ->
            val scale = minOf(120f/region.regionWidth, 142f/region.regionHeight)
            batch.draw(region, 690f-region.regionWidth*scale/2, 55f, region.regionWidth*scale, region.regionHeight*scale)
        } }
        assets.small.color=cream
        assets.small.draw(batch,"${if(state.active()==FigureColor.WHITE) "БЕЛЫЕ" else "ЧЁРНЫЕ"} • ХОД ${state.turn().number()}",365f,251f)
        selected?.let { piece ->
            val hero=HeroRegistry.fromRules(state.heroRules()).get(piece.hero())
            assets.small.draw(batch,"${hero.definition().displayName()} • КД ${piece.cooldown()} • Ульта ${piece.ultimateCharge()}",365f,226f)
        }
        assets.small.draw(batch,"ПРЕДМЕТЫ",846f,62f)
        batch.end()
    }

    private fun label(batch: SpriteBatch, text: String, point: Point, scale: Float) {
        assets.small.color=Color.valueOf("F7D68C")
        assets.small.draw(batch,text,point.x-25*scale,point.y+36*scale)
    }
    private fun cell(square: Position, color: Color) {
        val v=geometry.corners(square); shapes.color=color
        shapes.triangle(v[0].x,v[0].y,v[1].x,v[1].y,v[2].x,v[2].y)
        shapes.triangle(v[0].x,v[0].y,v[2].x,v[2].y,v[3].x,v[3].y)
    }
    fun dispose() { glows.dispose(); shadows.dispose(); shapes.dispose() }
}

package org.mobachess

import game.heroes.leshy.LeshyAbility
import game.heroes.ginseng.GinsengAbility
import game.moves.Promotion
import model.FigureColor
import model.Position
import org.junit.Assert.*
import org.junit.Test
import org.mobachess.presentation.AbilityChoice
import org.mobachess.presentation.MatchController
import org.mobachess.render.BoardGeometry
import org.mobachess.session.LocalGameSession

class LocalMatchTest {
    private fun pos(cell: String) = Position(cell[1] - '1', cell[0] - 'a')
    @Test fun projectedCellsMatchMousePickingInBothOrientations() {
        val geometry=BoardGeometry()
        for(flip in listOf(false,true)) {
            geometry.flipped=flip
            for(row in 0..7) for(col in 0..7) {
                val cell=Position(row,col); val p=geometry.anchor(cell)
                assertEquals(cell,geometry.cell(p.x,p.y))
            }
        }
        assertNull(geometry.cell(0f,0f)); assertNull(geometry.cell(1590f,850f))
    }
    @Test fun hotseatUsesEngineBudgetAndChangesSidesOnlyOnEndTurn() {
        val controller=MatchController(LocalGameSession.preview())
        controller.endTurn(); assertEquals(FigureColor.WHITE,controller.session.snapshot().active())
        controller.click(pos("a2")); assertTrue(pos("a3") in controller.highlights)
        controller.click(pos("a3")); assertTrue(controller.session.snapshot().turn().moveUsed())
        controller.click(pos("h2")); assertFalse(pos("h3") in controller.highlights)
        controller.endTurn(); assertEquals(FigureColor.BLACK,controller.session.snapshot().active())
        controller.click(pos("a7")); controller.click(pos("a6")); controller.endTurn()
        assertEquals(3L,controller.session.snapshot().turn().number())
    }
    @Test fun abilityUsesRealHeroAndDoesNotReplaceMandatoryMove() {
        val controller=MatchController(LocalGameSession.preview())
        controller.click(pos("c2")); assertTrue(controller.choices().any { it.ability==LeshyAbility.VINE })
        controller.choose(AbilityChoice(LeshyAbility.VINE))
        assertTrue(controller.session.snapshot().turn().abilityUsed())
        assertFalse(controller.session.snapshot().turn().moveUsed())
        controller.endTurn(); assertEquals(FigureColor.WHITE,controller.session.snapshot().active())
    }
    @Test fun selectingEnemyDoesNotExposeCommands() {
        val controller=MatchController(LocalGameSession.preview())
        controller.click(pos("h8")); assertNull(controller.selected); assertTrue(controller.choices().isEmpty())
    }
    @Test fun twoStageAbilitySelectsTargetThenDestinationThroughEngine() {
        val controller=MatchController(LocalGameSession.preview())
        controller.click(pos("a2"));controller.click(pos("a3"));controller.endTurn()
        val sourceId=controller.session.snapshot().pieceAt(pos("f7")).id()
        controller.click(pos("f8"));controller.choose(AbilityChoice(GinsengAbility.DISPLACE))
        assertTrue(pos("f7") in controller.highlights)
        controller.click(pos("f7"));assertEquals(pos("f7"),controller.target)
        assertTrue(pos("e6") in controller.highlights)
        controller.click(pos("e6"))
        assertEquals(sourceId,controller.session.snapshot().pieceAt(pos("e6")).id())
        assertTrue(controller.session.snapshot().turn().abilityUsed())
        assertFalse(controller.session.snapshot().turn().moveUsed())
    }
    @Test fun promotionWaitsForExplicitChoice() {
        val factory=model.Field(8)
        val board=java.util.HashMap<Position,model.Figure>()
        for(row in 0..7) for(col in 0..7) {
            val p=Position(row,col);board[p]=factory.createFigure(model.figures.Empty::class.java,p,FigureColor.NONE)
        }
        for((name,type,color) in listOf(Triple("h1",model.figures.King::class.java,FigureColor.WHITE),
            Triple("h8",model.figures.King::class.java,FigureColor.BLACK),Triple("a7",model.figures.Pawn::class.java,FigureColor.WHITE)))
            board[pos(name)]=factory.createFigure(type,pos(name),color)
        val match=game.core.Game.fromMobaPosition(game.state.GamePosition(board,FigureColor.WHITE,game.state.CastlingRights.none(),null,0),
            emptyMap(),game.heroes.HeroRules.defaults(),{0L})
        val controller=MatchController(LocalGameSession(match))
        controller.click(pos("a7"));controller.click(pos("a8"))
        assertNotNull(controller.promotion);assertFalse(controller.session.snapshot().turn().moveUsed())
        controller.promote(Promotion.KNIGHT)
        assertEquals(model.figures.Knight::class.java,controller.session.snapshot().pieceAt(pos("a8")).type())
    }
}

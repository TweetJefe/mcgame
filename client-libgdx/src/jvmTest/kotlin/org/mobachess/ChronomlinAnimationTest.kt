package org.mobachess

import game.core.Game
import game.heroes.AbilityRequest
import game.heroes.HeroType
import game.heroes.chronomlin.ChronomlinAbility
import model.FigureColor
import model.Position
import model.figures.Pawn
import model.figures.Rook
import org.junit.Assert.*
import org.junit.Test
import org.mobachess.presentation.animation.PieceAnimationTimeline
import org.mobachess.render.BoardGeometry

class ChronomlinAnimationTest {
    @Test fun movesCasterAndLeavesTargetStillForBothColors() {
        for(color in listOf(FigureColor.WHITE,FigureColor.BLACK)) {
            val game=Game.sandbox()
            if(color==FigureColor.BLACK) assertTrue(game.endTurn(game.currentPlayer).isSuccess)
            val from=Position(1,1);val target=Position(6,6);val landing=Position(5,5)
            assertTrue(game.placeEditorPiece(Pawn::class.java,HeroType.CHRONOMLIN,color,from).isSuccess)
            assertTrue(game.placeEditorPiece(Rook::class.java,HeroType.NONE,color,target).isSuccess)
            val casterId=game.getPieceState(from).id();val targetId=game.getPieceState(target).id()
            val timeline=PieceAnimationTimeline()
            timeline.update(game,game.snapshot,0f)
            assertTrue(game.useAbility(game.currentPlayer,AbilityRequest(from,ChronomlinAbility.GRAPPLE,target)).isSuccess)
            timeline.update(game,game.snapshot,0f)
            assertTrue(timeline.isPlaying(casterId));assertFalse(timeline.isPlaying(targetId))
            assertEquals(BoardGeometry().anchor(from),timeline.pose(casterId,BoardGeometry())!!.anchor)
            assertEquals(landing,game.getPieceState(casterId).position())
            assertEquals(target,game.getPieceState(targetId).position())
            timeline.update(game,game.snapshot,.5f)
            assertFalse(timeline.isPlaying(casterId))
        }
    }
    @Test fun blockedGrappleDoesNotAnimate() {
        val game=Game.sandbox()
        val from=Position(1,1);val target=Position(5,1)
        assertTrue(game.placeEditorPiece(Pawn::class.java,HeroType.CHRONOMLIN,FigureColor.WHITE,from).isSuccess)
        assertTrue(game.placeEditorPiece(Pawn::class.java,HeroType.NONE,FigureColor.BLACK,Position(3,1)).isSuccess)
        assertTrue(game.placeEditorPiece(Rook::class.java,HeroType.NONE,FigureColor.BLACK,target).isSuccess)
        val id=game.getPieceState(from).id();val timeline=PieceAnimationTimeline()
        timeline.update(game,game.snapshot,0f)
        assertFalse(game.useAbility(game.currentPlayer,AbilityRequest(from,ChronomlinAbility.GRAPPLE,target)).isSuccess)
        timeline.update(game,game.snapshot,0f)
        assertFalse(timeline.isPlaying(id))
    }
}

package org.mobachess

import game.core.Game
import game.heroes.AbilityRequest
import game.heroes.HeroType
import game.heroes.pudge.PudgeAbility
import model.FigureColor
import model.Position
import model.figures.Rook
import model.figures.Pawn
import org.junit.Assert.*
import org.junit.Test
import org.mobachess.presentation.animation.*
import org.mobachess.render.BoardGeometry

class HookAnimationTest {
    @Test fun travelsOnBoardInEveryDirectionAndOrientation() {
        val origin=Position(3,3)
        for (flipped in listOf(false,true)) for(dr in -1..1) for(dc in -1..1) {
            if(dr==0 && dc==0) continue
            val end=Position(3+dr,3+dc)
            val board=BoardGeometry().apply { this.flipped=flipped }
            val animation=BoardSlideAnimation(1,origin,end)
            assertEquals(board.anchor(origin),animation.pose(0f,board).anchor)
            val middle=animation.pose(animation.duration/2,board)
            assertEquals(middle.anchor,middle.ground)
            assertEquals(0f,middle.rotation,.001f)
            assertEquals((board.anchor(origin).x+board.anchor(end).x)/2,middle.anchor.x,.001f)
            assertEquals((board.anchor(origin).y+board.anchor(end).y)/2,middle.anchor.y,.001f)
            val landed=animation.pose(animation.duration,board)
            assertEquals(board.anchor(end).x,landed.anchor.x,.001f)
            assertEquals(board.anchor(end).y,landed.anchor.y,.001f)
        }
    }
    @Test fun usesActualHookLandingInsteadOfTargetSquareInEvent() {
        val game=Game.sandbox()
        val actor=Position(3,3);val target=Position(6,3);val landing=Position(4,3)
        assertTrue(game.placeEditorPiece(Rook::class.java,HeroType.PUDGE,FigureColor.WHITE,actor).isSuccess)
        assertTrue(game.placeEditorPiece(Pawn::class.java,HeroType.NONE,FigureColor.BLACK,target).isSuccess)
        val id=game.getPieceState(target).id()
        val timeline=PieceAnimationTimeline()
        timeline.update(game,game.snapshot,0f)
        assertTrue(game.useAbility(game.currentPlayer,AbilityRequest(actor,PudgeAbility.HOOK,target)).isSuccess)
        assertEquals(landing,game.getPieceState(id).position())
        timeline.update(game,game.snapshot,0f)
        assertTrue(timeline.isPlaying(id))
        assertEquals(BoardGeometry().anchor(target),timeline.pose(id,BoardGeometry())!!.anchor)
        timeline.update(game,game.snapshot,.5f)
        assertFalse(timeline.isPlaying(id))
    }
    @Test fun adjacentTargetDoesNotGetAnArtificialMove() {
        val game=Game.sandbox()
        val actor=Position(3,3);val target=Position(4,3)
        assertTrue(game.placeEditorPiece(Rook::class.java,HeroType.PUDGE,FigureColor.WHITE,actor).isSuccess)
        assertTrue(game.placeEditorPiece(Pawn::class.java,HeroType.NONE,FigureColor.WHITE,target).isSuccess)
        val id=game.getPieceState(target).id()
        val timeline=PieceAnimationTimeline()
        timeline.update(game,game.snapshot,0f)
        assertTrue(game.useAbility(game.currentPlayer,AbilityRequest(actor,PudgeAbility.HOOK,target)).isSuccess)
        timeline.update(game,game.snapshot,0f)
        assertFalse(timeline.isPlaying(id))
    }
}

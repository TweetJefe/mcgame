package org.mobachess

import game.core.Game
import game.heroes.AbilityRequest
import game.heroes.HeroType
import game.heroes.ginseng.GinsengAbility
import model.FigureColor
import model.Position
import model.figures.Bishop
import model.figures.Pawn
import org.junit.Assert.*
import org.junit.Test
import org.mobachess.presentation.animation.*
import org.mobachess.render.BoardGeometry

class GinsengAnimationTest {
    @Test fun liftHappensBeforeTravelAndLandsExactlyInBothOrientations() {
        val from=Position(2,2); val to=Position(3,3)
        val motion=GinsengDisplaceAnimation(1,from,to)
        for(flipped in listOf(false,true)) {
            val board=BoardGeometry().apply { this.flipped=flipped }
            val start=motion.pose(0f,board)
            assertEquals(board.anchor(from),start.anchor)
            val raised=motion.pose(.22f,board)
            assertEquals(board.anchor(from),raised.ground)
            assertTrue(raised.anchor.y>raised.ground.y+20f)
            assertTrue(kotlin.math.abs(raised.rotation)>1f)
            val landing=motion.pose(motion.duration,board)
            assertEquals(board.anchor(to).x,landing.anchor.x,.001f)
            assertEquals(board.anchor(to).y,landing.anchor.y,.001f)
            assertEquals(0f,landing.rotation,.001f)
        }
    }
    @Test fun confirmedCastAnimatesOnceWithoutReplayingLoadedHistory() {
        val game=Game.sandbox()
        val from=Position(1,1);val target=Position(2,2);val to=Position(3,3)
        assertTrue(game.placeEditorPiece(Bishop::class.java,HeroType.GINSENG,FigureColor.WHITE,from).isSuccess)
        assertTrue(game.placeEditorPiece(Pawn::class.java,HeroType.NONE,FigureColor.WHITE,target).isSuccess)
        val id=game.getPieceState(target).id()
        val timeline=PieceAnimationTimeline()
        timeline.update(game,game.snapshot,0f)
        assertFalse(timeline.isPlaying(id))
        assertFalse(game.useAbility(game.currentPlayer,AbilityRequest(from,GinsengAbility.DISPLACE,target,Position(6,6),false)).isSuccess)
        timeline.update(game,game.snapshot,0f)
        assertFalse(timeline.isPlaying(id))
        assertTrue(game.useAbility(game.currentPlayer,AbilityRequest(from,GinsengAbility.DISPLACE,target,to,false)).isSuccess)
        timeline.update(game,game.snapshot,0f)
        assertTrue(timeline.isPlaying(id))
        assertEquals(to,game.getPieceState(id).position())
        timeline.update(game,game.snapshot,.4f)
        assertTrue(timeline.isPlaying(id))
        timeline.update(game,game.snapshot,.4f)
        assertFalse(timeline.isPlaying(id))
        val loaded=PieceAnimationTimeline()
        loaded.update(game,game.snapshot,0f)
        assertFalse(loaded.isPlaying(id))
    }
}

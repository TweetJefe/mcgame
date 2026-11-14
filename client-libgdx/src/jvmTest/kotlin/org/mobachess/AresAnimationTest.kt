package org.mobachess
import game.core.Game
import game.heroes.*
import game.heroes.ares.AresAbility
import game.history.GameEvent
import model.*
import model.figures.*
import org.junit.Assert.*
import org.junit.Test
import org.mobachess.presentation.animation.*
import org.mobachess.render.BoardGeometry

class AresAnimationTest {
 @Test fun spearPicksUpFigureOnlyAfterContactAndStopsAtCollision() {
  val game=Game.sandbox();val origin=Position(3,3);val first=Position(4,3);val second=Position(6,3)
  assertTrue(game.placeEditorPiece(Bishop::class.java,HeroType.ARES,FigureColor.WHITE,origin).isSuccess)
  assertTrue(game.placeEditorPiece(Pawn::class.java,HeroType.NONE,FigureColor.BLACK,first).isSuccess)
  assertTrue(game.placeEditorPiece(Rook::class.java,HeroType.NONE,FigureColor.BLACK,second).isSuccess)
  val before=game.snapshot
  assertTrue(game.useAbility(game.currentPlayer,AbilityRequest(origin,AresAbility.SPEAR,second)).isSuccess)
  val after=game.snapshot;val event=after.events().last { it.kind()==GameEvent.Kind.ABILITY_USED }
  val plan=SpearPlan.create(event,before,after)!!
  assertEquals(second,plan.end);assertEquals(Position(5,3),plan.landing)
  val animation=AresCarryAnimationFactory().create(event,before,after)!!
  val board=BoardGeometry()
  assertEquals(board.anchor(first),animation.pose(.05f,board).anchor)
  assertEquals(board.anchor(Position(5,3)),animation.pose(animation.duration,board).anchor)
  assertTrue(after.pieceAt(Position(5,3)).isStunned)
  assertTrue(after.pieceAt(second).isStunned)
 }
 @Test fun emptyRayUsesFullRangeEvenWhenClickedCellIsNearer() {
  val game=Game.sandbox();val origin=Position(3,3);val direction=Position(4,3)
  assertTrue(game.placeEditorPiece(Bishop::class.java,HeroType.ARES,FigureColor.WHITE,origin).isSuccess)
  val before=game.snapshot
  assertTrue(game.useAbility(game.currentPlayer,AbilityRequest(origin,AresAbility.SPEAR,direction)).isSuccess)
  val after=game.snapshot;val event=after.events().last { it.kind()==GameEvent.Kind.ABILITY_USED }
  assertEquals(Position(6,3),SpearPlan.create(event,before,after)!!.end)
  assertNull(AresCarryAnimationFactory().create(event,before,after))
 }
 @Test fun kingStopsFlightWithoutBeingCarried() {
  val game=Game.sandbox();val origin=Position(3,3);val king=Position(4,3)
  assertTrue(game.placeEditorPiece(Bishop::class.java,HeroType.ARES,FigureColor.WHITE,origin).isSuccess)
  assertTrue(game.placeEditorPiece(King::class.java,HeroType.NONE,FigureColor.WHITE,king).isSuccess)
  val before=game.snapshot
  assertTrue(game.useAbility(game.currentPlayer,AbilityRequest(origin,AresAbility.SPEAR,Position(6,3))).isSuccess)
  val after=game.snapshot;val event=after.events().last { it.kind()==GameEvent.Kind.ABILITY_USED }
  val plan=SpearPlan.create(event,before,after)!!
  assertEquals(king,plan.end);assertNull(plan.carriedId)
 }
}

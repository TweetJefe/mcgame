package org.mobachess
import game.core.Game
import game.heroes.*
import game.heroes.ginseng.GinsengAbility
import game.heroes.lumeshard.LumeshardAbility
import model.*
import model.figures.*
import org.junit.Assert.*
import org.junit.Test
import org.mobachess.presentation.MatchController
import org.mobachess.session.LocalGameSession

class BorrowedHudTest {
 @Test fun borrowedAbilityReplacesUltimateAndUltimateReturnsAfterUse() {
  val game=Game.sandbox();val lum=Position(0,0);val gin=Position(1,1);val victim=Position(2,2)
  assertTrue(game.placeEditorPiece(Bishop::class.java,HeroType.LUMESHARD,FigureColor.WHITE,lum).isSuccess)
  assertTrue(game.placeEditorPiece(Bishop::class.java,HeroType.GINSENG,FigureColor.WHITE,gin).isSuccess)
  assertTrue(game.placeEditorPiece(Rook::class.java,HeroType.NONE,FigureColor.BLACK,victim).isSuccess)
  assertTrue(game.useAbility(game.currentPlayer,AbilityRequest(lum,LumeshardAbility.LASER,victim)).isSuccess)
  assertTrue(game.useAbility(game.currentPlayer,AbilityRequest(gin,GinsengAbility.COPY_SPELL,lum)).isSuccess)
  val controller=MatchController(LocalGameSession(game));controller.click(gin)
  assertTrue(controller.choices().any { it.borrowed && it.ability==LumeshardAbility.LASER })
  assertFalse(controller.choices().any { it.ability==GinsengAbility.COPY_SPELL })
  assertTrue(game.endTurn(game.currentPlayer).isSuccess);assertTrue(game.endTurn(game.currentPlayer).isSuccess)
  assertTrue(game.useAbility(game.currentPlayer,AbilityRequest(gin,LumeshardAbility.LASER,victim,null,true)).isSuccess)
  assertFalse(controller.choices().any { it.borrowed })
  assertTrue(controller.choices().any { it.ability==GinsengAbility.COPY_SPELL })
 }
}

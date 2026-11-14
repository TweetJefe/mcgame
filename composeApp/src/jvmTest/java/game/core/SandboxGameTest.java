package game.core;

import game.heroes.HeroDefinition;
import game.heroes.HeroRules;
import game.heroes.HeroType;
import game.heroes.AbilityRequest;
import game.heroes.lumeshard.LumeshardAbility;
import game.heroes.moleminer.MoleminerAbility;
import game.heroes.bolotnik.BolotnikAbility;
import game.heroes.phoenix.PhoenixAbility;
import game.heroes.shiva.ShivaAbility;
import game.moves.MoveResult;
import game.moves.TurnRejection;
import model.Field;
import model.FigureColor;
import model.Position;
import model.figures.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SandboxGameTest {
    private Position p(String s) { return new Position(s.charAt(1)-'1',s.charAt(0)-'a'); }
    private void ok(MoveResult r) { assertTrue(r.getMessage(),r.isSuccess()); }
    private void place(Game g, Class<? extends model.Figure> body, HeroType hero, FigureColor color, String square) {
        ok(g.placeEditorPiece(body,hero,color,p(square)));
    }
    private void cast(Game g, String from, game.heroes.Abilities ability, String target) {
        ok(g.useAbility(g.getCurrentPlayer(),new AbilityRequest(p(from),ability,target==null?null:p(target))));
    }
    @Test public void emptyBoardAllowsTurnsAndNormalFactoryStillRejectsIt() {
        Game g=Game.sandbox();
        assertTrue(g.getSnapshot().pieces().isEmpty());
        assertFalse(g.getOutcome().isFinished());
        assertFalse(g.getClockSnapshot().enabled());
        ok(g.endTurn(g.getCurrentPlayer()));
        assertEquals(FigureColor.BLACK,g.getSnapshot().active());
        assertThrows(IllegalArgumentException.class,()->Field.fromMobaPosition(g.getGameField().getGameField()));
    }
    @Test public void castsAreUnlimitedAndOrdinaryMovementKeepsItsTurnBudget() {
        Game g=Game.sandbox();
        place(g,Bishop.class,HeroType.LUMESHARD,FigureColor.WHITE,"d4");
        place(g,Rook.class,HeroType.NONE,FigureColor.BLACK,"d6");
        cast(g,"d4",LumeshardAbility.LASER,"d6");
        cast(g,"d4",LumeshardAbility.LASER,"d6");
        cast(g,"d4",LumeshardAbility.TELEPORT,"f6");
        cast(g,"f6",LumeshardAbility.TELEPORT,"d4");
        ok(g.moveFigure(g.getCurrentPlayer(),p("d4"),p("e5")));
        ok(g.moveFigure(g.getCurrentPlayer(),p("e5"),p("f4")));
        ok(g.moveFigure(g.getCurrentPlayer(),p("f4"),p("e5")));
        ok(g.endTurn(g.getCurrentPlayer()));
        ok(g.endTurn(g.getCurrentPlayer()));
        ok(g.moveFigure(g.getCurrentPlayer(),p("e5"),p("f4")));
        assertEquals(0,g.getPieceState(p("f4")).cooldown());
        assertEquals(0,g.getPieceState(p("f4")).ultimateCharge());
        assertFalse(g.getTurnState().abilityUsed());
        assertFalse(g.getTurnState().moveUsed());
    }
    @Test public void statusRestrictionsAndDurationRemainReal() {
        Game g=Game.sandbox();
        place(g,Bishop.class,HeroType.LUMESHARD,FigureColor.WHITE,"c4");
        place(g,Pawn.class,HeroType.NONE,FigureColor.WHITE,"d5");
        place(g,Rook.class,HeroType.NONE,FigureColor.BLACK,"d6");
        cast(g,"c4",LumeshardAbility.LASER,"d6");
        ok(g.endTurn(g.getCurrentPlayer()));
        assertFalse(g.moveFigure(g.getCurrentPlayer(),p("d6"),p("d5")).isSuccess());
        assertTrue(g.getPieceState(p("d6")).isBlinded());
        ok(g.endTurn(g.getCurrentPlayer()));
        assertFalse(g.getPieceState(p("d6")).isBlinded());
    }
    @Test public void minesCanBePlacedRepeatedlyAndResetRemovesEverything() {
        Game g=Game.sandbox();
        place(g,Pawn.class,HeroType.MOLEMINER,FigureColor.WHITE,"b2");
        cast(g,"b2",MoleminerAbility.PLANT_MINE,null);
        ok(g.moveFigure(g.getCurrentPlayer(),p("b2"),p("b3")));
        cast(g,"b3",MoleminerAbility.PLANT_MINE,null);
        assertEquals(2,g.getSnapshot().mines().cells().size());
        Game reset=Game.sandbox();
        assertTrue(reset.getSnapshot().pieces().isEmpty());
        assertTrue(reset.getSnapshot().mines().cells().isEmpty());
        assertTrue(reset.getSnapshot().terrain().cells().isEmpty());
    }
    @Test public void placingAnEnemyIntoMudTriggersOrdinaryTrap() {
        Game g=Game.sandbox();
        place(g,Knight.class,HeroType.BOLOTNIK,FigureColor.WHITE,"b2");
        cast(g,"b2",BolotnikAbility.MUD,"c3");
        place(g,Pawn.class,HeroType.NONE,FigureColor.BLACK,"c3");
        assertEquals(HeroType.BOLOTNIK,g.getPieceState(p("c3")).hero());
        assertNull(g.getPieceState(p("b2")));
        assertTrue(g.getSnapshot().terrain().cells().isEmpty());
    }
    @Test public void eggWorksWithNoOtherPiecesAndHatchesNormally() {
        Game g=Game.sandbox();
        place(g,Knight.class,HeroType.PHOENIX,FigureColor.WHITE,"d4");
        cast(g,"d4",PhoenixAbility.REBIRTH,null);
        assertEquals(Egg.class,g.getPieceState(p("d4")).type());
        for(int i=0;i<5;i++) ok(g.endTurn(g.getCurrentPlayer()));
        assertEquals(HeroType.SUPERNOVA,g.getPieceState(p("d4")).hero());
    }
    @Test public void directlyPlacedSummonedHeroKeepsItsLifecycle() {
        Game g=Game.sandbox();
        place(g,Pawn.class,HeroType.BABA_YAGA,FigureColor.WHITE,"c3");
        for(int i=0;i<7;i++) ok(g.endTurn(g.getCurrentPlayer()));
        assertEquals(HeroType.IZBA,g.getPieceState(p("c3")).hero());
    }
    @Test public void shivaHasNoCooldownAndNoCaptureCost() {
        Game g=Game.sandbox();
        place(g,Rook.class,HeroType.SHIVA,FigureColor.WHITE,"d4");
        cast(g,"d4",ShivaAbility.LOTUS,"c4");
        cast(g,"d4",ShivaAbility.LOTUS,"e4");
        assertEquals(2,g.getSnapshot().mechanics().get(game.heroes.shiva.ShivaMechanic.KEY).lotuses().size());
    }
    @Test public void standardGamesDoNotExposeEditor() {
        Game g=new Game(8);
        assertEquals(TurnRejection.UNSUPPORTED_ACTION,g.placeEditorPiece(Pawn.class,HeroType.NONE,FigureColor.WHITE,p("d4")));
        assertEquals(32,g.getSnapshot().pieces().size());
    }
    @Test public void onlyCostsAndCooldownsDifferFromMatchConfiguration() {
        Game g=Game.sandbox();
        for(var definition:g.getHeroRules().definitions()) audit(definition,g.getHeroRules());
    }
    private <R> void audit(HeroDefinition<R> d,HeroRules practice) {
        d.rulesCodec().ifPresent(codec->{
            var normal=codec.encode(d.defaultRules());
            var edited=codec.encode(practice.get(d));
            for(var entry:normal.entrySet()) {
                var key=entry.getKey().toLowerCase(java.util.Locale.ROOT);
                assertEquals(d.type()+" "+entry.getKey(),key.contains("cooldown")||key.endsWith("captures")?"0":entry.getValue(),edited.get(entry.getKey()));
            }
        });
    }
}

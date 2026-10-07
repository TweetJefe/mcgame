package rules.field_filling.filling_rules;

import model.FigureColor;
import model.Position;
import model.figures.*;
import rules.field_filling.FillingRule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FillingRule8x8 extends FillingRule {
    public FillingRule8x8() {
        blackRules = new HashMap<>();
        blackRules.put(new King(FigureColor.BLACK), List.of(new Position(7, 4)));
        blackRules.put(new Queen(FigureColor.BLACK), List.of(new Position(7, 3)));
        blackRules.put(new Bishop(FigureColor.BLACK), List.of(new Position(7, 2), new Position(7, 5)));
        blackRules.put(new Rook(FigureColor.BLACK), List.of(new Position(7, 0), new Position(7, 7)));
        blackRules.put(new Knight(FigureColor.BLACK), List.of(new Position(7, 1), new Position(7, 6)));

        List<Position> pawnBlackPositions = new ArrayList<>();
        for(int i = 0; i < 8; ++i){
            pawnBlackPositions.add(new Position(6, i));
        }
        blackRules.put(new Pawn(FigureColor.BLACK), pawnBlackPositions);


        whiteRules = new HashMap<>();
        whiteRules.put(new King(FigureColor.WHITE), List.of(new Position(0, 4)));
        whiteRules.put(new Queen(FigureColor.WHITE), List.of(new Position(0, 3)));
        whiteRules.put(new Bishop(FigureColor.WHITE), List.of(new Position(0, 2), new Position(0, 5)));
        whiteRules.put(new Rook(FigureColor.WHITE), List.of(new Position(0, 0), new Position(0, 7)));
        whiteRules.put(new Knight(FigureColor.WHITE), List.of(new Position(0, 1), new Position(0, 6)));

        List<Position> pawnWhitePositions = new ArrayList<>();
        for(int i = 0; i < 8; ++i){
            pawnWhitePositions.add(new Position(1, i));
        }
        whiteRules.put(new Pawn(FigureColor.WHITE), pawnWhitePositions);
    }
}

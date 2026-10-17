package game.state;


import model.FigureColor;

public record CastlingRights(boolean whiteKingSide, boolean whiteQueenSide,
                             boolean blackKingSide, boolean blackQueenSide) {
    public static CastlingRights initial() {
        return new CastlingRights(true, true, true, true);
    }

    public static CastlingRights none() {
        return new CastlingRights(false, false, false, false);
    }

    public boolean allows(FigureColor color, boolean kingSide) {
        return color == FigureColor.WHITE ? (kingSide ? whiteKingSide : whiteQueenSide)
                : color == FigureColor.BLACK && (kingSide ? blackKingSide : blackQueenSide);
    }

    public CastlingRights without(FigureColor color, boolean kingSide) {
        return new CastlingRights(whiteKingSide && !(color == FigureColor.WHITE && kingSide),
                whiteQueenSide && !(color == FigureColor.WHITE && !kingSide),
                blackKingSide && !(color == FigureColor.BLACK && kingSide),
                blackQueenSide && !(color == FigureColor.BLACK && !kingSide));
    }

    public CastlingRights withoutKing(FigureColor color) {
        return without(color, true).without(color, false);
    }
}

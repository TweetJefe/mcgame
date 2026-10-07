package game;

import model.FigureColor;

public record GameOutcome(Termination reason, FigureColor winner) {
    public static GameOutcome ongoing() { return new GameOutcome(Termination.IN_PROGRESS, FigureColor.NONE); }
    public Termination getReason() { return reason; }
    public FigureColor getWinner() { return winner; }
    public boolean isFinished() { return reason != Termination.IN_PROGRESS; }
    public String getMessage() {
        if (!isFinished()) return reason.getMessage();
        String result = winner == FigureColor.NONE ? "Ничья"
                : winner == FigureColor.WHITE ? "Победили белые" : "Победили чёрные";
        return result + ": " + reason.getMessage();
    }
}

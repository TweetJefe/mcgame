package game.draft;

import game.heroes.HeroType;
import model.FigureColor;
import model.Position;

public record DraftEvent(long sequence, Kind kind, FigureColor side, HeroType hero, Position square) {
    public enum Kind { BAN, PICK, FINISH_PICKS }
    public DraftEvent {
        if (sequence<1 || kind==null || (side!=FigureColor.WHITE && side!=FigureColor.BLACK)) throw new IllegalArgumentException("Invalid draft event");
        if (kind==Kind.FINISH_PICKS ? hero!=null || square!=null : hero==null || hero==HeroType.NONE || (kind==Kind.PICK)!=(square!=null))
            throw new IllegalArgumentException("Incomplete draft action");
    }
}

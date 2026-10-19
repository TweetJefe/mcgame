package game.heroes.faun;
import model.Position;
public record Cloud(long sourceId,Position center,long castTurn,long expiresAfterTurn) {
    public Cloud {
        if(sourceId<=0 || center==null || center.getRow()<0 || center.getRow()>7 || center.getColumn()<0 || center.getColumn()>7 || castTurn<1 || expiresAfterTurn<=castTurn)
            throw new IllegalArgumentException("Invalid cloud");
    }
    public boolean contains(Position square){return square!=null && Math.max(Math.abs(square.getRow()-center.getRow()),Math.abs(square.getColumn()-center.getColumn()))<=1;}
}

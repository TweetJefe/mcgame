package game.heroes.chronomlin;

public record ChronomlinRules(int grappleCooldown,int ultimateCaptures) {
    public ChronomlinRules {
        if(grappleCooldown<0 || ultimateCaptures<0)throw new IllegalArgumentException("Invalid Chronomlin rules");
    }
    public static ChronomlinRules prototype(){return new ChronomlinRules(3,1);}
    public static ChronomlinRules sandbox(){return new ChronomlinRules(0,0);}
}

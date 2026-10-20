package game.heroes.clownganger;
public record ClowngangerRules(int boxCooldown,int boxTurns,int illusionTurns,int ultimateCaptures) {
    public ClowngangerRules {if(boxCooldown<0 || boxTurns<1 || illusionTurns<1 || ultimateCaptures<0)throw new IllegalArgumentException("Invalid Clownganger rules");}
    public static ClowngangerRules prototype(){return new ClowngangerRules(3,1,2,1);}
    public static ClowngangerRules sandbox(){return new ClowngangerRules(0,1,2,0);}
}

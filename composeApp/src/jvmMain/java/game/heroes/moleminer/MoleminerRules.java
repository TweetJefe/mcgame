package game.heroes.moleminer;
public record MoleminerRules(int mineCooldown,int ultimateCaptures){
    public MoleminerRules{if(mineCooldown<0 || ultimateCaptures<0)throw new IllegalArgumentException("Invalid Moleminer rules");}
    public static MoleminerRules prototype(){return new MoleminerRules(3,2);}
    public static MoleminerRules sandbox(){return new MoleminerRules(0,0);}
}

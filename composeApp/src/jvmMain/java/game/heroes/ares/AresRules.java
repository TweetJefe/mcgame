package game.heroes.ares;
public record AresRules(int spearCooldown,int revengeCaptures) {
    public AresRules {if(spearCooldown<0 || spearCooldown>100 || revengeCaptures<0 || revengeCaptures>100)throw new IllegalArgumentException("Invalid Ares rules");}
    public static AresRules prototype(){return new AresRules(3,1);}
    public static AresRules sandbox(){return new AresRules(0,0);}
}

package game.heroes.zeus;
public record ZeusRules(int lightningCooldown,int chargeTurns,int ultimateCaptures){
    public ZeusRules{if(lightningCooldown<0 || chargeTurns<1 || ultimateCaptures<0)throw new IllegalArgumentException("Invalid Zeus rules");}
    public static ZeusRules prototype(){return new ZeusRules(1,2,2);}
    public static ZeusRules sandbox(){return new ZeusRules(0,2,0);}
}

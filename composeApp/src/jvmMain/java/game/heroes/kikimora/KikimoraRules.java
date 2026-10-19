package game.heroes.kikimora;
public record KikimoraRules(int yarnCooldown,int ultimateCaptures){
    public KikimoraRules{if(yarnCooldown<0 || yarnCooldown>100 || ultimateCaptures<0 || ultimateCaptures>100)throw new IllegalArgumentException("Invalid Kikimora rules");}
    public static KikimoraRules prototype(){return new KikimoraRules(3,2);}
    public static KikimoraRules sandbox(){return new KikimoraRules(0,0);}
}

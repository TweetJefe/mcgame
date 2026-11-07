package game.heroes.shiva;

import java.util.Arrays;
import java.util.HashSet;
import game.visibility.VisibilityRules;
import game.creation.CreatedBody;
import game.creation.CreationMechanic;
import game.heroes.HeroActionContext;
import game.heroes.HeroType;
import game.history.GameEvent;
import game.mechanics.MechanicDefinition;
import game.mechanics.MechanicEventDraft;
import game.mechanics.MechanicEventPolicy;
import game.mechanics.MechanicEventType;
import game.mechanics.MechanicHandler;
import game.mechanics.MechanicKey;
import game.mechanics.MechanicOperation;
import game.mechanics.MechanicSession;
import game.mechanics.MechanicStateCodec;
import game.mechanics.MechanicStep;
import game.mechanics.PieceRestriction;
import game.moves.Success;
import game.state.GameMode;
import game.state.GameSnapshot;
import game.state.PieceState;
import game.summoning.SummonState;
import game.visibility.Concealment;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import model.FigureColor;
import model.Position;

import static game.mechanics.MechanicPhase.ABILITY_PREPARE;
import static game.mechanics.MechanicPhase.CAN_END_SETTLED;
import static game.mechanics.MechanicPhase.END_TURN_SETTLED;
import static game.mechanics.MechanicPhase.SETUP;
import static game.mechanics.Mechanics.handler;
import static game.mechanics.Mechanics.update;

/** Лотус меняет видимость, а не исходный эффект невидимости. Ульта создаёт новые жизни. */
public final class ShivaMechanic implements MechanicDefinition<ShivaMechanic.State> {
    public record Lotus(long id, long source, long turn, long until, int radius, Position square) {
        public Lotus {
            if (id < 1 || source < 1 || id == source || turn < 1 || until <= turn || radius < 1 || radius > 7 || !inside(square))
                throw new IllegalArgumentException("Invalid lotus");
        }
    }
    public record State(long seed, long random, Map<Long, Lotus> lotuses) {
        public State { lotuses = Map.copyOf(lotuses); }
    }
    public record Plant(long actor, Position square) implements MechanicOperation {}
    public record Rebirth(long actor) implements MechanicOperation {}
    public record Revival(FigureColor heroSide, List<Long> lives, long random) {
        public Revival {
            lives = List.copyOf(lives);
            if (heroSide == null || heroSide == FigureColor.NONE || lives.size() > 2
                    || lives.stream().distinct().count() != lives.size()) throw new IllegalArgumentException("Invalid revival");
        }
    }
    public record Expired(long id) {}
    public static final ShivaMechanic INSTANCE = new ShivaMechanic();
    public static final MechanicKey<State> KEY = new MechanicKey<>("shiva", State.class, new State(0,0,Map.of()));
    public static final MechanicEventType<Lotus> PLANTED = new MechanicEventType<>("shiva.planted", Lotus.class,
            p -> Map.of("lotus", encodeLotus(p)), d -> { fields(d,"lotus"); return decodeLotus(d.get("lotus")); },
            new MechanicEventPolicy(true, true, true, true));
    public static final MechanicEventType<Expired> EXPIRED = new MechanicEventType<>("shiva.expired", Expired.class,
            p -> Map.of("id",Long.toString(p.id())), d -> { fields(d,"id"); return new Expired(Long.parseLong(d.get("id"))); },
            new MechanicEventPolicy(false, true, false, true));
    public static final MechanicEventType<Revival> REVIVED = new MechanicEventType<>("shiva.revived", Revival.class,
            p -> Map.of("heroSide",p.heroSide().name(),"lives",p.lives().stream().map(Object::toString).collect(Collectors.joining(",")),
                    "random",Long.toString(p.random())), d -> {
                fields(d,"heroSide","lives","random");
                return new Revival(FigureColor.valueOf(d.get("heroSide")), d.get("lives").isEmpty() ? List.of() : Arrays.stream(d.get("lives").split(",",-1))
                        .map(Long::parseLong).toList(), Long.parseLong(d.get("random")));
            }, new MechanicEventPolicy(true,true,false,true));
    private ShivaMechanic() {}
    @Override public MechanicKey<State> key() { return KEY; }
    @Override public List<MechanicDefinition<?>> dependencies() { return List.of(CreationMechanic.INSTANCE); }
    @Override public List<MechanicEventType<?>> events() { return List.of(PLANTED, EXPIRED, REVIVED); }
    @Override public MechanicStateCodec<State> codec() {
        return new MechanicStateCodec<>() {
            @Override public Map<String,String> encode(State s) {
                return Map.of("version","1","seed",Long.toString(s.seed()),"random",Long.toString(s.random()),
                        "lotuses",s.lotuses().values().stream().sorted(Comparator.comparingLong(Lotus::id))
                                .map(ShivaMechanic::encodeLotus).collect(Collectors.joining(";")));
            }
            @Override public State decode(Map<String,String> d) {
                fields(d,"version","seed","random","lotuses");
                if (!"1".equals(d.get("version"))) throw new IllegalArgumentException("Invalid Shiva state version");
                var lotuses = new HashMap<Long,Lotus>();
                if (!d.get("lotuses").isEmpty()) for (var line : d.get("lotuses").split(";",-1)) {
                    var p = decodeLotus(line);
                    if (lotuses.putIfAbsent(p.id(),p) != null) throw new IllegalArgumentException("Duplicate lotus");
                }
                return new State(Long.parseLong(d.get("seed")),Long.parseLong(d.get("random")),lotuses);
            }
        };
    }
    @Override public List<MechanicStep> steps() {
        return List.of(update("shiva.seed",SETUP,"summons.initialize",s -> {
            if (s.assignments().containsValue(HeroType.SHIVA)) {
                long seed = ThreadLocalRandom.current().nextLong();
                s.state(KEY,new State(seed,seed,Map.of()));
            }
        }),update("shiva.canEnd",CAN_END_SETTLED,"creation.canEnd",s -> expire(s,false)),
                update("shiva.expire",END_TURN_SETTLED,"creation.evolve",s -> expire(s,true)));
    }
    @Override public List<MechanicHandler<?>> handlers() {
        return List.of(handler(Plant.class,"shiva.plant",ABILITY_PREPARE,"spells.cast",(s,op) -> {
            var actor = s.pieces().get(op.actor());
            var piece = CreationMechanic.create(s,actor.id(),null,HeroType.NONE,CreatedBody.WARD,null,0,op.square(),null);
            var lotus = new Lotus(piece.id(),actor.id(),s.turn(),Math.addExact(s.turn(),2L*s.rules().get(ShivaDefinition.INSTANCE).lotusTurns()),s.rules().get(ShivaDefinition.INSTANCE).revealRadius(),op.square());
            var lotuses = new HashMap<>(s.state(KEY).lotuses());
            lotuses.put(piece.id(),lotus);
            s.state(KEY,new State(s.state(KEY).seed(),s.state(KEY).random(),lotuses));
            s.propose(new MechanicEventDraft(s.turn(),actor.color(),actor.id(),piece.id(),actor.position(),op.square(),
                    "Shiva: появился лотус",PLANTED.event(lotus)));
            return Success.SUCCESS;
        }),handler(Rebirth.class,"shiva.rebirth",ABILITY_PREPARE,"shiva.plant",(s,op) -> {
            var actor = s.pieces().get(op.actor());
            long random = s.state(KEY).random();
            var orientations = orientations(s.context());
            var choice = SummonState.initial(random).choose(orientations.size());
            var heroSide = orientations.get(choice.index());
            random = choice.nextState();
            var lives = new ArrayList<Long>();
            for (var color : List.of(FigureColor.WHITE,FigureColor.BLACK)) {
                var corpses = corpses(s.context(),color,color == heroSide);
                var cells = cells(s.context(),color);
                if (corpses.isEmpty() || cells.isEmpty()) continue;
                choice = SummonState.initial(random).choose(corpses.size());
                var corpse = corpses.get(choice.index());
                random = choice.nextState();
                choice = SummonState.initial(random).choose(cells.size());
                var square = cells.get(choice.index());
                random = choice.nextState();
                var plan = s.heroes().revival(corpse,s.context());
                var live = CreationMechanic.create(s,actor.id(),corpse,corpse.hero(),CreatedBody.of(plan.body()),
                        plan.startingColor(),plan.cooldown(),square,plan.evolution());
                lives.add(live.id());
            }
            s.state(KEY,new State(s.state(KEY).seed(),random,s.state(KEY).lotuses()));
            s.propose(new MechanicEventDraft(s.turn(),actor.color(),actor.id(),null,actor.position(),actor.position(),
                    "Shiva: появились новые жизни",REVIVED.event(new Revival(heroSide,lives,random))));
            return Success.SUCCESS;
        }));
    }
    static List<FigureColor> orientations(HeroActionContext context) {
        for (var side : List.of(FigureColor.WHITE,FigureColor.BLACK))
            if (!cells(context,side).isEmpty() && (!corpses(context,side,true).isEmpty() || !corpses(context,side,false).isEmpty()))
                return List.of(FigureColor.WHITE,FigureColor.BLACK);
        return List.of();
    }
    private static List<PieceState> corpses(HeroActionContext context, FigureColor side, boolean hero) {
        var used = context.state(CreationMechanic.KEY).usedCorpses();
        return context.pieces().values().stream().filter(p -> p.color() == side && (p.hero() != HeroType.NONE) == hero
                && CreationMechanic.eligibleCorpse(p,context.field()) && !used.contains(p.id()))
                .sorted(Comparator.comparingLong(PieceState::id)).toList();
    }
    private static List<Position> cells(HeroActionContext context, FigureColor side) {
        var result = new ArrayList<Position>();
        int start = side == FigureColor.WHITE ? 0 : 4;
        for (int row = start; row < start+4; row++) for (int col=0; col<8; col++) {
            var p = new Position(row,col);
            if (context.pieceAt(p) == null) result.add(p);
        }
        return List.copyOf(result);
    }
    @Override public boolean reveals(HeroActionContext context, FigureColor viewer, Position square, Concealment kind) {
        if (kind == Concealment.ILLUSION) return false;
        return context.state(KEY).lotuses().values().stream().anyMatch(l -> {
            var piece = context.pieces().get(l.id());
            return piece != null && piece.position() != null && !piece.isRemoved() && piece.color() == viewer
                    && !piece.isPotted() && !context.blocks(piece,PieceRestriction.PASSIVES) && ShivaHero.distance(l.square(),square) <= l.radius();
        });
    }
    private static void expire(MechanicSession s, boolean journal) {
        var lotuses = new HashMap<>(s.state(KEY).lotuses());
        for (var lotus : s.state(KEY).lotuses().values().stream().sorted(Comparator.comparingLong(Lotus::id)).toList()) {
            var piece = s.pieces().get(lotus.id());
            if (lotus.until() > s.turn() || piece != null && !piece.isRemoved()
                    && (piece.isPotted() || s.context().blocks(piece,PieceRestriction.CAPTURE))) continue;
            CreationMechanic.dismiss(s,lotus.id(),journal);
            lotuses.remove(lotus.id());
            if (journal) s.propose(new MechanicEventDraft(s.turn(),piece.color(),piece.id(),null,lotus.square(),lotus.square(),
                    "Shiva: закончился срок лотуса",EXPIRED.event(new Expired(lotus.id()))));
        }
        s.state(KEY,new State(s.state(KEY).seed(),s.state(KEY).random(),lotuses));
    }
    @Override public State restore(GameSnapshot snapshot) {
        long seed = snapshot.events().stream().filter(e -> e.isEffect(REVIVED))
                .map(e -> REVIVED.type().cast(e.effect().data()))
                .map(p -> p.random() - 0x9e3779b97f4a7c15L * (1 + 2L*p.lives().size())).findFirst().orElse(0L);
        return replay(snapshot,seed);
    }
    private static State replay(GameSnapshot snapshot,long seed) {
        var lotuses = new HashMap<Long,Lotus>();
        long random = seed;
        var rules = snapshot.heroRules().get(ShivaDefinition.INSTANCE);
        for (var e : snapshot.events()) {
            if (e.isEffect(PLANTED)) {
                var lotus = PLANTED.type().cast(e.effect().data());
                var birth = snapshot.mechanics().get(CreationMechanic.KEY).births().get(lotus.id());
                if (lotus.id()!=e.targetId() || lotus.source()!=e.actorId() || lotus.turn()!=e.turn()
                        || lotus.until()!=e.turn()+2L*rules.lotusTurns() || lotus.radius()!=rules.revealRadius() || !lotus.square().equals(e.to())
                        || ShivaHero.distance(e.from(),e.to())>rules.castRadius() || birth==null || birth.corpse()!=null
                        || birth.hero()!=HeroType.NONE || birth.body()!=CreatedBody.WARD || birth.caster()!=lotus.source()
                        || birth.turn()!=e.turn() || !birth.square().equals(lotus.square()) || !birth.ability().equals(ShivaAbility.LOTUS.id())
                        || lotuses.putIfAbsent(lotus.id(),lotus)!=null) throw new IllegalArgumentException("Invalid lotus origin");
                requireAbility(snapshot,e,ShivaAbility.LOTUS);
            }
            if (e.isEffect(EXPIRED)) {
                var id = EXPIRED.type().cast(e.effect().data()).id();
                var lotus = lotuses.remove(id);
                if (lotus==null || id!=e.actorId() || lotus.until()>e.turn() || !lotus.square().equals(e.from())
                        || !e.from().equals(e.to()) || snapshot.events().stream().noneMatch(end -> end.kind()==GameEvent.Kind.END_TURN && end.turn()==e.turn()))
                    throw new IllegalArgumentException("Invalid lotus expiry");
            }
            if (e.isEffect(REVIVED)) {
                var data = REVIVED.type().cast(e.effect().data());
                requireAbility(snapshot,e,ShivaAbility.REBIRTH);
                var sides = new HashSet<FigureColor>();
                for (long id : data.lives()) {
                    var birth = snapshot.mechanics().get(CreationMechanic.KEY).births().get(id);
                    var live = snapshot.pieces().get(id);
                    var corpse = birth==null || birth.corpse()==null ? null : snapshot.pieces().get(birth.corpse());
                    if (corpse==null || live==null || !sides.add(live.color()) || birth.caster()!=e.actorId() || birth.turn()!=e.turn()
                            || !birth.ability().equals(ShivaAbility.REBIRTH.id()) || (corpse.hero()!=HeroType.NONE)!=(live.color()==data.heroSide())
                            || (birth.square().getRow()<4)!=(live.color()==FigureColor.WHITE)) throw new IllegalArgumentException("Invalid resurrection role");
                }
                // Каждое воскрешение расходует выбор роли и по два выбора на новую жизнь.
                var role = SummonState.initial(random).choose(2);
                if (data.heroSide()!=List.of(FigureColor.WHITE,FigureColor.BLACK).get(role.index()))
                    throw new IllegalArgumentException("Revival role differs from random choice");
                long cursor = role.nextState();
                for (int i=0;i<2*data.lives().size();i++) cursor = SummonState.initial(cursor).choose(1).nextState();
                if (cursor!=data.random()) throw new IllegalArgumentException("Revival random cursor differs");
                random = data.random();
            }
        }
        return new State(seed,random,lotuses);
    }
    @Override public void validate(GameSnapshot snapshot) {
        var state = snapshot.mechanics().get(KEY);
        if (!state.equals(replay(snapshot,state.seed()))) throw new IllegalArgumentException("Shiva state differs from history");
        if (snapshot.mode()==GameMode.CLASSIC && !state.equals(KEY.initial())) throw new IllegalArgumentException("Shiva effects in Classic");
        var context = VisibilityRules.context(snapshot);
        for (var lotus : state.lotuses().values()) {
            var piece = snapshot.pieces().get(lotus.id());
            if (piece==null || !piece.isRemoved() && !lotus.square().equals(piece.position())
                    || lotus.until()<snapshot.turn().number() && !piece.isRemoved() && !piece.isPotted()
                    && !context.blocks(piece,PieceRestriction.CAPTURE)) throw new IllegalArgumentException("Missing or overdue lotus");
        }
        for (var e : snapshot.events()) if (e.isAbility(ShivaAbility.LOTUS) || e.isAbility(ShivaAbility.REBIRTH)) {
            var type = e.isAbility(ShivaAbility.LOTUS) ? PLANTED : REVIVED;
            if (snapshot.events().stream().filter(p -> p.isEffect(type) && p.turn()==e.turn() && Objects.equals(p.actorId(),e.actorId())).count()!=1)
                throw new IllegalArgumentException("Ability without exactly one Shiva effect");
        }
    }
    private static void requireAbility(GameSnapshot snapshot,GameEvent effect,ShivaAbility ability) {
        if (snapshot.events().stream().noneMatch(e -> e.sequence()<effect.sequence() && e.turn()==effect.turn()
                && Objects.equals(e.actorId(),effect.actorId()) && e.isAbility(ability) && Objects.equals(e.from(),effect.from())))
            throw new IllegalArgumentException("Shiva effect without ability");
    }
    private static boolean inside(Position p) { return p!=null && p.getRow()>=0 && p.getRow()<8 && p.getColumn()>=0 && p.getColumn()<8; }
    private static String encodeLotus(Lotus p) { return p.id()+","+p.source()+","+p.turn()+","+p.until()+","+p.radius()+","+p.square().getRow()+","+p.square().getColumn(); }
    private static Lotus decodeLotus(String text) {
        var a=text.split(",",-1);
        if(a.length!=7) throw new IllegalArgumentException("Invalid lotus fields");
        return new Lotus(Long.parseLong(a[0]),Long.parseLong(a[1]),Long.parseLong(a[2]),Long.parseLong(a[3]),Integer.parseInt(a[4]),
                new Position(Integer.parseInt(a[5]),Integer.parseInt(a[6])));
    }
    private static void fields(Map<String,String> d,String... names) {
        if(!d.keySet().equals(Set.of(names))) throw new IllegalArgumentException("Invalid Shiva fields");
    }
}

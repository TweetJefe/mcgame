package game.mechanics.compat;

import game.heroes.HeroActionContext;
import game.heroes.HeroType;
import game.heroes.ares.AresAbility;
import game.heroes.kikimora.KikimoraAbilities;
import game.heroes.kikimora.KikimoraAbility;
import game.heroes.moleminer.MineState;
import game.heroes.zeus.LightningState;
import game.history.GameEvent;
import game.mechanics.MechanicSession;
import game.moves.Capture;
import game.state.PieceState;
import game.summoning.SummonRecord;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import model.FigureColor;
import model.Position;

/** Адаптер прежней последовательности записей; новые механики публикуют типизированные события. */
public final class HistoricalJournal {
    private final List<GameEvent> events;
    private final long turnNumber;
    private final FigureColor side;
    private HistoricalJournal(MechanicSession session) {
        events = new ArrayList<>(session.history()); turnNumber = session.turn(); side = session.side();
    }
    public static void action(MechanicSession session) {
        var journal = new HistoricalJournal(session);
        journal.addActionEffects(session.before(), session);
        journal.events.stream().skip(session.history().size()).forEach(session::emit);
    }
    public static void endTurn(MechanicSession session) {
        var journal = new HistoricalJournal(session);
        journal.addSummonChanges(session.before(), session.context());
        journal.events.stream().skip(session.history().size()).forEach(session::emit);
    }
    private GameEvent lastHistoricalEvent() {
        return events.stream().filter(event -> event.kind() != GameEvent.Kind.CAPTURE
                && (event.effect() == null || HistoricalEvents.kind(event.effect()) != null))
                .reduce((first, second) -> second).orElse(null);
    }
    private void addEvent(GameEvent.Kind kind, FigureColor color, Long actor, Long target,
                          Position from, Position to, String message) {
        events.add(new GameEvent(events.size() + 1L, turnNumber, kind, color, actor, target, from, to, message));
    }
    private static String cell(Position square) {
        return square == null ? "—" : "" + (char) ('a' + square.getColumn()) + (square.getRow() + 1);
    }
    private void addSummonChanges(HeroActionContext before, HeroActionContext after) {
        for (var r : after.summons().records().values().stream().sorted(Comparator.comparingLong(SummonRecord::id)).toList()) {
            if (before.summons().records().containsKey(r.id()) && !before.summons().records().get(r.id()).transformed() && r.transformed()) {
                PieceState p = after.pieces().get(r.id());
                Position square = p.position() == null ? r.bornAt() : p.position();
                addEvent(GameEvent.Kind.YAGA_MATURE, p.color(), p.id(), p.id(), square, square, "Баба-яга превратилась в Избу");
            }
        }
        Long summoner = after.summons().records().values().stream().filter(r -> !before.summons().records().containsKey(r.id()) && r.cause() == SummonRecord.Cause.ULTIMATE).map(SummonRecord::sourceId).findFirst().orElse(null);
        for (PieceState p : after.pieces().values().stream().sorted(Comparator.comparingLong(PieceState::id)).toList()) {
            PieceState old = before.pieces().get(p.id());
            if (p.status() == PieceState.Status.REPLACED && old != null && old.status() != PieceState.Status.REPLACED) {
                Position square = old.position() == null ? before.pieces().get(summoner).position() : old.position();
                addEvent(GameEvent.Kind.REPLACE, side, summoner, p.id(), square, square, "Фигура заменена призывом без взятия");
            }
        }
    }

    private void addActionEffects(HeroActionContext before, MechanicSession session) {
        var after = session.context();
        var action = lastHistoricalEvent();
        if (action != null && action.kind() == GameEvent.Kind.MOVE) {
            var illusion = before.pieceAt(action.to());
            if (illusion != null && illusion.hero() == HeroType.ILLUSION)
                addEvent(GameEvent.Kind.ILLUSION_HIT, action.color(), action.actorId(), illusion.id(), action.from(), action.to(),
                        "Попытка взять иллюзию: фигура вернулась назад");
        }
        addSummonChanges(before, after);
        for(var pulse:after.lightning().pulses().subList(before.lightning().pulses().size(),after.lightning().pulses().size())){
            var source=after.pieces().get(pulse.casterId());var oldSource=before.pieces().get(pulse.casterId());
            var kind=switch(pulse.kind()){case HIT->GameEvent.Kind.LIGHTNING_HIT;case RESET->GameEvent.Kind.LIGHTNING_RESET;case DISCHARGE->GameEvent.Kind.LIGHTNING_DISCHARGE;};
            addEvent(kind,source.color(),source.id(),pulse.targetId(),oldSource.position(),pulse.square(),switch(pulse.kind()){case HIT->"Заряд молнии на "+cell(pulse.square());case RESET->"Заряды применившего молнию героя сброшены";case DISCHARGE->"Разряд не уничтожил иллюзию";});
        }
        for(var t:after.mines().triggers().subList(before.mines().triggers().size(),after.mines().triggers().size())){
            var owner=after.pieces().get(t.ownerId());addEvent(GameEvent.Kind.MINE_TRIGGER,owner.color(),owner.id(),t.victimId(),t.square(),t.square(),"Мина взорвалась на "+cell(t.square()));
        }
        for(var b:after.mines().blasts().subList(before.mines().blasts().size(),after.mines().blasts().size())){
            var owner=after.pieces().get(b.ownerId());addEvent(GameEvent.Kind.BLAST_AREA,owner.color(),owner.id(),owner.id(),b.corner(),b.end(),"Область взрыва: "+cell(b.corner())+" — "+cell(b.end()));
        }
        for(var i:after.clown().illusions().values()){
            var old=before.clown().illusions().get(i.id());
            if(i.extended() && (old==null || !old.extended()) && events.stream().noneMatch(e->e.turn()==turnNumber && e.legacyKind()==GameEvent.Kind.ILLUSION_HIT && Objects.equals(e.targetId(),i.id()))){
                var p=after.pieces().get(i.id());var previous=before.pieces().get(i.id());
                Position square=p.position()!=null?p.position():previous!=null && previous.position()!=null?previous.position():i.bornAt();
                var trap=after.terrain().cells().get(square);if(trap==null)trap=before.terrain().cells().get(square);
                if(trap==null){
                    Long bomber=after.mines().triggers().stream().skip(before.mines().triggers().size()).filter(t->t.victimId()==i.id()).map(MineState.Trigger::ownerId).findFirst().orElse(null);
                    if(bomber==null)bomber=after.mines().blasts().stream().skip(before.mines().blasts().size()).filter(b->b.contains(square)).map(MineState.Blast::ownerId).findFirst().orElse(null);
                    if(bomber==null)bomber=after.lightning().pulses().stream().skip(before.lightning().pulses().size()).filter(t->t.kind()==LightningState.Kind.DISCHARGE && t.targetId()==i.id()).map(LightningState.Pulse::casterId).findFirst().orElse(null);
                    if(bomber!=null){var attacker=after.pieces().get(bomber);addEvent(GameEvent.Kind.ILLUSION_HIT,attacker.color(),bomber,i.id(),square,square,"Воздействие не уничтожило иллюзию; срок продлён");}
                }
                if(trap!=null){var attacker=after.pieces().get(trap.ownerId());addEvent(GameEvent.Kind.ILLUSION_HIT,attacker.color(),attacker.id(),i.id(),attacker.position()==null?square:attacker.position(),square,"Болото не взяло иллюзию; её срок продлён");}
            }
        }
        var last=lastHistoricalEvent();
        var yarnEvent=last!=null && last.isAbility(KikimoraAbility.YARN)?last:null;
        if(yarnEvent!=null){
            var actor=before.pieces().get(yarnEvent.actorId());var target=before.pieces().get(yarnEvent.targetId());
            for(var p:KikimoraAbilities.yarnTargets(before.pieces(),actor,target))addEvent(GameEvent.Kind.YARN_STUN,actor.color(),actor.id(),p.id(),actor.position(),p.position(),"Пряжа оглушила фигуру на "+cell(p.position()));
        }
        Long spearActor = lastHistoricalEvent().isAbility(AresAbility.SPEAR) ? events.getLast().actorId() : null;
        if (spearActor != null)
            for (PieceState p : after.pieces().values().stream().sorted(Comparator.comparingLong(PieceState::id)).toList()) {
                var old = before.pieces().get(p.id());
                if (p.isStunned() && old != null && p.stunnedThroughTurn() > old.stunnedThroughTurn())
                    addEvent(GameEvent.Kind.STUN, side, spearActor, p.id(), p.position(), p.position(), "Копьё оглушило фигуру на " + cell(p.position()));
            }
        var births = after.summons().records().values().stream().filter(r -> !before.summons().records().containsKey(r.id()))
                .sorted(Comparator.comparingLong(SummonRecord::id)).toList();
        var logged = new HashSet<Long>();
        for (var birth : births)
            if (birth.cause() == SummonRecord.Cause.ULTIMATE) {
                addBirth(after, birth);
                logged.add(birth.id());
            }
        for (var capture : session.captures())
            if (capture.reaction()) {
                // A fresh pawn can die in mud during its creation action; its birth must precede that capture in the journal.
                for (var birth : births)
                    if (birth.id() == capture.victimId() && logged.add(birth.id())) addBirth(after, birth);
                addEvent(capture.cause() == Capture.Cause.MECHANIC?GameEvent.Kind.CAPTURE:capture.cause() == Capture.Cause.LIGHTNING?GameEvent.Kind.LIGHTNING_CAPTURE:capture.cause()==Capture.Cause.MINE?GameEvent.Kind.MINE_CAPTURE:capture.cause()==Capture.Cause.BLAST?(capture.rewarded()?GameEvent.Kind.BLAST_CAPTURE:GameEvent.Kind.BLAST_DEATH):capture.cause()==Capture.Cause.ARES ? GameEvent.Kind.ARES_REVENGE : capture.cause() == Capture.Cause.BOG ? GameEvent.Kind.BOG_CAPTURE : GameEvent.Kind.COUNTER_CAPTURE,
                        capture.creditedSide(), capture.actorId(), capture.victimId(), capture.square(), capture.square(),
                        (capture.cause()==Capture.Cause.MECHANIC?"Эффект взял ":capture.cause()==Capture.Cause.LIGHTNING?"Молния уничтожила ":capture.cause()==Capture.Cause.MINE?"Мина взяла ":capture.cause()==Capture.Cause.BLAST?"Взрыв уничтожил ":capture.cause() == Capture.Cause.ARES ? "Ares забрал с собой " : capture.cause() == Capture.Cause.BOG ? "Bolotnik из болота взял " : "Союзник из Пуджа взял ") + capture.victim().getName() + " на " + cell(capture.square()));
            }
        for (var birth : births) if (logged.add(birth.id())) addBirth(after, birth);
    }

    private void addBirth(HeroActionContext after, SummonRecord r) {
        PieceState source = after.pieces().get(r.sourceId());
        addEvent(GameEvent.Kind.YAGA_BIRTH, source.color(), r.sourceId(), r.id(), r.bornAt(), r.bornAt(), "Баба-яга призвана на " + cell(r.bornAt()));
    }

}

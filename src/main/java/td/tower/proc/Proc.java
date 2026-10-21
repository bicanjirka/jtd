package td.tower.proc;

import td.term.TermText;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Trigger, conditions and payload: what a tower does on an attack beyond the hit itself.
 *
 * @param trigger    what sets it off
 * @param conditions what the attack must be as well
 * @param payload    what it does
 */
public record Proc(ProcTrigger trigger, List<ProcCondition> conditions, ProcPayload payload) {

    public Proc {
        conditions = List.copyOf(conditions);
    }

    public static Proc onHit(ProcPayload payload) {
        return new Proc(new ProcTrigger.OnHit(), List.of(), payload);
    }

    public static Proc onCrit(ProcPayload payload) {
        return new Proc(new ProcTrigger.OnCrit(), List.of(), payload);
    }

    public static Proc onKill(ProcPayload payload) {
        return new Proc(new ProcTrigger.OnKill(), List.of(), payload);
    }

    public static Proc onDodge(ProcPayload payload) {
        return new Proc(new ProcTrigger.OnDodge(), List.of(), payload);
    }

    public static Proc everyNthShot(int n, ProcPayload payload) {
        return new Proc(new ProcTrigger.EveryNthShot(n), List.of(), payload);
    }

    /** This proc, firing only when {@code condition} holds too. */
    public Proc when(ProcCondition condition) {
        List<ProcCondition> next = new ArrayList<>(this.conditions);
        next.add(condition);
        return new Proc(this.trigger, next, this.payload);
    }

    /** "a crit shot applies a stack of [Sunder]", "every 4th shot is a [Deadeye Round]". */
    public TermText describe() {
        Set<HitKind> kinds = EnumSet.noneOf(HitKind.class);
        this.conditions.forEach(condition -> kinds.addAll(condition.hitKinds()));
        TermText noun = kinds.isEmpty() ? TermText.empty() : ProcText.hits(kinds, "or", ProcText::noun);
        TermText on = TermText.join(" ", this.conditions.stream().map(ProcCondition::describe).toList());
        TermText when = this.trigger.describe(noun);
        TermText head = on.isEmpty() ? when : when.plus(" ").plus(on);
        return head.plus(" ")
                .plus(this.trigger.countsShots() ? this.payload.describeForTheShot() : this.payload.describe());
    }

    boolean holds(AttackOutcome outcome) {
        for (ProcCondition condition : this.conditions) {
            if (!condition.holds(outcome)) {
                return false;
            }
        }
        return true;
    }
}

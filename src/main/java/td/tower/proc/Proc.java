package td.tower.proc;

import td.term.TermText;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Trigger, conditions and payload: what a tower does on an attack beyond the hit itself.
 *
 * @param trigger    what sets it off
 * @param conditions what the attack must be as well
 * @param payload    what it does
 * @param name       what the player calls it, for one that has a name of its own rather than its node's
 */
public record Proc(ProcTrigger trigger, List<ProcCondition> conditions, ProcPayload payload, Optional<String> name) {

    public Proc {
        conditions = List.copyOf(conditions);
    }

    private Proc(ProcTrigger trigger, ProcPayload payload) {
        this(trigger, List.of(), payload, Optional.empty());
    }

    public static Proc onHit(ProcPayload payload) {
        return new Proc(new ProcTrigger.OnHit(), payload);
    }

    public static Proc onCrit(ProcPayload payload) {
        return new Proc(new ProcTrigger.OnCrit(), payload);
    }

    public static Proc onKill(ProcPayload payload) {
        return new Proc(new ProcTrigger.OnKill(), payload);
    }

    public static Proc everyNthShot(int n, ProcPayload payload) {
        return new Proc(new ProcTrigger.EveryNthShot(n), payload);
    }

    /** This proc, firing only when {@code condition} holds too. */
    public Proc when(ProcCondition condition) {
        List<ProcCondition> next = new ArrayList<>(this.conditions);
        next.add(condition);
        return new Proc(this.trigger, next, this.payload, this.name);
    }

    /** This proc, called {@code name}: its description starts with it. */
    public Proc named(String name) {
        return new Proc(this.trigger, this.conditions, this.payload, Optional.of(name));
    }

    /** "a crit shot applies a stack of [Sunder]", "every 4th shot is a [Fourth Shot]", "Rime: a hit on ...". */
    public TermText describe() {
        Set<HitKind> kinds = EnumSet.noneOf(HitKind.class);
        this.conditions.forEach(condition -> kinds.addAll(condition.hitKinds()));
        TermText noun = kinds.isEmpty() ? TermText.empty() : ProcText.hits(kinds, "or", ProcText::noun);
        TermText on = TermText.join(" ", this.conditions.stream().map(ProcCondition::describe).toList());
        TermText when = this.trigger.describe(noun);
        TermText head = on.isEmpty() ? when : when.plus(" ").plus(on);
        TermText what = head.plus(" ")
                .plus(this.trigger.countsShots() ? this.payload.describeForTheShot() : this.payload.describe());
        return this.name.map(called -> TermText.of(called + ": ").plus(what)).orElse(what);
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

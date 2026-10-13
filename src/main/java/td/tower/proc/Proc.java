package td.tower.proc;

import java.util.ArrayList;
import java.util.List;

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

    /** The label from the trigger and the conditions, the value from the payload. */
    public Description describe() {
        List<String> parts = new ArrayList<>();
        parts.add(this.trigger.describe());
        this.conditions.forEach(condition -> parts.add(condition.describe()));
        return new Description(String.join(", ", parts), this.payload.describe());
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

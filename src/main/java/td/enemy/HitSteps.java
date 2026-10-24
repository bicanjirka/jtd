package td.enemy;

import td.damage.Damage;
import td.damage.Delivery;

import java.util.ArrayList;
import java.util.List;

/**
 * A hit from what arrived to what {@link HitResolution} made of it, step by step: what a trace shows of
 * how the damage was reduced, before any shield and the cap at health.
 *
 * @param arrived  the damage as it arrived; critical if it came critical
 * @param delivery how it was delivered: a periodic hit never crits
 * @param steps    each step in formula order
 * @param result   what the formula landed, rounded
 */
public record HitSteps(Damage arrived, Delivery delivery, List<HitStep> steps, Damage result) {

    public HitSteps {
        steps = List.copyOf(steps);
    }

    public static HitSteps arriving(Damage arrived, Delivery delivery) {
        return new HitSteps(arrived, delivery, List.of(), arrived);
    }

    public HitSteps then(HitStep step) {
        List<HitStep> more = new ArrayList<>(this.steps);
        more.add(step);
        return new HitSteps(this.arrived, this.delivery, more, this.result);
    }

    public HitSteps resolvedTo(Damage result) {
        return new HitSteps(this.arrived, this.delivery, this.steps, result);
    }
}

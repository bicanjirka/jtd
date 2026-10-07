package td.tower.pulse;

import td.tower.targeting.Reach;
import td.tower.targeting.Viewpoint;

/**
 * Whom the Pulse's field touches and how Toll builds there: the base rules reshaped by each perk it
 * owns. Each {@code withX} is a copy.
 *
 * @param reach the enemies in the field
 * @param toll  how Toll builds
 */
public record PulseSpec(Reach reach, TollSpec toll) {

    /** Every enemy in range, hidden ones included, and no Toll. */
    public static PulseSpec from(Viewpoint view) {
        return new PulseSpec(Reach.everyone(view), TollSpec.none());
    }

    public PulseSpec withReach(Reach reach) {
        return new PulseSpec(reach, this.toll);
    }

    public PulseSpec withToll(TollSpec toll) {
        return new PulseSpec(this.reach, toll);
    }

    public Viewpoint view() {
        return this.reach.view();
    }
}

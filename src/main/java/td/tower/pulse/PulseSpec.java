package td.tower.pulse;

import td.tower.targeting.Reach;
import td.tower.targeting.Viewpoint;

/**
 * Whom the Pulse's field touches, how Toll builds there, how hard the field hits and what its zap
 * does: the base rules reshaped by each perk it owns. Each {@code withX} is a copy.
 *
 * @param reach the enemies in the field
 * @param toll  how Toll builds
 * @param field how hard the field hits
 * @param zap   the zap it fires once a second
 */
public record PulseSpec(Reach reach, TollSpec toll, FieldSpec field, ZapSpec zap) {

    /** Every enemy in range, hidden ones included, no Toll and no zap. */
    public static PulseSpec from(Viewpoint view) {
        return new PulseSpec(Reach.everyone(view), TollSpec.none(), FieldSpec.base(), ZapSpec.none());
    }

    public PulseSpec withReach(Reach reach) {
        return new PulseSpec(reach, this.toll, this.field, this.zap);
    }

    public PulseSpec withToll(TollSpec toll) {
        return new PulseSpec(this.reach, toll, this.field, this.zap);
    }

    public PulseSpec withField(FieldSpec field) {
        return new PulseSpec(this.reach, this.toll, field, this.zap);
    }

    public PulseSpec withZap(ZapSpec zap) {
        return new PulseSpec(this.reach, this.toll, this.field, zap);
    }

    public Viewpoint view() {
        return this.reach.view();
    }
}

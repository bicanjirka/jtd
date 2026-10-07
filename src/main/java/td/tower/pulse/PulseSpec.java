package td.tower.pulse;

import td.tower.targeting.Reach;
import td.tower.targeting.Viewpoint;

/**
 * Whom the Pulse's field touches, how Toll builds there, how hard the field hits, what its zap does,
 * what a visit starts with and which rules hold inside: the base rules reshaped by each perk it
 * owns. Each {@code withX} is a copy.
 *
 * @param reach the enemies in the field
 * @param toll  how Toll builds
 * @param field how hard the field hits
 * @param zap   the zap it fires once a second
 * @param visit what an enemy's first Toll stack of a visit does
 * @param modes the rules that hold while an enemy is inside
 */
public record PulseSpec(Reach reach, TollSpec toll, FieldSpec field, ZapSpec zap, VisitSpec visit, ModeSpec modes) {

    /** Every enemy in range, hidden ones included, no Toll, no zap and no rules beyond the damage. */
    public static PulseSpec from(Viewpoint view) {
        return new PulseSpec(Reach.everyone(view), TollSpec.none(), FieldSpec.base(), ZapSpec.none(),
                VisitSpec.none(), ModeSpec.none());
    }

    public PulseSpec withReach(Reach reach) {
        return new PulseSpec(reach, this.toll, this.field, this.zap, this.visit, this.modes);
    }

    public PulseSpec withToll(TollSpec toll) {
        return new PulseSpec(this.reach, toll, this.field, this.zap, this.visit, this.modes);
    }

    public PulseSpec withField(FieldSpec field) {
        return new PulseSpec(this.reach, this.toll, field, this.zap, this.visit, this.modes);
    }

    public PulseSpec withZap(ZapSpec zap) {
        return new PulseSpec(this.reach, this.toll, this.field, zap, this.visit, this.modes);
    }

    public PulseSpec withVisit(VisitSpec visit) {
        return new PulseSpec(this.reach, this.toll, this.field, this.zap, visit, this.modes);
    }

    public PulseSpec withModes(ModeSpec modes) {
        return new PulseSpec(this.reach, this.toll, this.field, this.zap, this.visit, modes);
    }

    public Viewpoint view() {
        return this.reach.view();
    }
}

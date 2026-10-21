package td.tower.pulse;

import td.tower.targeting.Reach;
import td.tower.targeting.Viewpoint;

/**
 * Whom the Pulse's field touches, how Toll builds there, how it grows, what its zap does, what a visit starts with,
 * which rules hold inside, how it holds the enemies, what it shows of them and the line it casts: the base rules reshaped by each mechanic it
 * owns. Each {@code withX} is a copy.
 *
 * @param reach the enemies in the field
 * @param tolls whether Toll builds on the enemies inside
 * @param field how the field grows within a wave
 * @param zap   the zap it fires once a second
 * @param visit what an enemy's first Toll stack of a visit does
 * @param modes the rules that hold while an enemy is inside
 * @param hold  how hard it drags, and whether it stills
 * @param sight what it reveals
 * @param line  the Ley Line it casts
 */
public record PulseSpec(Reach reach, boolean tolls, FieldSpec field, ZapSpec zap, VisitSpec visit, ModeSpec modes,
                        HoldSpec hold, SightSpec sight, LineSpec line) {

    /** Every enemy in range, hidden ones included, the base drag, and nothing else. */
    public static PulseSpec from(Viewpoint view) {
        return new PulseSpec(Reach.everyone(view), false, FieldSpec.base(), ZapSpec.none(), VisitSpec.none(),
                ModeSpec.none(), HoldSpec.base(), SightSpec.none(), LineSpec.none());
    }

    public PulseSpec withReach(Reach reach) {
        return new PulseSpec(reach, this.tolls, this.field, this.zap, this.visit, this.modes, this.hold, this.sight, this.line);
    }

    public PulseSpec tolling() {
        return new PulseSpec(this.reach, true, this.field, this.zap, this.visit, this.modes, this.hold, this.sight, this.line);
    }

    public PulseSpec withField(FieldSpec field) {
        return new PulseSpec(this.reach, this.tolls, field, this.zap, this.visit, this.modes, this.hold, this.sight, this.line);
    }

    public PulseSpec withZap(ZapSpec zap) {
        return new PulseSpec(this.reach, this.tolls, this.field, zap, this.visit, this.modes, this.hold, this.sight, this.line);
    }

    public PulseSpec withVisit(VisitSpec visit) {
        return new PulseSpec(this.reach, this.tolls, this.field, this.zap, visit, this.modes, this.hold, this.sight, this.line);
    }

    public PulseSpec withModes(ModeSpec modes) {
        return new PulseSpec(this.reach, this.tolls, this.field, this.zap, this.visit, modes, this.hold, this.sight, this.line);
    }

    public PulseSpec withHold(HoldSpec hold) {
        return new PulseSpec(this.reach, this.tolls, this.field, this.zap, this.visit, this.modes, hold, this.sight, this.line);
    }

    public PulseSpec withSight(SightSpec sight) {
        return new PulseSpec(this.reach, this.tolls, this.field, this.zap, this.visit, this.modes, this.hold, sight, this.line);
    }

    public PulseSpec withLine(LineSpec line) {
        return new PulseSpec(this.reach, this.tolls, this.field, this.zap, this.visit, this.modes, this.hold, this.sight, line);
    }

    public Viewpoint view() {
        return this.reach.view();
    }
}

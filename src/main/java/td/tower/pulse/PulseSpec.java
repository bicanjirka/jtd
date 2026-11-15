package td.tower.pulse;

import td.effect.EffectKind;
import td.tower.targeting.Reach;
import td.tower.targeting.Viewpoint;

/**
 * Whom the field touches and what it does to them, and the shadow it casts: the base rules reshaped by each mechanic
 * the Obelisk owns. Each {@code withX} is a copy.
 *
 * @param view     the field's centre and radius
 * @param reach    whom the field touches: everyone inside, hidden enemies included
 * @param tollCap  the Toll stacks the field builds an enemy to
 * @param dragCap  the most of an enemy's speed the field's drag takes
 * @param silences whether an enemy inside is Silenced
 * @param gravity  the Daze an enemy a beat inside takes, once a visit
 * @param sundial  the shadow the stone casts
 * @param knell    what each ring of the stone deals every enemy inside, in points; 0 for none
 * @param look     which stone it became
 */
public record PulseSpec(Viewpoint view, Reach reach, int tollCap, float dragCap, boolean silences,
                        GravitySpec gravity, SundialSpec sundial, float knell, StoneLook look) {

    /** The Toll a plain field builds an enemy to, and full Toll, which sets the knell ringing whatever the cap. */
    public static final int TOLL_CAP = 5;
    /** The most of an enemy's speed a plain field's drag takes. */
    public static final float DRAG_CAP = 0.5f;

    /** The plain stone's field around {@code view}. */
    public static PulseSpec from(Viewpoint view) {
        return new PulseSpec(view, Reach.everyone(view), TOLL_CAP, DRAG_CAP, false, GravitySpec.none(),
                SundialSpec.none(), 0f, StoneLook.PLAIN);
    }

    public PulseSpec withTollCap(int stacks) {
        return new PulseSpec(this.view, this.reach, stacks, this.dragCap, this.silences, this.gravity, this.sundial,
                this.knell, this.look);
    }

    public PulseSpec withDragCap(float share) {
        return new PulseSpec(this.view, this.reach, this.tollCap, share, this.silences, this.gravity, this.sundial,
                this.knell, this.look);
    }

    public PulseSpec withSilence() {
        return new PulseSpec(this.view, this.reach, this.tollCap, this.dragCap, true, this.gravity, this.sundial,
                this.knell, this.look);
    }

    public PulseSpec withGravity(GravitySpec next) {
        return new PulseSpec(this.view, this.reach, this.tollCap, this.dragCap, this.silences, next, this.sundial,
                this.knell, this.look);
    }

    public PulseSpec withSundial(SundialSpec next) {
        return new PulseSpec(this.view, this.reach, this.tollCap, this.dragCap, this.silences, this.gravity, next,
                this.knell, this.look);
    }

    public PulseSpec withKnell(float damagePoints) {
        return new PulseSpec(this.view, this.reach, this.tollCap, this.dragCap, this.silences, this.gravity,
                this.sundial, damagePoints, this.look);
    }

    public PulseSpec withLook(StoneLook next) {
        return new PulseSpec(this.view, this.reach, this.tollCap, this.dragCap, this.silences, this.gravity,
                this.sundial, this.knell, next);
    }

    /** Whether this field builds Toll past a plain field's cap. */
    public boolean deepToll() {
        return this.tollCap > TOLL_CAP;
    }

    /** The most Toll any field builds. */
    public static int deepestToll() {
        return EffectKind.TOLL.maxStacks();
    }
}

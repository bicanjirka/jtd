package td.tower.proc;

import td.term.TermText;


/**
 * A conditional change to one hit: when it applies, how many times, and what it adds.
 *
 * @param when       the condition the hit must meet
 * @param scaling    how many times it applies to a hit that meets it
 * @param adjustment what it adds each time
 */
public record HitBonus(HitCondition when, HitScaling scaling, HitAdjustment adjustment) {

    /** {@code adjustment} once on every hit that meets {@code condition}. */
    public static HitBonus when(HitCondition condition, HitAdjustment adjustment) {
        return new HitBonus(condition, new HitScaling.Once(), adjustment);
    }

    /** {@code adjustment} on every hit, as many times as {@code scaling} says. */
    public static HitBonus per(HitScaling scaling, HitAdjustment adjustment) {
        return new HitBonus(new HitCondition.Always(), scaling, adjustment);
    }

    public static HitBonus of(HitCondition condition, HitScaling scaling, HitAdjustment adjustment) {
        return new HitBonus(condition, scaling, adjustment);
    }

    /** {@link HitAdjustment#none()} when the condition fails, else the adjustment times the scaling. */
    public HitAdjustment adjustmentFor(HitContext context) {
        if (!this.when.holds(context)) {
            return HitAdjustment.none();
        }
        float factor = this.scaling.factor(context);
        return factor == 1f ? this.adjustment : this.adjustment.times(factor);
    }

    /** "+75% damage against Elite and Boss enemies", "+20% damage per stack of [Focus], up to 5". */
    public TermText describe() {
        TermText when = this.when.describe();
        TermText what = this.scaling.around(TermText.of(this.adjustment.describe()));
        return when.isEmpty() ? what : what.plus(" ").plus(when);
    }
}

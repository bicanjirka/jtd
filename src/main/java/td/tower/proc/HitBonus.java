package td.tower.proc;

import td.stat.StatModifiers;
import td.term.TermText;
import td.tower.TowerStat;


/**
 * A conditional change to one hit: when it applies, how many times, and what it adds.
 *
 * @param when       the condition the hit must meet
 * @param scaling    how many times it applies to a hit that meets it
 * @param adjustment what it adds each time
 */
public record HitBonus(HitCondition when, HitScaling scaling, HitAdjustment adjustment) {

    public HitBonus {
        if (adjustment.range() != 0f && (!when.everyHit() || scaling.focusCounter().isEmpty())) {
            throw new IllegalArgumentException("Range picks the target, so a bonus that adds it holds on every hit"
                    + " and scales by the tower's focus alone: " + when + ", " + scaling);
        }
    }

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

    /**
     * Whether it holds on every hit and grows with the tower's focus alone: what it adds is known before any target,
     * so it is part of the tower's stats ({@link #standingAt}) rather than of a hit.
     */
    public boolean standing() {
        return this.when.everyHit() && this.scaling.focusCounter().isPresent();
    }

    /** What it adds to the stats of a tower whose focus stands at {@code focus}. */
    public StatModifiers<TowerStat> standingAt(int focus) {
        double times = this.scaling.beforeTarget(focus).orElse(0);
        return times == 0 ? StatModifiers.none() : this.adjustment.times((float) times).asModifiers();
    }

    /** "+75% damage against Elite and Boss enemies", "+20% damage per stack of [Focus], up to 5". */
    public TermText describe() {
        TermText when = this.when.describe();
        TermText what = this.scaling.around(this.adjustment.describe());
        return when.isEmpty() ? what : what.plus(" ").plus(when);
    }
}

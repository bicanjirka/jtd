package td.tower.proc;

import java.util.List;
import java.util.stream.Stream;

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

    /** The label from the condition and the scaling, the value from the adjustment. */
    public Description describe() {
        List<String> parts = Stream.of(this.when.describe(), this.scaling.describe())
                .filter(part -> !part.isEmpty()).toList();
        String label = parts.isEmpty() ? "Every hit" : Words.capitalized(String.join(", ", parts));
        return new Description(label, this.adjustment.describe());
    }
}

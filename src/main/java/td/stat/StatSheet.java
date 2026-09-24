package td.stat;

import td.util.ThreadConfined;

import java.util.Arrays;

/**
 * One enemy's resolved stats. Values are cached until {@link #invalidate()}; the next read asks the
 * {@link StatContributor} for every modifier again and resolves all stats at once, reusing its own
 * arrays, so resolving allocates nothing.
 * <p>
 * A stat resolves as {@code (base + Σflat) * (1 + Σpercent) * Πmultiply}, then its spirit-scaled
 * restorative contributions, then the lowest set value if any, then its clamp. Spirit resolves
 * first, since it scales the others' restorative parts.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class StatSheet implements StatView {

    private static final int STAT_COUNT = EnemyStat.values().length;
    private static final EnemyStat[] STATS = EnemyStat.values();

    private final BaseStats base;
    private final StatContributor contributor;
    private final float[] flat = new float[STAT_COUNT];
    private final float[] percent = new float[STAT_COUNT];
    private final float[] multiply = new float[STAT_COUNT];
    private final float[] setTo = new float[STAT_COUNT];
    private final float[] restoreFlat = new float[STAT_COUNT];
    private final float[] restoreReduction = new float[STAT_COUNT];
    private final float[] resolved = new float[STAT_COUNT];
    private final StatAccumulator accumulator = new SheetAccumulator();

    private boolean dirty = true;

    public StatSheet(BaseStats base, StatContributor contributor) {
        this.base = base;
        this.contributor = contributor;
    }

    /** Marks every value stale; the next read resolves again. */
    public void invalidate() {
        this.dirty = true;
    }

    @Override
    public float value(EnemyStat stat) {
        if (this.dirty) {
            this.resolve();
            this.dirty = false;
        }
        return this.resolved[stat.ordinal()];
    }

    private void resolve() {
        Arrays.fill(this.flat, 0f);
        Arrays.fill(this.percent, 0f);
        Arrays.fill(this.multiply, 1f);
        Arrays.fill(this.setTo, Float.POSITIVE_INFINITY);
        Arrays.fill(this.restoreFlat, 0f);
        Arrays.fill(this.restoreReduction, 0f);
        this.contributor.contributeTo(this.accumulator);
        float spirit = this.resolveOne(EnemyStat.SPIRIT, 1f);
        float spiritFactor = Math.max(0f, 1f + spirit / 100f);
        for (EnemyStat stat : STATS) {
            this.resolved[stat.ordinal()] = stat == EnemyStat.SPIRIT ? spirit : this.resolveOne(stat, spiritFactor);
        }
    }

    private float resolveOne(EnemyStat stat, float spiritFactor) {
        int i = stat.ordinal();
        if (this.setTo[i] != Float.POSITIVE_INFINITY) {
            return stat.clamp(this.setTo[i]);
        }
        float value = (this.base.value(stat) + this.flat[i]) * (1f + this.percent[i]) * this.multiply[i];
        value += this.restoreFlat[i] * spiritFactor;
        value *= 1f - Math.min(1f, this.restoreReduction[i] * spiritFactor);
        return stat.clamp(value);
    }

    private final class SheetAccumulator implements StatAccumulator {

        @Override
        public void add(EnemyStat stat, StatModifier modifier) {
            int i = stat.ordinal();
            StatSheet.this.flat[i] += modifier.flat();
            StatSheet.this.percent[i] += modifier.percentAdd();
            StatSheet.this.multiply[i] *= modifier.multiply();
            StatSheet.this.setTo[i] = Math.min(StatSheet.this.setTo[i], modifier.setTo());
        }

        @Override
        public void multiply(EnemyStat stat, float factor) {
            StatSheet.this.multiply[stat.ordinal()] *= factor;
        }

        @Override
        public void restoreFlat(EnemyStat stat, float amount) {
            StatSheet.this.restoreFlat[stat.ordinal()] += amount;
        }

        @Override
        public void restoreReduction(EnemyStat stat, float fraction) {
            StatSheet.this.restoreReduction[stat.ordinal()] += fraction;
        }
    }
}

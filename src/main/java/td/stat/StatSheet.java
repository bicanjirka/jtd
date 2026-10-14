package td.stat;

import td.util.ThreadConfined;

import java.util.Arrays;

/**
 * One enemy's resolved stats. Values are cached until {@link #invalidate()}; the next read asks the
 * {@link StatContributor} for every modifier again and resolves all stats at once, reusing its own
 * arrays, so resolving allocates nothing.
 * <p>
 * A stat resolves as {@code (base + Σflat) * (1 + Σpercent) * Πmultiply}, then its restorative
 * contributions scaled by spirit and healing received, then the lowest set value if any, then its clamp. Spirit resolves
 * and healing received resolve first, since they scale the others' restorative parts.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class StatSheet implements StatView {

    private static final int STAT_COUNT = EnemyStat.values().length;
    private static final EnemyStat[] STATS = EnemyStat.values();

    private final BaseStats<EnemyStat> base;
    private final StatContributor<EnemyStat> contributor;
    private final float[] flat = new float[STAT_COUNT];
    private final float[] percent = new float[STAT_COUNT];
    private final float[] multiply = new float[STAT_COUNT];
    private final float[] setTo = new float[STAT_COUNT];
    private final float[] capShare = new float[STAT_COUNT];
    private final float[] restoreFlat = new float[STAT_COUNT];
    private final float[] resolved = new float[STAT_COUNT];
    private final StatAccumulator<EnemyStat> accumulator = new SheetAccumulator();

    private boolean dirty = true;

    public StatSheet(BaseStats<EnemyStat> base, StatContributor<EnemyStat> contributor) {
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
        Arrays.fill(this.capShare, Float.POSITIVE_INFINITY);
        Arrays.fill(this.restoreFlat, 0f);
        this.contributor.contributeTo(this.accumulator);
        float spirit = this.resolveOne(EnemyStat.SPIRIT, 1f);
        float received = this.resolveOne(EnemyStat.HEALING_RECEIVED, 0f);
        float restoreFactor = Math.max(0f, 1f + spirit / 100f) * received;
        for (EnemyStat stat : STATS) {
            this.resolved[stat.ordinal()] = switch (stat) {
                case SPIRIT -> spirit;
                case HEALING_RECEIVED -> received;
                default -> this.resolveOne(stat, restoreFactor);
            };
        }
    }

    private float resolveOne(EnemyStat stat, float restoreFactor) {
        int i = stat.ordinal();
        if (this.setTo[i] != Float.POSITIVE_INFINITY) {
            return stat.clamp(this.setTo[i]);
        }
        float value = (this.base.value(stat) + this.flat[i]) * (1f + this.percent[i]) * this.multiply[i];
        value += this.restoreFlat[i] * restoreFactor;
        if (this.capShare[i] != Float.POSITIVE_INFINITY) {
            value = Math.min(value, this.base.value(stat) * this.capShare[i]);
        }
        return stat.clamp(value);
    }

    private final class SheetAccumulator implements StatAccumulator<EnemyStat> {

        @Override
        public void add(EnemyStat stat, StatModifier modifier) {
            int i = stat.ordinal();
            StatSheet.this.flat[i] += modifier.flat();
            StatSheet.this.percent[i] += modifier.percentAdd();
            StatSheet.this.multiply[i] *= modifier.multiply();
            StatSheet.this.setTo[i] = Math.min(StatSheet.this.setTo[i], modifier.setTo());
            StatSheet.this.capShare[i] = Math.min(StatSheet.this.capShare[i], modifier.capShareOfBase());
        }

        @Override
        public void multiply(EnemyStat stat, float factor) {
            StatSheet.this.multiply[stat.ordinal()] *= factor;
        }

        @Override
        public void capAtShareOfBase(EnemyStat stat, float share) {
            int i = stat.ordinal();
            StatSheet.this.capShare[i] = Math.min(StatSheet.this.capShare[i], share);
        }

        @Override
        public void restoreFlat(EnemyStat stat, float amount) {
            StatSheet.this.restoreFlat[stat.ordinal()] += amount;
        }

    }
}

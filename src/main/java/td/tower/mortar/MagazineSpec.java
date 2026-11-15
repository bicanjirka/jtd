package td.tower.mortar;

import td.util.RandomSource;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * How the Mortar's magazine draws a new shell: each kind it owns by its weight, a kind's chance its weight over the sum
 * of them; with Fusion a Heavy drawn behind a Heavy fuses with it into a Nuke, and with Rich Mix a new shell is never
 * the kind of the one in front of it. Each {@code withX} is a copy.
 */
public final class MagazineSpec {

    private static final int NORMAL_WEIGHT = 9;
    private static final int HEAVY_WEIGHT = 1;

    private final EnumMap<ShellKind, Integer> weights;
    private final boolean fuses;
    private final boolean mixes;

    private MagazineSpec(EnumMap<ShellKind, Integer> weights, boolean fuses, boolean mixes) {
        this.weights = weights;
        this.fuses = fuses;
        this.mixes = mixes;
    }

    /** Normal and Heavy shells only, nine Normals to one Heavy. */
    public static MagazineSpec standard() {
        EnumMap<ShellKind, Integer> weights = new EnumMap<>(ShellKind.class);
        weights.put(ShellKind.NORMAL, NORMAL_WEIGHT);
        weights.put(ShellKind.HEAVY, HEAVY_WEIGHT);
        return new MagazineSpec(weights, false, false);
    }

    /** {@code kind} drawn {@code added} weight more often: a kind not owned yet joins the draw. */
    public MagazineSpec withWeight(ShellKind kind, int added) {
        EnumMap<ShellKind, Integer> next = new EnumMap<>(this.weights);
        next.merge(kind, added, Integer::sum);
        return new MagazineSpec(next, this.fuses, this.mixes);
    }

    public MagazineSpec fusing() {
        return new MagazineSpec(this.weights, true, this.mixes);
    }

    public MagazineSpec mixing() {
        return new MagazineSpec(this.weights, this.fuses, true);
    }

    public boolean fuses() {
        return this.fuses;
    }

    public boolean mixes() {
        return this.mixes;
    }

    public int weightOf(ShellKind kind) {
        return this.weights.getOrDefault(kind, 0);
    }

    /** Whether a shell of {@code kind} may stay in the magazine: it is drawn, or it is a Nuke and Heavies fuse. */
    public boolean owns(ShellKind kind) {
        return kind == ShellKind.NUKE ? this.fuses : this.weightOf(kind) > 0;
    }

    /** The kind a new slot behind {@code inFront} draws, by weight, one roll of {@code random}. */
    public ShellKind draw(Optional<ShellKind> inFront, RandomSource random) {
        int total = 0;
        for (Map.Entry<ShellKind, Integer> entry : this.weights.entrySet()) {
            total += this.counts(entry.getKey(), inFront) ? entry.getValue() : 0;
        }
        if (total == 0) {
            return inFront.isEmpty() ? ShellKind.NORMAL : this.draw(Optional.empty(), random);
        }
        double roll = random.nextDouble() * total;
        ShellKind last = ShellKind.NORMAL;
        for (Map.Entry<ShellKind, Integer> entry : this.weights.entrySet()) {
            if (!this.counts(entry.getKey(), inFront)) {
                continue;
            }
            last = entry.getKey();
            roll -= entry.getValue();
            if (roll < 0) {
                return last;
            }
        }
        return last;
    }

    /** The chance a new slot behind {@code inFront} draws {@code kind}. */
    float chanceOf(ShellKind kind, Optional<ShellKind> inFront) {
        int total = this.weights.keySet().stream().filter(each -> this.counts(each, inFront))
                .mapToInt(this::weightOf).sum();
        if (total == 0) {
            return inFront.isEmpty() ? (kind == ShellKind.NORMAL ? 1f : 0f) : this.chanceOf(kind, Optional.empty());
        }
        return this.counts(kind, inFront) ? (float) this.weightOf(kind) / total : 0f;
    }

    /** Whether {@code kind} is in the draw behind {@code inFront}: Rich Mix leaves out the kind in front. */
    private boolean counts(ShellKind kind, Optional<ShellKind> inFront) {
        return this.weightOf(kind) > 0 && !(this.mixes && inFront.isPresent() && inFront.get() == kind);
    }

    /**
     * The share of the shells it fires that are each kind, over a long run of draws: each kind's weight over the sum,
     * as Rich Mix's alternation and Fusion's pairs bend it. Every kind is listed, at zero when it never comes up.
     */
    public Map<ShellKind, Float> chances() {
        return MagazineOdds.of(this);
    }
}

package td.tower.seeker;

import td.util.RandomSource;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * How the Aerie's nest draws a new missile: Frost alone, or with a second kind drawn by lot, one in three or, rich,
 * one in two. Two specs that draw alike are equal.
 *
 * @param second the second kind, when one is owned
 * @param rich   whether the second kind comes up one in two
 */
public record BroodSpec(Optional<MissileKind> second, boolean rich) {

    private static final int ONE_IN = 3;
    private static final int RICH_ONE_IN = 2;

    /** Frost missiles only. */
    public static BroodSpec frostOnly() {
        return new BroodSpec(Optional.empty(), false);
    }

    /** {@code kind} drawn beside Frost. */
    public BroodSpec withSecond(MissileKind kind) {
        return new BroodSpec(Optional.of(kind), this.rich);
    }

    public BroodSpec richer() {
        return new BroodSpec(this.second, true);
    }

    /** The share of new missiles that are the second kind, when one is owned. */
    public float secondShare() {
        return this.second.isEmpty() ? 0f : 1f / (this.rich ? RICH_ONE_IN : ONE_IN);
    }

    /** The kind of a new missile, one roll of {@code random}; Frost alone rolls nothing. */
    public MissileKind draw(RandomSource random) {
        return this.second.map(kind -> random.nextDouble() < this.secondShare() ? kind : MissileKind.FROST)
                .orElse(MissileKind.FROST);
    }

    /** Whether a missile of {@code kind} may stay in the nest. */
    public boolean owns(MissileKind kind) {
        return kind == MissileKind.FROST || this.second.filter(owned -> owned == kind).isPresent();
    }

    /** The share of new missiles each kind it draws comes to, in the kinds' order. */
    public Map<MissileKind, Float> chances() {
        Map<MissileKind, Float> chances = new EnumMap<>(MissileKind.class);
        chances.put(MissileKind.FROST, 1f - this.secondShare());
        this.second.ifPresent(kind -> chances.put(kind, this.secondShare()));
        return chances;
    }
}

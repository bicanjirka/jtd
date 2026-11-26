package td.tower.seeker;

import java.util.Optional;

/**
 * How the Aerie's nest makes each new missile: Frost alone, or with a second kind in a fixed order, every third or,
 * rich, every second. Two specs that make alike are equal.
 *
 * @param second the second kind, when one is owned
 * @param rich   whether the second kind comes every second missile
 */
public record BroodSpec(Optional<MissileKind> second, boolean rich) {

    private static final int ONE_IN = 3;
    private static final int RICH_ONE_IN = 2;

    /** Frost missiles only. */
    public static BroodSpec frostOnly() {
        return new BroodSpec(Optional.empty(), false);
    }

    /** {@code kind} made beside Frost. */
    public BroodSpec withSecond(MissileKind kind) {
        return new BroodSpec(Optional.of(kind), this.rich);
    }

    public BroodSpec richer() {
        return new BroodSpec(this.second, true);
    }

    /** One missile in how many is the second kind. */
    public int oneIn() {
        return this.rich ? RICH_ONE_IN : ONE_IN;
    }

    /** The kind of the {@code made}-th missile it makes, counting from 1: the last of every {@link #oneIn()} is the second kind. */
    public MissileKind kindOf(int made) {
        return this.second.filter(_ -> made % this.oneIn() == 0).orElse(MissileKind.FROST);
    }

    /** Whether a missile of {@code kind} may stay in the nest. */
    public boolean owns(MissileKind kind) {
        return kind == MissileKind.FROST || this.second.filter(owned -> owned == kind).isPresent();
    }
}

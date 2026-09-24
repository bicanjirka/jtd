package td.wave;

import td.util.RandomSource;

/**
 * How a shape's members are offset from the spawn point: {@code x} forward, {@code y} lateral,
 * relative to the spawn-point facing, held for the whole run so a formation follows the path.
 */
public enum SpawnSpread {

    /** All members on the spawn point. */
    NONE {
        @Override
        public Vec2 offsetFor(int memberIndex, int members, double maxRadius, RandomSource random) {
            return new Vec2(0, 0);
        }
    },

    /** Members scattered semi-evenly across a disc; see {@link #sunflowerDisc}. */
    SCATTERED {
        @Override
        public Vec2 offsetFor(int memberIndex, int members, double maxRadius, RandomSource random) {
            return sunflowerDisc(memberIndex, members, maxRadius, random);
        }
    },

    /** Members evenly spaced across the footprint. */
    EVEN {
        @Override
        public Vec2 offsetFor(int memberIndex, int members, double maxRadius, RandomSource random) {
            if (members == 1) {
                return new Vec2(0, 0);
            }
            double fraction = (double) memberIndex / (members - 1);
            return new Vec2(0, (fraction * 2.0 - 1.0) * maxRadius);
        }
    },

    /** Exactly two members, on opposite edges. */
    EDGES {
        @Override
        public Vec2 offsetFor(int memberIndex, int members, double maxRadius, RandomSource random) {
            return new Vec2(0, memberIndex == 0 ? -maxRadius : maxRadius);
        }
    };

    // Vogel's sunflower-seed-head constant: the irrational angle that keeps any two members'
    // base angles from ever lining up at a nearby radius, no matter how many there are.
    private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));
    // Fractions of the room the base placement guarantees, so jitter cannot break separation.
    private static final double RADIUS_JITTER_FRACTION = 0.2;
    private static final double ANGLE_JITTER_FRACTION = 0.5;

    public abstract Vec2 offsetFor(int memberIndex, int members, double maxRadius, RandomSource random);

    /**
     * Scatters {@code members} points semi-evenly in a disc without any pairwise distance check.
     * Radius grows with {@code sqrt(i / members)} and the angle advances by the golden angle, which
     * alone keeps points apart; a small jitter bounded by that spacing breaks the spiral look.
     * Draws twice per member from {@code random}, in member order.
     */
    private static Vec2 sunflowerDisc(int memberIndex, int members, double maxRadius, RandomSource random) {
        double baseRadius = maxRadius * Math.sqrt((memberIndex + 0.5) / members);
        double baseAngle = memberIndex * GOLDEN_ANGLE;

        double radiusJitter = baseRadius * RADIUS_JITTER_FRACTION * (random.nextDouble() * 2.0 - 1.0);
        double angleJitter = (Math.PI / members) * ANGLE_JITTER_FRACTION * (random.nextDouble() * 2.0 - 1.0);

        double radius = clamp(baseRadius + radiusJitter, 0, maxRadius);
        double angle = baseAngle + angleJitter;
        return new Vec2(radius * Math.cos(angle), radius * Math.sin(angle));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }
}

package td.wave;

import td.util.RandomSource;

/**
 * How a {@link SpawnShape}'s members are displaced from the slot's own spawn point - each
 * member's offset is fixed relative to the mob's own spawn-facing direction ({@code x} =
 * forward, along the path at spawn; {@code y} = lateral, perpendicular to it) and held for its
 * whole run, so a formation follows the path around corners rather than smearing or rotating.
 * {@link AbstractEnemyMob} is what turns this into a fixed world-space vector, once, at
 * construction - this type only ever describes the *shape* of a formation. A closed set with
 * one body per constant, so a new pattern is a compile error at every switch over it rather
 * than a silent default.
 */
public enum SpawnSpread {

    /**
     * Every member sits on the spawn point - the identity, used by every single-member shape.
     */
    NONE {
        @Override
        public Vec2 offsetFor(int memberIndex, int members, double maxRadius, RandomSource random) {
            return new Vec2(0, 0);
        }
    },

    /**
     * Members fill a disc around the spawn point, semi-evenly rather than purely at random -
     * see the doc comment on {@link #sunflowerDisc}, which does the actual placement.
     */
    SCATTERED {
        @Override
        public Vec2 offsetFor(int memberIndex, int members, double maxRadius, RandomSource random) {
            return sunflowerDisc(memberIndex, members, maxRadius, random);
        }
    },

    /**
     * Members space out evenly across the footprint, deterministic rather than scattered.
     */
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

    /**
     * Exactly two members, hugging opposite edges of the footprint.
     */
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
     * Fills a disc of radius {@code maxRadius} with {@code members} points that read as
     * semi-evenly scattered - the reference is a sunflower's seed head, or the dot clusters a
     * "random dots in a circle" generator produces - with <strong>no pairwise distance check of
     * any kind</strong>. A member's *base* position alone already guarantees separation:
     * {@code baseRadius} grows with {@code sqrt(memberIndex / members)} so equal-area rings get
     * equal member counts, and each successive member's {@code baseAngle} advances by the
     * golden angle, the classic result that keeps a spiral from ever stacking two points at a
     * similar radius and angle. A small jitter on top - bounded by the room that base placement
     * already leaves - breaks the rigid spiral look without ever being able to threaten it, so
     * there is nothing left to check for overlap. Two draws from {@code random} per member,
     * consuming the same per-slot {@link RandomSource} every other member of this slot draws
     * from, in member order - see {@link Wave}'s scatter seed for why it is not
     * {@code GameWorld.random()}.
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

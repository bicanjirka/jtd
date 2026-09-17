package td.wave;

import td.util.RandomSource;

/**
 * How a {@link SpawnShape}'s members are displaced sideways from the path centre - a fixed
 * offset each member holds for its whole run, so a formation follows the path around corners
 * rather than smearing. A closed set with one body per constant, so a new pattern is a compile
 * error at every switch over it rather than a silent default.
 */
public enum LateralSpread {

    /**
     * Every member sits on the path centre - the identity, used by every single-member shape.
     */
    NONE {
        @Override
        public double offsetFor(int memberIndex, int members, double maxOffset, RandomSource random) {
            return 0.0;
        }
    },

    /**
     * Members scatter independently within the footprint, seeded so the same slot always
     * scatters the same way (see {@link Wave}'s scatter seed).
     */
    SCATTERED {
        @Override
        public double offsetFor(int memberIndex, int members, double maxOffset, RandomSource random) {
            return (random.nextDouble() * 2.0 - 1.0) * maxOffset;
        }
    },

    /**
     * Members space out evenly across the footprint, deterministic rather than scattered.
     */
    EVEN {
        @Override
        public double offsetFor(int memberIndex, int members, double maxOffset, RandomSource random) {
            if (members == 1) {
                return 0.0;
            }
            double fraction = (double) memberIndex / (members - 1);
            return (fraction * 2.0 - 1.0) * maxOffset;
        }
    },

    /**
     * Exactly two members, hugging opposite edges of the footprint.
     */
    EDGES {
        @Override
        public double offsetFor(int memberIndex, int members, double maxOffset, RandomSource random) {
            return memberIndex == 0 ? -maxOffset : maxOffset;
        }
    };

    public abstract double offsetFor(int memberIndex, int members, double maxOffset, RandomSource random);
}

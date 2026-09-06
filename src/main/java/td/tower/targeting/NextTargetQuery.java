package td.tower.targeting;

import td.util.Context;

import java.util.OptionalInt;

/**
 * A round-robin shape: "the next legal target strictly after this index",
 * scanning forward only, no wraparound. This is index-stable against
 * {@code Context}'s per-wave enemy array, which is exactly what a
 * {@link TargetQuery}'s fresh-snapshot {@code List} can't offer - the two
 * interfaces model genuinely different access patterns, not one interface
 * with an awkward extra method.
 */
public interface NextTargetQuery {
    OptionalInt nextIndexAfter(Context context, int index);
}

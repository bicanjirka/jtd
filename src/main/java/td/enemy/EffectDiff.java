package td.enemy;

import td.effect.EffectKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * The discrete changes between two looks at an enemy's effects: what a trace reports of them. A level that
 * only fades, and a clock that only runs down, are no change.
 */
final class EffectDiff {

    /** The cause an effect tick reports under; an effect that ends under it ran out. */
    static final String TICK = "tick";

    private EffectDiff() {
    }

    /** In kind order. */
    static List<MobEvent.EffectChanged> between(Map<EffectKind, EffectState> before, Map<EffectKind, EffectState> after,
                                                String cause) {
        List<MobEvent.EffectChanged> changes = new ArrayList<>();
        for (EffectKind kind : EffectKind.values()) {
            EffectState was = before.get(kind);
            EffectState now = after.get(kind);
            if (was == null && now != null) {
                changes.add(new MobEvent.EffectChanged(MobEvent.Change.GAINED, now, now, cause));
            } else if (was != null && now == null) {
                changes.add(new MobEvent.EffectChanged(MobEvent.Change.ENDED, was, was,
                        cause.equals(TICK) ? "expired" : cause));
            } else if (was != null) {
                changeOf(was, now, cause).ifPresent(change -> changes.add(new MobEvent.EffectChanged(change, was, now, cause)));
            }
        }
        return changes;
    }

    private static Optional<MobEvent.Change> changeOf(EffectState was, EffectState now, String cause) {
        if (was.stacks() != now.stacks()) {
            return Optional.of(MobEvent.Change.STACKS);
        }
        if (longer(now.remaining(), was.remaining())) {
            return Optional.of(MobEvent.Change.REFRESHED);
        }
        if (higher(now.level(), was.level())) {
            return Optional.of(MobEvent.Change.TOPPED_UP);
        }
        // A tick fades levels and a hit drains a shield: both are reported elsewhere, not as a change.
        if (higher(was.level(), now.level()) && !cause.equals(TICK) && !cause.equals(MobObserver.ABSORBED)) {
            return Optional.of(MobEvent.Change.WEAKENED);
        }
        return Optional.empty();
    }

    private static boolean longer(OptionalInt now, OptionalInt was) {
        return now.isPresent() && was.isPresent() && now.getAsInt() > was.getAsInt();
    }

    private static boolean higher(OptionalDouble a, OptionalDouble b) {
        return a.isPresent() && b.isPresent() && a.getAsDouble() > b.getAsDouble();
    }
}

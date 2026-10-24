package td.enemy;

import td.effect.ActiveEffects;
import td.effect.EffectKind;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/** A look at a mob's active effects, one {@link EffectState} per kind, for its trace. */
final class EffectStates {

    private EffectStates() {
    }

    static Map<EffectKind, EffectState> of(ActiveEffects effects) {
        Map<EffectKind, EffectState> states = new EnumMap<>(EffectKind.class);
        for (EffectKind kind : effects.activeKinds()) {
            OptionalDouble level = kind == EffectKind.SHIELD ? OptionalDouble.of(effects.shieldGauge().remaining())
                    : effects.fullness(kind);
            states.put(kind, new EffectState(kind, effects.stacks(kind), effects.remainingTicks(kind), level));
        }
        return Collections.unmodifiableMap(states);
    }

    /** {@code kind} when it isn't active. */
    static EffectState absent(EffectKind kind) {
        return new EffectState(kind, 0, OptionalInt.empty(), OptionalDouble.empty());
    }
}

package td.effect;

import td.util.TickRate;

/**
 * The stacking debuffs that last the same whoever adds them, each for its own time on its kind's one shared clock.
 * Naming a kind that isn't one of these doesn't compile.
 */
public enum StackingDebuff {
    VULNERABLE(EffectKind.VULNERABLE, 4f),
    SUNDERED(EffectKind.SUNDERED, 5f),
    RESONATING(EffectKind.RESONATING, 4f),
    UNRAVELED(EffectKind.UNRAVELED, 5f);

    private final EffectKind kind;
    private final int durationTicks;

    StackingDebuff(EffectKind kind, float seconds) {
        this.kind = kind;
        this.durationTicks = Math.round(seconds * TickRate.TICKS_PER_SECOND);
    }

    public EffectKind kind() {
        return this.kind;
    }

    /** {@code stacks} more of this debuff, its damage if any going to {@code sink}. */
    public Effect effect(int stacks, DamageSink sink) {
        return Effect.stacking(this.kind, stacks, this.durationTicks, sink);
    }
}

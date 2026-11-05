package td.effect;

import td.util.TickRate;

/**
 * The debuffs a tower adds by the stack, each lasting its own time on its kind's one shared clock. Only these can
 * be added that way, so naming a kind that has no such clock doesn't compile.
 */
public enum StackingDebuff {
    VULNERABLE(EffectKind.VULNERABLE, 4f, Effect::vulnerable),
    SUNDERED(EffectKind.SUNDERED, 5f, Effect::sundered),
    RESONATING(EffectKind.RESONATING, 4f, Effect::resonating),
    UNRAVELED(EffectKind.UNRAVELED, 5f, Effect::unraveled);

    private final EffectKind kind;
    private final int durationTicks;
    private final Factory factory;

    StackingDebuff(EffectKind kind, float seconds, Factory factory) {
        this.kind = kind;
        this.durationTicks = Math.round(seconds * TickRate.TICKS_PER_SECOND);
        this.factory = factory;
    }

    public EffectKind kind() {
        return this.kind;
    }

    /** {@code stacks} more of this debuff, its damage if any going to {@code sink}. */
    public Effect effect(int stacks, DamageSink sink) {
        return this.factory.of(stacks, this.durationTicks, sink);
    }

    private interface Factory {
        Effect of(int stacks, int durationTicks, DamageSink sink);
    }
}

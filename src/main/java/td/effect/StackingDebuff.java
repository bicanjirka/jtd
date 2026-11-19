package td.effect;

/**
 * The shared counts a tower's proc may add stacks to, each held and faded the same whoever adds them. Naming a kind
 * that isn't one of these doesn't compile.
 */
public enum StackingDebuff {
    SUNDERED(EffectKind.SUNDERED),
    UNRAVELED(EffectKind.UNRAVELED);

    private final EffectKind kind;

    StackingDebuff(EffectKind kind) {
        this.kind = kind;
    }

    public EffectKind kind() {
        return this.kind;
    }

    /** {@code stacks} more of this debuff. */
    public Effect effect(int stacks) {
        return Effect.counting(this.kind, stacks);
    }
}

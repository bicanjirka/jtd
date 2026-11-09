package td.effect;

/** {@code stacks} stacks of less damage taken for {@code durationTicks}, held at {@code stacks} by a recast. */
public record ResolveTemplate(int stacks, int durationTicks) implements EffectTemplate {

    @Override
    public Effect toEffect(DamageSink sink) {
        return Effect.resolve(this.stacks, this.durationTicks, sink);
    }

    @Override
    public EffectKind kind() {
        return EffectKind.RESOLVE;
    }
}

package td.effect;

/** Less damage taken, held at {@code stacks} for {@code durationTicks} by each recast, never added to. */
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

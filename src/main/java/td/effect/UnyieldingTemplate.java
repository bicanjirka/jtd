package td.effect;

/** A last stand that lasts {@code durationTicks}. */
public record UnyieldingTemplate(int durationTicks) implements EffectTemplate {

    @Override
    public Effect toEffect(DamageSink sink) {
        return Effect.unyielding(this.durationTicks, sink);
    }

    @Override
    public EffectKind kind() {
        return EffectKind.UNYIELDING;
    }
}

package td.effect;

public record SprintTemplate(int durationTicks) implements EffectTemplate {

    @Override
    public Effect toEffect(DamageSink sink) {
        return Effect.sprinting(this.durationTicks, sink);
    }

    @Override
    public EffectKind kind() {
        return EffectKind.SPRINTING;
    }
}

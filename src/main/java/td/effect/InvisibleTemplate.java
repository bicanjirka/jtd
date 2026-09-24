package td.effect;

public record InvisibleTemplate(int durationTicks) implements EffectTemplate {

    @Override
    public Effect toEffect(DamageSink sink) {
        return Effect.invisible(this.durationTicks, sink);
    }

    @Override
    public EffectKind kind() {
        return EffectKind.INVISIBLE;
    }
}

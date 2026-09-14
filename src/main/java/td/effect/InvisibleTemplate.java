package td.effect;

/** Applies temporary invisibility - see {@link ActiveEffects#isInvisible}. */
public record InvisibleTemplate(int durationTicks) implements EffectTemplate {

    @Override
    public Effect toEffect(DamageSink sink) {
        return Effect.invisible(this.durationTicks, sink);
    }
}

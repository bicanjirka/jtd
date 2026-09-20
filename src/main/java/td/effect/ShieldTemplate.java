package td.effect;

/**
 * Applies a timed, percentage damage-absorbing shield - see {@link ActiveEffects#applyShield}.
 */
public record ShieldTemplate(float percent, int durationTicks) implements EffectTemplate {

    @Override
    public Effect toEffect(DamageSink sink) {
        return Effect.shield(this.percent, this.durationTicks, sink);
    }

    @Override
    public EffectKind kind() {
        return EffectKind.SHIELD;
    }
}

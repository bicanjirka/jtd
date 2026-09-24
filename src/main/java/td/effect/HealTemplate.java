package td.effect;

/** Restores {@code healPerTick} health every tick for {@code durationTicks}. */
public record HealTemplate(int healPerTick, int durationTicks) implements EffectTemplate {

    @Override
    public Effect toEffect(DamageSink sink) {
        return Effect.heal(this.healPerTick, this.durationTicks, sink);
    }

    @Override
    public EffectKind kind() {
        return EffectKind.HEAL;
    }
}

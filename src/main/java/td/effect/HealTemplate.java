package td.effect;

import td.damage.DamageUnits;

/** Restores {@code pointsPerTick} points of health every tick for {@code durationTicks}. */
public record HealTemplate(float pointsPerTick, int durationTicks) implements EffectTemplate {

    @Override
    public Effect toEffect(DamageSink sink) {
        return Effect.heal(DamageUnits.ofPoints(this.pointsPerTick), this.durationTicks, sink);
    }

    @Override
    public EffectKind kind() {
        return EffectKind.HEAL;
    }
}

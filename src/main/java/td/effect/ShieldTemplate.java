package td.effect;

import td.damage.DamageType;

import java.util.Optional;

/**
 * Applies a timed, percentage damage-absorbing shield - see {@link ActiveEffects#applyShield}.
 * {@code restrictedTo}, when present, narrows the shield to one {@link DamageType} - a hit of
 * the other type is not absorbed at all. Empty (the 2-arg constructor, every pre-existing
 * caller's shape) absorbs both, unchanged from before this field existed - see
 * {@link #physicalOnly}/{@link #magicOnly}.
 */
public record ShieldTemplate(float percent, int durationTicks, Optional<DamageType> restrictedTo)
        implements EffectTemplate {

    public ShieldTemplate(float percent, int durationTicks) {
        this(percent, durationTicks, Optional.empty());
    }

    public static ShieldTemplate physicalOnly(float percent, int durationTicks) {
        return new ShieldTemplate(percent, durationTicks, Optional.of(DamageType.PHYSICAL));
    }

    public static ShieldTemplate magicOnly(float percent, int durationTicks) {
        return new ShieldTemplate(percent, durationTicks, Optional.of(DamageType.MAGIC));
    }

    @Override
    public Effect toEffect(DamageSink sink) {
        Effect effect = Effect.shield(this.percent, this.durationTicks, sink);
        return this.restrictedTo.map(effect::withShieldRestrictedTo).orElse(effect);
    }

    @Override
    public EffectKind kind() {
        return EffectKind.SHIELD;
    }
}

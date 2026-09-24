package td.effect;

import td.damage.DamageType;

import java.util.Optional;

/**
 * A timed percentage shield. {@code restrictedTo}, when present, limits it to one
 * {@link DamageType}.
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

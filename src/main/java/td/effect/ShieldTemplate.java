package td.effect;

import td.damage.DamageType;
import td.damage.DamageUnits;

import java.util.Optional;

/**
 * A shield that absorbs up to {@code points} of damage for {@code durationTicks}, or until broken. {@code restrictedTo}, when present,
 * limits it to one {@link DamageType}.
 */
public record ShieldTemplate(float points, int durationTicks, Optional<DamageType> restrictedTo)
        implements EffectTemplate {

    public ShieldTemplate(float points, int durationTicks) {
        this(points, durationTicks, Optional.empty());
    }

    /** A shield that holds until its points are spent. */
    public static ShieldTemplate untilBroken(float points) {
        return new ShieldTemplate(points, Effect.LASTING_TICKS);
    }

    public static ShieldTemplate physicalOnly(float points, int durationTicks) {
        return new ShieldTemplate(points, durationTicks, Optional.of(DamageType.PHYSICAL));
    }

    public static ShieldTemplate magicOnly(float points, int durationTicks) {
        return new ShieldTemplate(points, durationTicks, Optional.of(DamageType.MAGIC));
    }

    @Override
    public Effect toEffect(DamageSink sink) {
        Effect effect = Effect.shield(DamageUnits.ofPoints(this.points), this.durationTicks, sink);
        return this.restrictedTo.map(effect::withShieldRestrictedTo).orElse(effect);
    }

    @Override
    public EffectKind kind() {
        return EffectKind.SHIELD;
    }
}

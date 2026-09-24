package td.enemy;

import td.effect.EffectKind;
import td.stat.EnemyStat;
import td.stat.StatModifier;
import td.stat.StatModifiers;

/**
 * Resistance to one kind of effect: it shortens that effect's duration by {@code amount}, and at
 * {@code 1} blocks it outright.
 *
 * @param stat   one of the effect resistances, from {@link EffectKind#resistedBy()}
 * @param amount the fraction of the duration removed
 */
public record EffectResistTrait(EnemyStat stat, float amount) implements Trait {

    /** Full resistance to {@code kind}; throws if nothing resists that kind. */
    public static EffectResistTrait immuneTo(EffectKind kind) {
        return resisting(kind, 1f);
    }

    /** Resistance {@code amount} to {@code kind}; throws if nothing resists that kind. */
    public static EffectResistTrait resisting(EffectKind kind, float amount) {
        EnemyStat stat = kind.resistedBy()
                .orElseThrow(() -> new IllegalArgumentException(kind + " cannot be resisted"));
        return new EffectResistTrait(stat, amount);
    }

    @Override
    public StatModifiers modifiers(TraitContext context) {
        return StatModifiers.of(this.stat, StatModifier.flat(this.amount));
    }

    @Override
    public TraitLine describe() {
        String kind = TraitText.capitalized(TraitText.effectName(this.stat));
        if (this.amount >= 1f) {
            return TraitLine.of(this.marker(), kind + " immune");
        }
        return new TraitLine(this.marker(), kind + " resist", Math.round(this.amount * 100) + "%");
    }

    @Override
    public TraitMarker marker() {
        if (this.amount < 1f) {
            return TraitMarker.EFFECT_RESIST;
        }
        return switch (this.stat) {
            case BURN_RESIST -> TraitMarker.BURN_IMMUNE;
            case FREEZE_RESIST -> TraitMarker.FREEZE_IMMUNE;
            default -> TraitMarker.EFFECT_RESIST;
        };
    }
}

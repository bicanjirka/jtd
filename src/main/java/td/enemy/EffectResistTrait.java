package td.enemy;

import td.effect.EffectKind;
import td.stat.EnemyStat;
import td.stat.StatModifier;
import td.stat.StatModifiers;
import td.term.TermNumbers;
import td.term.TermText;

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
    public StatModifiers<EnemyStat> modifiers(TraitContext context) {
        return StatModifiers.of(this.stat, StatModifier.flat(this.amount));
    }

    @Override
    public TraitEntry entry() {
        EffectKind kind = EffectKind.resistedThrough(this.stat);
        TermText text = this.amount >= 1f ? TermText.of("immune to ").plus(kind)
                : TermText.of(kind).plus(" lasts " + TermNumbers.percent(this.amount) + " less on it");
        return new TraitEntry(kind, this.amount, text);
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

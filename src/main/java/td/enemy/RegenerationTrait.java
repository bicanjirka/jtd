package td.enemy;

import td.damage.DamageUnits;
import td.stat.EnemyStat;
import td.stat.StatModifier;
import td.stat.StatModifiers;
import td.term.TermNumbers;
import td.term.TermText;
import td.util.TickRate;

/** Regrows what it loses: {@code pointsPerSecond} health back every second, never past its maximum. */
public record RegenerationTrait(float pointsPerSecond) implements Trait {

    @Override
    public StatModifiers<EnemyStat> modifiers(TraitContext context) {
        return StatModifiers.of(EnemyStat.REGENERATION,
                StatModifier.flat(DamageUnits.ofPoints(this.pointsPerSecond) / TickRate.TICKS_PER_SECOND));
    }

    @Override
    public TraitEntry entry() {
        return TraitEntry.of(TraitTerm.REGENERATION,
                TermText.of("heals " + TermNumbers.number(this.pointsPerSecond) + " health a second"));
    }

    @Override
    public TraitMarker marker() {
        return TraitMarker.REGENERATION;
    }
}

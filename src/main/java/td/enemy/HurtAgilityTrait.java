package td.enemy;

import td.stat.EnemyStat;
import td.stat.StatModifier;
import td.stat.StatModifiers;
import td.term.TermNumbers;
import td.term.TermText;

/**
 * Evasive when hurt: agility grows as health drops, from none at full health to {@code maxAgility}
 * near death. Derived from the health fraction whenever the stats resolve, so it never drifts.
 */
public record HurtAgilityTrait(float maxAgility) implements Trait {

    @Override
    public StatModifiers<EnemyStat> modifiers(TraitContext context) {
        return StatModifiers.of(EnemyStat.AGILITY, StatModifier.flat(this.maxAgility * (1f - context.healthFraction())));
    }

    @Override
    public TraitLine describe() {
        return new TraitLine(this.marker(), "Evasive when hurt", "up to +" + Math.round(this.maxAgility) + " agility");
    }

    @Override
    public TraitEntry entry() {
        return TraitEntry.of(TraitTerm.ELUSIVE, TermText.of("gains agility as it is hurt, up to "
                + TermNumbers.signed(Math.round(this.maxAgility)) + " near death"));
    }

    @Override
    public TraitMarker marker() {
        return TraitMarker.HURT_AGILITY;
    }
}

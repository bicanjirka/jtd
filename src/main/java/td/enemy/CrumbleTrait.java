package td.enemy;

import td.damage.DamageUnits;
import td.stat.EnemyStat;
import td.stat.StatModifier;
import td.stat.StatModifiers;
import td.term.TermNumbers;
import td.term.TermText;
import td.util.TickRate;

/**
 * Comes apart on its own: {@code pointsPerSecond} health lost every second, as regeneration below zero. What holds it
 * back holds it on the board longer, so it crumbles further; a heal it is given offsets it.
 */
public record CrumbleTrait(float pointsPerSecond) implements Trait {

    public CrumbleTrait {
        if (pointsPerSecond <= 0f) {
            throw new IllegalArgumentException("A crumble loses health, was " + pointsPerSecond);
        }
    }

    @Override
    public StatModifiers<EnemyStat> modifiers(TraitContext context) {
        return StatModifiers.of(EnemyStat.REGENERATION,
                StatModifier.flat(-DamageUnits.ofPoints(this.pointsPerSecond) / TickRate.TICKS_PER_SECOND));
    }

    @Override
    public TraitEntry entry() {
        return TraitEntry.of(TraitTerm.CRUMBLE,
                TermText.of("loses " + TermNumbers.number(this.pointsPerSecond) + " health a second"));
    }
}

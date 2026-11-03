package td.enemy;

import td.stat.EnemyStat;
import td.stat.StatModifier;
import td.stat.StatModifiers;
import td.term.TermNumbers;
import td.term.TermText;

/**
 * Armor and magic resist for every ally standing near it, up to {@code mostAllies} of them: a pack is hard to wear
 * down, and one picked off alone is not.
 */
public record HivemindTrait(float resistPerAlly, int mostAllies) implements Trait {

    @Override
    public StatModifiers<EnemyStat> modifiers(TraitContext context) {
        int allies = Math.min(this.mostAllies, context.alliesNearby());
        if (allies == 0) {
            return StatModifiers.none();
        }
        StatModifier bonus = StatModifier.flat(this.resistPerAlly * allies);
        return StatModifiers.of(EnemyStat.ARMOR, bonus).and(EnemyStat.MAGIC_RESIST, bonus);
    }

    @Override
    public boolean readsAllies() {
        return true;
    }

    @Override
    public TraitEntry entry() {
        return TraitEntry.of(TraitTerm.HIVEMIND, TermText.of("+" + TermNumbers.number(this.resistPerAlly)
                + " armor and magic resist for every other enemy within " + TermNumbers.cells(TraitContext.ALLY_REACH_CELLS)
                + ", up to " + this.mostAllies + " of them"));
    }

    @Override
    public TraitMarker marker() {
        return TraitMarker.HIVEMIND;
    }
}

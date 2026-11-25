package td.tower.splash;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;
import td.tower.ability.Ability;
import td.tower.ability.AbilityCharge;

import java.util.Optional;

/** Arc: once its Sparks are in, the next stone carries lightning on from where it lands. */
public final class ArcMechanic implements SplashMechanic {

    private static final int COST = 1;

    private final AbilityCharge charge = new AbilityCharge(Ability.of(TowerTerm.ARC, TowerTerm.SPARK, COST));

    @Override
    public Optional<AbilityCharge> ability() {
        return Optional.of(this.charge);
    }

    @Override
    public boolean cast(CastActions actions) {
        return actions.primeArc();
    }

    @Override
    public TermText describe() {
        return TermText.of("costs " + COST + " ").plus(TowerTerm.SPARK).plus("s: the next stone carries an ").plus(TowerTerm.ARC)
                .plus(" that jumps " + ArcSpec.JUMPS + " times, to the nearest enemy within "
                        + TermNumbers.cells(ArcSpec.REACH_CELLS) + " it hasn't hit, each jump "
                        + TermNumbers.number(ArcSpec.POINTS) + " magic damage");
    }
}

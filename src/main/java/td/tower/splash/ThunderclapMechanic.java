package td.tower.splash;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;
import td.tower.ability.Ability;
import td.tower.ability.AbilityCharge;

import java.util.Optional;

/** Thunderclap: once its Sparks are in, the Stormcaller discharges into every enemy in range. */
public final class ThunderclapMechanic implements SplashMechanic {

    private static final int COST = 3;
    private static final float SHARE = 1f;

    private final AbilityCharge charge = new AbilityCharge(Ability.of(TowerTerm.THUNDERCLAP, TowerTerm.SPARK, COST));

    @Override
    public Optional<AbilityCharge> ability() {
        return Optional.of(this.charge);
    }

    @Override
    public boolean cast(CastActions actions) {
        return actions.thunderclap(SHARE);
    }

    @Override
    public TermText describe() {
        return TermText.of("costs " + COST + " ").plus(TowerTerm.SPARK).plus("s: a ").plus(TowerTerm.THUNDERCLAP)
                .plus(" into every enemy in range for " + TermNumbers.percent(SHARE) + " of base damage as magic");
    }
}

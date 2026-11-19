package td.tower.splash;

import td.effect.EffectKind;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;
import td.tower.ability.Ability;
import td.tower.ability.AbilityCharge;

import java.util.Optional;

/** Thunderstrike: once its Sparks are in, the Stormcaller calls lightning onto the healthiest enemy in range. */
public final class ThunderstrikeMechanic implements SplashMechanic {

    private static final int COST = 3;
    private static final float FACTOR = 4f;
    private static final float DAZE_SECONDS = 1.5f;

    private final AbilityCharge charge = new AbilityCharge(Ability.of(TowerTerm.THUNDERSTRIKE, TowerTerm.SPARK, COST));

    @Override
    public Optional<AbilityCharge> ability() {
        return Optional.of(this.charge);
    }

    @Override
    public boolean cast(CastActions actions) {
        return actions.thunderstrike(FACTOR, DAZE_SECONDS);
    }

    @Override
    public TermText describe() {
        return TermText.of("costs " + COST + " ").plus(TowerTerm.SPARK).plus("s: a ").plus(TowerTerm.THUNDERSTRIKE)
                .plus(" onto the enemy in range with the most health, for " + TermNumbers.percent(FACTOR)
                        + " of base damage as magic, leaving it ").plus(EffectKind.DAZED)
                .plus(" for " + TermNumbers.seconds(DAZE_SECONDS));
    }
}

package td.tower.splash;

import td.term.Term;
import td.term.TermText;
import td.tower.TowerTerm;
import td.tower.ability.Ability;
import td.tower.ability.AbilityCharge;

import java.util.Optional;
import java.util.Set;

/** A hex the Hexer casts once its Souls are in. */
public final class HexMechanic implements SplashMechanic {

    private final Hex hex;
    private final AbilityCharge charge;

    public HexMechanic(Hex hex, int cost) {
        this.hex = hex;
        this.charge = new AbilityCharge(Ability.of(hex.kind(), TowerTerm.SOUL, cost));
    }

    @Override
    public Optional<AbilityCharge> ability() {
        return Optional.of(this.charge);
    }

    @Override
    public boolean cast(CastActions actions) {
        return actions.hex(this.hex);
    }

    @Override
    public Set<Term> applies() {
        return this.hex.applies();
    }

    @Override
    public TermText describe() {
        return TermText.of("costs " + this.charge.ability().cost() + " ").plus(TowerTerm.SOUL)
                .plus("s: curses one enemy with ").plus(this.hex.describe());
    }
}

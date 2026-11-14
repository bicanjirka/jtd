package td.fixtures;

import td.term.Term;
import td.term.TermText;
import td.tower.ability.Ability;
import td.tower.ability.AbilityCharge;
import td.tower.upgrade.Mechanic;

import java.util.Locale;
import java.util.Optional;

/** A mechanic that only gives its tower an ability, for testing how abilities charge and cast apart from content. */
public final class FakeAbilityMechanic implements Mechanic {

    /** The currency every fake ability charges with. */
    public static final FakeTerm SPARK = new FakeTerm("fake.spark", "Spark", TermText.of("a fake currency"));

    private final AbilityCharge charge;

    private FakeAbilityMechanic(Ability ability) {
        this.charge = new AbilityCharge(ability);
    }

    /** An ability named {@code title} that costs {@code cost} Sparks. */
    public static FakeAbilityMechanic costing(String title, int cost) {
        FakeTerm term = new FakeTerm("fake." + title.toLowerCase(Locale.ROOT), title,
                TermText.of("a fake ability"));
        return new FakeAbilityMechanic(Ability.of(term, SPARK, cost));
    }

    /** An ability named and pictured as {@code term}, charging with {@code currency}: for a test that draws it. */
    public static FakeAbilityMechanic of(Term term, Term currency, int cost) {
        return new FakeAbilityMechanic(Ability.of(term, currency, cost));
    }

    @Override
    public Optional<AbilityCharge> ability() {
        return Optional.of(this.charge);
    }

    @Override
    public TermText describe() {
        return TermText.of("gives " + this.charge.ability().term().title());
    }
}

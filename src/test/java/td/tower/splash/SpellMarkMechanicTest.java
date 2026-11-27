package td.tower.splash;

import org.junit.jupiter.api.Test;
import td.fixtures.FakeAbilityMechanic;
import td.fixtures.FakeTerm;
import td.term.TermText;
import td.tower.TowerTerm;
import td.tower.ability.Ability;

import static org.assertj.core.api.Assertions.assertThat;

class SpellMarkMechanicTest {

    private static final FakeTerm NEW_SPELL = new FakeTerm("fake.new_spell", "New Spell", TermText.of("a fake ability"));

    @Test
    void anAbilityIsASpellByItsCurrencyAloneWhateverItIsCalled() {
        boolean sparks = SpellMarkMechanic.isSpell(Ability.of(NEW_SPELL, TowerTerm.SPARK, 1));
        boolean souls = SpellMarkMechanic.isSpell(Ability.of(NEW_SPELL, TowerTerm.SOUL, 1));
        boolean other = SpellMarkMechanic.isSpell(Ability.of(NEW_SPELL, FakeAbilityMechanic.SPARK, 1));

        assertThat(sparks).isTrue();
        assertThat(souls).isTrue();
        assertThat(other).isFalse();
    }
}

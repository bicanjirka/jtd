package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link FlatResistTrait}, tested directly against the trait rather than through a spawned mob -
 * unlike {@link PercentResistTraitTest}'s unrestricted case, nothing here needs a real
 * {@code EnemyDefinition} to prove {@code onHit}'s own arithmetic or its damage-kind guard.
 */
class FlatResistTraitTest {

    private final TraitContext traitContext = new TraitContext(1f);

    @Test
    void damageTakenIsReducedByTheFlatAmountAndClampedAtZero() {
        FlatResistTrait resist = new FlatResistTrait(30);

        assertThat(resist.onHit(Damage.physical(100), this.traitContext)).isEqualTo(Damage.physical(70));
        assertThat(resist.onHit(Damage.physical(10), this.traitContext)).isEqualTo(Damage.physical(0));
    }

    @Test
    void aPhysicalOnlyResistanceReducesPhysicalDamageButLeavesMagicUntouched() {
        FlatResistTrait resist = FlatResistTrait.physicalOnly(30);

        assertThat(resist.onHit(Damage.physical(100), this.traitContext)).isEqualTo(Damage.physical(70));
        assertThat(resist.onHit(Damage.magic(100), this.traitContext)).isEqualTo(Damage.magic(100));
    }
}

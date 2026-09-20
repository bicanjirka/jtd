package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PercentResistTrait}. The unrestricted case is exercised end to end through the built-in
 * Armored definition that carries it - {@code BuiltInEnemies.ARMORED}. There is no Armored class
 * to test: every real enemy is a {@link DefinedEnemyMob}, and what distinguishes one is its
 * {@link EnemyDefinition}'s traits. The damage-kind-restricted case is tested directly against
 * the trait, since it needs no enemy to carry it to prove {@code onHit}'s own guard.
 */
class PercentResistTraitTest {

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void damageTakenIsReducedByTheArmoredMobsResistance() {
        EnemyMob armored = EnemyFactory.getEnemy("s", context, 0, 100, 5, Rank.GRUNT);
        // healthMax = 100*100 = 10000; the fraction is 0.8, so 1000 raw damage becomes 800 applied
        armored.doDamage(Damage.physical(1000));

        assertThat(armored.getHealth()).isEqualTo(10000 - 800);
    }

    @Test
    void aPhysicalOnlyResistanceReducesPhysicalDamageButLeavesMagicUntouched() {
        PercentResistTrait resist = PercentResistTrait.physicalOnly(0.5f);
        TraitContext traitContext = new TraitContext(1f);

        assertThat(resist.onHit(Damage.physical(1000), traitContext)).isEqualTo(Damage.physical(500));
        assertThat(resist.onHit(Damage.magic(1000), traitContext)).isEqualTo(Damage.magic(1000));
    }
}

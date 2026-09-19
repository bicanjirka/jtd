package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PercentResistTrait}, exercised end to end through the built-in Armored definition that
 * carries it - {@code BuiltInEnemies.ARMORED}. There is no Armored class to test: every real
 * enemy is a {@link DefinedEnemyMob}, and what distinguishes one is its {@link EnemyDefinition}'s
 * traits. This is the trait's test, spawned the way the game spawns it.
 */
class PercentResistTraitTest {

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void damageTakenIsReducedByTheArmoredMobsResistance() {
        EnemyMob armored = EnemyFactory.getEnemy("s", context, 0, 100, 5, 1);
        // healthMax = 100*100 = 10000; K at level 1 is 0.8-0.05 = 0.75, so 1000 raw damage becomes 750 applied
        armored.doDamage(Damage.physical(1000));

        assertThat(armored.getHealth()).isEqualTo(10000 - 750);
    }

    @Test
    void higherLevelArmoredMobsTakeEvenLessDamage() {
        EnemyMob levelOne = EnemyFactory.getEnemy("s", context, 0, 1000, 5, 1);
        EnemyMob levelFive = EnemyFactory.getEnemy("s", context, 0, 1000, 5, 5);

        levelOne.doDamage(Damage.physical(1000));
        levelFive.doDamage(Damage.physical(1000));

        assertThat(levelFive.getHealth()).isGreaterThan(levelOne.getHealth());
    }
}

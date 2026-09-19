package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CriticalImmunityTrait}, exercised end to end through the built-in Armored definition
 * that carries it - {@code BuiltInEnemies.ARMORED} - the same shape {@link PercentResistTraitTest}
 * uses for the trait it shares that definition with.
 */
class CriticalImmunityTraitTest {

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void aCriticalHitAgainstAnArmoredMobLandsAtItsNonCriticalAmount() {
        EnemyMob armored = EnemyFactory.getEnemy("s", this.context, 0, 100000, 5, Rank.GRUNT);
        int healthBefore = armored.getHealth();

        Damage landed = armored.doDamage(Damage.physical(1000).asCritical());

        // PercentResistTrait still applies on top: 1000 * 0.8 = 800, unaffected by stripping the
        // crit bonus first, since stripCritical divides the amount, not the type.
        assertThat(landed.amount()).isEqualTo(800);
        assertThat(healthBefore - armored.getHealth()).isEqualTo(800);
    }

    @Test
    void anArmoredMobNeverSurvivesACriticalHitForAbilityPurposes() {
        DefinedEnemyMob armored = (DefinedEnemyMob) EnemyFactory.getEnemy("s", this.context, 0, 100000, 5, Rank.GRUNT);

        Damage landed = armored.doDamage(Damage.physical(1000).asCritical());

        assertThat(landed.critical()).isFalse();
    }
}

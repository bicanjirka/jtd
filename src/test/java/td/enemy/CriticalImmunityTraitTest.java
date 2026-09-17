package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CriticalImmunityTrait}, exercised end to end through the built-in Armored definition
 * that carries it - {@code BuiltInEnemies.ARMORED} - the same shape {@link PercentResistTraitTest}
 * uses for the trait it shares that definition with.
 */
class CriticalImmunityTraitTest {

    private final GameWorld context = new GameWorld(new RecordingGameHost());

    @Test
    void aCriticalHitAgainstAnArmoredMobLandsAtItsNonCriticalAmount() {
        EnemyMob armored = EnemyFactory.getEnemy("s", this.context, 0, 100000, 5, 1);
        int healthBefore = armored.getHealth();

        Damage landed = armored.doDamage(Damage.physical(1000).asCritical());

        // PercentResistTrait still applies on top: 1000 * 0.75 (level 1) = 750, unaffected by
        // stripping the crit bonus first, since stripCritical divides the amount, not the type.
        assertThat(landed.amount()).isEqualTo(750);
        assertThat(healthBefore - armored.getHealth()).isEqualTo(750);
    }

    @Test
    void anArmoredMobNeverSurvivesACriticalHitForAbilityPurposes() {
        DefinedEnemyMob armored = (DefinedEnemyMob) EnemyFactory.getEnemy("s", this.context, 0, 100000, 5, 1);

        Damage landed = armored.doDamage(Damage.physical(1000).asCritical());

        assertThat(landed.critical()).isFalse();
    }
}

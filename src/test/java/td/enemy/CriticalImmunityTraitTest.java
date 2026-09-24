package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

class CriticalImmunityTraitTest {

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void aCriticalHitAgainstAnArmoredMobLandsAtItsNonCriticalAmount() {
        EnemyMob armored = EnemyFactory.getEnemy("s", this.context, 0, 100000, 5, Rank.GRUNT);
        int healthBefore = armored.getHealth();

        Damage landed = armored.doDamage(Damage.physical(1000).asCritical());

        // 1000 * 0.8: resistance still applies once the crit bonus is stripped
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

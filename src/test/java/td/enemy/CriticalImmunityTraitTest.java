package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

class CriticalImmunityTraitTest {

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void aSureCritAgainstAnArmoredMobLandsAtItsNonCriticalAmount() {
        EnemyMob armored = EnemyFactory.getEnemy("s", this.context, 0, 100000, 5, Rank.GRUNT);
        int healthBefore = armored.getHealth();

        Damage landed = armored.doDamage(Damage.physical(1000), AttackProfile.critChance(1f));

        // 1000 * 0.8: resistance applies, the crit never rolls
        assertThat(landed.amount()).isEqualTo(800);
        assertThat(healthBefore - armored.getHealth()).isEqualTo(800);
    }

    @Test
    void anArmoredMobNeverSurvivesACriticalHitForAbilityPurposes() {
        DefinedEnemyMob armored = (DefinedEnemyMob) EnemyFactory.getEnemy("s", this.context, 0, 100000, 5, Rank.GRUNT);

        Damage landed = armored.doDamage(Damage.physical(1000), AttackProfile.critChance(1f));

        assertThat(landed.critical()).isFalse();
    }
}

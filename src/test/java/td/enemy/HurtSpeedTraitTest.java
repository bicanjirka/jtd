package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class HurtSpeedTraitTest {

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void speedIncreasesProportionallyAsTheFrenziedMobLosesHealth() {
        EnemyMob frenzied = EnemyFactory.getEnemy("t", context, 0, 100, 5, Rank.GRUNT);
        // healthMax = 100*100 = 10000; base speed 1.28, maxMultiplier 1.4, so at half health
        // speedFactor is 1 + (1.4-1)*(1-0.5) = 1.2, and speed is 1.28*1.2 = 1.536.
        frenzied.doDamage(Damage.physical(5000));

        assertThat(frenzied.getSpeed()).isCloseTo(1.536f, within(0.001f));
    }

    @Test
    void speedIsUnchangedBeforeAnyDamageIsTaken() {
        EnemyMob frenzied = EnemyFactory.getEnemy("t", context, 0, 100, 5, Rank.GRUNT);

        assertThat(frenzied.getSpeed()).isCloseTo(1.28f, within(0.001f));
    }
}

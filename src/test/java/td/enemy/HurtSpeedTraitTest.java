package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * {@link HurtSpeedTrait}, exercised end to end through the built-in Frenzied definition that
 * carries it - {@code BuiltInEnemies.FRENZIED}. There is no Frenzied class to test: every real
 * enemy is a {@link DefinedEnemyMob}, and what distinguishes one is its {@link EnemyDefinition}'s
 * traits. This is the trait's test, spawned the way the game spawns it.
 */
class HurtSpeedTraitTest {

    private final GameWorld context = new GameWorld(new RecordingGameHost());

    @Test
    void speedIncreasesProportionallyAsTheFrenziedMobLosesHealth() {
        EnemyMob frenzied = EnemyFactory.getEnemy("t", context, 0, 100, 5, 1);
        // healthMax = 100*100 = 10000; base speed 1.28, so the level-1 maximum is
        // 1.28*(1.4+0.1) = 1.92. Losing half its health -> 1.28 + (1.92-1.28)*(1-0.5) = 1.6
        frenzied.doDamage(Damage.physical(5000));

        assertThat(frenzied.getSpeed()).isCloseTo(1.6f, within(0.001f));
    }

    @Test
    void speedIsUnchangedBeforeAnyDamageIsTaken() {
        EnemyMob frenzied = EnemyFactory.getEnemy("t", context, 0, 100, 5, 1);

        assertThat(frenzied.getSpeed()).isCloseTo(1.28f, within(0.001f));
    }
}

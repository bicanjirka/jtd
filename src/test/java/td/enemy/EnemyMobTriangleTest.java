package td.enemy;

import org.junit.jupiter.api.Test;
import td.util.Context;
import td.util.RecordingGameHost;

import static org.assertj.core.api.Assertions.assertThat;

/** EnemyMobTriangle's distinguishing rule: it speeds up as it takes damage. */
class EnemyMobTriangleTest {

    private final Context context = new Context(new RecordingGameHost());

    @Test
    void speedIncreasesProportionallyAsTheTriangleLosesHealth() {
        EnemyMob triangle = EnemyFactory.getEnemy("t", context, 0, 100, 5, 1);
        // healthMax = 100*100 = 10000; speedMax at level 1 is 40*(1.4+0.1) = 60
        // losing half its health -> speed = 40 + (60-40)*(1-0.5) = 50
        triangle.doDamage(5000);

        assertThat(triangle.getSpeed()).isEqualTo(50);
    }

    @Test
    void speedIsUnchangedBeforeAnyDamageIsTaken() {
        EnemyMob triangle = EnemyFactory.getEnemy("t", context, 0, 100, 5, 1);

        assertThat(triangle.getSpeed()).isEqualTo(40);
    }
}

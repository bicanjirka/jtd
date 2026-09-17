package td.enemy;

import org.junit.jupiter.api.Test;
import td.board.BoardGeometry;
import td.damage.Damage;
import td.effect.Effect;
import td.util.GameWorld;
import td.util.RecordingGameHost;
import td.wave.PathNormal;
import td.wave.Vec2;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Exercises the effect wiring AbstractEnemyMob adds on top of movement and damage.
 */
class AbstractEnemyMobEffectTest {

    private static GameWorld newContext() {
        GameWorld context = new GameWorld(new RecordingGameHost());
        context.setBoard(BoardGeometry.of(1, 1001, 1001));
        return context;
    }

    private static PathNormal straightPath(int scale, int... xCoords) {
        List<Vec2> points = new ArrayList<>();
        for (int x : xCoords) {
            points.add(new Vec2(x * scale + (scale / 2), scale / 2));
        }
        return new PathNormal(points);
    }

    @Test
    void aSlowReducesTheDistanceCoveredInOneTick() {
        GameWorld context = newContext();
        context.setPath(straightPath(1, 0, 100));
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, 1);
        float fullSpeed = enemy.getSpeed();

        enemy.applyEffect(Effect.slow(0.5f, 5, d -> {
        }));
        enemy.doTick(1);

        assertThat(enemy.getX()).isCloseTo(fullSpeed * 0.5f, within(1e-6));
    }

    @Test
    void aFreezeHaltsMovementForExactlyItsDurationInTicks() {
        GameWorld context = newContext();
        context.setPath(straightPath(1, 0, 100));
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, 1);

        enemy.applyEffect(Effect.freeze(2, d -> {
        }));
        enemy.doTick(1);
        enemy.doTick(2);
        assertThat(enemy.getX()).isEqualTo(0.0);

        enemy.doTick(3); // the freeze has expired by now
        assertThat(enemy.getX()).isGreaterThan(0.0);
    }

    @Test
    void aBurnThatKillsTheEnemyStopsItsMovementInTheSameTick() {
        GameWorld context = newContext();
        context.enemies().setCount(1);
        context.setPath(straightPath(1, 0, 100));
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 1, 3, 1); // 100 health

        enemy.applyEffect(Effect.burn(Damage.magic(1000), 3, enemy::doDamage));
        enemy.doTick(1);

        assertThat(enemy.isDead()).isTrue();
        assertThat(enemy.getX()).isEqualTo(0.0);
    }

    @Test
    void aBurnEffectAppliesItsDamagePerTickThroughItsBoundSinkEveryTick() {
        GameWorld context = newContext();
        context.setPath(straightPath(1, 0, 100));
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 100, 3, 1); // 10000 health

        enemy.applyEffect(Effect.burn(Damage.magic(2000), 5, enemy::doDamage));
        enemy.doTick(1);

        assertThat(enemy.getHealth()).isEqualTo(8000);
    }
}

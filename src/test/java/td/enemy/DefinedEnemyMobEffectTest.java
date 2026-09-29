package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.effect.Effect;
import td.effect.EffectKind;
import td.fixtures.LevelFixtures;
import td.fixtures.WorldFixtures;
import td.stat.EnemyStat;
import td.util.GameWorld;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DefinedEnemyMobEffectTest {

    private static GameWorld newContext() {
        return WorldFixtures.newWorldOnBoard(1, 1001, 1001);
    }

    @Test
    void aSlowReducesTheDistanceCoveredInOneTick() {
        GameWorld context = newContext();
        context.setPath(LevelFixtures.straightPath(1, 0, 100));
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        float fullSpeed = enemy.getSpeed();

        enemy.applyEffect(Effect.chill(0.5f, 5, d -> {
        }));
        enemy.doTick(1);

        assertThat(enemy.getX()).isCloseTo(fullSpeed * 0.5f, within(1e-6));
    }

    @Test
    void aFreezeHaltsMovementForExactlyItsDurationInTicks() {
        GameWorld context = newContext();
        context.setPath(LevelFixtures.straightPath(1, 0, 100));
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);

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
        context.setPath(LevelFixtures.straightPath(1, 0, 100));
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 1, 3, Rank.GRUNT); // 100 health

        enemy.applyEffect(Effect.burn(Damage.magic(1000), 3, enemy::doDamage));
        enemy.doTick(1);

        assertThat(enemy.isDead()).isTrue();
        assertThat(enemy.getX()).isEqualTo(0.0);
    }

    @Test
    void aBurnEffectAppliesItsDamagePerTickThroughItsBoundSinkEveryTick() {
        GameWorld context = newContext();
        context.setPath(LevelFixtures.straightPath(1, 0, 100));
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 100, 3, Rank.GRUNT); // 10000 health

        enemy.applyEffect(Effect.burn(Damage.magic(2000), 5, enemy::doDamage));
        enemy.doTick(1);

        assertThat(enemy.getHealth()).isEqualTo(8000);
    }

    @Test
    void aHealEffectRestoresHealthEveryTickUntilItExpires() {
        GameWorld context = newContext();
        context.setPath(LevelFixtures.straightPath(1, 0, 100));
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 100, 3, Rank.GRUNT); // 10000 health
        enemy.doDamage(Damage.physical(4000)); // down to 6000

        enemy.applyEffect(Effect.heal(1000, 2, d -> {
        }));
        enemy.doTick(1);

        assertThat(enemy.getHealth()).isEqualTo(7000);
    }

    @Test
    void aHealEffectNeverRestoresHealthAboveTheMobsMax() {
        GameWorld context = newContext();
        context.setPath(LevelFixtures.straightPath(1, 0, 100));
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 100, 3, Rank.GRUNT); // 10000 health
        enemy.doDamage(Damage.physical(500)); // down to 9500

        enemy.applyEffect(Effect.heal(1000, 2, d -> {
        }));
        enemy.doTick(1);

        assertThat(enemy.getHealth()).isEqualTo(10000);
    }

    @Test
    void aHealEffectNeverAppliesToAMobAlreadyKilledThisTick() {
        GameWorld context = newContext();
        context.enemies().setCount(1);
        context.setPath(LevelFixtures.straightPath(1, 0, 100));
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 1, 3, Rank.GRUNT); // 100 health

        enemy.applyEffect(Effect.heal(1000, 5, d -> {
        }));
        enemy.doDamage(Damage.physical(100)); // kills it before this tick's doTick runs
        enemy.doTick(1);

        assertThat(enemy.isDead()).isTrue();
        assertThat(enemy.getHealth()).isZero();
    }

    @Test
    void anEnemyAtTheSpiritFloorNeverLosesItsScorchedStacksWhileOneAtNeutralSpiritDoes() {
        GameWorld context = newContext();
        context.setPath(LevelFixtures.straightPath(1, 0, 100));
        context.getEnemyCatalog().register(EnemyDefinition.of("brittle", "Brittle", 100_000, 5, 0f, BodyArchetype.CIRCLE)
                .withStat(EnemyStat.SPIRIT, -100f));
        context.getEnemyCatalog().register(EnemyDefinition.of("plain", "Plain", 100_000, 5, 0f, BodyArchetype.CIRCLE));
        DefinedEnemyMob brittle = (DefinedEnemyMob) context.getEnemyCatalog().spawn("brittle", context, 0, 100_000, 5, Rank.GRUNT);
        DefinedEnemyMob plain = (DefinedEnemyMob) context.getEnemyCatalog().spawn("plain", context, 0, 100_000, 5, Rank.GRUNT);
        for (DefinedEnemyMob mob : List.of(brittle, plain)) {
            mob.applyEffect(Effect.burn(Damage.magic(10), 30, d -> {
            }));
            for (int t = 1; t <= 40; t++) {
                mob.doTick(t);
            }
        }
        int brittleStacks = brittle.effectStacks(EffectKind.SCORCHED);
        int plainStacks = plain.effectStacks(EffectKind.SCORCHED);

        for (int t = 41; t <= 240; t++) {
            brittle.doTick(t);
            plain.doTick(t);
        }

        assertThat(brittleStacks).isGreaterThan(0);
        assertThat(brittle.effectStacks(EffectKind.SCORCHED)).isEqualTo(brittleStacks);
        assertThat(plainStacks).isGreaterThan(0);
        assertThat(plain.effectStacks(EffectKind.SCORCHED)).isZero();
    }
}

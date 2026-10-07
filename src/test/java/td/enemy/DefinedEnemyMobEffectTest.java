package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.AttackOrigin;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.effect.Effect;
import td.effect.EffectKind;
import td.fixtures.LevelFixtures;
import td.fixtures.WorldFixtures;
import td.stat.EnemyStat;
import td.util.GameWorld;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DefinedEnemyMobEffectTest {

    private static GameWorld newContext() {
        return WorldFixtures.newWorldOnBoard(1, 1001, 1001);
    }

    @Test
    void afterAFreezeTheNextOneIsDiminishedButAnEnemyNeverFrozenIsNot() {
        GameWorld context = newContext();
        context.setPath(LevelFixtures.straightPath(1, 0, 100));
        EnemyMob frozenOnce = EnemyFactory.getEnemy("c", context, 0, 50, 3, Rank.GRUNT);
        EnemyMob fresh = EnemyFactory.getEnemy("c", context, 0, 50, 4, Rank.GRUNT);

        frozenOnce.applyEffect(Effect.freeze(10, d -> {
        }));

        assertThat(frozenOnce.freezeDiminished()).isTrue();
        assertThat(fresh.freezeDiminished()).isFalse();
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
    void aBurnEffectDealsItsFirstPulseThroughItsBoundSinkOnItsFirstTick() {
        GameWorld context = newContext();
        context.setPath(LevelFixtures.straightPath(1, 0, 100));
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 100, 3, Rank.GRUNT); // 10000 health

        enemy.applyEffect(Effect.burn(Damage.magic(2000), 5, enemy::doDamage));
        enemy.doTick(1);

        // alpha = e^(-3/5); the pulse is 2000 * (1 + a + a^2 + a^3 + a^4) ~= 4212
        assertThat(enemy.getHealth()).isEqualTo(5788);
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
    void aHealTooSmallForOneUnitPerTickStillAddsUpAtReducedSpirit() {
        GameWorld context = newContext();
        context.setPath(LevelFixtures.straightPath(1, 0, 100));
        context.getEnemyCatalog().register(EnemyDefinition.of("dull", "Dull", 100_000, 5, 0f, BodyArchetype.CIRCLE)
                .withStat(EnemyStat.SPIRIT, -50f));
        EnemyMob enemy = context.getEnemyCatalog().spawn("dull", context, 0, 100_000, 5, Rank.GRUNT);
        enemy.doDamage(Damage.physical(4000));
        int damaged = enemy.getHealth();

        enemy.applyEffect(Effect.heal(1, 10, d -> {
        }));
        for (int t = 1; t <= 10; t++) {
            enemy.doTick(t);
        }

        assertThat(enemy.getHealth() - damaged).isEqualTo(5);
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

    @Test
    void aHitOnAMarkedEnemyIsAGuaranteedCritAndSpendsTheMark() {
        GameWorld context = WorldFixtures.newWorld(() -> 0.99);
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 100000, 3, Rank.GRUNT);
        enemy.applyEffect(Effect.marked(100, d -> {
        }));

        Damage first = enemy.doDamage(Damage.physical(1000), AttackProfile.none());
        Damage second = enemy.doDamage(Damage.physical(1000), AttackProfile.none());

        assertThat(first.critical()).isTrue();
        assertThat(second.critical()).isFalse();
        assertThat(enemy.activeEffectKinds()).doesNotContain(EffectKind.MARKED);
    }

    @Test
    void anotherAttackersHitDischargesAChargeForThirtyPercentThroughTheChargersSink() {
        GameWorld context = WorldFixtures.newWorld(() -> 0.99);
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 100000, 3, Rank.GRUNT);
        AttackOrigin charger = AttackOrigin.fresh();
        List<Damage> paid = new ArrayList<>();
        enemy.applyEffect(Effect.charged(100, charger, paid::add));

        enemy.doDamage(Damage.physical(1000), AttackProfile.none().withOrigin(charger));
        enemy.doDamage(Damage.physical(1000), AttackProfile.none().withOrigin(AttackOrigin.fresh()));
        enemy.doDamage(Damage.physical(1000), AttackProfile.none().withOrigin(AttackOrigin.fresh()));

        assertThat(paid).containsExactly(Damage.magic(300));
        assertThat(enemy.activeEffectKinds()).doesNotContain(EffectKind.CHARGED);
    }

    @Test
    void aCritDischargesAChargeAtDoubleAndPeriodicDamageNeverDischargesOne() {
        GameWorld context = WorldFixtures.newWorld(() -> 0.0);
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 100000, 3, Rank.GRUNT);
        List<Damage> paid = new ArrayList<>();
        enemy.applyEffect(Effect.charged(100, AttackOrigin.fresh(), paid::add));

        enemy.doDamage(Damage.physical(1000), AttackProfile.critChance(1f).asPeriodic());
        Damage crit = enemy.doDamage(Damage.physical(1000), AttackProfile.critChance(1f));

        assertThat(paid).containsExactly(Damage.magic(Math.round(crit.amount() * 0.6f)));
    }

    @Test
    void periodicDamageNeitherCritsNorSpendsAMark() {
        GameWorld context = WorldFixtures.newWorld(() -> 0.0);
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 100000, 3, Rank.GRUNT);
        enemy.applyEffect(Effect.marked(100, d -> {
        }));

        Damage landed = enemy.doDamage(Damage.physical(1000), AttackProfile.critChance(1f).asPeriodic());

        assertThat(landed.critical()).isFalse();
        assertThat(enemy.activeEffectKinds()).contains(EffectKind.MARKED);
    }

    @Test
    void aMarkWaitsOnACritImmuneEnemyUntilItsResilienceDrops() {
        GameWorld context = WorldFixtures.newWorld(() -> 0.99);
        EnemyMob armored = EnemyFactory.getEnemy("s", context, 0, 100000, 5, Rank.GRUNT);
        armored.applyEffect(Effect.marked(100, d -> {
        }));

        Damage blocked = armored.doDamage(Damage.physical(1000), AttackProfile.none());
        armored.applyEffect(Effect.fractured(5, d -> {
        }));
        Damage opened = armored.doDamage(Damage.physical(1000), AttackProfile.none());

        assertThat(blocked.critical()).isFalse();
        assertThat(opened.critical()).isTrue();
        assertThat(armored.activeEffectKinds()).doesNotContain(EffectKind.MARKED);
    }

    @Test
    void aRevealedEnemyIsExposedAndHidesAgainWhenItIsShroudedAgain() {
        GameWorld context = newContext();
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 100000, 3, Rank.GRUNT);
        enemy.applyEffect(Effect.invisible(100, d -> {
        }));
        enemy.applyEffect(Effect.revealed(100, d -> {
        }));
        boolean visibleWhileRevealed = enemy.canBeTargeted();

        enemy.applyEffect(Effect.invisible(100, d -> {
        }));

        assertThat(visibleWhileRevealed).isTrue();
        assertThat(enemy.canBeTargeted()).isFalse();
        assertThat(enemy.activeEffectKinds()).doesNotContain(EffectKind.REVEALED);
    }
}

package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.enemy.EnemyFactory;
import td.enemy.EnemyMob;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises AbstractTower's damage/range math and the TowerAura buff
 * mechanism through a concrete subclass (TowerOne). Lives in the same
 * package as AbstractTower so it can read the protected damageBase/
 * damageCurrent fields directly instead of parsing getStatusString().
 */
class AbstractTowerTest {

    private final GameWorld context = new GameWorld(new RecordingGameHost());

    @Test
    void sellPriceIsSeventyFivePercentOfPriceRoundedHalfUp() {
        TowerOne tower = new TowerOne(context, 0, 0);

        assertThat(tower.getSellPrice()).isEqualTo((int) Math.round(0.75 * TowerOne.price));
    }

    @Test
    void registerTowerAppliesAuraBuffToDamage() {
        TowerOne tower = new TowerOne(context, 0, 0);
        context.addTower(tower);

        // constructing a TowerAura in range scans context.towers and
        // registers itself with anything nearby, buffing it immediately
        new TowerAura(context, 0, 0);

        float expectedMultiplier = 1f + TowerAura.DEFAULT_POWER; // one aura tower registered
        assertThat(tower.damageCurrent).isEqualTo((int) (tower.damageBase * expectedMultiplier));
        assertThat(tower.damageCurrent).isNotEqualTo(tower.damageBase);
    }

    @Test
    void twoAuraTowersStackAdditively() {
        TowerOne tower = new TowerOne(context, 0, 0);
        context.addTower(tower);

        new TowerAura(context, 0, 0);
        new TowerAura(context, 0, 0);

        float expectedMultiplier = 1f + 2 * TowerAura.DEFAULT_POWER;
        assertThat(tower.damageCurrent).isEqualTo((int) (tower.damageBase * expectedMultiplier));
    }

    @Test
    void unequalAuraTowersStackTheirDifferentStrengths() {
        TowerOne tower = new TowerOne(context, 0, 0);
        context.addTower(tower);

        new TowerAura(context, 0, 0, 0.1f);
        new TowerAura(context, 0, 0, 0.3f);

        float expectedMultiplier = 1f + 0.1f + 0.3f;
        assertThat(tower.damageCurrent).isEqualTo((int) (tower.damageBase * expectedMultiplier));
    }

    @Test
    void unregisterTowerRevertsTheBuff() {
        TowerOne tower = new TowerOne(context, 0, 0);
        context.addTower(tower);
        TowerAura aura = new TowerAura(context, 0, 0);

        tower.unregisterTower(aura);

        assertThat(tower.damageCurrent).isEqualTo(tower.damageBase);
    }

    @Test
    void towerOutsideAuraRangeIsNotBuffed() {
        TowerOne near = new TowerOne(context, 0, 0);
        context.addTower(near);
        // TowerAura.range is 1.5 cells; placing far away puts this well outside it
        TowerOne far = new TowerOne(context, 100, 100);
        context.addTower(far);

        new TowerAura(context, 0, 0);

        assertThat(near.damageCurrent).isNotEqualTo(near.damageBase);
        assertThat(far.damageCurrent).isEqualTo(far.damageBase);
    }

    @Test
    void dealDamageTracksDamageDealtWithoutKillingTheTarget() {
        TowerOne tower = new TowerOne(context, 0, 0);
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 1000, 3, 1);

        tower.dealDamage(enemy, Damage.of(4000));

        assertThat(tower.getDamageDealt()).isEqualTo(4000);
        assertThat(tower.getKillCount()).isZero();
    }

    @Test
    void dealDamageCountsAKillWhenTheHitIsLethal() {
        TowerOne tower = new TowerOne(context, 0, 0);
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 10, 3, 1);
        long healthBefore = enemy.getHealth();

        tower.dealDamage(enemy, Damage.of(4000));

        assertThat(enemy.isDead()).isTrue();
        assertThat(tower.getKillCount()).isEqualTo(1);
        // the hit was far bigger than what was left, and only what was left counts
        assertThat(healthBefore).isLessThan(4000);
        assertThat(tower.getDamageDealt()).isEqualTo(healthBefore);
    }

    @Test
    void dealDamageIntoAnAlreadyDeadEnemyIsNotCountedAgain() {
        TowerOne tower = new TowerOne(context, 0, 0);
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 10, 3, 1);
        long healthBefore = enemy.getHealth();
        tower.dealDamage(enemy, Damage.of(4000));

        // simulates a second tower's shot landing on the same tick, after this one already killed it
        tower.dealDamage(enemy, Damage.of(4000));

        assertThat(tower.getKillCount()).isEqualTo(1);
        assertThat(tower.getDamageDealt()).isEqualTo(healthBefore);
    }

    @Test
    void aKilledEnemyIsLeftAtZeroHealthRatherThanNegative() {
        TowerOne tower = new TowerOne(context, 0, 0);
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 10, 3, 1);

        tower.dealDamage(enemy, Damage.of(4000));

        assertThat(enemy.getHealth()).isZero();
    }

    @Test
    void damageDealtAgainstAResistantEnemyMatchesTheHealthItActuallyLost() {
        TowerOne tower = new TowerOne(context, 0, 0);
        // a square absorbs part of every hit, unlike the circle every other case here uses
        EnemyMob square = EnemyFactory.getEnemy("s", context, 0, 1000, 3, 1);
        long healthBefore = square.getHealth();

        tower.dealDamage(square, Damage.of(4000));

        long healthLost = healthBefore - square.getHealth();
        assertThat(healthLost).isLessThan(4000);
        assertThat(tower.getDamageDealt()).isEqualTo(healthLost);
    }

    @Test
    void multipleHitsAccumulateDamageDealt() {
        TowerOne tower = new TowerOne(context, 0, 0);
        EnemyMob enemy = EnemyFactory.getEnemy("c", context, 0, 1000, 3, 1);

        tower.dealDamage(enemy, Damage.of(1000));
        tower.dealDamage(enemy, Damage.of(500));

        assertThat(tower.getDamageDealt()).isEqualTo(1500);
        assertThat(tower.getKillCount()).isZero();
    }
}

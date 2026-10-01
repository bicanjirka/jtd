package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.damage.DamageUnits;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyFactory;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.fixtures.BoardFixtures;
import td.fixtures.FakeEnemyMob;
import td.fixtures.TowerFixtures;
import td.fixtures.WorldFixtures;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeSlot;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

class MortarTowerTest {

    private final GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);

    private MortarTower towerAt(int cellX, int cellY) {
        return new MortarTower(this.context, cellX, cellY);
    }

    @Test
    void firingLaunchesExactlyOneShellAtTheTarget() {
        MortarTower tower = towerAt(3, 3);
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tower.doTick(1);

        assertThat(this.context.projectiles().getProjectiles()).hasSize(1);
    }

    @Test
    void theShellEventuallySplashesDamageAndSlowsTheTarget() {
        MortarTower tower = towerAt(3, 3);
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        tower.doTick(1);
        TowerFixtures.flyProjectilesToCompletion(this.context);

        assertThat(target.onlyHitAmount()).isEqualTo(DamageUnits.ofPoints(MortarTower.DAMAGE_POINTS));
        assertThat(target.appliedEffects()).hasSize(1);
        assertThat(target.appliedEffects().getFirst().kind()).isEqualTo(EffectKind.CHILL);
    }

    @Test
    void aGhostIsNeverSelectedAsTheInitialTargetButIsStillCaughtByTheSplash() {
        MortarTower tower = towerAt(3, 3);
        FakeEnemyMob normal = FakeEnemyMob.at(100, 100);
        FakeEnemyMob ghost = FakeEnemyMob.ghostAt(105, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{normal, ghost});

        tower.doTick(1);
        TowerFixtures.flyProjectilesToCompletion(this.context);

        assertThat(normal.hits()).hasSize(1);
        assertThat(ghost.hits()).hasSize(1);
    }

    @Test
    void noTargetInRangeFiresNoShell() {
        MortarTower tower = towerAt(3, 3);
        this.context.enemies().setEnemies(new EnemyMob[]{});

        tower.doTick(1);

        assertThat(this.context.projectiles().getProjectiles()).isEmpty();
    }

    @Test
    void siegeRoundsIsChoosableOnceAwakenIsBoughtAndAppliesItsDamageBonus() {
        this.context.economy().startEconomy(1000, 5);
        MortarTower tower = towerAt(3, 3);
        tower.buyUpgrade(UpgradePaths.named(tower, "Awaken"));
        EnemyMob fodder = EnemyFactory.getEnemy("c", this.context, 0, 100000, 3, Rank.GRUNT);
        tower.dealDamage(fodder, Damage.physical(16000));
        UpgradeNode siegeRounds = UpgradePaths.named(tower, "Siege Rounds");

        boolean chosen = tower.buyUpgrade(siegeRounds);

        assertThat(chosen).isTrue();
        assertThat(tower.damageCurrent()).isGreaterThan(tower.damageBase);
    }

    @Test
    void fragmentationRoundsIsNotYetChoosableBeforeTwelveKills() {
        this.context.economy().startEconomy(1000, 5);
        MortarTower tower = towerAt(3, 3);
        tower.buyUpgrade(UpgradePaths.named(tower, "Awaken"));
        UpgradeNode fragmentationRounds = UpgradePaths.named(tower, "Fragmentation Rounds");

        boolean chosen = tower.buyUpgrade(fragmentationRounds);

        assertThat(chosen).isFalse();
        assertThat(tower.upgrades().tip(UpgradeSlot.HEAD)).isEmpty();
    }

    @Test
    void siegeRoundsIiBumpsTheSplashRadiusBeyondTheBase() {
        MortarTower tower = towerAt(3, 3);
        UpgradeNode siegeRoundsTwo = UpgradePaths.named(tower, "Siege Rounds II");
        float radiusBeforeChoosing = tower.getSplashRadius();

        tower.onUpgradeBought(siegeRoundsTwo);

        assertThat(tower.getSplashRadius()).isGreaterThan(radiusBeforeChoosing);
    }

    @Test
    void cursedShrapnelAppliesAVulnerabilityStackToEveryEnemyCaughtInTheBlast() {
        MortarTower tower = new MortarTower(this.context, 3, 3);
        UpgradePaths.buy(tower, this.context, "Cursed Shrapnel");
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        FakeEnemyMob ghost = FakeEnemyMob.ghostAt(105, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{target, ghost});

        tower.doTick(1);
        TowerFixtures.flyProjectilesToCompletion(this.context);

        assertThat(target.appliedEffects()).extracting(Effect::kind).contains(EffectKind.VULNERABLE);
        assertThat(ghost.appliedEffects()).extracting(Effect::kind).contains(EffectKind.VULNERABLE);
    }

    @Test
    void fragmentationRoundsHitEnemiesInTheRingPastTheSplashForAQuarterDamageAndOnlyOnce() {
        MortarTower tower = towerAt(3, 3);
        UpgradePaths.buy(tower, this.context, "Fragmentation Rounds");
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        FakeEnemyMob ring = FakeEnemyMob.ghostAt(100, 100 + Math.round(MortarTower.SPLASH_RADIUS_BASE * BoardFixtures.SCALE) + 20);
        FakeEnemyMob outside = FakeEnemyMob.ghostAt(100, 400);
        this.context.enemies().setEnemies(new EnemyMob[]{target, ring, outside});

        tower.doTick(1);
        TowerFixtures.flyProjectilesToCompletion(this.context);

        assertThat(ring.onlyHitAmount()).isEqualTo(Math.round(tower.damageCurrent() * 0.25f));
        assertThat(target.hits()).hasSize(1);
        assertThat(outside.hits()).isEmpty();
    }

    @Test
    void fragmentationRoundsTwoSlowsTheRingForHalfTheSlowDuration() {
        for (int i = 0; i < 3; i++) {
            this.context.towers().add(new SniperTower(this.context, 3, 3));
        }
        MortarTower tower = towerAt(3, 3);
        this.context.towers().add(tower);
        UpgradePaths.buy(tower, this.context, "Fragmentation Rounds", "Fragmentation Rounds II");
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        FakeEnemyMob ring = FakeEnemyMob.ghostAt(100, 100 + Math.round(MortarTower.SPLASH_RADIUS_BASE * BoardFixtures.SCALE) + 20);
        this.context.enemies().setEnemies(new EnemyMob[]{target, ring});

        tower.doTick(1);
        TowerFixtures.flyProjectilesToCompletion(this.context);

        assertThat(ring.appliedEffects()).hasSize(1);
        assertThat(ring.appliedEffects().getFirst().kind()).isEqualTo(EffectKind.CHILL);
        assertThat(ring.appliedEffects().getFirst().remainingTicks()).isEqualTo(20);
        assertThat(target.appliedEffects().getFirst().remainingTicks()).isEqualTo(40);
    }
}

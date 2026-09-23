package td.tower;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyMob;
import td.fixtures.BoardFixtures;
import td.fixtures.WorldFixtures;
import td.tower.upgrade.UpgradeNode;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers SplashTower's splash falloff. The blast centre is whichever visible enemy the tower
 * picks at random, so every case here puts exactly one Normal mob in range - making it
 * necessarily the primary - and uses ghosts as the splash targets, since a ghost cannot be
 * chosen as the primary but is still caught by the any-type splash query.
 */
class SplashTowerTest {

    // SplashTower derives its blast radius from the board scale at construction.
    private static final float SPREAD_RADIUS = SplashTower.SPREAD_RADIUS_BASE * BoardFixtures.SCALE; // 56.0

    private final GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);

    private SplashTower towerNear(int cellX, int cellY) {
        return new SplashTower(this.context, cellX, cellY);
    }

    @Test
    void splashDamageFallsOffWithDistanceFromTheBlastCentre() {
        SplashTower tower = towerNear(3, 3);
        RecordingEnemyMob blastCentre = RecordingEnemyMob.normalAt(100, 100);
        RecordingEnemyMob quarterOut = RecordingEnemyMob.ghostAt(100, 128);
        RecordingEnemyMob threeQuartersOut = RecordingEnemyMob.ghostAt(100, 142);
        RecordingEnemyMob atTheEdge = RecordingEnemyMob.ghostAt(100, 155);
        this.context.enemies().setEnemies(new EnemyMob[]{blastCentre, quarterOut, threeQuartersOut, atTheEdge});

        tower.doTick(0);

        // damage * (1 - (d/radius)^2), radius 56 and base damage 1600:
        // d=0 -> 1600, d=28 -> 1200, d=42 -> 700, d=55 -> 57
        assertThat(blastCentre.onlyHitAmount()).isEqualTo(1600);
        assertThat(quarterOut.onlyHitAmount()).isEqualTo(1200);
        assertThat(threeQuartersOut.onlyHitAmount()).isEqualTo(700);
        assertThat(atTheEdge.onlyHitAmount()).isEqualTo(57);
    }

    @Test
    void everyStepAwayFromTheBlastCentreTakesStrictlyLessDamage() {
        SplashTower tower = towerNear(3, 3);
        RecordingEnemyMob blastCentre = RecordingEnemyMob.normalAt(100, 100);
        RecordingEnemyMob[] ring = new RecordingEnemyMob[11];
        for (int i = 0; i < ring.length; i++) {
            ring[i] = RecordingEnemyMob.ghostAt(100, 100 + i * 5);
        }
        EnemyMob[] all = new EnemyMob[ring.length + 1];
        all[0] = blastCentre;
        System.arraycopy(ring, 0, all, 1, ring.length);
        this.context.enemies().setEnemies(all);

        tower.doTick(0);

        for (int i = 1; i < ring.length; i++) {
            assertThat(ring[i].onlyHitAmount())
                    .as("mob %d cells further out than mob %d", i, i - 1)
                    .isLessThan(ring[i - 1].onlyHitAmount());
        }
    }

    @Test
    void theBlastCentreItselfTakesFullDamage() {
        SplashTower tower = towerNear(3, 3);
        RecordingEnemyMob blastCentre = RecordingEnemyMob.normalAt(100, 100);
        this.context.enemies().setEnemies(new EnemyMob[]{blastCentre});

        tower.doTick(0);

        assertThat(blastCentre.onlyHitAmount()).isEqualTo(SplashTower.DAMAGE);
    }

    @Test
    void anEnemyBeyondTheBlastRadiusIsNotHitAtAll() {
        SplashTower tower = towerNear(3, 3);
        RecordingEnemyMob blastCentre = RecordingEnemyMob.normalAt(100, 100);
        // one pixel past the radius, but still well inside the tower's own range
        RecordingEnemyMob outside = RecordingEnemyMob.ghostAt(100, 100 + SPREAD_RADIUS + 1);
        this.context.enemies().setEnemies(new EnemyMob[]{blastCentre, outside});

        tower.doTick(0);

        assertThat(blastCentre.hits()).hasSize(1);
        assertThat(outside.hits()).isEmpty();
    }

    @Test
    void siegeBumpsTheSpreadRadiusBeyondTheBase() {
        SplashTower tower = towerNear(3, 3);
        UpgradeNode siege = UpgradePaths.named(tower, "Siege");
        float radiusBeforeChoosing = tower.getSpreadRadius();

        // onUpgradeBought is exercised directly - Siege's own gate (a damage-dealt
        // threshold) is covered generically by DamageDealtConditionTest and by
        // AbstractTowerTest's condition-gating test; this proves the stat bump itself.
        tower.onUpgradeBought(siege);

        assertThat(tower.getSpreadRadius()).isGreaterThan(radiusBeforeChoosing);
    }

    @Test
    void clusterChargeIsChoosableOnceTwoNeighboursExistAndAppliesItsDamageAndRangeBonus() {
        this.context.economy().startEconomy(1000, 5);
        SplashTower tower = towerNear(3, 3);
        this.context.towers().add(tower);
        this.context.towers().add(new SniperTower(this.context, 2, 2));
        this.context.towers().add(new SniperTower(this.context, 4, 4));
        UpgradeNode clusterCharge = UpgradePaths.named(tower, "Cluster Charge");

        boolean chosen = tower.buyUpgrade(clusterCharge);

        assertThat(chosen).isTrue();
        assertThat(tower.damageCurrent()).isGreaterThan(tower.damageBase);
        assertThat(tower.getRangeReal()).isGreaterThan(SplashTower.RANGE * BoardFixtures.SCALE);
    }
}

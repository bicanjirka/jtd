package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.enemy.EnemyFactory;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.fixtures.BoardFixtures;
import td.fixtures.FakeEnemyMob;
import td.fixtures.WorldFixtures;
import td.tower.upgrade.UpgradeNode;
import td.util.GameWorld;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Scale 32. A 2-second revolution at 20 ticks per second is 40 ticks, so a quarter turn is 10. The
 * scan starts pointing east.
 */
class SonarTowerTest {

    private static final int TICKS_PER_REVOLUTION = 40;
    // cell (3,3) centre, in board pixels
    private static final int TOWER_X = 3 * BoardFixtures.SCALE + BoardFixtures.SCALE / 2;
    private static final int TOWER_Y = 3 * BoardFixtures.SCALE + BoardFixtures.SCALE / 2;
    private static final int NEAR = 50;

    private final GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);

    private static int hitCount(FakeEnemyMob mob) {
        return mob.hits().size();
    }

    private SonarTower tower() {
        return new SonarTower(this.context, 3, 3);
    }

    @Test
    void enemiesAreSweptInCounterclockwiseOrderOfTheirBearingNotOfTheirWaveOrder() {
        SonarTower tower = tower();
        FakeEnemyMob east = FakeEnemyMob.at(TOWER_X + NEAR, TOWER_Y);
        FakeEnemyMob north = FakeEnemyMob.at(TOWER_X, TOWER_Y - NEAR);
        FakeEnemyMob west = FakeEnemyMob.at(TOWER_X - NEAR, TOWER_Y);
        FakeEnemyMob south = FakeEnemyMob.at(TOWER_X, TOWER_Y + NEAR);
        // deliberately not in sweep order: where they sit decides, not where they are in the array
        this.context.enemies().setEnemies(new EnemyMob[]{south, west, north, east});

        Map<FakeEnemyMob, Integer> firstHit = new LinkedHashMap<>();
        for (int tick = 1; tick <= TICKS_PER_REVOLUTION; tick++) {
            tower.doTick(tick);
            for (FakeEnemyMob mob : List.of(east, north, west, south)) {
                if (!mob.hits().isEmpty()) {
                    firstHit.putIfAbsent(mob, tick);
                }
            }
        }

        // east is where the scan starts; then a quarter turn counterclockwise each time
        assertThat(firstHit.get(east)).isEqualTo(1);
        assertThat(firstHit.get(north)).isEqualTo(11);
        assertThat(firstHit.get(west)).isEqualTo(21);
        assertThat(firstHit.get(south)).isEqualTo(31);
    }

    @Test
    void aStationaryEnemyIsHitExactlyOncePerRevolution() {
        SonarTower tower = tower();
        FakeEnemyMob enemy = FakeEnemyMob.at(TOWER_X, TOWER_Y - NEAR);
        this.context.enemies().setEnemies(new EnemyMob[]{enemy});

        for (int tick = 1; tick <= TICKS_PER_REVOLUTION * 3; tick++) {
            tower.doTick(tick);
        }

        assertThat(hitCount(enemy)).isEqualTo(3);
    }

    @Test
    void everyEnemyOnTheSameBearingIsHitOnTheSameTick() {
        SonarTower tower = tower();
        // same direction from the tower, different distances - the beam is a ray, not one target
        FakeEnemyMob close = FakeEnemyMob.at(TOWER_X, TOWER_Y - 40);
        FakeEnemyMob far = FakeEnemyMob.at(TOWER_X, TOWER_Y - 80);
        this.context.enemies().setEnemies(new EnemyMob[]{close, far});

        for (int tick = 1; tick <= TICKS_PER_REVOLUTION; tick++) {
            tower.doTick(tick);
        }

        assertThat(hitCount(close)).isEqualTo(1);
        assertThat(hitCount(far)).isEqualTo(1);
    }

    @Test
    void anEnemyBeyondTheTowersRangeIsNeverHit() {
        SonarTower tower = tower();
        // range is 5.2 cells = 166.4px at this scale
        FakeEnemyMob outOfRange = FakeEnemyMob.at(TOWER_X + 200, TOWER_Y);
        this.context.enemies().setEnemies(new EnemyMob[]{outOfRange});

        for (int tick = 1; tick <= TICKS_PER_REVOLUTION; tick++) {
            tower.doTick(tick);
        }

        assertThat(outOfRange.hits()).isEmpty();
    }

    @Test
    void aGhostIsNeverHitBecauseTheScanOnlySeesVisibleEnemies() {
        SonarTower tower = tower();
        FakeEnemyMob ghost = FakeEnemyMob.ghostAt(TOWER_X + NEAR, TOWER_Y);
        this.context.enemies().setEnemies(new EnemyMob[]{ghost});

        for (int tick = 1; tick <= TICKS_PER_REVOLUTION; tick++) {
            tower.doTick(tick);
        }

        assertThat(ghost.hits()).isEmpty();
    }

    @Test
    void anEnemyThatTheBeamWouldHaveJumpedOverBetweenTicksIsStillHit() {
        SonarTower tower = tower();
        // a bearing deliberately off any exact tick boundary, so a naive "is the beam pointing
        // at it right now" test would step straight past it
        double awkward = -(Math.PI * 2 / TICKS_PER_REVOLUTION) * 4.37;
        FakeEnemyMob enemy = FakeEnemyMob.at(
                TOWER_X + Math.cos(awkward) * NEAR, TOWER_Y + Math.sin(awkward) * NEAR);
        this.context.enemies().setEnemies(new EnemyMob[]{enemy});

        for (int tick = 1; tick <= TICKS_PER_REVOLUTION; tick++) {
            tower.doTick(tick);
        }

        assertThat(hitCount(enemy)).isEqualTo(1);
    }

    @Test
    void theTurretHeadPointsWhereTheBeamIs() {
        SonarTower tower = tower();
        this.context.enemies().setEnemies(new EnemyMob[]{});

        tower.doTick(1);

        double start = tower.sweepRadiansAt(0);
        double end = tower.sweepRadiansAt(1);
        // counterclockwise on screen is a decreasing angle, a fortieth of a turn per tick
        assertThat(start - end).isCloseTo(Math.PI * 2 / TICKS_PER_REVOLUTION, within(1e-9));
    }

    @Test
    void theStatusReportsARotationSpeedRatherThanAFireRate() {
        SonarTower tower = tower();

        assertThat(tower.getStatusString()).contains("Rotation");
        assertThat(tower.getStatusString()).doesNotContain("Fire rate");
    }

    @Test
    void twinArrayIsChoosableOnceAwakenIsBoughtAndAppliesItsDamageBonus() {
        this.context.economy().startEconomy(1000, 5);
        SonarTower tower = tower();
        tower.buyUpgrade(UpgradePaths.named(tower, "Awaken"));
        EnemyMob fodder = EnemyFactory.getEnemy("c", this.context, 0, 100000, 3, Rank.GRUNT);
        tower.dealDamage(fodder, Damage.physical(11000));
        UpgradeNode twinArray = UpgradePaths.named(tower, "Twin Array");

        boolean chosen = tower.buyUpgrade(twinArray);

        assertThat(chosen).isTrue();
        assertThat(tower.damageCurrent()).isGreaterThan(tower.damageBase);
    }

    @Test
    void longReachIsNotYetChoosableBeforeTenKills() {
        this.context.economy().startEconomy(1000, 5);
        SonarTower tower = tower();
        tower.buyUpgrade(UpgradePaths.named(tower, "Awaken"));
        UpgradeNode longReach = UpgradePaths.named(tower, "Long Reach");

        boolean chosen = tower.buyUpgrade(longReach);

        assertThat(chosen).isFalse();
    }
}

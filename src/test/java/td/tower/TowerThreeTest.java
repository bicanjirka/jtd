package td.tower;

import org.junit.jupiter.api.Test;
import td.board.BoardGeometry;
import td.enemy.EnemyMob;
import td.tower.upgrade.UpgradePath;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Covers TowerThree's sonar scan. The board scale is 32 and the scan takes 2 seconds at 20
 * ticks a second, so a revolution is 40 ticks and each quarter turn is 10 - which is where
 * the tick numbers asserted below come from. The scan starts pointing along +X (east).
 */
class TowerThreeTest {

    private static final int SCALE = 32;
    private static final int TICKS_PER_REVOLUTION = 40;
    // cell (3,3) centre, in board pixels
    private static final int TOWER_X = 3 * SCALE + SCALE / 2;
    private static final int TOWER_Y = 3 * SCALE + SCALE / 2;
    private static final int NEAR = 50;

    private final GameWorld context = new GameWorld(new RecordingGameHost());

    private TowerThree tower() {
        this.context.setBoard(BoardGeometry.of(SCALE, 20, 20));
        return new TowerThree(this.context, 3, 3);
    }

    private static int hitCount(RecordingEnemyMob mob) {
        return mob.hits().size();
    }

    @Test
    void enemiesAreSweptInCounterclockwiseOrderOfTheirBearingNotOfTheirWaveOrder() {
        TowerThree tower = tower();
        RecordingEnemyMob east = RecordingEnemyMob.normalAt(TOWER_X + NEAR, TOWER_Y);
        RecordingEnemyMob north = RecordingEnemyMob.normalAt(TOWER_X, TOWER_Y - NEAR);
        RecordingEnemyMob west = RecordingEnemyMob.normalAt(TOWER_X - NEAR, TOWER_Y);
        RecordingEnemyMob south = RecordingEnemyMob.normalAt(TOWER_X, TOWER_Y + NEAR);
        // deliberately not in sweep order: where they sit decides, not where they are in the array
        this.context.setEnemies(new EnemyMob[]{south, west, north, east});

        Map<RecordingEnemyMob, Integer> firstHit = new LinkedHashMap<>();
        for (int tick = 1; tick <= TICKS_PER_REVOLUTION; tick++) {
            tower.doTick(tick);
            for (RecordingEnemyMob mob : List.of(east, north, west, south)) {
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
        TowerThree tower = tower();
        RecordingEnemyMob enemy = RecordingEnemyMob.normalAt(TOWER_X, TOWER_Y - NEAR);
        this.context.setEnemies(new EnemyMob[]{enemy});

        for (int tick = 1; tick <= TICKS_PER_REVOLUTION * 3; tick++) {
            tower.doTick(tick);
        }

        assertThat(hitCount(enemy)).isEqualTo(3);
    }

    @Test
    void everyEnemyOnTheSameBearingIsHitOnTheSameTick() {
        TowerThree tower = tower();
        // same direction from the tower, different distances - the beam is a ray, not one target
        RecordingEnemyMob close = RecordingEnemyMob.normalAt(TOWER_X, TOWER_Y - 40);
        RecordingEnemyMob far = RecordingEnemyMob.normalAt(TOWER_X, TOWER_Y - 80);
        this.context.setEnemies(new EnemyMob[]{close, far});

        for (int tick = 1; tick <= TICKS_PER_REVOLUTION; tick++) {
            tower.doTick(tick);
        }

        assertThat(hitCount(close)).isEqualTo(1);
        assertThat(hitCount(far)).isEqualTo(1);
    }

    @Test
    void anEnemyBeyondTheTowersRangeIsNeverHit() {
        TowerThree tower = tower();
        // range is 5.2 cells = 166.4px at this scale
        RecordingEnemyMob outOfRange = RecordingEnemyMob.normalAt(TOWER_X + 200, TOWER_Y);
        this.context.setEnemies(new EnemyMob[]{outOfRange});

        for (int tick = 1; tick <= TICKS_PER_REVOLUTION; tick++) {
            tower.doTick(tick);
        }

        assertThat(outOfRange.hits()).isEmpty();
    }

    @Test
    void aGhostIsNeverHitBecauseTheScanOnlySeesVisibleEnemies() {
        TowerThree tower = tower();
        RecordingEnemyMob ghost = RecordingEnemyMob.ghostAt(TOWER_X + NEAR, TOWER_Y);
        this.context.setEnemies(new EnemyMob[]{ghost});

        for (int tick = 1; tick <= TICKS_PER_REVOLUTION; tick++) {
            tower.doTick(tick);
        }

        assertThat(ghost.hits()).isEmpty();
    }

    @Test
    void anEnemyThatTheBeamWouldHaveJumpedOverBetweenTicksIsStillHit() {
        TowerThree tower = tower();
        // a bearing deliberately off any exact tick boundary, so a naive "is the beam pointing
        // at it right now" test would step straight past it
        double awkward = -(Math.PI * 2 / TICKS_PER_REVOLUTION) * 4.37;
        RecordingEnemyMob enemy = RecordingEnemyMob.normalAt(
                TOWER_X + Math.cos(awkward) * NEAR, TOWER_Y + Math.sin(awkward) * NEAR);
        this.context.setEnemies(new EnemyMob[]{enemy});

        for (int tick = 1; tick <= TICKS_PER_REVOLUTION; tick++) {
            tower.doTick(tick);
        }

        assertThat(hitCount(enemy)).isEqualTo(1);
    }

    @Test
    void theTurretHeadPointsWhereTheBeamIs() {
        TowerThree tower = tower();
        this.context.setEnemies(new EnemyMob[]{});

        tower.doTick(1);

        double start = tower.sweepRadiansAt(0);
        double end = tower.sweepRadiansAt(1);
        // counterclockwise on screen is a decreasing angle, a fortieth of a turn per tick
        assertThat(start - end).isCloseTo(Math.PI * 2 / TICKS_PER_REVOLUTION, within(1e-9));
    }

    @Test
    void theStatusReportsARotationSpeedRatherThanAFireRate() {
        TowerThree tower = tower();

        assertThat(tower.getStatusString()).contains("Rotation");
        assertThat(tower.getStatusString()).doesNotContain("Fire rate");
    }

    @Test
    void overchargedArraySpeedsUpTheSweepBeyondTheBaseRate() {
        TowerThree tower = tower();
        UpgradePath overchargedArray = UpgradePaths.named(tower, "Overcharged Array");
        tower.doTick(1);
        double radiansPerTickBeforeChoosing = tower.sweepRadiansAt(0) - tower.sweepRadiansAt(1);

        // onUpgradePathChosen is exercised directly - Overcharged Array's own gate (a cluster
        // of nearby towers) is covered generically by ClusterConditionTest and by
        // AbstractTowerTest's condition-gating test; this proves the sweep-speed bump itself.
        tower.onUpgradePathChosen(overchargedArray);
        tower.doTick(2);
        double radiansPerTickAfterChoosing = tower.sweepRadiansAt(0) - tower.sweepRadiansAt(1);

        assertThat(radiansPerTickAfterChoosing).isGreaterThan(radiansPerTickBeforeChoosing);
        assertThat(tower.getStatusString()).contains("Rotation");
    }
}

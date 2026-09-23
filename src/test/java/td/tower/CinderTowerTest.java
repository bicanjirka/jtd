package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.DamageType;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.fixtures.BoardFixtures;
import td.fixtures.WorldFixtures;
import td.tower.upgrade.UpgradePath;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers CinderTower's wedge targeting and its cooldown-gated, travelling-wave burn - it never
 * calls dealDamage directly, so a hit only ever shows up once the applied burn effect itself
 * ticks (see AbstractEnemyMobEffectTest for that side of the contract). A shot's wave takes
 * CinderTower.WAVE_TRAVEL_TICKS to reach its full range, so every case here drives enough ticks
 * for the wave to actually arrive rather than firing once and checking immediately.
 */
class CinderTowerTest {

    private final GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);

    private CinderTower towerAt(int cellX, int cellY) {
        return new CinderTower(this.context, cellX, cellY);
    }

    private static void tickThrough(CinderTower tower, int fromInclusive, int toInclusive) {
        for (int t = fromInclusive; t <= toInclusive; t++) {
            tower.doTick(t);
        }
    }

    @Test
    void anEnemyDirectlyAheadOfTheDefaultHeadingIsBurnedOnceTheWaveReachesIt() {
        CinderTower tower = towerAt(3, 3); // centre at (112, 112)
        RecordingEnemyMob ahead = RecordingEnemyMob.normalAt(150, 112); // due +X of the tower, 38px out
        this.context.enemies().setEnemies(new EnemyMob[]{ahead});

        tickThrough(tower, 1, 1 + CinderTower.WAVE_TRAVEL_TICKS);

        assertThat(ahead.appliedEffects()).hasSize(1);
        assertThat(ahead.appliedEffects().getFirst().kind()).isEqualTo(EffectKind.BURN);
        assertThat(ahead.appliedEffects().getFirst().damagePerTick().type()).isEqualTo(DamageType.MAGIC);
    }

    @Test
    void anEnemyIsNotBurnedOnTheSameTickItsWaveFires() {
        CinderTower tower = towerAt(3, 3);
        RecordingEnemyMob ahead = RecordingEnemyMob.normalAt(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{ahead});

        tower.doTick(1); // fires the wave; it hasn't travelled anywhere yet

        assertThat(ahead.appliedEffects()).isEmpty();
    }

    @Test
    void aWaveBurnsALingeringEnemyExactlyOnceAcrossSeveralTicksInsideItsExpandingBand() {
        CinderTower tower = towerAt(3, 3);
        RecordingEnemyMob close = RecordingEnemyMob.normalAt(119, 112); // 7px out - caught almost immediately
        this.context.enemies().setEnemies(new EnemyMob[]{close});

        tower.doTick(1); // fires
        tower.doTick(2); // radius already covers 7px - first (and only) burn
        assertThat(close.appliedEffects()).hasSize(1);

        // the enemy stays inside the expanding band for the rest of the wave's travel, but the
        // wave must not re-burn it on every subsequent tick it remains caught
        tickThrough(tower, 3, 1 + CinderTower.WAVE_TRAVEL_TICKS);
        assertThat(close.appliedEffects()).hasSize(1);
    }

    @Test
    void aWaveThatHasFullyTravelledStopsBeingTracked() {
        CinderTower tower = towerAt(3, 3);
        RecordingEnemyMob ahead = RecordingEnemyMob.normalAt(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{ahead});

        tickThrough(tower, 1, 1 + CinderTower.WAVE_TRAVEL_TICKS);

        assertThat(tower.getInFlightWaves()).isEmpty();
    }

    @Test
    void theCooldownGatesFiringASecondWaveUntilItActuallyElapses() {
        CinderTower tower = towerAt(3, 3);
        RecordingEnemyMob ahead = RecordingEnemyMob.normalAt(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{ahead});

        tickThrough(tower, 1, 1 + CinderTower.WAVE_TRAVEL_TICKS); // first wave fires and lands
        assertThat(ahead.appliedEffects()).hasSize(1);

        // still well within the first shot's cooldown - no second wave fired yet
        tickThrough(tower, 2 + CinderTower.WAVE_TRAVEL_TICKS, CinderTower.COOLDOWN_MAX);
        assertThat(ahead.appliedEffects()).hasSize(1);

        // cooldown has now elapsed (fired at tick 1, resets after COOLDOWN_MAX ticks) and the
        // second wave has had time to travel back out to the same enemy
        tickThrough(tower, CinderTower.COOLDOWN_MAX + 1, CinderTower.COOLDOWN_MAX + 1 + CinderTower.WAVE_TRAVEL_TICKS);
        assertThat(ahead.appliedEffects()).hasSize(2);
    }

    @Test
    void aGhostIsNotCaughtByTheCone() {
        CinderTower tower = towerAt(3, 3);
        RecordingEnemyMob ghost = RecordingEnemyMob.ghostAt(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{ghost});

        tickThrough(tower, 1, 1 + CinderTower.WAVE_TRAVEL_TICKS);

        assertThat(ghost.appliedEffects()).isEmpty();
    }

    @Test
    void anEnemyOutOfRangeIsNotBurned() {
        CinderTower tower = towerAt(3, 3);
        RecordingEnemyMob farAway = RecordingEnemyMob.normalAt(10_000, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{farAway});

        tickThrough(tower, 1, 1 + CinderTower.WAVE_TRAVEL_TICKS);

        assertThat(farAway.appliedEffects()).isEmpty();
    }

    @Test
    void noEnemyInRangeAppliesNoBurnAndHoldsTheLastHeading() {
        CinderTower tower = towerAt(3, 3);
        this.context.enemies().setEnemies(new EnemyMob[]{});

        tower.doTick(1);

        assertThat(tower.getTurretAim().currentRadians()).isEqualTo(0.0);
        assertThat(tower.getInFlightWaves()).isEmpty();
    }

    @Test
    void wideNozzleIsChoosableWithMoneyAloneAndAppliesItsRangeBonus() {
        this.context.economy().startEconomy(1000, 5);
        CinderTower tower = towerAt(3, 3);
        UpgradePath wideNozzle = UpgradePaths.named(tower, "Wide Nozzle");

        boolean chosen = tower.chooseUpgradePath(wideNozzle);

        assertThat(chosen).isTrue();
        assertThat(tower.getRangeReal()).isGreaterThan(CinderTower.RANGE * BoardFixtures.SCALE);
    }

    @Test
    void whiteFlameIsNotYetChoosableBeforeEnoughDamageDealt() {
        this.context.economy().startEconomy(1000, 5);
        CinderTower tower = towerAt(3, 3);
        UpgradePath whiteFlame = UpgradePaths.named(tower, "White Flame");

        boolean chosen = tower.chooseUpgradePath(whiteFlame);

        assertThat(chosen).isFalse();
        assertThat(tower.getChosenPath()).isEmpty();
    }

    @Test
    void wideNozzleBumpsTheHalfWidthRadiansBeyondTheBase() {
        CinderTower tower = towerAt(3, 3);
        UpgradePath wideNozzle = UpgradePaths.named(tower, "Wide Nozzle");
        double halfWidthBeforeChoosing = tower.getHalfWidthRadians();

        // onUpgradePathChosen is exercised directly - Wide Nozzle's own gate (money alone) is
        // trivially satisfied and covered by the choosability test above; this proves the bump itself.
        tower.onUpgradePathChosen(wideNozzle);

        assertThat(tower.getHalfWidthRadians()).isGreaterThan(halfWidthBeforeChoosing);
    }
}

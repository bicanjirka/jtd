package td.tower;

import org.junit.jupiter.api.Test;
import td.board.BoardGeometry;
import td.damage.DamageType;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.tower.upgrade.UpgradePath;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers TowerCinder's wedge targeting and its burn-only attack - it never calls dealDamage
 * directly, so a hit only ever shows up once the applied burn effect itself ticks (see
 * AbstractEnemyMobEffectTest for that side of the contract).
 */
class TowerCinderTest {

    private static final int SCALE = 32;

    private final GameWorld context = new GameWorld(new RecordingGameHost());

    private TowerCinder towerAt(int cellX, int cellY) {
        this.context.setBoard(BoardGeometry.of(SCALE, 20, 20));
        return new TowerCinder(this.context, cellX, cellY);
    }

    @Test
    void anEnemyDirectlyAheadOfTheDefaultHeadingIsBurned() {
        TowerCinder tower = towerAt(3, 3); // centre at (112, 112)
        RecordingEnemyMob ahead = RecordingEnemyMob.normalAt(150, 112); // due +X of the tower

        this.context.enemies().setEnemies(new EnemyMob[]{ahead});

        tower.doTick(1);

        assertThat(ahead.appliedEffects()).hasSize(1);
        assertThat(ahead.appliedEffects().get(0).kind()).isEqualTo(EffectKind.BURN);
        assertThat(ahead.appliedEffects().get(0).damagePerTick().type()).isEqualTo(DamageType.MAGIC);
    }

    @Test
    void aGhostIsStillCaughtByTheCone() {
        TowerCinder tower = towerAt(3, 3);
        RecordingEnemyMob ghost = RecordingEnemyMob.ghostAt(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{ghost});

        tower.doTick(1);

        assertThat(ghost.appliedEffects()).hasSize(1);
    }

    @Test
    void anEnemyOutOfRangeIsNotBurned() {
        TowerCinder tower = towerAt(3, 3);
        RecordingEnemyMob farAway = RecordingEnemyMob.normalAt(10_000, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{farAway});

        tower.doTick(1);

        assertThat(farAway.appliedEffects()).isEmpty();
    }

    @Test
    void noEnemyInRangeAppliesNoBurnAndHoldsTheLastHeading() {
        TowerCinder tower = towerAt(3, 3);
        this.context.enemies().setEnemies(new EnemyMob[]{});

        tower.doTick(1);

        assertThat(tower.getTurretAim().currentRadians()).isEqualTo(0.0);
    }

    @Test
    void wideNozzleIsChoosableWithMoneyAloneAndAppliesItsRangeBonus() {
        this.context.economy().startEconomy(1000, 5);
        TowerCinder tower = towerAt(3, 3);
        UpgradePath wideNozzle = UpgradePaths.named(tower, "Wide Nozzle");

        boolean chosen = tower.chooseUpgradePath(wideNozzle);

        assertThat(chosen).isTrue();
        assertThat(tower.getRangeReal()).isGreaterThan(TowerCinder.RANGE * SCALE);
    }

    @Test
    void whiteFlameIsNotYetChoosableBeforeEnoughDamageDealt() {
        this.context.economy().startEconomy(1000, 5);
        TowerCinder tower = towerAt(3, 3);
        UpgradePath whiteFlame = UpgradePaths.named(tower, "White Flame");

        boolean chosen = tower.chooseUpgradePath(whiteFlame);

        assertThat(chosen).isFalse();
        assertThat(tower.getChosenPath()).isNull();
    }

    @Test
    void wideNozzleBumpsTheHalfWidthRadiansBeyondTheBase() {
        TowerCinder tower = towerAt(3, 3);
        UpgradePath wideNozzle = UpgradePaths.named(tower, "Wide Nozzle");
        double halfWidthBeforeChoosing = tower.getHalfWidthRadians();

        // onUpgradePathChosen is exercised directly - Wide Nozzle's own gate (money alone) is
        // trivially satisfied and covered by the choosability test above; this proves the bump itself.
        tower.onUpgradePathChosen(wideNozzle);

        assertThat(tower.getHalfWidthRadians()).isGreaterThan(halfWidthBeforeChoosing);
    }
}

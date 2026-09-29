package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.damage.DamageType;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyFactory;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.fixtures.BoardFixtures;
import td.fixtures.FakeEnemyMob;
import td.fixtures.WorldFixtures;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeSlot;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cinder only applies burns, so hits appear as burn ticks, and each case ticks long enough for the
 * wave to arrive.
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
        FakeEnemyMob ahead = FakeEnemyMob.at(150, 112); // due +X of the tower, 38px out
        this.context.enemies().setEnemies(new EnemyMob[]{ahead});

        tickThrough(tower, 1, 1 + CinderTower.WAVE_TRAVEL_TICKS);

        assertThat(ahead.appliedEffects()).hasSize(1);
        assertThat(ahead.appliedEffects().getFirst().kind()).isEqualTo(EffectKind.BURN);
        assertThat(ahead.appliedEffects().getFirst().damagePerTick().type()).isEqualTo(DamageType.MAGIC);
    }

    @Test
    void anEnemyIsNotBurnedOnTheSameTickItsWaveFires() {
        CinderTower tower = towerAt(3, 3);
        FakeEnemyMob ahead = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{ahead});

        tower.doTick(1); // fires the wave; it hasn't travelled anywhere yet

        assertThat(ahead.appliedEffects()).isEmpty();
    }

    @Test
    void aWaveBurnsALingeringEnemyExactlyOnceAcrossSeveralTicksInsideItsExpandingBand() {
        CinderTower tower = towerAt(3, 3);
        FakeEnemyMob close = FakeEnemyMob.at(119, 112); // 7px out - caught almost immediately
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
        FakeEnemyMob ahead = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{ahead});

        tickThrough(tower, 1, 1 + CinderTower.WAVE_TRAVEL_TICKS);

        assertThat(tower.getInFlightWaves()).isEmpty();
    }

    @Test
    void theCooldownGatesFiringASecondWaveUntilItActuallyElapses() {
        CinderTower tower = towerAt(3, 3);
        FakeEnemyMob ahead = FakeEnemyMob.at(150, 112);
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
    void anEnemyBehindTheTurretIsNotFiredAtUntilTheTurretHasTurnedToIt() {
        CinderTower tower = towerAt(3, 3);
        FakeEnemyMob behind = FakeEnemyMob.at(74, 112); // due -X, half a turn from the default heading
        this.context.enemies().setEnemies(new EnemyMob[]{behind});

        tower.doTick(1);

        assertThat(tower.getInFlightWaves()).isEmpty();
    }

    @Test
    void theFirstWaveFiredAtAnEnemyBehindTheTurretHeadsAtTheEnemy() {
        CinderTower tower = towerAt(3, 3);
        FakeEnemyMob behind = FakeEnemyMob.at(74, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{behind});

        tickThrough(tower, 1, 40);

        assertThat(behind.appliedEffects()).isNotEmpty();
    }

    @Test
    void aGhostCaughtInAWaveFiredAtAVisibleEnemyIsBurnedToo() {
        CinderTower tower = towerAt(3, 3);
        FakeEnemyMob visible = FakeEnemyMob.at(150, 112);
        FakeEnemyMob ghost = FakeEnemyMob.ghostAt(140, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{visible, ghost});

        tickThrough(tower, 1, 1 + CinderTower.WAVE_TRAVEL_TICKS);

        assertThat(ghost.appliedEffects()).hasSize(1);
    }

    @Test
    void aLoneGhostIsNeitherAimedAtNorFiredAt() {
        CinderTower tower = towerAt(3, 3);
        FakeEnemyMob ghost = FakeEnemyMob.ghostAt(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{ghost});

        tickThrough(tower, 1, 1 + CinderTower.WAVE_TRAVEL_TICKS);

        assertThat(tower.getInFlightWaves()).isEmpty();
        assertThat(ghost.appliedEffects()).isEmpty();
    }

    @Test
    void anEnemyOutOfRangeIsNotBurned() {
        CinderTower tower = towerAt(3, 3);
        FakeEnemyMob farAway = FakeEnemyMob.at(10_000, 112);
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
    void wideNozzleIsChoosableAfterTenKillsAndAppliesItsRangeBonus() {
        this.context.economy().startEconomy(1000, 5);
        CinderTower tower = towerAt(3, 3);
        tower.buyUpgrade(UpgradePaths.named(tower, "Awaken"));
        EnemyMob fodder = EnemyFactory.getEnemy("c", this.context, 0, 1, 1, Rank.GRUNT);
        for (int i = 0; i < 10; i++) {
            tower.dealDamage(fodder, Damage.physical(1_000_000));
            fodder = EnemyFactory.getEnemy("c", this.context, 0, 1, 1, Rank.GRUNT);
        }
        UpgradeNode wideNozzle = UpgradePaths.named(tower, "Wide Nozzle");

        boolean chosen = tower.buyUpgrade(wideNozzle);

        assertThat(chosen).isTrue();
        assertThat(tower.getRangeReal()).isGreaterThan(CinderTower.RANGE * BoardFixtures.SCALE);
    }

    @Test
    void whiteFlameIsNotYetChoosableBeforeAwakenIsBought() {
        this.context.economy().startEconomy(1000, 5);
        CinderTower tower = towerAt(3, 3);
        UpgradeNode whiteFlame = UpgradePaths.named(tower, "White Flame");

        boolean chosen = tower.buyUpgrade(whiteFlame);

        assertThat(chosen).isFalse();
        assertThat(tower.upgrades().tip(UpgradeSlot.HEAD)).isEmpty();
    }

    @Test
    void wideNozzleBumpsTheHalfWidthRadiansBeyondTheBase() {
        CinderTower tower = towerAt(3, 3);
        UpgradeNode wideNozzle = UpgradePaths.named(tower, "Wide Nozzle");
        double halfWidthBeforeChoosing = tower.getHalfWidthRadians();

        tower.onUpgradeBought(wideNozzle);

        assertThat(tower.getHalfWidthRadians()).isGreaterThan(halfWidthBeforeChoosing);
    }

    @Test
    void whiteFlameIiExtendsTheBurnDurationBeyondTheBase() {
        CinderTower before = towerAt(3, 3);
        FakeEnemyMob beforeTarget = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{beforeTarget});
        tickThrough(before, 1, 1 + CinderTower.WAVE_TRAVEL_TICKS);

        CinderTower after = towerAt(3, 3);
        after.onUpgradeBought(UpgradePaths.named(after, "White Flame II"));
        FakeEnemyMob afterTarget = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{afterTarget});
        tickThrough(after, 1, 1 + CinderTower.WAVE_TRAVEL_TICKS);

        assertThat(afterTarget.appliedEffects().getFirst().authoredDurationTicks())
                .isGreaterThan(beforeTarget.appliedEffects().getFirst().authoredDurationTicks());
    }

    @Test
    void hexflameAppliesOneVulnerabilityStackWhenAWaveNewlyIgnitesAnEnemyAndNotWhenItIsAlreadyBurning() {
        CinderTower tower = towerAt(3, 3);
        UpgradePaths.buy(tower, this.context, "Hexflame");
        FakeEnemyMob ahead = FakeEnemyMob.at(150, 112);
        this.context.enemies().setEnemies(new EnemyMob[]{ahead});

        tickThrough(tower, 1, 2 * (CinderTower.COOLDOWN_MAX + CinderTower.WAVE_TRAVEL_TICKS));

        assertThat(ahead.appliedEffects()).extracting(Effect::kind)
                .containsOnly(EffectKind.BURN, EffectKind.VULNERABLE);
        assertThat(ahead.appliedEffects().stream().filter(e -> e.kind() == EffectKind.BURN).count()).isGreaterThan(1);
        assertThat(ahead.appliedEffects().stream().filter(e -> e.kind() == EffectKind.VULNERABLE).count()).isEqualTo(1);
    }
}

package td.tower;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyMob;
import td.fixtures.BoardFixtures;
import td.fixtures.FakeEnemyMob;
import td.fixtures.WorldFixtures;
import td.tower.buff.TowerBuff;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

/** What a fire-rate buff or debuff does to the towers, counted over long runs against exact arithmetic. */
class FireRateTest {

    private static final int TICKS = 1200;

    private static GameWorld board() {
        return WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
    }

    /** Shots in {@code ticks} ticks of a tower whose first shot is due at once, one every {@code period} ticks at {@code rate}. */
    private static long expectedShots(int ticks, double rate, int period) {
        return (long) Math.floor(1 + (ticks - 1) * rate / period + 1e-6);
    }

    private static double rateOf(float bonus) {
        return TowerBuff.fireRate(bonus).fireRateMultiplier();
    }

    private static int pulseHits(float bonus) {
        GameWorld world = board();
        PulseTower tower = new PulseTower(world, 3, 3);
        tower.grantTimedBuff(TowerBuff.fireRate(bonus), Integer.MAX_VALUE / 2);
        FakeEnemyMob target = FakeEnemyMob.at(tower.getX(), tower.getY());
        world.enemies().setEnemies(new EnemyMob[]{target});
        for (int t = 0; t < TICKS; t++) {
            tower.doTick(t);
        }
        return target.hits().size();
    }

    @Test
    void anUnbuffedPulseHitsOncePerTick() {
        assertThat(pulseHits(0f)).isEqualTo(TICKS);
    }

    @Test
    void aTenPercentBuffedPulseHitsTwentyTwoPointTwoTimesPerTwentyTicks() {
        assertThat((long) pulseHits(0.1f)).isEqualTo(expectedShots(TICKS, rateOf(0.1f), 1));
    }

    @Test
    void aBuffedPulseHitsTwiceInATickOnceTheRememberedFractionsAddUp() {
        GameWorld world = board();
        PulseTower tower = new PulseTower(world, 3, 3);
        tower.grantTimedBuff(TowerBuff.fireRate(0.5f), Integer.MAX_VALUE / 2);
        FakeEnemyMob target = FakeEnemyMob.at(tower.getX(), tower.getY());
        world.enemies().setEnemies(new EnemyMob[]{target});

        tower.doTick(0);
        tower.doTick(1);

        assertThat(target.hits()).hasSize(3);
    }

    @Test
    void aJammedPulseHitsLessOften() {
        assertThat((long) pulseHits(-0.25f)).isEqualTo(expectedShots(TICKS, rateOf(-0.25f), 1));
    }

    @Test
    void aPulseBuffShowsItsExactRateOnTheInfoRow() {
        GameWorld world = board();
        PulseTower tower = new PulseTower(world, 3, 3);
        tower.grantTimedBuff(TowerBuff.fireRate(0.1f), Integer.MAX_VALUE / 2);

        TowerStatLine row = tower.inspect().stats().stream().filter(line -> line.stat() == TowerStat.FIRE_RATE)
                .findFirst().orElseThrow();

        assertThat(row.base()).isEqualTo(20f);
        assertThat(row.current()).isEqualTo((float) (20 * rateOf(0.1f)));
    }

    private record Run(long shots, int period) {
    }

    private static Run sniperShots(float bonus) {
        GameWorld world = board();
        SniperTower tower = new SniperTower(world, 3, 3);
        tower.grantTimedBuff(TowerBuff.fireRate(bonus), Integer.MAX_VALUE / 2);
        FakeEnemyMob target = FakeEnemyMob.at(tower.getX() + 10, tower.getY());
        world.enemies().setEnemies(new EnemyMob[]{target});
        for (int t = 1; t <= TICKS; t++) {
            tower.doTick(t);
        }
        return new Run(target.hits().size(), tower.coolDownMax + 1);
    }

    @Test
    void aSniperShootsExactlyAsOftenAsItsRateSays() {
        for (float bonus : new float[]{0f, 0.1f, 0.25f, 0.5f, -0.25f}) {
            Run run = sniperShots(bonus);

            assertThat(run.shots()).as("bonus %s", bonus).isEqualTo(expectedShots(TICKS, rateOf(bonus), run.period()));
        }
    }

    private static Run splashShots(float bonus) {
        GameWorld world = board();
        SplashTower tower = new SplashTower(world, 3, 3);
        tower.grantTimedBuff(TowerBuff.fireRate(bonus), Integer.MAX_VALUE / 2);
        FakeEnemyMob target = FakeEnemyMob.at(tower.getX() + 10, tower.getY());
        world.enemies().setEnemies(new EnemyMob[]{target});
        for (int t = 1; t <= TICKS; t++) {
            tower.doTick(t);
        }
        return new Run(target.hits().size(), tower.coolDownMax + 1);
    }

    @Test
    void aSplashTowerBlastsExactlyAsOftenAsItsRateSays() {
        for (float bonus : new float[]{0f, 0.1f, 0.5f, -0.25f}) {
            Run run = splashShots(bonus);

            assertThat(run.shots()).as("bonus %s", bonus).isEqualTo(expectedShots(TICKS, rateOf(bonus), run.period()));
        }
    }

    private static long sonarPasses(float bonus) {
        GameWorld world = board();
        SonarTower tower = new SonarTower(world, 3, 3);
        tower.grantTimedBuff(TowerBuff.fireRate(bonus), Integer.MAX_VALUE / 2);
        FakeEnemyMob target = FakeEnemyMob.at(tower.getX() + 10, tower.getY() + 10);
        world.enemies().setEnemies(new EnemyMob[]{target});
        for (int t = 1; t <= TICKS; t++) {
            tower.doTick(t);
        }
        return target.hits().size();
    }

    @Test
    void aSonarSpinsFasterByTheFireRateBuff() {
        long base = sonarPasses(0f);
        long buffed = sonarPasses(0.5f);

        assertThat(base).isBetween(19L, 21L);
        assertThat(buffed).isBetween(39L, 41L);
    }
}

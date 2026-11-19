package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.effect.DamageSink;
import td.effect.Effect;
import td.enemy.EnemyMob;
import td.fixtures.BoardFixtures;
import td.fixtures.FakeEnemyMob;
import td.fixtures.TowerFixtures;
import td.fixtures.UpgradePaths;
import td.fixtures.WorldFixtures;
import td.stat.StatModifiers;
import td.tower.sniper.AimFocus;
import td.util.GameWorld;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * How the Sniper shoots and what its own mechanics do on the board. What a node adds is the node's
 * data; its pieces are tested in {@code td.tower.proc} and its mechanics in {@code td.tower.sniper}.
 */
class SniperTowerTest {

    private static final int FLIGHT_WINDOW_TICKS = 5;

    private final GameWorld world = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
    private int clock;

    private SniperTower sniperWith(String... nodes) {
        SniperTower tower = new SniperTower(this.world, 3, 3);
        UpgradePaths.buy(tower, this.world, nodes);
        return tower;
    }

    private FakeEnemyMob onlyTarget() {
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        this.world.enemies().setEnemies(new EnemyMob[]{target});
        return target;
    }

    private void tick(SniperTower tower) {
        this.clock++;
        tower.beginTick(this.clock);
        tower.doTick(this.clock);
    }

    /** Fires at tick 1 and ticks on until the bullet has landed, well inside one reload. */
    private static void landFirstShot(SniperTower tower) {
        for (int tick = 1; tick <= FLIGHT_WINDOW_TICKS; tick++) {
            tower.doTick(tick);
        }
    }

    private void fireShots(SniperTower tower, FakeEnemyMob target, int shots) {
        while (target.hits().size() < shots) {
            this.tick(tower);
        }
    }

    private static List<Float> critChances(FakeEnemyMob target) {
        return target.attackers().stream().map(AttackProfile::critChance).toList();
    }

    @Test
    void theLastShotRecordsWhetherItWasACriticalHit() {
        GameWorld alwaysCrits = WorldFixtures.newWorldOnBoard(() -> 0.0, BoardFixtures.SCALE, 20, 20);
        SniperTower tower = new SniperTower(alwaysCrits, 3, 3);
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        target.landEveryHitCritical();
        alwaysCrits.enemies().setEnemies(new EnemyMob[]{target});

        landFirstShot(tower);

        assertThat(tower.lastShot()).hasValueSatisfying(shot -> assertThat(shot.critical()).isTrue());
        assertThat(target.attackers().getFirst().critChance()).isEqualTo(tower.critChance());
    }

    @Test
    void aShotLandsOnlyWhenItsBulletArrives() {
        SniperTower tower = new SniperTower(this.world, 3, 3);
        FakeEnemyMob target = this.onlyTarget();

        tower.doTick(1);
        int landsAt = tower.bulletsInFlight().getFirst().landsAt();
        for (int tick = 2; tick < landsAt; tick++) {
            tower.doTick(tick);
        }
        List<Damage> beforeArrival = target.hits();
        tower.doTick(landsAt);

        assertThat(landsAt).isGreaterThan(2);
        assertThat(beforeArrival).isEmpty();
        assertThat(target.hits()).hasSize(1);
        assertThat(tower.bulletsInFlight()).isEmpty();
    }

    @Test
    void aBulletLandsWithTheStatsItWasFiredWithThoughTheChargeItSpentIsGone() {
        SniperTower tower = new SniperTower(this.world, 3, 3);
        FakeEnemyMob target = this.onlyTarget();
        float times = 3f;
        tower.grantStatus(TowerStatus.nextShots(TowerTerm.HASTE, 1, TowerModifiers.damageTimes(times)));
        int charged = tower.damageCurrent();

        landFirstShot(tower);

        assertThat(tower.damageCurrent()).isLessThan(charged);
        assertThat(target.onlyHitAmount()).isEqualTo(charged);
    }

    @Test
    void aBulletWhoseTargetDiesOnTheWayIsLost() {
        SniperTower tower = new SniperTower(this.world, 3, 3);
        FakeEnemyMob target = this.onlyTarget();

        tower.doTick(1);
        target.invalidate();
        landFirstShot(tower);

        assertThat(target.hits()).isEmpty();
        assertThat(tower.bulletsInFlight()).isEmpty();
    }

    @Test
    void aSteadyAimStackRaisesTheNextShotsCritChance() {
        SniperTower tower = this.sniperWith("Steady Aim");
        FakeEnemyMob target = this.onlyTarget();

        this.fireShots(tower, target, 2);

        assertThat(critChances(target).get(1)).isGreaterThan(critChances(target).get(0));
    }

    @Test
    void anIdleSnipersSteadyAimFadesOnceItHasHadNoTargetForAStacksTime() {
        SniperTower tower = this.sniperWith("Steady Aim");
        FakeEnemyMob target = this.onlyTarget();
        this.fireShots(tower, target, 2);
        this.world.enemies().setEnemies(new EnemyMob[]{});
        int built = tower.steadyAimStacks();

        for (int i = 0; i < AimFocus.STACK_TICKS; i++) {
            this.tick(tower);
        }

        assertThat(built).isPositive();
        assertThat(tower.steadyAimStacks()).isEqualTo(built - 1);
    }

    @Test
    void steadyAimBuildsAsManyStacksAsItsAimStacksAllow() {
        SniperTower tower = TowerFixtures.buffed(new SniperTower(this.world, 3, 3), this.world, TowerModifiers.aimStacks(2));
        FakeEnemyMob target = this.onlyTarget();

        this.fireShots(tower, target, 8);

        int cap = Math.round(tower.stats().value(TowerStat.AIM_STACKS));
        assertThat(cap).isGreaterThan(1);
        assertThat(tower.steadyAimStacks()).isEqualTo(cap);
    }

    @Test
    void aFocusIsKeenOnlyOnceItIsFullAndOnlyWhileAHitBonusGrowsWithIt() {
        SniperTower plain = this.sniperWith("Steady Aim");
        SniperTower keen = this.sniperWith("Steady Aim", "Keen Eye");
        boolean keenBefore = keen.keenAtFullFocus();

        this.fireShots(plain, this.onlyTarget(), 8);
        this.fireShots(keen, this.onlyTarget(), 8);

        assertThat(keenBefore).isFalse();
        assertThat(plain.keenAtFullFocus()).isFalse();
        assertThat(keen.keenAtFullFocus()).isTrue();
    }

    @Test
    void steadyAimShowsItsStacksAndTheTimeToItsNextFade() {
        SniperTower tower = this.sniperWith("Steady Aim");
        FakeEnemyMob target = this.onlyTarget();
        this.fireShots(tower, target, 3);

        TowerBuffState aim = tower.conditions().stream().filter(state -> state.term() == TowerTerm.STEADY_AIM)
                .findFirst().orElseThrow();

        assertThat(aim.stacks()).isEqualTo(tower.steadyAimStacks());
        assertThat(aim.clockTicks()).isEqualTo(AimFocus.STACK_TICKS);
        assertThat(aim.remainingTicks().orElseThrow()).isLessThanOrEqualTo(AimFocus.STACK_TICKS);
    }


    @Test
    void aShotGivenTheMagicCapabilityLandsAsMagicAndTheNextAsItWas() {
        SniperTower tower = new SniperTower(this.world, 3, 3);
        FakeEnemyMob target = this.onlyTarget();
        tower.grantStatus(TowerStatus.nextShots(TowerTerm.SILVER_ROUND, 1, StatModifiers.none())
                .withCapability(TowerCapability.MAGIC_DAMAGE));

        this.fireShots(tower, target, 2);

        assertThat(target.hits()).extracting(Damage::type).containsExactly(DamageType.MAGIC, DamageType.PHYSICAL);
    }

    @Test
    void anEnemyUnderPriorityIsShotBeforeOneFurtherAlongThePath() {
        SniperTower tower = new SniperTower(this.world, 3, 3);
        FakeEnemyMob furthest = FakeEnemyMob.at(100, 100).withProgression(900);
        FakeEnemyMob prioritised = FakeEnemyMob.at(100, 110).withProgression(10);
        prioritised.applyEffect(Effect.priority(100, DamageSink.none()));
        this.world.enemies().setEnemies(new EnemyMob[]{furthest, prioritised});

        landFirstShot(tower);

        assertThat(prioritised.hits()).hasSize(1);
        assertThat(furthest.hits()).isEmpty();
    }

    @Test
    void eachShotAddsAsManySteadyAimStacksAsItsStacksAShotAllow() {
        SniperTower tower = TowerFixtures.buffed(this.sniperWith("Steady Aim"), this.world,
                TowerModifiers.aimStacksPerShot(1));
        FakeEnemyMob target = this.onlyTarget();

        this.fireShots(tower, target, 1);

        assertThat(tower.steadyAimStacks()).isEqualTo(Math.round(tower.stats().value(TowerStat.AIM_STACKS_PER_SHOT)));
    }

    @Test
    void aCritHopsOnceWhenItArrivesToTheNearestOtherEnemyForTheShotsDamageAndTheHopCannotCrit() {
        SniperTower tower = this.sniperWith("Ricochet");
        float edge = tower.getRangeReal() * 0.85f;
        FakeEnemyMob aimed = FakeEnemyMob.at(tower.getX() + edge, tower.getY());
        aimed.landEveryHitCritical();
        FakeEnemyMob beside = FakeEnemyMob.at(aimed.getX() + 30, aimed.getY());
        FakeEnemyMob next = FakeEnemyMob.at(beside.getX() + 30, beside.getY());
        this.world.enemies().setEnemies(new EnemyMob[]{aimed, beside, next});
        while (aimed.hits().isEmpty()) {
            this.tick(tower);
        }
        boolean hopArrivedWithTheShot = !beside.hits().isEmpty();

        while (beside.hits().isEmpty()) {
            this.tick(tower);
        }

        assertThat(hopArrivedWithTheShot).isFalse();
        assertThat(beside.onlyHitAmount()).isEqualTo(aimed.onlyHitAmount());
        assertThat(beside.attackers().getFirst().critChance()).isZero();
        assertThat(beside.attackers().getFirst().guaranteedCrit()).isFalse();
        assertThat(next.hits()).isEmpty();
    }
}

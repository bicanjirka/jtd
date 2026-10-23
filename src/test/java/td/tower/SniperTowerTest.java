package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.AttackProfile;
import td.effect.DamageSink;
import td.effect.Effect;
import td.enemy.EnemyMob;
import td.fixtures.BoardFixtures;
import td.fixtures.FakeEnemyMob;
import td.fixtures.UpgradePaths;
import td.fixtures.WorldFixtures;
import td.tower.sniper.AimFocus;
import td.ui.TowerSpriteFrameBuilder;
import td.ui.render.Palette;
import td.ui.render.TurretHeadDraw;
import td.util.GameWorld;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * How the Sniper shoots and what its own mechanics do on the board. What a node adds is the node's
 * data; its pieces are tested in {@code td.tower.proc} and its mechanics in {@code td.tower.sniper}.
 */
class SniperTowerTest {

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

    private void fireShots(SniperTower tower, FakeEnemyMob target, int shots) {
        while (target.hits().size() < shots) {
            this.tick(tower);
        }
    }

    private static List<Float> critChances(FakeEnemyMob target) {
        return target.attackers().stream().map(AttackProfile::critChance).toList();
    }

    @Test
    void doTickRecordsWhetherTheShotThatJustFiredWasACriticalHit() {
        GameWorld alwaysCrits = WorldFixtures.newWorldOnBoard(() -> 0.0, BoardFixtures.SCALE, 20, 20);
        SniperTower tower = new SniperTower(alwaysCrits, 3, 3);
        FakeEnemyMob target = FakeEnemyMob.at(100, 100);
        target.landEveryHitCritical();
        alwaysCrits.enemies().setEnemies(new EnemyMob[]{target});

        tower.doTick(1);

        assertThat(tower.wasLastShotCritical()).isTrue();
        assertThat(target.attackers().getFirst().critChance()).isEqualTo(tower.critChance());
    }

    @Test
    void aSteadyAimStackRaisesTheNextShotsCritChance() {
        SniperTower tower = this.sniperWith("Attune");
        FakeEnemyMob target = this.onlyTarget();

        this.fireShots(tower, target, 2);

        assertThat(critChances(target).get(1)).isGreaterThan(critChances(target).get(0));
    }

    @Test
    void anIdleSnipersSteadyAimFadesOnceItHasHadNoTargetForAStacksTime() {
        SniperTower tower = this.sniperWith("Attune");
        FakeEnemyMob target = this.onlyTarget();
        this.fireShots(tower, target, 2);
        this.world.enemies().setEnemies(new EnemyMob[]{});
        int built = tower.steadyAimStacks().orElseThrow();

        for (int i = 0; i < AimFocus.STACK_TICKS; i++) {
            this.tick(tower);
        }

        assertThat(built).isPositive();
        assertThat(tower.steadyAimStacks()).hasValue(built - 1);
    }

    @Test
    void steadyAimShowsItsStacksAndTheTimeToItsNextFade() {
        SniperTower tower = this.sniperWith("Attune");
        FakeEnemyMob target = this.onlyTarget();
        this.fireShots(tower, target, 3);

        TowerBuffState aim = tower.conditions().stream().filter(state -> state.term() == TowerTerm.STEADY_AIM)
                .findFirst().orElseThrow();

        assertThat(aim.stacks()).isEqualTo(tower.steadyAimStacks().orElseThrow());
        assertThat(aim.clockTicks()).isEqualTo(AimFocus.STACK_TICKS);
        assertThat(aim.remainingTicks().orElseThrow()).isLessThanOrEqualTo(AimFocus.STACK_TICKS);
    }

    @Test
    void steadyAimShotsCountAsTheDeedOfTheTowerOncePerShot() {
        SniperTower tower = this.sniperWith("Attune");
        FakeEnemyMob target = this.onlyTarget();

        this.fireShots(tower, target, 4);

        assertThat(tower.experience().deeds()).isEqualTo(3);
    }

    @Test
    void anEnemyUnderPriorityIsShotBeforeOneFurtherAlongThePath() {
        SniperTower tower = new SniperTower(this.world, 3, 3);
        FakeEnemyMob furthest = FakeEnemyMob.at(100, 100).withProgression(900);
        FakeEnemyMob prioritised = FakeEnemyMob.at(100, 110).withProgression(10);
        prioritised.applyEffect(Effect.priority(100, DamageSink.none()));
        this.world.enemies().setEnemies(new EnemyMob[]{furthest, prioritised});

        tower.doTick(1);

        assertThat(prioritised.hits()).hasSize(1);
        assertThat(furthest.hits()).isEmpty();
    }

    @Test
    void overwatchStopsTheTowerShootingWhatIsWithinItsDeadZoneHoweverFarItsRangeGrows() {
        SniperTower tower = this.sniperWith("Range III");
        FakeEnemyMob close = FakeEnemyMob.at(tower.getX() + 40, tower.getY()).withProgression(50);
        FakeEnemyMob distant = FakeEnemyMob.at(tower.getX() + 100, tower.getY()).withProgression(1);
        this.world.enemies().setEnemies(new EnemyMob[]{close, distant});

        tower.doTick(1);

        assertThat(close.hits()).isEmpty();
        assertThat(distant.hits()).hasSize(1);
    }

    @Test
    void overwatchKeepsItsDeadZoneWhicheverWayRoundTheReachWasWidened() {
        for (String[] order : new String[][]{{"Range III", "Spotter Uplink"}, {"Spotter Uplink", "Range III"}}) {
            GameWorld board = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
            SniperTower tower = new SniperTower(board, 3, 3);
            UpgradePaths.buy(tower, board, order);
            FakeEnemyMob close = FakeEnemyMob.at(tower.getX() + 40, tower.getY()).withProgression(50);
            close.applyEffect(Effect.marked(100, DamageSink.none()));
            board.enemies().setEnemies(new EnemyMob[]{close});

            tower.doTick(1);

            assertThat(close.hits()).as(String.join(" then ", order)).isEmpty();
        }
    }

    @Test
    void withoutOverwatchTheTowerShootsWhatIsClose() {
        SniperTower tower = this.sniperWith("Range");
        FakeEnemyMob close = FakeEnemyMob.at(tower.getX() + 40, tower.getY()).withProgression(50);
        this.world.enemies().setEnemies(new EnemyMob[]{close});

        tower.doTick(1);

        assertThat(close.hits()).hasSize(1);
    }

    @Test
    void railgunPiercesEveryEnemyOnTheLineEvenPastRangeEachTakingLessThanTheOneBefore() {
        SniperTower tower = this.sniperWith("Railgun");
        float scale = BoardFixtures.SCALE;
        FakeEnemyMob first = FakeEnemyMob.at(tower.getX() + 60, tower.getY());
        FakeEnemyMob aimed = FakeEnemyMob.at(tower.getX() + 100, tower.getY()).withProgression(10);
        FakeEnemyMob pastRange = FakeEnemyMob.at(tower.getX() + tower.getRangeReal() + scale, tower.getY());
        FakeEnemyMob beside = FakeEnemyMob.at(tower.getX() + 100, tower.getY() + 2 * scale);
        this.world.enemies().setEnemies(new EnemyMob[]{first, aimed, pastRange, beside});

        tower.doTick(1);

        assertThat(first.onlyHitAmount()).isEqualTo(tower.damageCurrent());
        assertThat(aimed.onlyHitAmount()).isLessThan(first.onlyHitAmount());
        assertThat(pastRange.onlyHitAmount()).isLessThan(aimed.onlyHitAmount());
        assertThat(beside.hits()).isEmpty();
    }

    @Test
    void aRailgunShotsTraceRunsPastRangeWhereAPlainShotsEndsAtItsTarget() {
        SniperTower railgun = this.sniperWith("Railgun");
        SniperTower plain = new SniperTower(this.world, 3, 3);
        FakeEnemyMob target = FakeEnemyMob.at(railgun.getX() + 60, railgun.getY());
        this.world.enemies().setEnemies(new EnemyMob[]{target});

        railgun.doTick(1);
        plain.doTick(1);

        assertThat(railgun.lastShot()).hasValueSatisfying(trace -> {
            assertThat(trace.toX()).isGreaterThan(railgun.getX() + railgun.getRangeReal());
            assertThat(trace.toY()).isCloseTo(railgun.getY(), within(0.5f));
        });
        assertThat(plain.lastShot()).hasValueSatisfying(trace -> assertThat(trace.toX()).isEqualTo((float) target.getX()));
    }

    @Test
    void aCritRicochetsToTheNearestEnemiesBesideTheTargetForLessThanTheShot() {
        SniperTower tower = this.sniperWith("Ricochet");
        // only the first is within range; the rest are a chain of neighbours leading away from the tower
        float edge = tower.getRangeReal() * 0.85f;
        FakeEnemyMob aimed = FakeEnemyMob.at(tower.getX() + edge, tower.getY());
        aimed.landEveryHitCritical();
        FakeEnemyMob first = FakeEnemyMob.at(aimed.getX() + 40, aimed.getY());
        FakeEnemyMob second = FakeEnemyMob.at(first.getX() + 40, first.getY());
        FakeEnemyMob faraway = FakeEnemyMob.at(aimed.getX(), aimed.getY() + 300);
        this.world.enemies().setEnemies(new EnemyMob[]{aimed, first, second, faraway});

        tower.doTick(1);

        assertThat(aimed.onlyHitAmount()).isEqualTo(tower.damageCurrent());
        assertThat(first.onlyHitAmount()).isLessThan(aimed.onlyHitAmount());
        assertThat(second.onlyHitAmount()).isEqualTo(first.onlyHitAmount());
        assertThat(faraway.hits()).isEmpty();
    }

    @Test
    void spotterUplinkLetsTheSniperShootOnlyAMarkedEnemyBeyondItsRange() {
        SniperTower tower = this.sniperWith("Spotter Uplink");
        float far = tower.getRangeReal() * 1.5f;
        FakeEnemyMob unmarked = FakeEnemyMob.at(tower.getX() + far, tower.getY());
        FakeEnemyMob marked = FakeEnemyMob.at(tower.getX(), tower.getY() + far);
        marked.applyEffect(Effect.marked(100, DamageSink.none()));
        this.world.enemies().setEnemies(new EnemyMob[]{unmarked, marked});

        tower.doTick(1);

        assertThat(marked.hits()).hasSize(1);
        assertThat(unmarked.hits()).isEmpty();
    }

    @Test
    void aTranscendedSnipersBarrelTurnsGold() {
        SniperTower transcended = this.sniperWith("Transcendent");
        SniperTower awakened = this.sniperWith("Awaken");
        TowerSpriteFrameBuilder sprites = new TowerSpriteFrameBuilder(this.world, 0, 0.0, 0.0);

        transcended.accept(sprites);
        awakened.accept(sprites);

        assertThat(sprites.buildHeads()).extracting(TurretHeadDraw::palette)
                .containsExactly(Palette.TOWER_SNIPER_GOLD_BARREL, Palette.TOWER_SNIPER_BODY);
    }
}

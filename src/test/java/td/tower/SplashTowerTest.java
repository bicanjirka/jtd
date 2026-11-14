package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.damage.DamageUnits;
import td.effect.EffectKind;
import td.fixtures.BoardFixtures;
import td.fixtures.FakeEnemyMob;
import td.fixtures.TowerFixtures;
import td.fixtures.UpgradePaths;
import td.fixtures.WorldFixtures;
import td.tower.splash.ArcSpec;
import td.tower.splash.HexSpec;
import td.tower.splash.SoulbrandMechanic;
import td.tower.splash.StaticMechanic;
import td.util.GameWorld;

import java.util.List;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A visible enemy is one the tower may aim at; a ghost is one only what lands reaches. A tower that has to cast is
 * buffed to deal so much that every stone it lands fills its bars.
 */
class SplashTowerTest {

    private static final int S = BoardFixtures.SCALE;
    /** Ticks to wait for something that must happen, so a test that never gets there fails fast. */
    private static final int PATIENCE = 200;

    private final GameWorld context = WorldFixtures.newWorldOnBoard(S, 20, 20);
    private int now;

    private SplashTower tower(String... nodes) {
        SplashTower tower = new SplashTower(this.context, 3, 3);
        if (nodes.length > 0) {
            UpgradePaths.buy(tower, this.context, nodes);
        }
        return tower;
    }

    /** A tower whose every landed stone earns more than any ability costs. */
    private SplashTower casting(String... nodes) {
        return TowerFixtures.buffed(this.tower(nodes), this.context, TowerModifiers.damageTimes(1000f));
    }

    private void enemies(FakeEnemyMob... enemies) {
        this.context.enemies().setEnemies(enemies);
    }

    private void run(SplashTower tower, int ticks) {
        for (int i = 0; i < ticks; i++) {
            tower.doTick(this.now++);
        }
    }

    private void runUntil(SplashTower tower, BooleanSupplier done) {
        for (int i = 0; i < PATIENCE && !done.getAsBoolean(); i++) {
            tower.doTick(this.now++);
        }
    }

    /** Ticks until its first stone has landed, and no further. */
    private void landFirstStone(SplashTower tower) {
        this.run(tower, 1);
        this.runUntil(tower, () -> tower.stonesInFlight().isEmpty());
    }

    private static FakeEnemyMob near(SplashTower tower, float dxCells, float dyCells) {
        return FakeEnemyMob.at(tower.getX() + dxCells * S, tower.getY() + dyCells * S);
    }

    private static FakeEnemyMob ghostBeside(FakeEnemyMob enemy, float cells) {
        return FakeEnemyMob.ghostAt(enemy.getX() + cells * S, enemy.getY());
    }

    private static List<Damage> magicHits(FakeEnemyMob enemy) {
        return enemy.hits().stream().filter(hit -> hit.type() == DamageType.MAGIC).toList();
    }

    @Test
    void aStoneLandsWhereItsTargetStoodWhenItWasThrown() {
        SplashTower tower = this.tower();
        FakeEnemyMob target = near(tower, 1, 0);
        FakeEnemyMob left = FakeEnemyMob.ghostAt(target.getX(), target.getY());
        this.enemies(target, left);

        this.run(tower, 1);
        target.moveTo(target.getX(), target.getY() + 3 * SplashTower.EDGE_CELLS * S);
        this.runUntil(tower, () -> tower.stonesInFlight().isEmpty());

        assertThat(target.hits()).isEmpty();
        assertThat(left.hits()).hasSize(1);
    }

    @Test
    void nothingLandsWhileTheStoneIsInTheAir() {
        SplashTower tower = this.tower();
        FakeEnemyMob target = near(tower, 2, 0);
        this.enemies(target);

        this.run(tower, 1);

        assertThat(tower.stonesInFlight()).hasSize(1);
        assertThat(target.hits()).isEmpty();
    }

    @Test
    void theCoreTakesFullDamageTheEdgeItsShareAndBeyondTheEdgeNothing() {
        SplashTower tower = this.tower();
        int full = tower.damageCurrent();
        FakeEnemyMob target = near(tower, 1, 0);
        FakeEnemyMob inEdge = ghostBeside(target, (SplashTower.CORE_CELLS + SplashTower.EDGE_CELLS) / 2);
        FakeEnemyMob beyond = ghostBeside(target, SplashTower.EDGE_CELLS * 1.2f);
        this.enemies(target, inEdge, beyond);

        this.landFirstStone(tower);

        assertThat(target.onlyHitAmount()).isEqualTo(full);
        assertThat(inEdge.onlyHitAmount()).isEqualTo(Math.round(full * SplashTower.EDGE_SHARE));
        assertThat(beyond.hits()).isEmpty();
    }

    @Test
    void aStoneDealsWhatTheTowerHadWhenItWasThrown() {
        SplashTower tower = this.tower();
        int full = tower.damageCurrent();
        FakeEnemyMob target = near(tower, 2, 0);
        this.enemies(target);

        this.run(tower, 1);
        TowerFixtures.buffed(tower, this.context, TowerModifiers.damageTimes(2f));
        this.runUntil(tower, () -> tower.stonesInFlight().isEmpty());

        assertThat(target.onlyHitAmount()).isEqualTo(full);
    }

    @Test
    void staticDischargesIntoAnEnemyItDamagedOnceThatEnemyIsBadlyHurt() {
        SplashTower tower = this.tower("Stormcaller");
        FakeEnemyMob hurt = near(tower, 1, 0).atHealthFraction(StaticMechanic.DISCHARGE_AT / 2);
        FakeEnemyMob whole = ghostBeside(hurt, 0f);
        this.enemies(hurt, whole);

        this.landFirstStone(tower);
        this.run(tower, 1);

        assertThat(magicHits(hurt)).containsExactly(Damage.magic(DamageUnits.ofPoints(StaticMechanic.DISCHARGE_POINTS)));
        assertThat(magicHits(whole)).isEmpty();
    }

    @Test
    void theSparkADischargeSendsHomeChargesTheNextStoneWithAnArc() {
        SplashTower tower = this.tower("Arc");
        FakeEnemyMob heavy = near(tower, 1, 0).atHealthFraction(StaticMechanic.DISCHARGE_AT / 2)
                .withMaxHealthPoints(1_000_000);
        this.enemies(heavy);

        this.landFirstStone(tower);
        this.runUntil(tower, () -> !tower.stonesInFlight().isEmpty());

        assertThat(tower.stonesInFlight().getFirst().charged()).isTrue();
    }

    @Test
    void anArcJumpsFromWhereItsStoneLandsToTheNearestEnemiesAsMagic() {
        SplashTower tower = this.casting("Arc");
        int full = tower.damageCurrent();
        FakeEnemyMob target = near(tower, 1, 0);
        FakeEnemyMob next = ghostBeside(target, (SplashTower.EDGE_CELLS + ArcSpec.REACH_CELLS) / 2);
        FakeEnemyMob tooFar = ghostBeside(next, ArcSpec.REACH_CELLS * 1.2f);
        this.enemies(target, next, tooFar);

        this.runUntil(tower, () -> !magicHits(next).isEmpty());
        this.run(tower, 10);

        Damage jump = Damage.magic(Math.round(full * ArcSpec.SHARE));
        assertThat(magicHits(target)).containsExactly(jump);
        assertThat(magicHits(next)).containsExactly(jump);
        assertThat(tooFar.hits()).isEmpty();
    }

    @Test
    void wildArcStrikesALoneEnemyWithEveryJump() {
        SplashTower tower = this.casting("Wild Arc");
        FakeEnemyMob lone = near(tower, 1, 0);
        this.enemies(lone);

        this.runUntil(tower, () -> !magicHits(lone).isEmpty());
        this.run(tower, 10);

        assertThat(magicHits(lone)).hasSize(ArcSpec.JUMPS);
    }

    @Test
    void thunderclapStrikesEveryEnemyInRangeAlikeAndNothingBeyond() {
        SplashTower tower = this.casting("Thunderclap");
        FakeEnemyMob first = near(tower, 1, 0);
        FakeEnemyMob second = near(tower, -1, 0);
        FakeEnemyMob outOfRange = near(tower, 0, SplashTower.RANGE * 2);
        this.enemies(first, second, outOfRange);

        this.runUntil(tower, () -> !magicHits(first).isEmpty());

        assertThat(magicHits(first)).hasSize(1).isEqualTo(magicHits(second));
        assertThat(outOfRange.hits()).isEmpty();
    }

    @Test
    void thunderstrikeStrikesOnlyTheHealthiestEnemyInRange() {
        SplashTower tower = this.casting("Thunderstrike");
        FakeEnemyMob weak = near(tower, 1, 0).withHealth(100);
        FakeEnemyMob strong = near(tower, -1, 0).withHealth(900);
        this.enemies(weak, strong);

        this.runUntil(tower, () -> !magicHits(strong).isEmpty());

        assertThat(magicHits(strong)).hasSize(1);
        assertThat(magicHits(weak)).isEmpty();
    }

    @Test
    void aKilledEnemyTheHexerDamagedBurstsOnTheEnemiesAroundIt() {
        SplashTower tower = this.tower("Hexer");
        FakeEnemyMob victim = near(tower, 1, 0);
        victim.dieOnAnyHit();
        FakeEnemyMob close = ghostBeside(victim, SoulbrandMechanic.BURST_CELLS / 2);
        FakeEnemyMob apart = ghostBeside(victim, (SoulbrandMechanic.BURST_CELLS + SplashTower.EDGE_CELLS) / 2);
        this.enemies(victim, close, apart);

        this.landFirstStone(tower);
        this.run(tower, 1);

        assertThat(magicHits(close)).containsExactly(Damage.magic(DamageUnits.ofPoints(SoulbrandMechanic.BURST_POINTS)));
        assertThat(magicHits(apart)).isEmpty();
    }

    @Test
    void hexOfDoomCursesTheHealthiestEnemyOnlyOnceItsCurseArrives() {
        SplashTower tower = this.casting("Hex of Doom");
        FakeEnemyMob weak = near(tower, 1, 0).withHealth(100);
        FakeEnemyMob strong = near(tower, -1, 0).withHealth(900);
        this.enemies(weak, strong);

        this.runUntil(tower, () -> !tower.cursesInFlight().isEmpty());
        boolean cursedAtCast = strong.hasEffect(EffectKind.DOOM);
        this.runUntil(tower, () -> strong.hasEffect(EffectKind.DOOM));

        assertThat(cursedAtCast).isFalse();
        assertThat(strong.hasEffect(EffectKind.DOOM)).isTrue();
        assertThat(weak.hasEffect(EffectKind.DOOM)).isFalse();
    }

    @Test
    void whenDoomEndsItsEnemyTakesItsShareOfWhatItTookMeanwhileAsPeriodicMagic() {
        SplashTower tower = this.casting("Hex of Doom");
        FakeEnemyMob enemy = near(tower, 1, 0);
        this.enemies(enemy);
        this.runUntil(tower, () -> enemy.hasEffect(EffectKind.DOOM));
        enemy.doDamage(Damage.physical(10_000), AttackProfile.none());
        int hitsBeforeEnd = enemy.hits().size();

        enemy.expire(EffectKind.DOOM);
        this.run(tower, 1);

        assertThat(enemy.hits().get(hitsBeforeEnd)).isEqualTo(Damage.magic(Math.round(10_000 * HexSpec.DOOM_SHARE)));
        assertThat(enemy.attackers().get(hitsBeforeEnd).delivery().name()).isEqualTo("PERIODIC");
    }

    @Test
    void twinHexCursesTwoEnemiesWithOneCast() {
        SplashTower tower = this.casting("Hex of Doom", "Twin Hex");
        FakeEnemyMob first = near(tower, 1, 0).withHealth(100);
        FakeEnemyMob second = near(tower, -1, 0).withHealth(900);
        this.enemies(first, second);

        this.runUntil(tower, () -> !tower.cursesInFlight().isEmpty());

        assertThat(tower.cursesInFlight()).extracting(SplashTower.Curse::target).containsExactlyInAnyOrder(first, second);
    }

    @Test
    void agonyMakesAHexedEnemyVulnerable() {
        SplashTower tower = this.casting("Hex of Doom", "Agony");
        FakeEnemyMob enemy = near(tower, 1, 0);
        this.enemies(enemy);

        this.runUntil(tower, () -> enemy.hasEffect(EffectKind.DOOM));

        assertThat(enemy.hasEffect(EffectKind.VULNERABLE)).isTrue();
    }

    @Test
    void chokingBlightSilencesWhatBlightCurses() {
        SplashTower tower = this.casting("Hex of Blight", "Choking Blight");
        FakeEnemyMob enemy = near(tower, 1, 0);
        this.enemies(enemy);

        this.runUntil(tower, () -> enemy.hasEffect(EffectKind.BLIGHT));

        assertThat(enemy.hasEffect(EffectKind.SILENCED)).isTrue();
    }

    @Test
    void blightCloudAlsoCursesTheEnemiesAroundItsTarget() {
        SplashTower tower = this.casting("Hex of Blight", "Blight Cloud");
        FakeEnemyMob target = near(tower, 1, 0);
        FakeEnemyMob close = ghostBeside(target, HexSpec.CLOUD_CELLS / 2);
        FakeEnemyMob apart = ghostBeside(target, HexSpec.CLOUD_CELLS * 1.5f);
        this.enemies(target, close, apart);

        this.runUntil(tower, () -> target.hasEffect(EffectKind.BLIGHT));

        assertThat(close.hasEffect(EffectKind.BLIGHT)).isTrue();
        assertThat(apart.hasEffect(EffectKind.BLIGHT)).isFalse();
    }

    @Test
    void contagionSendsTheHexOfAnEnemyThatDiedToTheNearestEnemyWithoutOne() {
        SplashTower tower = this.casting("Hex of Doom", "Contagion");
        FakeEnemyMob carrier = near(tower, 1, 0);
        FakeEnemyMob nearest = ghostBeside(carrier, HexSpec.SPREAD_CELLS / 3);
        FakeEnemyMob further = ghostBeside(carrier, HexSpec.SPREAD_CELLS * 2 / 3);
        this.enemies(carrier, nearest, further);
        this.runUntil(tower, () -> carrier.hasEffect(EffectKind.DOOM));

        carrier.dieOnAnyHit();
        carrier.doDamage(Damage.physical(1), AttackProfile.none());
        this.runUntil(tower, () -> nearest.hasEffect(EffectKind.DOOM));

        assertThat(nearest.hasEffect(EffectKind.DOOM)).isTrue();
        assertThat(further.hasEffect(EffectKind.DOOM)).isFalse();
    }

    @Test
    void theForkNamesTheTower() {
        SplashTower plain = this.tower();
        SplashTower stormcaller = this.tower("Stormcaller");
        SplashTower hexer = this.tower("Hexer");

        assertThat(List.of(plain.inspect().name(), stormcaller.inspect().name(), hexer.inspect().name()))
                .doesNotHaveDuplicates();
    }

    @Test
    void everyNodeBelongsToTheForkSideItsPathCrosses() {
        SplashTower tower = this.tower();

        assertThat(SplashTower.forkOf(UpgradePaths.named(tower, "Arc"))).isEqualTo(SplashTower.Fork.STORMCALLER);
        assertThat(SplashTower.forkOf(UpgradePaths.named(tower, "Agony"))).isEqualTo(SplashTower.Fork.HEXER);
        assertThat(SplashTower.forkOf(UpgradePaths.named(tower, "Heavy Stones"))).isEqualTo(SplashTower.Fork.NONE);
    }
}

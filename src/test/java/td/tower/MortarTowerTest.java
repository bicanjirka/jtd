package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.damage.DamageUnits;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyFactory;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.fixtures.BoardFixtures;
import td.fixtures.FakeEnemyMob;
import td.fixtures.TowerFixtures;
import td.fixtures.WorldFixtures;
import td.projectile.CannonballProjectile;
import td.tower.upgrade.UpgradeNode;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class MortarTowerTest {

    private static final int SCALE = BoardFixtures.SCALE;
    /** Far enough out to be past the dead zone and still in range. */
    private static final int OUT = 3 * SCALE;

    private final GameWorld context = WorldFixtures.newWorldOnBoard(SCALE, 20, 20);
    private int tick = 1;

    private MortarTower towerAt(int cellX, int cellY) {
        return new MortarTower(this.context, cellX, cellY);
    }

    private MortarTower attunedTower() {
        MortarTower tower = this.towerAt(3, 3);
        UpgradePaths.buy(tower, this.context);
        return tower;
    }

    private FakeEnemyMob enemyAt(MortarTower tower, double dx, double dy) {
        return FakeEnemyMob.at(tower.getX() + dx, tower.getY() + dy);
    }

    /** Ticks until the tower fires, then flies the shell to where it lands. */
    private void landAShell(MortarTower tower) {
        for (int i = 0; i < 80 && this.context.projectiles().getProjectiles().isEmpty(); i++) {
            tower.doTick(this.tick++);
        }
        TowerFixtures.flyProjectilesToCompletion(this.context);
    }

    private static int hitAmount(FakeEnemyMob enemy, int index) {
        return enemy.hits().get(index).amount();
    }

    @Test
    void firingLaunchesExactlyOneShellAtTheTarget() {
        MortarTower tower = this.towerAt(3, 3);
        this.context.enemies().setEnemies(new EnemyMob[]{this.enemyAt(tower, OUT, 0)});

        tower.doTick(1);

        assertThat(this.context.projectiles().getProjectiles()).hasSize(1);
    }

    @Test
    void aPriorityEnemyInRangeIsAimedAtBeforeTheOneFurthestAlongThePath() {
        MortarTower tower = this.towerAt(3, 3);
        FakeEnemyMob furthest = this.enemyAt(tower, 0, -OUT).withProgression(900);
        FakeEnemyMob prioritised = this.enemyAt(tower, 0, OUT).withProgression(10);
        prioritised.applyEffect(Effect.priority(100, d -> {
        }));
        this.context.enemies().setEnemies(new EnemyMob[]{furthest, prioritised});

        this.landAShell(tower);

        assertThat(prioritised.hits()).isNotEmpty();
        assertThat(furthest.hits()).isEmpty();
    }

    @Test
    void theShellIsSlowAndFliesAtItsOwnSpeed() {
        MortarTower tower = this.towerAt(3, 3);
        this.context.enemies().setEnemies(new EnemyMob[]{this.enemyAt(tower, OUT, 0)});
        tower.doTick(1);
        CannonballProjectile shell = (CannonballProjectile) this.context.projectiles().getProjectiles().getFirst();

        this.context.projectiles().doTick(2);

        assertThat(shell.getX() - tower.getX()).isEqualTo(MortarTower.SHELL_SPEED);
    }

    @Test
    void theBlastDealsFullDamageAtItsCentreAndCracksPlatingWithoutSlowing() {
        MortarTower tower = this.towerAt(3, 3);
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        this.landAShell(tower);

        assertThat(target.onlyHitAmount()).isEqualTo(DamageUnits.ofPoints(MortarTower.DAMAGE_POINTS));
        assertThat(target.hits().getFirst().type()).isEqualTo(td.damage.DamageType.PHYSICAL);
        assertThat(target.activeEffectKinds()).containsExactly(EffectKind.CRACKED);
    }

    @Test
    void theBlastFallsOffWithDistanceFromTheCentreAndStopsAtItsRadius() {
        MortarTower tower = this.towerAt(3, 3);
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        float radius = MortarTower.SPLASH_RADIUS_BASE * SCALE;
        FakeEnemyMob half = this.enemyAt(tower, OUT, radius / 2).hidden();
        FakeEnemyMob beyond = this.enemyAt(tower, OUT, radius + 2).hidden();
        this.context.enemies().setEnemies(new EnemyMob[]{target, half, beyond});

        this.landAShell(tower);

        assertThat(half.onlyHitAmount()).isCloseTo(Math.round(DamageUnits.ofPoints(MortarTower.DAMAGE_POINTS) * 0.75f), within(1));
        assertThat(beyond.hits()).isEmpty();
    }

    @Test
    void aGhostIsNeverSelectedAsTheTargetButIsStillCaughtByTheBlast() {
        MortarTower tower = this.towerAt(3, 3);
        FakeEnemyMob normal = this.enemyAt(tower, OUT, 0);
        FakeEnemyMob ghost = FakeEnemyMob.ghostAt(tower.getX() + OUT + 5, tower.getY());
        this.context.enemies().setEnemies(new EnemyMob[]{normal, ghost});

        this.landAShell(tower);

        assertThat(normal.hits()).hasSize(1);
        assertThat(ghost.hits()).hasSize(1);
    }

    @Test
    void noTargetInRangeFiresNoShell() {
        MortarTower tower = this.towerAt(3, 3);
        this.context.enemies().setEnemies(new EnemyMob[]{});

        tower.doTick(1);

        assertThat(this.context.projectiles().getProjectiles()).isEmpty();
    }

    @Test
    void anEnemyInsideTheDeadZoneIsNeverShelled() {
        MortarTower tower = this.towerAt(3, 3);
        this.context.enemies().setEnemies(new EnemyMob[]{this.enemyAt(tower, SCALE, 0)});

        tower.doTick(1);

        assertThat(this.context.projectiles().getProjectiles()).isEmpty();
    }

    @Test
    void aShellIsDrawnBiggerTheHarderItHits() {
        MortarTower plain = this.towerAt(3, 3);
        MortarTower heavy = this.towerAt(8, 3);
        UpgradePaths.buy(heavy, this.context, "Siege Rounds");
        this.context.enemies().setEnemies(new EnemyMob[]{this.enemyAt(plain, OUT, 0), this.enemyAt(heavy, OUT, 0)});

        plain.doTick(1);
        heavy.doTick(1);

        float plainSize = ((CannonballProjectile) this.context.projectiles().getProjectiles().get(0)).stats().size();
        float heavySize = ((CannonballProjectile) this.context.projectiles().getProjectiles().get(1)).stats().size();
        assertThat(plainSize).isEqualTo(1f);
        assertThat(heavySize).isGreaterThan(plainSize);
    }

    @Test
    void withoutAttuneShellsOnTheSameSpotAddNothing() {
        MortarTower tower = this.towerAt(3, 3);
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        this.landAShell(tower);
        this.landAShell(tower);

        assertThat(hitAmount(target, 1)).isEqualTo(hitAmount(target, 0));
        assertThat(tower.getRangingMarker()).isEmpty();
    }

    @Test
    void attunedEachShellOnTheSpotOfTheLastHitsTenPercentHarderUpToThreeSteps() {
        MortarTower tower = this.attunedTower();
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        for (int i = 0; i < 5; i++) {
            this.landAShell(tower);
        }

        int base = DamageUnits.ofPoints(MortarTower.DAMAGE_POINTS);
        assertThat(target.hits()).extracting(Damage::amount).containsExactly(base, Math.round(base * 1.1f),
                Math.round(base * 1.2f), Math.round(base * 1.3f), Math.round(base * 1.3f));
    }

    @Test
    void aBracketedShellBlastsTenPercentWiderPerStep() {
        MortarTower tower = this.attunedTower();
        float radius = MortarTower.SPLASH_RADIUS_BASE * SCALE;
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        FakeEnemyMob edge = this.enemyAt(tower, OUT, radius * 1.05f).hidden();
        this.context.enemies().setEnemies(new EnemyMob[]{target, edge});

        this.landAShell(tower);
        boolean caughtByTheFirst = !edge.hits().isEmpty();
        this.landAShell(tower);

        assertThat(caughtByTheFirst).isFalse();
        assertThat(edge.hits()).hasSize(1);
    }

    @Test
    void aShellLandingElsewhereStartsTheBracketOver() {
        MortarTower tower = this.attunedTower();
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        this.context.enemies().setEnemies(new EnemyMob[]{target});
        this.landAShell(tower);
        this.landAShell(tower);

        target.moveTo(tower.getX(), tower.getY() + OUT);
        this.landAShell(tower);

        int base = DamageUnits.ofPoints(MortarTower.DAMAGE_POINTS);
        assertThat(hitAmount(target, 2)).isEqualTo(base);
        assertThat(tower.getRangingMarker().orElseThrow().step()).isZero();
    }

    @Test
    void theRangingMarkerSitsOnTheLastLandingAndTightensWithEachStep() {
        MortarTower tower = this.attunedTower();
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        this.landAShell(tower);
        this.landAShell(tower);

        assertThat(tower.getRangingMarker().orElseThrow().x()).isEqualTo(target.getX());
        assertThat(tower.getRangingMarker().orElseThrow().step()).isEqualTo(1);
    }

    @Test
    void everyBracketedShellCountsAsADeedAndTheFirstDoesNot() {
        MortarTower tower = this.attunedTower();
        this.context.enemies().setEnemies(new EnemyMob[]{this.enemyAt(tower, OUT, 0)});

        this.landAShell(tower);
        int afterTheFirst = tower.experience().deeds();
        this.tick += 5;
        this.landAShell(tower);

        assertThat(afterTheFirst).isZero();
        assertThat(tower.experience().deeds()).isEqualTo(1);
    }

    @Test
    void siegeRoundsIsChoosableOnceAwakenIsBoughtAndAppliesItsDamageBonus() {
        this.context.economy().startEconomy(1000, 5);
        MortarTower tower = this.towerAt(3, 3);
        UpgradePaths.awakenVeteran(tower);
        EnemyMob fodder = EnemyFactory.getEnemy("c", this.context, 0, 100000, 3, Rank.GRUNT);
        tower.dealDamage(fodder, Damage.physical(16000));
        UpgradeNode siegeRounds = UpgradePaths.named(tower, "Siege Rounds");

        boolean chosen = tower.buyUpgrade(siegeRounds);

        assertThat(chosen).isTrue();
        assertThat(tower.damageCurrent()).isGreaterThan(tower.damageBase);
    }

    @Test
    void siegeRoundsMakeEachBracketStepAddFifteenPercent() {
        MortarTower tower = this.towerAt(3, 3);
        UpgradePaths.buy(tower, this.context, "Siege Rounds");
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        this.landAShell(tower);
        this.landAShell(tower);

        assertThat(hitAmount(target, 1)).isEqualTo(Math.round(hitAmount(target, 0) * 1.15f));
    }

    @Test
    void siegeRoundsTwoWidensTheBlastByAQuarterAndAddsCritChance() {
        MortarTower tower = this.towerAt(3, 3);
        float before = tower.getSplashRadius();
        float critBefore = tower.critChance();

        UpgradePaths.buy(tower, this.context, "Siege Rounds", "Siege Rounds II");

        assertThat(tower.getSplashRadius()).isEqualTo(before * 1.25f);
        assertThat(tower.critChance()).isCloseTo(critBefore + 0.1f, within(1e-5f));
    }

    @Test
    void heavyShellFliesSlowerAndDazesWhatItLandsWithinHalfACellOf() {
        MortarTower tower = this.towerAt(3, 3);
        this.context.playtestRules().setUpgradeGatesIgnored(true);
        UpgradePaths.buy(tower, this.context, "Siege Rounds", "Siege Rounds II", "Heavy Shell");
        FakeEnemyMob centre = this.enemyAt(tower, OUT, 0);
        FakeEnemyMob near = this.enemyAt(tower, OUT, SCALE * 0.4).hidden();
        FakeEnemyMob far = this.enemyAt(tower, OUT, SCALE * 0.8).hidden();
        this.context.enemies().setEnemies(new EnemyMob[]{centre, near, far});
        tower.doTick(1);
        CannonballProjectile shell = (CannonballProjectile) this.context.projectiles().getProjectiles().getFirst();
        float speed = shell.stats().speed();

        this.landAShell(tower);

        assertThat(speed).isLessThan(MortarTower.SHELL_SPEED);
        assertThat(centre.activeEffectKinds()).contains(EffectKind.DAZED);
        assertThat(near.activeEffectKinds()).contains(EffectKind.DAZED);
        assertThat(far.activeEffectKinds()).doesNotContain(EffectKind.DAZED);
    }

    @Test
    void heavyShellWaitsOnFifteenBracketedShells() {
        MortarTower tower = this.towerAt(3, 3);
        this.context.economy().startEconomy(1_000_000, 5);
        UpgradePaths.awakenVeteran(tower);
        UpgradePaths.named(tower, "Siege Rounds");
        tower.buyUpgrade(UpgradePaths.named(tower, "Siege Rounds"));
        tower.buyUpgrade(UpgradePaths.named(tower, "Siege Rounds II"));

        boolean early = tower.buyUpgrade(UpgradePaths.named(tower, "Heavy Shell"));

        assertThat(early).isFalse();
        assertThat(UpgradePaths.named(tower, "Heavy Shell").gate().describe()).contains("15 Bracketed shells");
    }
}

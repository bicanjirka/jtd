package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.damage.DamageType;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.fixtures.BoardFixtures;
import td.fixtures.FakeEnemyMob;
import td.fixtures.TowerFixtures;
import td.fixtures.UpgradePaths;
import td.fixtures.WorldFixtures;
import td.projectile.CannonballProjectile;
import td.projectile.Projectile;
import td.projectile.ShellLook;
import td.stat.StatModifiers;
import td.util.GameWorld;
import td.wave.PathNormal;
import td.wave.Vec2;
import td.zone.Zone;
import td.zone.ZoneKind;

import java.util.ArrayList;
import java.util.List;

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
        return this.towerWith("Attune");
    }

    private MortarTower towerWith(String... nodes) {
        MortarTower tower = this.towerAt(3, 3);
        UpgradePaths.buy(tower, this.context, nodes);
        return tower;
    }

    private FakeEnemyMob enemyAt(MortarTower tower, double dx, double dy) {
        return FakeEnemyMob.at(tower.getX() + dx, tower.getY() + dy);
    }

    /** Ticks until the tower fires, then flies the shell to where it lands. */
    private void landAShell(MortarTower tower) {
        for (int i = 0; i < 200 && this.context.projectiles().getProjectiles().isEmpty(); i++) {
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
        prioritised.applyEffect(Effect.priority(100, DamageSink.none()));
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

        assertThat(target.onlyHitAmount()).isEqualTo(tower.damageCurrent());
        assertThat(target.hits().getFirst().type()).isEqualTo(DamageType.PHYSICAL);
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

        assertThat(half.onlyHitAmount()).isCloseTo(Math.round(tower.damageCurrent() * 0.75f), within(1));
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
        MortarTower tower = this.towerAt(3, 3);
        this.context.enemies().setEnemies(new EnemyMob[]{this.enemyAt(tower, OUT, 0)});
        float plainSize = ((CannonballProjectile) this.fire(tower)).stats().size();
        TowerFixtures.flyProjectilesToCompletion(this.context);
        TowerFixtures.buffed(tower, this.context, TowerModifiers.damage(50f));

        float buffedSize = ((CannonballProjectile) this.fire(tower)).stats().size();

        assertThat(buffedSize).isGreaterThan(plainSize);
    }

    @Test
    void aShellFliesAtItsTowersProjectileSpeedAndSize() {
        MortarTower plain = this.towerAt(3, 3);
        MortarTower tuned = TowerFixtures.buffed(this.towerAt(8, 3), this.context,
                TowerModifiers.projectileSpeed(0.5f).plus(TowerModifiers.projectileSize(0.5f)));
        this.context.enemies().setEnemies(new EnemyMob[]{this.enemyAt(plain, OUT, 0), this.enemyAt(tuned, OUT, 0)});

        plain.doTick(1);
        tuned.doTick(1);

        CannonballProjectile slow = (CannonballProjectile) this.context.projectiles().getProjectiles().get(0);
        CannonballProjectile fast = (CannonballProjectile) this.context.projectiles().getProjectiles().get(1);
        assertThat(fast.stats().speed()).isGreaterThan(slow.stats().speed());
        assertThat(fast.stats().size()).isGreaterThan(slow.stats().size());
    }

    @Test
    void aWiderBlastRadiusCatchesWhatThePlainBlastMisses() {
        MortarTower tower = TowerFixtures.buffed(this.towerAt(3, 3), this.context, TowerModifiers.areaRadius(1f));
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        FakeEnemyMob pastThePlainRadius = this.enemyAt(tower, OUT, MortarTower.SPLASH_RADIUS_BASE * SCALE + 4).hidden();
        this.context.enemies().setEnemies(new EnemyMob[]{target, pastThePlainRadius});

        this.landAShell(tower);

        assertThat(pastThePlainRadius.hits()).hasSize(1);
    }

    @Test
    void aBiggerBracketStepMakesTheNextShellOnTheSpotHitHarder() {
        int plain = this.secondBracketedHit(this.attunedTower());

        int widerStep = this.secondBracketedHit(
                TowerFixtures.buffed(this.attunedTower(), this.context, TowerModifiers.bracketDamageStep(0.2f)));

        assertThat(widerStep).isGreaterThan(plain);
    }

    /** What {@code tower}'s second shell on the spot of its first deals. */
    private int secondBracketedHit(MortarTower tower) {
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        this.context.enemies().setEnemies(new EnemyMob[]{target});
        this.landAShell(tower);
        this.landAShell(tower);
        return hitAmount(target, 1);
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
    void attunedEachShellOnTheSpotOfTheLastHitsHarderThanTheOneBefore() {
        MortarTower tower = this.attunedTower();
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        this.landAShell(tower);
        this.landAShell(tower);
        this.landAShell(tower);

        assertThat(hitAmount(target, 0)).isEqualTo(tower.damageCurrent());
        assertThat(hitAmount(target, 1)).isGreaterThan(hitAmount(target, 0));
        assertThat(hitAmount(target, 2)).isGreaterThan(hitAmount(target, 1));
    }

    @Test
    void aBracketedShellBlastsWiderThanTheOneBefore() {
        MortarTower tower = this.attunedTower();
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        FakeEnemyMob edge = this.enemyAt(tower, OUT, tower.getSplashRadius() + 1).hidden();
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

        assertThat(hitAmount(target, 2)).isEqualTo(hitAmount(target, 0));
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
    void heavyShellFliesSlowerAndDazesWhatItLandsWithinHalfACellOf() {
        MortarTower tower = this.towerWith("Heavy Shell");
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

    private List<ZoneKind> zoneKinds() {
        return this.context.zones().zones().stream().map(Zone::kind).toList();
    }

    @Test
    void everyThirdShellIsTheSpecialShellAndLeavesItsZoneWhereItLands() {
        MortarTower tower = this.towerAt(3, 3);
        UpgradePaths.buy(tower, this.context, "Cryo Shells");
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        this.landAShell(tower);
        this.landAShell(tower);
        List<ZoneKind> afterTwo = this.zoneKinds();
        this.landAShell(tower);

        assertThat(afterTwo).isEmpty();
        assertThat(this.zoneKinds()).containsExactly(ZoneKind.FROST_GROUND);
        Zone frost = this.context.zones().zones().getFirst();
        assertThat(frost.x()).isEqualTo(target.getX());
    }

    @Test
    void withTwoSpecialsOwnedTheyTakeTurnsAndEveryThirdShellIsPlain() {
        MortarTower tower = this.towerWith("Napalm", "Transcendent", "Cryo Shells");
        this.context.enemies().setEnemies(new EnemyMob[]{this.enemyAt(tower, OUT, 0)});

        for (int i = 0; i < 8; i++) {
            this.landAShell(tower);
        }

        assertThat(this.zoneKinds()).containsExactly(ZoneKind.BURNING_GROUND, ZoneKind.FROST_GROUND,
                ZoneKind.BURNING_GROUND, ZoneKind.FROST_GROUND, ZoneKind.BURNING_GROUND, ZoneKind.FROST_GROUND);
    }

    @Test
    void aSpecialShellFliesInItsOwnLook() {
        MortarTower tower = this.towerAt(3, 3);
        UpgradePaths.buy(tower, this.context, "Tar");
        this.context.enemies().setEnemies(new EnemyMob[]{this.enemyAt(tower, OUT, 0)});
        List<ShellLook> looks = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            for (int t = 0; t < 200 && this.context.projectiles().getProjectiles().isEmpty(); t++) {
                tower.doTick(this.tick++);
            }
            looks.add(((CannonballProjectile) this.context.projectiles().getProjectiles().getFirst()).look());
            TowerFixtures.flyProjectilesToCompletion(this.context);
        }

        assertThat(looks).containsExactly(ShellLook.PLAIN, ShellLook.PLAIN, ShellLook.TAR);
    }

    @Test
    void aNapalmShellBlastsForMagicAndItsGroundSetsEnemiesAlightCreditedToTheMortar() {
        MortarTower tower = this.towerAt(3, 3);
        UpgradePaths.buy(tower, this.context, "Napalm");
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        this.context.enemies().setEnemies(new EnemyMob[]{target});
        this.landAShell(tower);
        this.landAShell(tower);

        this.landAShell(tower);
        this.context.zones().doTick(1);

        assertThat(target.hits()).extracting(Damage::type).containsExactly(DamageType.PHYSICAL,
                DamageType.PHYSICAL, DamageType.MAGIC);
        assertThat(target.activeEffectKinds()).contains(EffectKind.BURN);
    }

    @Test
    void aTarShellLeavesTarWhereItLands() {
        MortarTower tower = this.towerWith("Tar");
        this.context.enemies().setEnemies(new EnemyMob[]{this.enemyAt(tower, OUT, 0)});

        for (int i = 0; i < 3; i++) {
            this.landAShell(tower);
        }

        assertThat(this.zoneKinds()).containsExactly(ZoneKind.TAR);
    }

    @Test
    void longBatteryWidensTheDeadZoneAndMoreRangeNeverMovesIt() {
        MortarTower tower = this.towerWith("Transcendent");
        this.context.enemies().setEnemies(new EnemyMob[]{this.enemyAt(tower, 2 * SCALE, 0)});
        tower.doTick(1);
        boolean shelledBefore = !this.context.projectiles().getProjectiles().isEmpty();
        this.context.projectiles().clear();

        UpgradePaths.buy(tower, this.context, "Range III");
        for (int t = 2; t < 200; t++) {
            tower.doTick(t);
        }

        assertThat(shelledBefore).isTrue();
        assertThat(this.context.projectiles().getProjectiles()).isEmpty();
    }

    @Test
    void everyFourthShellIsANukeThatHitsFourTimesAsHardOverAWiderBlastAndLeavesFallout() {
        MortarTower tower = this.towerWith("Napalm", "Tactical Nuke");
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        FakeEnemyMob wide = this.enemyAt(tower, OUT, tower.getSplashRadius() * 1.4f).hidden();
        this.context.enemies().setEnemies(new EnemyMob[]{target, wide});

        for (int i = 0; i < 4; i++) {
            this.landAShell(tower);
        }

        assertThat(target.hits()).hasSize(4);
        assertThat(hitAmount(target, 0)).isEqualTo(tower.damageCurrent());
        assertThat(hitAmount(target, 3)).isGreaterThan(3 * hitAmount(target, 1));
        assertThat(wide.hits()).hasSize(1);
        assertThat(this.zoneKinds()).containsExactly(ZoneKind.BURNING_GROUND, ZoneKind.FALLOUT);
    }

    @Test
    void aNukesFalloutLiesWhereItLandedAndDrainsSpiritAndKeepsHealsOut() {
        MortarTower tower = this.towerWith("Tactical Nuke");
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        this.context.enemies().setEnemies(new EnemyMob[]{target});
        for (int i = 0; i < 4; i++) {
            this.landAShell(tower);
        }

        Zone fallout = this.context.zones().zones().getLast();
        this.context.zones().doTick(1);

        assertThat(fallout.x()).isEqualTo(target.getX());
        assertThat(target.activeEffectKinds()).contains(EffectKind.SICKENED, EffectKind.FALLOUT);
    }

    @Test
    void aNukeShellFliesBiggerAndInItsOwnLook() {
        MortarTower tower = this.towerWith("Tactical Nuke");
        this.context.enemies().setEnemies(new EnemyMob[]{this.enemyAt(tower, OUT, 0)});
        for (int i = 0; i < 2; i++) {
            this.landAShell(tower);
        }
        float plainSize = ((CannonballProjectile) this.fire(tower)).stats().size();
        TowerFixtures.flyProjectilesToCompletion(this.context);

        CannonballProjectile nuke = (CannonballProjectile) this.fire(tower);

        assertThat(nuke.look()).isEqualTo(ShellLook.NUKE);
        assertThat(nuke.stats().size()).isGreaterThan(plainSize);
    }

    private Projectile fire(MortarTower tower) {
        for (int t = 0; t < 200 && this.context.projectiles().getProjectiles().isEmpty(); t++) {
            tower.doTick(this.tick++);
        }
        return this.context.projectiles().getProjectiles().getFirst();
    }

    @Test
    void bunkerBusterHitsTheEnemyAtTheCentreHarderAndSundersItButNotThoseAroundIt() {
        MortarTower tower = this.towerWith("Bunker Buster");
        FakeEnemyMob centre = this.enemyAt(tower, OUT, 0);
        FakeEnemyMob beside = this.enemyAt(tower, OUT, SCALE * 0.8).hidden();
        this.context.enemies().setEnemies(new EnemyMob[]{centre, beside});

        this.landAShell(tower);

        assertThat(centre.onlyHitAmount()).isGreaterThan(tower.damageCurrent());
        assertThat(centre.activeEffectKinds()).contains(EffectKind.SUNDERED);
        assertThat(beside.onlyHitAmount()).isLessThan(tower.damageCurrent());
        assertThat(beside.activeEffectKinds()).doesNotContain(EffectKind.SUNDERED);
    }

    /** A straight path along the row 3 cells below the tower, for bomblets to run along. */
    private void pathBelow(MortarTower tower) {
        this.context.setPath(new PathNormal(List.of(new Vec2(0, tower.getY() + OUT), new Vec2(900, tower.getY() + OUT))));
    }

    @Test
    void fragmentationRoundsHitEnemiesInTheRingPastTheBlastForLessAndCrackThem() {
        MortarTower tower = this.towerWith("Fragmentation Rounds");
        float radius = MortarTower.SPLASH_RADIUS_BASE * SCALE;
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        FakeEnemyMob ring = this.enemyAt(tower, OUT, radius + 20).hidden();
        FakeEnemyMob outside = this.enemyAt(tower, OUT, 3 * radius).hidden();
        this.context.enemies().setEnemies(new EnemyMob[]{target, ring, outside});

        this.landAShell(tower);

        assertThat(ring.onlyHitAmount()).isLessThan(target.onlyHitAmount());
        assertThat(ring.activeEffectKinds()).containsExactly(EffectKind.CRACKED);
        assertThat(target.hits()).hasSize(1);
        assertThat(outside.hits()).isEmpty();
    }

    @Test
    void shrapnelReachGrowsWithEachBracketingStepBeyondTheWideningOfTheBlast() {
        MortarTower tower = this.towerAt(3, 3);
        UpgradePaths.buy(tower, this.context, "Fragmentation Rounds");
        float ring = 1.75f * MortarTower.SPLASH_RADIUS_BASE * SCALE;
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        FakeEnemyMob far = this.enemyAt(tower, OUT, ring + 0.2f * SCALE).hidden();
        this.context.enemies().setEnemies(new EnemyMob[]{target, far});

        this.landAShell(tower);
        int atStepZero = far.hits().size();
        this.landAShell(tower);

        assertThat(atStepZero).isZero();
        assertThat(far.hits()).hasSize(1);
    }

    @Test
    void fragmentationRoundsTwoMakeTheShrapnelCarryTheShellsOwnEffect() {
        MortarTower tower = this.towerWith("Fragmentation Rounds II", "Napalm");
        float radius = MortarTower.SPLASH_RADIUS_BASE * SCALE * 1.0f;
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        FakeEnemyMob ring = this.enemyAt(tower, OUT, radius + 20).hidden();
        this.context.enemies().setEnemies(new EnemyMob[]{target, ring});
        this.pathBelow(tower);

        this.landAShell(tower);
        this.landAShell(tower);
        long burningBefore = ring.activeEffectKinds().stream().filter(EffectKind.BURN::equals).count();
        this.landAShell(tower);

        assertThat(burningBefore).isZero();
        assertThat(ring.activeEffectKinds()).contains(EffectKind.BURN);
        assertThat(this.zoneKinds()).containsExactly(ZoneKind.BURNING_GROUND);
    }

    @Test
    void shrapnelNeverLeavesAZoneOfItsOwn() {
        MortarTower tower = this.towerAt(3, 3);
        UpgradePaths.buy(tower, this.context, "Fragmentation Rounds");
        this.context.enemies().setEnemies(new EnemyMob[]{this.enemyAt(tower, OUT, 0)});

        this.landAShell(tower);

        assertThat(this.zoneKinds()).isEmpty();
    }

    @Test
    void clusterShellScattersFourBombletsAlongThePathAroundTheImpact() {
        MortarTower tower = this.towerWith("Cluster Shell");
        this.pathBelow(tower);
        FakeEnemyMob target = FakeEnemyMob.at(tower.getX() + OUT, tower.getY() + OUT);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        this.landAShell(tower);

        List<Double> xs = tower.getBlastMarks().stream().map(mark -> mark.at().x() - target.getX())
                .sorted().toList();
        assertThat(xs).hasSize(5);
        assertThat(xs.get(0)).isCloseTo(-1.6 * SCALE, within(0.01));
        assertThat(xs.get(1)).isCloseTo(-0.8 * SCALE, within(0.01));
        assertThat(xs.get(2)).isCloseTo(0.0, within(0.01));
        assertThat(xs.get(4)).isCloseTo(1.6 * SCALE, within(0.01));
    }

    @Test
    void aBombletHitsWhatItLandsBesideAndCracksIt() {
        MortarTower tower = this.towerWith("Cluster Shell");
        this.pathBelow(tower);
        FakeEnemyMob target = FakeEnemyMob.at(tower.getX() + OUT, tower.getY() + OUT);
        FakeEnemyMob aside = FakeEnemyMob.at(target.getX() + 0.8 * SCALE, target.getY() + 0.5 * SCALE).hidden();
        this.context.enemies().setEnemies(new EnemyMob[]{target, aside});

        this.landAShell(tower);

        assertThat(aside.hits()).isNotEmpty();
        assertThat(aside.activeEffectKinds()).contains(EffectKind.CRACKED);
    }

    @Test
    void carpetBombingLaysEightBombletsInALineAheadOfTheImpact() {
        MortarTower tower = this.towerWith("Carpet Bombing");
        this.pathBelow(tower);
        FakeEnemyMob target = FakeEnemyMob.at(tower.getX() + OUT, tower.getY() + OUT);
        FakeEnemyMob ahead = FakeEnemyMob.at(target.getX() + 5.6 * SCALE, target.getY()).hidden();
        this.context.enemies().setEnemies(new EnemyMob[]{target, ahead});

        this.landAShell(tower);

        assertThat(tower.getBlastMarks()).hasSize(9);
        assertThat(ahead.hits()).isNotEmpty();
    }

    @Test
    void shrapnelStormLeavesWhatTheShrapnelHitsBleedingButNotTheMainBlast() {
        MortarTower tower = this.towerWith("Shrapnel Storm");
        float radius = MortarTower.SPLASH_RADIUS_BASE * SCALE;
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        FakeEnemyMob ring = this.enemyAt(tower, OUT, radius + 20).hidden();
        this.context.enemies().setEnemies(new EnemyMob[]{target, ring});
        this.pathBelow(tower);

        this.landAShell(tower);

        assertThat(ring.activeEffectKinds()).contains(EffectKind.BLEEDING);
        assertThat(target.activeEffectKinds()).doesNotContain(EffectKind.BLEEDING);
    }

    @Test
    void predictiveFireAimsWhereAWalkingEnemyWillBeWhenTheShellLands() {
        MortarTower plain = this.towerAt(3, 3);
        MortarTower predictive = this.towerAt(12, 3);
        UpgradePaths.buy(predictive, this.context, "Predictive Fire");
        FakeEnemyMob walker = this.enemyAt(plain, OUT, 0).walkingBy(0, 2);
        FakeEnemyMob walker2 = this.enemyAt(predictive, OUT, 0).walkingBy(0, 2);
        this.context.enemies().setEnemies(new EnemyMob[]{walker, walker2});

        this.landAShell(plain);
        this.landAShell(predictive);

        double plainLanding = plain.getBlastMarks().isEmpty() ? Double.NaN : plain.getBlastMarks().getFirst().at().y();
        double predictiveLanding = predictive.getBlastMarks().getFirst().at().y();
        assertThat(plainLanding).isEqualTo(walker.getY());
        assertThat(predictiveLanding).isGreaterThan(walker2.getY());
    }

    @Test
    void predictiveFireLeadsAFasterShellLess() {
        double slowLead = predictiveLead(StatModifiers.none());

        double fastLead = predictiveLead(TowerModifiers.projectileSpeed(1f));

        assertThat(fastLead).isPositive().isLessThan(slowLead);
    }

    /** How far ahead of a walking enemy a Predictive Fire Mortar given {@code buff} lands its shell. */
    private static double predictiveLead(StatModifiers<TowerStat> buff) {
        GameWorld world = WorldFixtures.newWorldOnBoard(SCALE, 20, 20);
        MortarTower tower = new MortarTower(world, 3, 3);
        UpgradePaths.buy(tower, world, "Predictive Fire");
        TowerFixtures.buffed(tower, world, buff);
        FakeEnemyMob walker = FakeEnemyMob.at(tower.getX() + OUT, tower.getY()).walkingBy(0, 2);
        world.enemies().setEnemies(new EnemyMob[]{walker});
        for (int t = 1; t < 200 && world.projectiles().getProjectiles().isEmpty(); t++) {
            tower.doTick(t);
        }
        TowerFixtures.flyProjectilesToCompletion(world);
        return tower.getBlastMarks().getFirst().at().y() - walker.getY();
    }

    @Test
    void airburstWidensTheBlastAndGivesFullDamageAcrossItsInnerHalf() {
        MortarTower tower = this.towerWith("Airburst");
        float radius = tower.getSplashRadius();
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        FakeEnemyMob inner = this.enemyAt(tower, OUT, radius * 0.45f).hidden();
        FakeEnemyMob outer = this.enemyAt(tower, OUT, radius * 0.9f).hidden();
        this.context.enemies().setEnemies(new EnemyMob[]{target, inner, outer});

        this.landAShell(tower);

        assertThat(inner.onlyHitAmount()).isEqualTo(tower.damageCurrent());
        assertThat(outer.onlyHitAmount()).isLessThan(tower.damageCurrent());
    }

    @Test
    void barrageFiresThreeShellsHalfASecondApartAndReloadsForTwiceAsLong() {
        MortarTower tower = this.towerWith("Barrage");
        this.context.enemies().setEnemies(new EnemyMob[]{this.enemyAt(tower, OUT, 0)});

        int shells = 0;
        int firstShellAt = -1;
        int lastShellAt = -1;
        for (int t = 1; t <= 400; t++) {
            int before = this.context.projectiles().getProjectiles().size();
            tower.doTick(t);
            if (this.context.projectiles().getProjectiles().size() > before) {
                shells++;
                firstShellAt = firstShellAt < 0 ? t : firstShellAt;
                lastShellAt = t;
            }
        }

        assertThat(firstShellAt).isEqualTo(1);
        assertThat((double) (lastShellAt - firstShellAt)).isGreaterThan(2 * tower.coolDownMax / tower.attackRate());
        assertThat(shells % 3).isZero();
        assertThat(shells).isGreaterThanOrEqualTo(6);
    }

    @Test
    void everyShellOfABarrageHitsAtFullDamage() {
        MortarTower tower = this.towerWith("Barrage");
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        for (int t = 1; t <= 40; t++) {
            tower.doTick(t);
            this.context.projectiles().doTick(t);
        }
        TowerFixtures.flyProjectilesToCompletion(this.context);

        assertThat(target.hits()).hasSize(3);
        assertThat(target.hits()).extracting(Damage::amount).allMatch(amount -> amount >= tower.damageCurrent());
    }

    @Test
    void aBarrageShowsItsShellsAMinuteOfRateOverTheWholeCycle() {
        MortarTower tower = this.towerAt(3, 3);
        TowerStatLine before = tower.inspect().stats().stream().filter(line -> line.stat() == TowerStatRow.FIRE_RATE)
                .findFirst().orElseThrow();
        UpgradePaths.buy(tower, this.context, "Barrage");

        TowerStatLine after = tower.inspect().stats().stream().filter(line -> line.stat() == TowerStatRow.FIRE_RATE)
                .findFirst().orElseThrow();

        assertThat(after.base()).isEqualTo(before.base());
        assertThat(after.current()).isCloseTo((float) (3 * 20 * tower.attackRate() / ((tower.coolDownMax + 1) * 2f)),
                within(1e-3f));
    }

    @Test
    void theNextSpecialShellIsPrimed() {
        MortarTower tower = this.towerWith("Tactical Nuke");
        this.context.enemies().setEnemies(new EnemyMob[]{this.enemyAt(tower, OUT, 0)});

        for (int i = 0; i < 3; i++) {
            this.landAShell(tower);
        }

        assertThat(tower.progress()).anySatisfy(progress -> {
            assertThat(progress.of()).isEqualTo(4);
            assertThat(progress.count()).isEqualTo(3);
        });
        assertThat(tower.conditions()).extracting(TowerBuffState::term).isNotEmpty();
    }
}

package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.damage.DamageUnits;
import td.damage.Delivery;
import td.effect.DamageSink;
import td.effect.Effect;
import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.fixtures.BoardFixtures;
import td.fixtures.FakeEnemyMob;
import td.fixtures.MagazineFixtures;
import td.fixtures.TowerFixtures;
import td.fixtures.UpgradePaths;
import td.fixtures.WorldFixtures;
import td.projectile.CannonballProjectile;
import td.projectile.Projectile;
import td.projectile.ShellLook;
import td.stat.StatModifiers;
import td.term.TermText;
import td.tower.mortar.BlastMark;
import td.tower.mortar.Magazine;
import td.tower.mortar.MagazineSpec;
import td.tower.mortar.MortarSpec;
import td.tower.mortar.PayloadMechanic;
import td.tower.mortar.ShellKind;
import td.tower.mortar.ShellProfile;
import td.tower.upgrade.UpgradeNode;
import td.util.GameWorld;
import td.wave.Path;
import td.wave.Vec2;
import td.zone.Zone;
import td.zone.ZoneKind;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class MortarTowerTest {

    private static final int SCALE = BoardFixtures.SCALE;
    /** Far enough out to be past the minimum range and still in range. */
    private static final int OUT = 3 * SCALE;

    private final GameWorld context = WorldFixtures.newWorldOnBoard(SCALE, 20, 20);
    private int tick = 1;

    private MortarTower towerAt(int cellX, int cellY) {
        return new MortarTower(this.context, cellX, cellY);
    }

    private MortarTower towerWith(String... nodes) {
        MortarTower tower = this.towerAt(3, 3);
        UpgradePaths.buy(tower, this.context, nodes);
        return tower;
    }

    /** A Mortar owning {@code nodes} whose magazine starts with {@code front}. */
    private MortarTower loaded(List<ShellKind> front, String... nodes) {
        MortarTower tower = this.towerWith(nodes);
        MortarTower.loadMagazine(tower, MagazineFixtures.frontedBy(front.toArray(ShellKind[]::new)));
        return tower;
    }

    private FakeEnemyMob enemyAt(MortarTower tower, double dx, double dy) {
        return FakeEnemyMob.at(tower.getX() + dx, tower.getY() + dy);
    }

    /** Ticks until the tower fires, then flies the shell to where it lands. */
    private void landAShell(MortarTower tower) {
        this.fire(tower);
        TowerFixtures.flyProjectilesToCompletion(this.context);
    }

    /** Ticks until the tower fires; the shell it fired. */
    private Projectile fire(MortarTower tower) {
        for (int i = 0; i < 200 && this.context.projectiles().getProjectiles().isEmpty(); i++) {
            tower.doTick(this.tick++);
        }
        return this.context.projectiles().getProjectiles().getLast();
    }

    private List<ZoneKind> zoneKinds() {
        return this.context.zones().zones().stream().map(Zone::kind).toList();
    }

    private static int remainingTicksOf(FakeEnemyMob enemy, EffectKind kind) {
        return enemy.activeEffects().stream().filter(effect -> effect.kind() == kind).findFirst().orElseThrow()
                .remainingTicks();
    }

    @Test
    void aShellLandsWithTheStatsItWasFiredWithThoughTheChargeItSpentIsGone() {
        MortarTower tower = this.loaded(List.of(ShellKind.NORMAL));
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        this.context.enemies().setEnemies(new EnemyMob[]{target});
        tower.grantStatus(TowerStatus.nextShots(TowerTerm.HASTE, 1, TowerModifiers.damageTimes(3f)));
        int charged = tower.damageCurrent();

        this.landAShell(tower);

        assertThat(tower.damageCurrent()).isLessThan(charged);
        assertThat(target.onlyHitAmount()).isEqualTo(charged);
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
    void aNormalShellIsSlowAndFliesAtItsOwnSpeed() {
        MortarTower tower = this.loaded(List.of(ShellKind.NORMAL));
        this.context.enemies().setEnemies(new EnemyMob[]{this.enemyAt(tower, OUT, 0)});
        tower.doTick(1);
        CannonballProjectile shell = (CannonballProjectile) this.context.projectiles().getProjectiles().getFirst();

        this.context.projectiles().doTick(2);

        assertThat(shell.getX() - tower.getX()).isCloseTo(MortarTower.SHELL_SPEED, within(1e-4));
    }

    @Test
    void theBlastDealsFullDamageInItsCoreAndCracksPlatingWithoutSlowing() {
        MortarTower tower = this.loaded(List.of(ShellKind.NORMAL), "Cracking Shells");
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        this.landAShell(tower);

        assertThat(target.onlyHitAmount()).isEqualTo(tower.damageCurrent());
        assertThat(target.hits().getFirst().type()).isEqualTo(DamageType.PHYSICAL);
        assertThat(target.activeEffectKinds()).containsExactly(EffectKind.CRACKED);
    }

    @Test
    void aMortarWithoutCrackingShellsCracksNothing() {
        MortarTower tower = this.loaded(List.of(ShellKind.NORMAL));
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        this.landAShell(tower);

        assertThat(target.hits()).isNotEmpty();
        assertThat(target.activeEffectKinds()).doesNotContain(EffectKind.CRACKED);
    }

    @Test
    void theBlastsEdgeDealsLessThanItsCoreAndNothingReachesPastIt() {
        MortarTower tower = this.loaded(List.of(ShellKind.NORMAL), "Cracking Shells");
        float core = tower.stats().value(TowerStat.AREA_RADIUS) * SCALE;
        float edge = tower.stats().value(TowerStat.BLAST_EDGE) * SCALE;
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        FakeEnemyMob atTheEdge = this.enemyAt(tower, OUT, (core + edge) / 2).hidden();
        FakeEnemyMob beyond = this.enemyAt(tower, OUT, edge + 2).hidden();
        this.context.enemies().setEnemies(new EnemyMob[]{target, atTheEdge, beyond});

        this.landAShell(tower);

        assertThat(atTheEdge.onlyHitAmount()).isPositive().isLessThan(target.onlyHitAmount());
        assertThat(atTheEdge.activeEffectKinds()).contains(EffectKind.CRACKED);
        assertThat(beyond.hits()).isEmpty();
    }

    @Test
    void aWiderCoreDealsFullDamageWhereTheEdgeDealtLess() {
        MortarTower plain = this.loaded(List.of(ShellKind.NORMAL));
        float core = plain.stats().value(TowerStat.AREA_RADIUS) * SCALE;
        FakeEnemyMob besidePlain = this.enemyAt(plain, OUT, core + 4).hidden();
        this.context.enemies().setEnemies(new EnemyMob[]{this.enemyAt(plain, OUT, 0), besidePlain});
        this.landAShell(plain);
        MortarTower wide = TowerFixtures.buffed(this.loaded(List.of(ShellKind.NORMAL)), this.context,
                TowerModifiers.areaRadius(1f));
        FakeEnemyMob besideWide = this.enemyAt(wide, OUT, core + 4).hidden();
        this.context.enemies().setEnemies(new EnemyMob[]{this.enemyAt(wide, OUT, 0), besideWide});

        this.landAShell(wide);

        assertThat(besidePlain.onlyHitAmount()).isLessThan(plain.damageCurrent());
        assertThat(besideWide.onlyHitAmount()).isEqualTo(wide.damageCurrent());
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
    void anEnemyInsideTheMinimumRangeIsNeverShelled() {
        MortarTower tower = this.towerAt(3, 3);
        this.context.enemies().setEnemies(new EnemyMob[]{this.enemyAt(tower, SCALE, 0)});

        tower.doTick(1);

        assertThat(this.context.projectiles().getProjectiles()).isEmpty();
    }

    @Test
    void aShellFliesAtItsTowersProjectileSpeedAndSize() {
        MortarTower plain = this.loaded(List.of(ShellKind.NORMAL));
        MortarTower tuned = TowerFixtures.buffed(this.towerAt(8, 3), this.context,
                TowerModifiers.projectileSpeed(0.5f).plus(TowerModifiers.projectileSize(0.5f)));
        MortarTower.loadMagazine(tuned, MagazineFixtures.frontedBy(ShellKind.NORMAL));
        this.context.enemies().setEnemies(new EnemyMob[]{this.enemyAt(plain, OUT, 0), this.enemyAt(tuned, OUT, 0)});

        plain.doTick(1);
        tuned.doTick(1);

        CannonballProjectile slow = (CannonballProjectile) this.context.projectiles().getProjectiles().get(0);
        CannonballProjectile fast = (CannonballProjectile) this.context.projectiles().getProjectiles().get(1);
        assertThat(fast.stats().speed()).isGreaterThan(slow.stats().speed());
        assertThat(fast.stats().size()).isGreaterThan(slow.stats().size());
    }

    @Test
    void aMortarCanLoadEveryKindItDrawsAndANukeOnlyWhileItsHeaviesFuse() {
        MortarTower plain = new MortarTower(this.context, 3, 3);
        MortarTower fusing = new MortarTower(this.context, 6, 6);
        UpgradePaths.buy(fusing, this.context, "Fusion");

        List<ShellKind> drawn = plain.shellChances().entrySet().stream().filter(chance -> chance.getValue() > 0f)
                .map(Map.Entry::getKey).toList();

        assertThat(plain.loadableKinds()).containsExactlyElementsOf(drawn).doesNotContain(ShellKind.NUKE);
        assertThat(fusing.loadableKinds()).contains(ShellKind.NUKE);
    }

    @Test
    void aNewMortarLoadsTheSameMagazineAsEveryOther() {
        MortarTower one = this.towerAt(3, 3);
        MortarTower other = this.towerAt(9, 9);

        assertThat(one.magazine()).isEqualTo(other.magazine());
        assertThat(one.magazine().shells()).hasSize(Magazine.SIZE);
    }

    @Test
    void firingTakesTheFrontShellAndTheOthersMoveUpWithANewOneDrawnAtTheBack() {
        MortarTower tower = this.loaded(List.of(ShellKind.NORMAL, ShellKind.HEAVY, ShellKind.NORMAL));
        List<ShellKind> before = tower.magazine().shells();
        this.context.enemies().setEnemies(new EnemyMob[]{this.enemyAt(tower, OUT, 0)});

        CannonballProjectile shell = (CannonballProjectile) this.fire(tower);

        assertThat(shell.look()).isEqualTo(ShellLook.NORMAL);
        assertThat(tower.magazine().shells()).hasSize(Magazine.SIZE)
                .startsWith(before.subList(1, Magazine.SIZE).toArray(ShellKind[]::new));
    }

    @Test
    void aShellItNoLongerOwnsLeavesTheMagazineAndTheOnesBehindMoveUp() {
        MortarTower tower = this.loaded(List.of(ShellKind.NAPALM, ShellKind.HEAVY), "Napalm");
        tower.doTick(this.tick++);
        boolean keptWhileOwned = tower.magazine().front() == ShellKind.NAPALM;

        tower.downgrade(UpgradePaths.named(tower, "Napalm"));
        tower.doTick(this.tick++);

        assertThat(keptWhileOwned).isTrue();
        assertThat(tower.magazine().front()).isEqualTo(ShellKind.HEAVY);
        assertThat(tower.magazine().shells()).hasSize(Magazine.SIZE).doesNotContain(ShellKind.NAPALM);
    }

    @Test
    void aHeavyShellHitsForANormalsDamageAndItsOwnInANormalBlastAndFliesSlower() {
        MortarTower tower = this.loaded(List.of(ShellKind.HEAVY));
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        this.context.enemies().setEnemies(new EnemyMob[]{target});
        CannonballProjectile shell = (CannonballProjectile) this.fire(tower);
        float speed = shell.stats().speed();

        TowerFixtures.flyProjectilesToCompletion(this.context);

        assertThat(speed).isLessThan(MortarTower.SHELL_SPEED);
        assertThat(target.onlyHitAmount()).isEqualTo(
                Math.round(tower.damageCurrent() + tower.stats().value(TowerStat.HEAVY_DAMAGE)));
    }

    @Test
    void moreHeavyDamageMakesAHeavyHitHarder() {
        MortarTower plain = this.loaded(List.of(ShellKind.HEAVY));
        FakeEnemyMob plainTarget = this.enemyAt(plain, OUT, 0);
        this.context.enemies().setEnemies(new EnemyMob[]{plainTarget});
        this.landAShell(plain);
        MortarTower charged = TowerFixtures.buffed(this.loaded(List.of(ShellKind.HEAVY)), this.context,
                TowerModifiers.heavyDamage(10f));
        FakeEnemyMob chargedTarget = this.enemyAt(charged, OUT, 0);
        this.context.enemies().setEnemies(new EnemyMob[]{chargedTarget});

        this.landAShell(charged);

        assertThat(chargedTarget.onlyHitAmount()).isGreaterThan(plainTarget.onlyHitAmount());
    }

    @Test
    void concussionDazesAHeavysCoreLongerThanItsEdgeAndANormalDazesNothing() {
        MortarTower tower = this.loaded(List.of(ShellKind.HEAVY, ShellKind.NORMAL), "Concussion");
        float core = tower.stats().value(TowerStat.AREA_RADIUS) * SCALE;
        float edge = tower.stats().value(TowerStat.BLAST_EDGE) * SCALE;
        FakeEnemyMob centre = this.enemyAt(tower, OUT, 0);
        FakeEnemyMob atTheEdge = this.enemyAt(tower, OUT, (core + edge) / 2).hidden();
        this.context.enemies().setEnemies(new EnemyMob[]{centre, atTheEdge});
        this.landAShell(tower);
        int centreDaze = remainingTicksOf(centre, EffectKind.DAZED);
        int edgeDaze = remainingTicksOf(atTheEdge, EffectKind.DAZED);
        FakeEnemyMob next = this.enemyAt(tower, OUT, 0);
        this.context.enemies().setEnemies(new EnemyMob[]{next});

        this.landAShell(tower);

        assertThat(centreDaze).isGreaterThan(edgeDaze);
        assertThat(next.activeEffectKinds()).doesNotContain(EffectKind.DAZED);
    }

    @Test
    void shrapnelMakesWhatAHeavysBlastReachesBleedAndANormalBleedsNothing() {
        MortarTower tower = this.loaded(List.of(ShellKind.HEAVY, ShellKind.NORMAL), "Shrapnel");
        float edge = tower.stats().value(TowerStat.BLAST_EDGE) * SCALE;
        FakeEnemyMob centre = this.enemyAt(tower, OUT, 0);
        FakeEnemyMob nearTheEdge = this.enemyAt(tower, OUT, edge - 2).hidden();
        this.context.enemies().setEnemies(new EnemyMob[]{centre, nearTheEdge});
        this.landAShell(tower);
        FakeEnemyMob next = this.enemyAt(tower, OUT, 0);
        this.context.enemies().setEnemies(new EnemyMob[]{next});

        this.landAShell(tower);

        assertThat(centre.activeEffectKinds()).contains(EffectKind.BLEEDING);
        assertThat(nearTheEdge.activeEffectKinds()).contains(EffectKind.BLEEDING);
        assertThat(next.activeEffectKinds()).doesNotContain(EffectKind.BLEEDING);
    }

    @Test
    void aBleedLandsAsPeriodicDamagePastArmorAndPlating() {
        MortarTower tower = this.loaded(List.of(ShellKind.HEAVY), "Shrapnel");
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        this.context.enemies().setEnemies(new EnemyMob[]{target});
        this.landAShell(tower);
        Effect bleed = target.activeEffects().stream()
                .filter(effect -> effect.kind() == EffectKind.BLEEDING).findFirst().orElseThrow();

        bleed.sink().apply(Damage.physical(100));

        AttackProfile dealtBy = target.attackers().getLast();
        assertThat(dealtBy.delivery()).isEqualTo(Delivery.PERIODIC);
        assertThat(dealtBy.penetrate(DamageType.PHYSICAL, 50f, false)).isZero();
        assertThat(dealtBy.penetrateFlatReduction(200f, false)).isZero();
    }

    @Test
    void aNapalmShellBlastsForMagicAndItsGroundSetsEnemiesAlight() {
        MortarTower tower = this.loaded(List.of(ShellKind.NAPALM), "Napalm");
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        this.landAShell(tower);
        this.context.zones().doTick(1);

        assertThat(target.hits().getFirst().type()).isEqualTo(DamageType.MAGIC);
        assertThat(target.hits().getFirst().amount())
                .isEqualTo(Math.round(tower.damageCurrent() + DamageUnits.ofPoints(ShellKind.NAPALM.extraPoints())));
        assertThat(this.zoneKinds()).containsExactly(ZoneKind.BURNING_GROUND);
        assertThat(target.activeEffectKinds()).contains(EffectKind.BURN);
    }

    @Test
    void aTarShellLeavesTarWhereItLands() {
        MortarTower tower = this.loaded(List.of(ShellKind.TAR), "Tar Shell");
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        this.landAShell(tower);

        assertThat(this.zoneKinds()).containsExactly(ZoneKind.TAR);
        assertThat(this.context.zones().zones().getFirst().x()).isEqualTo(target.getX());
    }

    /** The ground the payload shell of a Mortar owning {@code nodes} leaves, one pulse after it lands. */
    private static Zone payloadGround(String... nodes) {
        GameWorld world = WorldFixtures.newWorldOnBoard(SCALE, 20, 20);
        MortarTower tower = new MortarTower(world, 3, 3);
        UpgradePaths.buy(tower, world, nodes);
        MortarTower.loadMagazine(tower, MagazineFixtures.frontedBy(ShellKind.CRYO));
        world.enemies().setEnemies(new EnemyMob[]{FakeEnemyMob.at(tower.getX() + OUT, tower.getY())});
        for (int t = 1; t < 200 && world.projectiles().getProjectiles().isEmpty(); t++) {
            tower.doTick(t);
        }
        TowerFixtures.flyProjectilesToCompletion(world);
        world.zones().doTick(1);
        return world.zones().zones().getFirst();
    }

    @Test
    void aMagazineDrawnAfreshHoldsWhatItsBuildDrawsInPlaceOfTheOpener() {
        GameWorld rolledHigh = WorldFixtures.newWorldOnBoard(() -> 0.99, SCALE, 20, 20);
        MortarTower tower = new MortarTower(rolledHigh, 3, 3);
        UpgradePaths.buy(tower, rolledHigh, "Napalm");

        MortarTower.drawMagazineAfresh(tower);

        assertThat(tower.magazine().shells()).hasSize(Magazine.SIZE).containsOnly(ShellKind.NAPALM);
    }

    @Test
    void withThickMixANormalShellsBlastCarriesThePayloadsGround() {
        MortarTower plain = this.loaded(List.of(ShellKind.NORMAL), "Napalm");
        FakeEnemyMob unburnt = this.enemyAt(plain, OUT, 0);
        this.context.enemies().setEnemies(new EnemyMob[]{unburnt});
        this.landAShell(plain);
        MortarTower thick = this.loaded(List.of(ShellKind.NORMAL), "Napalm", "Thick Mix");
        FakeEnemyMob burnt = this.enemyAt(thick, OUT, 0);
        this.context.enemies().setEnemies(new EnemyMob[]{burnt});

        this.landAShell(thick);

        assertThat(unburnt.hasEffect(EffectKind.BURN)).isFalse();
        assertThat(burnt.hasEffect(EffectKind.BURN)).isTrue();
    }

    @Test
    void lastingMixMakesAPayloadsGroundLastLonger() {
        Zone plain = payloadGround("Cryo");

        Zone lasting = payloadGround("Cryo", "Thick Mix", "Lasting Mix");

        assertThat(lasting.lifeLeft()).isGreaterThan(plain.lifeLeft());
    }

    @Test
    void aNukeHitsAsTwoHeaviesOverAWiderCoreAndLeavesFalloutThatPoisons() {
        MortarTower tower = this.loaded(List.of(ShellKind.NUKE), "Fusion");
        float core = tower.stats().value(TowerStat.AREA_RADIUS) * SCALE;
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        FakeEnemyMob pastANormalCore = this.enemyAt(tower, OUT, core + 4).hidden();
        this.context.enemies().setEnemies(new EnemyMob[]{target, pastANormalCore});

        this.landAShell(tower);
        this.context.zones().doTick(1);

        int twoHeavies = Math.round(2 * (tower.damageCurrent() + tower.stats().value(TowerStat.HEAVY_DAMAGE)));
        assertThat(target.hits().getFirst().amount()).isEqualTo(twoHeavies);
        assertThat(pastANormalCore.hits().getFirst().amount()).isEqualTo(twoHeavies);
        assertThat(this.zoneKinds()).containsExactly(ZoneKind.FALLOUT);
        assertThat(target.activeEffectKinds()).contains(EffectKind.SICKENED, EffectKind.DEAD_ZONE, EffectKind.POISON);
    }

    @Test
    void aFlareLeavesAFlareThatRevealsAnEnemyInsideAndANewOnePutsOutTheOldest() {
        MortarTower tower = this.loaded(List.of(ShellKind.FLARE, ShellKind.FLARE, ShellKind.FLARE, ShellKind.FLARE),
                "Flare");
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        this.context.enemies().setEnemies(new EnemyMob[]{target});
        this.landAShell(tower);
        this.context.zones().doTick(1);
        boolean revealed = target.activeEffectKinds().contains(EffectKind.REVEALED);

        for (int i = 0; i < 3; i++) {
            this.landAShell(tower);
        }

        List<Zone> flares = this.context.zones().zones();
        assertThat(revealed).isTrue();
        assertThat(flares).extracting(Zone::kind).containsOnly(ZoneKind.FLARE);
        assertThat(flares.getFirst().isBurning()).isFalse();
        assertThat(flares.subList(1, flares.size())).hasSize(3).allMatch(Zone::isBurning);
    }

    @Test
    void deepCrackMakesEachShellsShareOfCrackWorthMoreStacks() {
        MortarTower tower = this.towerWith("Deep Crack");
        FakeEnemyMob target = this.enemyAt(tower, OUT, 0);
        this.context.enemies().setEnemies(new EnemyMob[]{target});

        this.landAShell(tower);

        assertThat(target.effectStacks(EffectKind.CRACKED)).isEqualTo(MortarSpec.DEEP_CRACK_STACKS);
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
    void predictiveFireShellsThePackOverTheLoneEnemyFurthestAlong() {
        MortarTower tower = this.towerWith("Predictive Fire");
        FakeEnemyMob lone = this.enemyAt(tower, 0, -OUT).withProgression(900);
        FakeEnemyMob packed = this.enemyAt(tower, OUT, 0).withProgression(10);
        FakeEnemyMob near = this.enemyAt(tower, OUT, SCALE / 2.0).withProgression(5);
        this.context.enemies().setEnemies(new EnemyMob[]{lone, packed, near});

        this.landAShell(tower);

        assertThat(packed.hits()).isNotEmpty();
        assertThat(lone.hits()).isEmpty();
    }

    @Test
    void aShellsProfileIsTheHitItWouldLandWithAndWhatItsPickAdds() {
        MortarTower tower = this.towerWith("Concussion", "Cracking Shells");
        TowerStats stats = tower.stats();

        Map<ShellKind, ShellProfile> profiles = tower.shellProfiles().stream()
                .collect(Collectors.toMap(ShellProfile::kind, profile -> profile));

        ShellProfile heavy = profiles.get(ShellKind.HEAVY);
        ShellProfile normal = profiles.get(ShellKind.NORMAL);
        assertThat(heavy.hit().amount()).isEqualTo(Math.round(stats.damage() + stats.value(TowerStat.HEAVY_DAMAGE)));
        assertThat(heavy.blast().applies()).containsExactly(EffectKind.DAZED, EffectKind.CRACKED);
        assertThat(normal.blast().applies()).containsExactly(EffectKind.CRACKED);
        assertThat(heavy.cellsPerSecond()).isLessThan(normal.cellsPerSecond());
    }

    @Test
    void aPayloadsProfileSaysWhatEachPulseOfItsGroundDeals() {
        MortarTower tower = this.towerWith("Napalm");
        ShellKind.Ground ground = ShellKind.NAPALM.ground().orElseThrow();

        ShellProfile napalm = tower.shellProfiles().stream().filter(profile -> profile.kind() == ShellKind.NAPALM)
                .findFirst().orElseThrow();

        assertThat(napalm.groundPulse()).hasValue(Damage.magic(ground.pulse()));
    }

    @Test
    void moreDamageMakesEveryKindOfShellHitHarderByTheSamePointsAndANukeByTwice() {
        MortarTower plain = this.towerWith("Napalm", "Fusion");
        MortarTower buffed = TowerFixtures.buffed(this.towerWith("Napalm", "Fusion"), this.context,
                TowerModifiers.damage(10f));

        Map<ShellKind, Integer> plainHits = hitsByKind(plain);
        Map<ShellKind, Integer> buffedHits = hitsByKind(buffed);

        int more = DamageUnits.ofPoints(10f);
        assertThat(plainHits).containsKeys(ShellKind.NORMAL, ShellKind.HEAVY, ShellKind.NAPALM, ShellKind.NUKE);
        plainHits.forEach((kind, hit) -> assertThat(buffedHits.get(kind))
                .as(kind.name()).isEqualTo(hit + (kind == ShellKind.NUKE ? 2 * more : more)));
    }

    private static Map<ShellKind, Integer> hitsByKind(MortarTower tower) {
        return tower.shellProfiles().stream()
                .collect(Collectors.toMap(ShellProfile::kind, profile -> profile.hit().amount()));
    }

    @Test
    void predictiveFireLeadsAFasterShellLess() {
        double slowLead = predictiveLead(StatModifiers.none());

        double fastLead = predictiveLead(TowerModifiers.projectileSpeed(1f));

        assertThat(fastLead).isPositive().isLessThan(slowLead);
    }

    /** How far ahead of a walking enemy a Predictive Fire Mortar given {@code buff} lands a Normal shell. */
    private static double predictiveLead(StatModifiers<TowerStat> buff) {
        GameWorld world = WorldFixtures.newWorldOnBoard(SCALE, 20, 20);
        MortarTower tower = new MortarTower(world, 3, 3);
        UpgradePaths.buy(tower, world, "Predictive Fire");
        TowerFixtures.buffed(tower, world, buff);
        MortarTower.loadMagazine(tower, MagazineFixtures.frontedBy(ShellKind.NORMAL));
        FakeEnemyMob walker = FakeEnemyMob.at(tower.getX() + OUT, tower.getY()).walkingBy(0, 2);
        world.enemies().setEnemies(new EnemyMob[]{walker});
        for (int t = 1; t < 200 && world.projectiles().getProjectiles().isEmpty(); t++) {
            tower.doTick(t);
        }
        TowerFixtures.flyProjectilesToCompletion(world);
        return tower.getBlastMarks().getFirst().at().y() - walker.getY();
    }

    /** The looks of the shells a Mortar fired, in the order fired, and the ticks it fired them on. */
    private record Firing(List<ShellLook> looks, List<Integer> ticks) {
    }

    private Firing fireFor(MortarTower tower, int ticks) {
        List<ShellLook> looks = new ArrayList<>();
        List<Integer> fired = new ArrayList<>();
        for (int t = 1; t <= ticks; t++) {
            int before = this.context.projectiles().getProjectiles().size();
            tower.doTick(t);
            List<Projectile> flying = this.context.projectiles().getProjectiles();
            if (flying.size() > before) {
                looks.add(((CannonballProjectile) flying.getLast()).look());
                fired.add(t);
            }
        }
        return new Firing(looks, fired);
    }

    @Test
    void aBarrageFiresTheThreeFrontShellsOneAfterAnotherAndReloadsLonger() {
        MortarTower plain = this.loaded(List.of(ShellKind.NORMAL));
        this.context.enemies().setEnemies(new EnemyMob[]{this.enemyAt(plain, OUT, 0)});
        Firing single = this.fireFor(plain, 400);
        this.context.projectiles().clear();
        MortarTower tower = this.loaded(List.of(ShellKind.HEAVY, ShellKind.NORMAL, ShellKind.HEAVY), "Barrage");
        this.context.enemies().setEnemies(new EnemyMob[]{this.enemyAt(tower, OUT, 0)});

        Firing barrage = this.fireFor(tower, 400);

        assertThat(barrage.looks().subList(0, 3)).containsExactly(ShellLook.HEAVY, ShellLook.NORMAL, ShellLook.HEAVY);
        assertThat(barrage.ticks().get(1)).isGreaterThan(barrage.ticks().get(0));
        assertThat(barrage.ticks().get(3) - barrage.ticks().get(0))
                .isGreaterThan(single.ticks().get(1) - single.ticks().get(0));
    }

    @Test
    void carpetBombingSneaksANormalBetweenEachTwoShellsItLoads() {
        MortarTower tower = this.loaded(List.of(ShellKind.HEAVY, ShellKind.HEAVY, ShellKind.HEAVY), "Carpet Bombing");
        this.context.setPath(new Path(List.of(new Vec2(0, tower.getY()), new Vec2(2000, tower.getY()))));
        this.context.enemies().setEnemies(new EnemyMob[]{this.enemyAt(tower, OUT + SCALE, 0)});

        Firing carpet = this.fireFor(tower, 400);

        assertThat(carpet.looks().subList(0, 5)).containsExactly(ShellLook.HEAVY, ShellLook.NORMAL, ShellLook.HEAVY,
                ShellLook.NORMAL, ShellLook.HEAVY);
    }

    @Test
    void carpetBombingLaysItsShellsAlongThePathCentredOnTheTarget() {
        MortarTower tower = this.towerWith("Carpet Bombing");
        FakeEnemyMob target = this.enemyAt(tower, OUT + SCALE, 0);
        this.context.setPath(new Path(List.of(new Vec2(0, tower.getY()), new Vec2(2000, tower.getY()))));
        this.context.enemies().setEnemies(new EnemyMob[]{target});
        Set<BlastMark> landed = new LinkedHashSet<>();

        for (int t = 1; t <= 200 && landed.size() < 5; t++) {
            tower.doTick(t);
            this.context.projectiles().doTick(t);
            landed.addAll(tower.getBlastMarks());
        }

        List<Double> landedAt = landed.stream().map(mark -> mark.at().x()).toList();
        assertThat(landed).hasSize(5).allMatch(mark -> mark.at().y() == tower.getY());
        assertThat(landedAt.get(2)).isCloseTo(target.getX(), within(1.0));
        assertThat(landedAt).isSortedAccordingTo((a, b) -> Double.compare(b, a));
    }

    @Test
    void aVolleyShowsItsShellsASecondOverTheWholeCycle() {
        MortarTower tower = this.towerAt(3, 3);
        TowerStatLine before = tower.inspect().stats().stream().filter(line -> line.stat() == TowerStatRow.FIRE_RATE)
                .findFirst().orElseThrow();
        UpgradePaths.buy(tower, this.context, "Barrage");

        TowerStatLine after = tower.inspect().stats().stream().filter(line -> line.stat() == TowerStatRow.FIRE_RATE)
                .findFirst().orElseThrow();

        assertThat(after.current()).isGreaterThan(before.current());
    }

    @Test
    void theShellLoadingShowsAsAConditionUnlessItIsANormal() {
        MortarTower heavy = this.loaded(List.of(ShellKind.HEAVY));
        MortarTower normal = this.loaded(List.of(ShellKind.NORMAL));

        List<TowerBuffState> loadingHeavy = heavy.conditions();

        assertThat(loadingHeavy).extracting(TowerBuffState::term).contains(TowerTerm.HEAVY_SHELL);
        assertThat(normal.conditions()).extracting(TowerBuffState::term).doesNotContain(TowerTerm.HEAVY_SHELL);
    }

    @Test
    void aNodeNotYetOwnedSaysHowItWouldMoveEachKindsShareAndAnOwnedOneDoesNot() {
        MortarTower tower = this.towerAt(3, 3);
        UpgradeNode tar = UpgradePaths.named(tower, "Tar Shell");
        Map<ShellKind, Float> before = tower.shellChances();
        Map<ShellKind, Float> after = MagazineSpec.standard().withWeight(ShellKind.TAR, PayloadMechanic.WEIGHT).chances();

        List<TermText> offered = tower.wordsOf(tar).sentences();
        UpgradePaths.buy(tower, this.context, "Tar Shell");
        List<TermText> owned = tower.wordsOf(tar).sentences();

        assertThat(offered).endsWith(MortarTower.chanceChange(before, after).orElseThrow());
        assertThat(owned).doesNotContain(MortarTower.chanceChange(before, after).orElseThrow());
    }

    @Test
    void aClusterMembersHoverLeavesOutWhatTheMemberItWouldReplaceAdds() {
        MortarTower tower = this.towerWith("Napalm");
        Map<ShellKind, Float> withTarInstead = MagazineSpec.standard().withWeight(ShellKind.TAR, PayloadMechanic.WEIGHT)
                .chances();

        List<TermText> offered = tower.wordsOf(UpgradePaths.named(tower, "Tar Shell")).sentences();

        assertThat(offered).endsWith(MortarTower.chanceChange(tower.shellChances(), withTarInstead).orElseThrow());
    }

    @Test
    void aChanceChangeNamesOnlyTheKindsWhoseShownShareMoves() {
        Map<ShellKind, Float> before = Map.of(ShellKind.NORMAL, 0.9f, ShellKind.HEAVY, 0.1f);
        Map<ShellKind, Float> after = Map.of(ShellKind.NORMAL, 0.8f, ShellKind.HEAVY, 0.1f, ShellKind.FLARE, 0.1f);

        TermText change = MortarTower.chanceChange(before, after).orElseThrow();

        assertThat(change).isEqualTo(TermText.of("Normal 90% → 80%, ").plus(TowerTerm.FLARE).plus(" 0% → 10%"));
        assertThat(MortarTower.chanceChange(before, before)).isEmpty();
    }
}

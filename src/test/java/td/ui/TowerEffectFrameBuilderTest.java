package td.ui;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.damage.DamageUnits;
import td.effect.DamageSink;
import td.effect.Effect;
import td.enemy.EnemyMob;
import td.fixtures.BoardFixtures;
import td.fixtures.FakeEnemyMob;
import td.fixtures.MagazineFixtures;
import td.fixtures.TowerFixtures;
import td.fixtures.UpgradePaths;
import td.fixtures.WorldFixtures;
import td.stat.DisruptionAura;
import td.tower.AuraTower;
import td.tower.CinderTower;
import td.tower.MortarTower;
import td.tower.SeekerTower;
import td.tower.SniperTower;
import td.tower.SplashTower;
import td.tower.Tower;
import td.tower.TowerModifiers;
import td.tower.cinder.FlashoverMechanic;
import td.tower.mortar.BlastMark;
import td.tower.mortar.ShellKind;
import td.ui.render.CannonballDraw;
import td.ui.render.LoadedMissile;
import td.ui.render.LoadedShell;
import td.ui.render.NestDraw;
import td.ui.render.Palette;
import td.ui.render.RefundMarkDraw;
import td.ui.render.RingDraw;
import td.ui.render.StreamDraw;
import td.ui.render.TowerCue;
import td.ui.render.TowerEffectDraw;
import td.ui.render.TowerLook;
import td.ui.render.TowerPose;
import td.ui.render.TowerStatusDraw;
import td.ui.render.TracerDraw;
import td.util.GameWorld;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Covers the tower effects {@code BoardRendererTest} leaves out. */
class TowerEffectFrameBuilderTest {

    private static SplashTower splashWith(GameWorld context, String... nodes) {
        SplashTower splash = new SplashTower(context, 3, 3);
        context.towers().add(splash);
        UpgradePaths.buy(splash, context, nodes);
        return splash;
    }

    /** What the Splash draws on the tick its first stone lands by one enemy standing in range. */
    private static List<TowerEffectDraw> firedBy(GameWorld context, SplashTower splash) {
        context.enemies().setEnemies(new EnemyMob[]{FakeEnemyMob.at(splash.getX() + 30, splash.getY())});
        int tick = 1;
        splash.doTick(tick);
        while (!splash.stonesInFlight().isEmpty()) {
            splash.doTick(++tick);
        }
        TowerEffectFrameBuilder builder = new TowerEffectFrameBuilder(context, tick, 0.0, 0.0);
        splash.accept(builder);
        return builder.build();
    }

    @Test
    void aSplashDrawsItsBlastInTheColourOfItsFork() {
        GameWorld plainWorld = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        GameWorld stormWorld = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        GameWorld hexWorld = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);

        List<TowerEffectDraw> plain = firedBy(plainWorld, splashWith(plainWorld));
        List<TowerEffectDraw> storm = firedBy(stormWorld, splashWith(stormWorld, "Stormcaller"));
        List<TowerEffectDraw> hex = firedBy(hexWorld, splashWith(hexWorld, "Hexer"));

        assertThat(ringsIn(plain, Palette.TOWER_SPLASH_DETONATION)).hasSize(1);
        assertThat(ringsIn(storm, Palette.TOWER_STORMCALLER_DETONATION)).hasSize(1);
        assertThat(ringsIn(storm, Palette.TOWER_SPLASH_DETONATION)).isEmpty();
        assertThat(ringsIn(hex, Palette.TOWER_HEXER_DETONATION)).hasSize(1);
        assertThat(ringsIn(hex, Palette.TOWER_SPLASH_DETONATION)).isEmpty();
    }

    @Test
    void aStoneHalfwayIsDrawnOverItsShadowBetweenTheTowerAndWhereItLands() {
        GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        SplashTower splash = splashWith(context);
        FakeEnemyMob target = FakeEnemyMob.at(splash.getX() + 2 * BoardFixtures.SCALE, splash.getY());
        context.enemies().setEnemies(new EnemyMob[]{target});
        splash.doTick(1);
        SplashTower.Flight flight = splash.stonesInFlight().getFirst().flight();
        int halfway = (flight.firedAt() + flight.landsAt()) / 2;

        TowerEffectFrameBuilder builder = new TowerEffectFrameBuilder(context, halfway, 0.0, 0.0);
        splash.accept(builder);
        List<CannonballDraw> stones = builder.build().stream().filter(CannonballDraw.class::isInstance)
                .map(CannonballDraw.class::cast).toList();

        assertThat(stones).singleElement().satisfies(stone -> {
            assertThat(stone.lift()).isPositive();
            assertThat(stone.x()).isBetween((float) splash.getX(), (float) target.getX());
        });
    }

    @Test
    void aBurstWearsTheChargedCueWhileAFullAbilityWaitsForATarget() {
        GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        SplashTower splash = TowerFixtures.buffed(splashWith(context, "Thunderstrike"), context,
                TowerModifiers.damageTimes(1000f));
        FakeEnemyMob target = FakeEnemyMob.at(splash.getX() + BoardFixtures.SCALE, splash.getY());
        context.enemies().setEnemies(new EnemyMob[]{target});
        splash.doTick(1);
        boolean chargedBefore = TowerSpriteFrameBuilder.poseOf(splash).has(TowerCue.CHARGED);

        context.enemies().setEnemies(new EnemyMob[]{FakeEnemyMob.ghostAt(target.getX(), target.getY())});
        for (int tick = 2; !splash.stonesInFlight().isEmpty(); tick++) {
            splash.doTick(tick);
        }

        assertThat(chargedBefore).isFalse();
        assertThat(TowerSpriteFrameBuilder.poseOf(splash).has(TowerCue.CHARGED)).isTrue();
    }

    private static AuraTower auraWith(GameWorld context, String... nodes) {
        AuraTower aura = new AuraTower(context, 0, 0);
        context.towers().add(aura);
        UpgradePaths.buy(aura, context, nodes);
        return aura;
    }

    private static List<TowerEffectDraw> drawnBy(GameWorld context, AuraTower aura) {
        TowerEffectFrameBuilder builder = new TowerEffectFrameBuilder(context, 0, 0.0, 0.0);
        aura.accept(builder);
        return builder.build();
    }

    private static List<RingDraw> ringsIn(List<TowerEffectDraw> draws, Palette palette) {
        return draws.stream().filter(RingDraw.class::isInstance).map(RingDraw.class::cast)
                .filter(ring -> ring.palette() == palette).toList();
    }

    @Test
    void theApprenticeWearsAMentoringRing() {
        GameWorld context = WorldFixtures.newWorld();
        AuraTower aura = auraWith(context, "Mentor");
        SniperTower veteran = new SniperTower(context, 0, 0);
        SniperTower novice = new SniperTower(context, 0, 0);
        context.towers().add(veteran);
        context.towers().add(novice);
        veteran.earnXp(500);

        List<RingDraw> marks = ringsIn(drawnBy(context, aura), Palette.TOWER_AURA_MENTEE);

        assertThat(marks).hasSize(1);
        assertThat(marks.getFirst().centerX()).isEqualTo((float) novice.getX());
    }

    @Test
    void aTowerNoWaveHasUsedWearsTheRefundMarkUntilOneHas() {
        GameWorld context = WorldFixtures.newWorld();
        SniperTower tower = new SniperTower(context, 0, 0);
        TowerEffectFrameBuilder unused = new TowerEffectFrameBuilder(context, 0, 0.0, 0.0);
        unused.addStatus(tower);

        WorldFixtures.passAWave(context);
        TowerEffectFrameBuilder used = new TowerEffectFrameBuilder(context, 0, 0.0, 0.0);
        used.addStatus(tower);

        assertThat(unused.build()).singleElement().isInstanceOf(RefundMarkDraw.class);
        assertThat(used.build()).isEmpty();
    }

    @Test
    void aDisruptedTowerWearsAMarkerInTheDisruptionColourAndAnUndisruptedOneNone() {
        GameWorld context = WorldFixtures.newWorld();
        SniperTower jammed = new SniperTower(context, 0, 0);
        SniperTower clear = new SniperTower(context, 10, 10);
        context.disruptions().add(jammed.getX(), jammed.getY(), new DisruptionAura(20f, 0.3f, 0.2f));
        WorldFixtures.passAWave(context);
        jammed.beginTick(0);
        clear.beginTick(0);

        TowerEffectFrameBuilder builder = new TowerEffectFrameBuilder(context, 0, 0.0, 0.0);
        builder.addStatus(jammed);
        builder.addStatus(clear);

        assertThat(builder.build()).singleElement().isInstanceOfSatisfying(TowerStatusDraw.class,
                marker -> assertThat(marker.palette()).isEqualTo(Palette.DISRUPTION));
    }

    private static List<TracerDraw> tracersOf(Tower tower, GameWorld context, int gameTime, double alpha) {
        TowerEffectFrameBuilder builder = new TowerEffectFrameBuilder(context, gameTime, alpha, 0.0);
        tower.accept(builder);
        return builder.build().stream().filter(TracerDraw.class::isInstance).map(TracerDraw.class::cast).toList();
    }

    @Test
    void aBulletsStreakRunsTowardItsEnemyAndReachesItOnlyAsItLands() {
        GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        SniperTower sniper = new SniperTower(context, 3, 3);
        FakeEnemyMob target = FakeEnemyMob.at(sniper.getX() + 60, sniper.getY());
        context.enemies().setEnemies(new EnemyMob[]{target});
        sniper.doTick(1);
        int landsAt = sniper.bulletsInFlight().getFirst().landsAt();

        TracerDraw fired = tracersOf(sniper, context, 1, 0.0).getFirst();
        TracerDraw arriving = tracersOf(sniper, context, landsAt - 1, 0.99).getFirst();

        assertThat(fired.toX()).isLessThan(arriving.toX());
        assertThat(arriving.toX()).isLessThanOrEqualTo((float) target.getX());
        assertThat(arriving.toX()).isGreaterThan((float) target.getX() - 2f);
    }

    @Test
    void aBulletLeavesFromTheMuzzleOfTheRifleAsItIsPosedNotAsItRests() {
        GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        SniperTower sniper = new SniperTower(context, 3, 3);
        context.enemies().setEnemies(new EnemyMob[]{FakeEnemyMob.at(sniper.getX() + 60, sniper.getY())});
        sniper.doTick(1);
        TowerPose recoiled = TowerSpriteFrameBuilder.poseOf(sniper);

        TracerDraw fired = tracersOf(sniper, context, 1, 0.0).getFirst();

        float posedMuzzle = TowerSprites.muzzle(TowerLook.SNIPER, 0, recoiled, sniper.getX(), sniper.getY(),
                BoardFixtures.SCALE, 0.0).x;
        float restingMuzzle = TowerSprites.muzzle(TowerLook.SNIPER, 0, TowerPose.idle(), sniper.getX(), sniper.getY(),
                BoardFixtures.SCALE, 0.0).x;
        assertThat(fired.fromX()).isEqualTo(posedMuzzle).isNotEqualTo(restingMuzzle);
    }

    @Test
    void theStreakALandedBulletLeavesFadesLongBeforeTheReloadEnds() {
        GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        SniperTower sniper = new SniperTower(context, 3, 3);
        context.enemies().setEnemies(new EnemyMob[]{FakeEnemyMob.at(sniper.getX() + 60, sniper.getY())});
        sniper.doTick(1);
        int landsAt = sniper.bulletsInFlight().getFirst().landsAt();
        int tick = 2;
        for (; tick <= landsAt; tick++) {
            sniper.doTick(tick);
        }
        List<TracerDraw> landed = tracersOf(sniper, context, landsAt, 0.0);
        for (; tracersOf(sniper, context, tick, 0.0).size() == 1; tick++) {
            sniper.doTick(tick);
        }

        assertThat(landed).singleElement().extracting(TracerDraw::alpha).isEqualTo(1f);
        assertThat(sniper.getCoolDownFraction()).isGreaterThan(0.5f);
    }

    @Test
    void aMortarThatHasNotLandedAShellDrawsNothing() {
        GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        MortarTower mortar = new MortarTower(context, 3, 3);

        TowerEffectFrameBuilder builder = new TowerEffectFrameBuilder(context, 0, 0.0, 0.0);
        mortar.accept(builder);

        assertThat(builder.build()).isEmpty();
    }

    @Test
    void theRifleShowsAFourthShotOnlyWhileTheNextShotIsOne() {
        GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        SniperTower sniper = new SniperTower(context, 3, 3);
        UpgradePaths.buy(sniper, context, "Fourth Shot");
        FakeEnemyMob target = FakeEnemyMob.at(sniper.getX() + 60, sniper.getY());
        context.enemies().setEnemies(new EnemyMob[]{target});
        int tick = 0;
        boolean primedEarly = false;
        while (sniper.bulletsInFlight().size() + target.hits().size() < 3) {
            primedEarly |= TowerSpriteFrameBuilder.poseOf(sniper).has(TowerCue.FOURTH_SHOT);
            sniper.doTick(++tick);
        }
        boolean primed = TowerSpriteFrameBuilder.poseOf(sniper).has(TowerCue.FOURTH_SHOT);
        while (sniper.bulletsInFlight().size() + target.hits().size() < 4) {
            sniper.doTick(++tick);
        }

        assertThat(primedEarly).isFalse();
        assertThat(primed).isTrue();
        assertThat(TowerSpriteFrameBuilder.poseOf(sniper).has(TowerCue.FOURTH_SHOT)).isFalse();
    }

    @Test
    void aRicochetFliesFromTheEnemyItHoppedFromOnAnArcNotAStraightLine() {
        GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        SniperTower sniper = new SniperTower(context, 3, 3);
        UpgradePaths.buy(sniper, context, "Ricochet");
        FakeEnemyMob aimed = FakeEnemyMob.at(sniper.getX() + 60, sniper.getY());
        aimed.landEveryHitCritical();
        FakeEnemyMob beside = FakeEnemyMob.at(aimed.getX() + 40, aimed.getY());
        context.enemies().setEnemies(new EnemyMob[]{aimed, beside});
        int tick = 0;
        while (sniper.hopsInFlight().isEmpty()) {
            sniper.doTick(++tick);
        }
        SniperTower.Hop hop = sniper.hopsInFlight().getFirst();
        int midFlight = (hop.bullet().firedAt() + hop.bullet().landsAt()) / 2;

        List<TracerDraw> streak = tracersOf(sniper, context, midFlight, 0.0).stream()
                .filter(tracer -> tracer.tail() == Palette.TOWER_SNIPER_BODY).toList();

        assertThat(streak).isNotEmpty();
        assertThat(streak.getFirst().fromX()).isGreaterThan((float) aimed.getX());
        assertThat(streak.getLast().toY()).isNotCloseTo((float) aimed.getY(), within(1f));
    }

    @Test
    void aNukeLandsInAWhiteFlashAndAnExpandingRingThatEndAfterTenTicks() {
        GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        MortarTower mortar = new MortarTower(context, 3, 3);
        UpgradePaths.buy(mortar, context, "Fusion");
        MortarTower.loadMagazine(mortar, MagazineFixtures.frontedBy(ShellKind.NUKE));
        FakeEnemyMob target = FakeEnemyMob.at(mortar.getX() + 3 * BoardFixtures.SCALE, mortar.getY());
        context.enemies().setEnemies(new EnemyMob[]{target});
        int landedAt = 0;
        for (int t = 1; t < 2000 && landedAt == 0; t++) {
            mortar.doTick(t);
            context.projectiles().doTick(t);
            if (mortar.getNukeFlash().isPresent()) {
                landedAt = t;
            }
        }

        TowerEffectFrameBuilder during = new TowerEffectFrameBuilder(context, landedAt + 2, 0.0, 0.0);
        mortar.accept(during);
        TowerEffectFrameBuilder after = new TowerEffectFrameBuilder(context, landedAt + 12, 0.0, 0.0);
        mortar.accept(after);

        assertThat(during.build()).extracting(draw -> draw.getClass().getSimpleName()).contains("FlashDraw", "RingDraw");
        assertThat(after.build()).extracting(draw -> draw.getClass().getSimpleName()).doesNotContain("FlashDraw");
    }

    /** Ticks {@code mortar} at {@code target} until its first shell lands, and returns that tick. */
    private static int firstLanding(MortarTower mortar, GameWorld context) {
        for (int t = 1; t < 2000; t++) {
            mortar.doTick(t);
            context.projectiles().doTick(t);
            if (!mortar.getBlastMarks().isEmpty()) {
                return t;
            }
        }
        throw new AssertionError("No shell landed");
    }

    private static List<RingDraw> ringsOf(MortarTower mortar, GameWorld context, int tick) {
        TowerEffectFrameBuilder builder = new TowerEffectFrameBuilder(context, tick, 0.0, 0.0);
        mortar.accept(builder);
        return builder.build().stream().filter(RingDraw.class::isInstance).map(RingDraw.class::cast).toList();
    }

    @Test
    void aBlastRingsItsFullDamageCoreBrighterThanItsEdge() {
        GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        MortarTower mortar = new MortarTower(context, 3, 3);
        context.enemies().setEnemies(new EnemyMob[]{FakeEnemyMob.at(mortar.getX() + 3 * BoardFixtures.SCALE,
                mortar.getY())});
        int landed = firstLanding(mortar, context);
        BlastMark mark = mortar.getBlastMarks().getFirst();

        List<RingDraw> rings = ringsOf(mortar, context, landed + 1);

        RingDraw core = rings.stream().filter(ring -> ring.radius() == mark.core()).findFirst().orElseThrow();
        RingDraw edge = rings.stream().filter(ring -> ring.radius() == mark.edge()).findFirst().orElseThrow();
        assertThat(core.palette()).isEqualTo(Palette.BLAST_RING);
        assertThat(edge.palette()).isEqualTo(Palette.BLAST_RING);
        assertThat(core.alpha()).isGreaterThan(edge.alpha());
    }

    @Test
    void aNukesStrongerEdgeIsRingedInItsOwnColour() {
        GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        MortarTower mortar = new MortarTower(context, 3, 3);
        UpgradePaths.buy(mortar, context, "Fusion");
        MortarTower.loadMagazine(mortar, MagazineFixtures.frontedBy(ShellKind.NUKE));
        context.enemies().setEnemies(new EnemyMob[]{FakeEnemyMob.at(mortar.getX() + 3 * BoardFixtures.SCALE,
                mortar.getY())});
        int landed = firstLanding(mortar, context);
        BlastMark mark = mortar.getBlastMarks().getFirst();

        List<RingDraw> rings = ringsOf(mortar, context, landed + 1);

        assertThat(rings).filteredOn(ring -> ring.radius() == mark.edge()).extracting(RingDraw::palette)
                .containsExactly(Palette.BLAST_NUKE_RING);
    }

    @Test
    void onlyAHeavyWithShrapnelThrowsFragmentsOutOverItsBlast() {
        GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        MortarTower shrapnel = new MortarTower(context, 3, 3);
        UpgradePaths.buy(shrapnel, context, "Shrapnel");
        MortarTower.loadMagazine(shrapnel, MagazineFixtures.frontedBy(ShellKind.HEAVY));
        MortarTower plain = new MortarTower(context, 3, 3);
        MortarTower.loadMagazine(plain, MagazineFixtures.frontedBy(ShellKind.HEAVY));
        context.enemies().setEnemies(new EnemyMob[]{FakeEnemyMob.at(shrapnel.getX() + 3 * BoardFixtures.SCALE,
                shrapnel.getY())});
        int landed = firstLanding(shrapnel, context);
        context.projectiles().clear();
        int plainLanded = firstLanding(plain, context);

        assertThat(tracersOf(shrapnel, context, landed + 2, 0.0)).isNotEmpty();
        assertThat(tracersOf(plain, context, plainLanded + 2, 0.0)).isEmpty();
    }

    @Test
    void aMortarHoldsItsFrontShellInItsBoreAndATrayOnlyWhileItLoadsAVolley() {
        GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        MortarTower single = new MortarTower(context, 3, 3);
        MortarTower.loadMagazine(single, MagazineFixtures.frontedBy(ShellKind.HEAVY));
        MortarTower volley = new MortarTower(context, 8, 8);
        UpgradePaths.buy(volley, context, "Barrage");

        single.doTick(1);
        volley.doTick(1);

        assertThat(TowerSpriteFrameBuilder.poseOf(single).shell()).isEqualTo(LoadedShell.HEAVY);
        assertThat(TowerSpriteFrameBuilder.poseOf(single).has(TowerCue.VOLLEY)).isFalse();
        assertThat(TowerSpriteFrameBuilder.poseOf(volley).has(TowerCue.VOLLEY)).isTrue();
    }

    private static CinderTower scorcherWith(GameWorld context, String... nodes) {
        CinderTower scorcher = new CinderTower(context, 3, 3);
        context.towers().add(scorcher);
        UpgradePaths.buy(scorcher, context, nodes);
        return scorcher;
    }

    /** The streams the Scorcher draws on its first tick, with {@code enemies} standing before it. */
    private static List<StreamDraw> streamsOf(GameWorld context, CinderTower scorcher, EnemyMob... enemies) {
        context.enemies().setEnemies(enemies);
        scorcher.doTick(1);
        TowerEffectFrameBuilder builder = new TowerEffectFrameBuilder(context, 1, 0.0, 0.0);
        scorcher.accept(builder);
        return builder.build().stream().filter(StreamDraw.class::isInstance).map(StreamDraw.class::cast).toList();
    }

    @Test
    void aStreamLeavesFromTheNozzleAsItIsPosed() {
        GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        CinderTower scorcher = scorcherWith(context);

        StreamDraw stream = streamsOf(context, scorcher, FakeEnemyMob.at(scorcher.getX() + 40, scorcher.getY()))
                .getFirst();

        float muzzle = TowerSprites.muzzle(TowerLook.CINDER, 0, TowerSpriteFrameBuilder.poseOf(scorcher),
                scorcher.getX(), scorcher.getY(), BoardFixtures.SCALE, 0.0).x;
        assertThat(stream.originX()).isEqualTo(muzzle);
        assertThat(muzzle + stream.tongues().getFirst().reach()).isCloseTo(scorcher.getX() + scorcher.getRangeReal(),
                within(1e-3f));
    }

    @Test
    void theCoreDrawsTheStreamsInnerTongueFurtherOut() {
        GameWorld plainWorld = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        CinderTower plain = scorcherWith(plainWorld);
        GameWorld coreWorld = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        CinderTower cored = scorcherWith(coreWorld, "Core");

        float plainCore = streamsOf(plainWorld, plain, FakeEnemyMob.at(plain.getX() + 40, plain.getY()))
                .getFirst().tongues().getLast().reach();
        float core = streamsOf(coreWorld, cored, FakeEnemyMob.at(cored.getX() + 40, cored.getY()))
                .getFirst().tongues().getLast().reach();

        assertThat(core).isGreaterThan(plainCore);
    }

    @Test
    void aFlashoverIsDrawnAsATongueFromTheHotEnemyToTheOneBesideIt() {
        GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        CinderTower scorcher = scorcherWith(context, "Flashover");
        FakeEnemyMob hot = FakeEnemyMob.at(scorcher.getX() + 40, scorcher.getY());
        hot.applyEffect(Effect.burn(Damage.magic(DamageUnits.ofPoints(2 * FlashoverMechanic.HOT_FUEL_POINTS)),
                DamageSink.none()));
        FakeEnemyMob beside = FakeEnemyMob.at(hot.getX(), hot.getY() + 38);

        List<StreamDraw> streams = streamsOf(context, scorcher, hot, beside);

        assertThat(streams).hasSize(2);
        StreamDraw flash = streams.getLast();
        assertThat(flash.originX()).isEqualTo((float) hot.getX());
        assertThat(flash.originY()).isEqualTo((float) hot.getY());
        assertThat(flash.headingRadians()).isCloseTo((float) (Math.PI / 2), within(1e-4f));
        assertThat(flash.tongues().getFirst().reach()).isCloseTo(38f, within(1e-3f));
    }

    @Test
    void aSoulfireScorcherWearsTheSoulfireLook() {
        GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);

        assertThat(TowerSpriteFrameBuilder.bodyLookOf(scorcherWith(context))).isEqualTo(TowerLook.CINDER);
        assertThat(TowerSpriteFrameBuilder.bodyLookOf(scorcherWith(context, "Soulfire"))).isEqualTo(TowerLook.SOULFIRE);
    }

    @Test
    void anAerieWearsTheCrystalOfItsSecondMissile() {
        GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        SeekerTower plain = new SeekerTower(context, 3, 3);
        SeekerTower ruby = new SeekerTower(context, 6, 6);
        UpgradePaths.buy(ruby, context, "Ruby");

        assertThat(TowerSpriteFrameBuilder.bodyLookOf(plain)).isEqualTo(TowerLook.SEEKER);
        assertThat(TowerSpriteFrameBuilder.bodyLookOf(ruby)).isEqualTo(TowerLook.RUBY);
    }

    @Test
    void theNestDrawsEachMissileItHoldsOnARingAboveTheTower() {
        GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        SeekerTower tower = new SeekerTower(context, 3, 3);
        tower.readyForWave();

        TowerEffectFrameBuilder builder = new TowerEffectFrameBuilder(context, 1, 0.0, 0.0);
        tower.accept(builder);

        assertThat(builder.build()).singleElement().isInstanceOfSatisfying(NestDraw.class, nest -> {
            assertThat(nest.missiles()).hasSize(tower.nestMissiles().size()).containsOnly(LoadedMissile.FROST);
            assertThat(nest.centerY()).isLessThan(tower.getY());
            assertThat(nest.releaseReady()).isFalse();
        });
    }
}

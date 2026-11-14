package td.ui;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyMob;
import td.fixtures.BoardFixtures;
import td.fixtures.FakeEnemyMob;
import td.fixtures.UpgradePaths;
import td.fixtures.WorldFixtures;
import td.stat.DisruptionAura;
import td.tower.AuraTower;
import td.tower.MortarTower;
import td.tower.SniperTower;
import td.tower.SplashTower;
import td.ui.render.BeamDraw;
import td.ui.render.Palette;
import td.ui.render.RefundMarkDraw;
import td.ui.render.RingDraw;
import td.ui.render.TowerCue;
import td.ui.render.TowerEffectDraw;
import td.ui.render.TowerLook;
import td.ui.render.TowerPose;
import td.ui.render.TowerStatusDraw;
import td.ui.render.TracerDraw;
import td.util.GameWorld;
import td.wave.Path;
import td.wave.Vec2;

import java.util.ArrayList;
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

    /** What the Splash draws on the tick it fires at one enemy standing in range. */
    private static List<TowerEffectDraw> firedBy(GameWorld context, SplashTower splash) {
        context.enemies().setEnemies(new EnemyMob[]{FakeEnemyMob.at(splash.getX() + 30, splash.getY())});
        splash.doTick(1);
        TowerEffectFrameBuilder builder = new TowerEffectFrameBuilder(context, 1, 0.0, 0.0);
        splash.accept(builder);
        return builder.build();
    }

    @Test
    void aSplashDrawsItsShotAndBlastInTheColourOfItsFork() {
        GameWorld plainWorld = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        GameWorld stormWorld = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        GameWorld hexWorld = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);

        List<TowerEffectDraw> plain = firedBy(plainWorld, splashWith(plainWorld));
        List<TowerEffectDraw> storm = firedBy(stormWorld, splashWith(stormWorld, "Arc"));
        List<TowerEffectDraw> hex = firedBy(hexWorld, splashWith(hexWorld, "Hex"));

        assertThat(beamsIn(plain, Palette.TOWER_SPLASH_BEAM)).hasSize(1);
        assertThat(ringsIn(plain, Palette.TOWER_SPLASH_DETONATION)).hasSize(1);
        assertThat(beamsIn(storm, Palette.TOWER_STORMCALLER_BEAM)).hasSize(1);
        assertThat(ringsIn(storm, Palette.TOWER_STORMCALLER_DETONATION)).hasSize(1);
        assertThat(ringsIn(storm, Palette.TOWER_SPLASH_DETONATION)).isEmpty();
        assertThat(beamsIn(hex, Palette.TOWER_HEXER_BEAM)).hasSize(1);
        assertThat(ringsIn(hex, Palette.TOWER_HEXER_DETONATION)).hasSize(1);
        assertThat(ringsIn(hex, Palette.TOWER_SPLASH_DETONATION)).isEmpty();
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
        AuraTower aura = auraWith(context, "Tutelage", "Shared Lessons", "Apprentice");
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
    void ralliesSpotIsMarkedOnlyWhileTheAuraIsSelected() {
        GameWorld context = WorldFixtures.newWorld();
        context.setPath(new Path(List.of(new Vec2(0, 0), new Vec2(900, 0))));
        AuraTower aura = auraWith(context, "Rally");

        int unselected = ringsIn(drawnBy(context, aura), Palette.TOWER_AURA_RALLY).size();
        aura.setSelected(true);
        int selected = ringsIn(drawnBy(context, aura), Palette.TOWER_AURA_RALLY).size();

        assertThat(unselected).isZero();
        assertThat(selected).isEqualTo(1);
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

    private static List<TracerDraw> tracersOf(SniperTower sniper, GameWorld context, int gameTime, double alpha) {
        TowerEffectFrameBuilder builder = new TowerEffectFrameBuilder(context, gameTime, alpha, 0.0);
        sniper.accept(builder);
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
    void anAttunedMortarRingsItsLastLandingAndTheRingDrawsInWithEachStep() {
        GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        context.economy().startEconomy(1000, 5);
        MortarTower mortar = new MortarTower(context, 3, 3);
        mortar.earnXp(1000);
        mortar.buyUpgrade(UpgradePaths.named(mortar, "Attune"));
        FakeEnemyMob target = FakeEnemyMob.at(mortar.getX() + 3 * BoardFixtures.SCALE, mortar.getY());
        context.enemies().setEnemies(new EnemyMob[]{target});
        List<Float> radii = new ArrayList<>();

        for (int t = 1; t < 400 && radii.size() < 2; t++) {
            mortar.doTick(t);
            context.projectiles().doTick(t);
            if (mortar.getRangingMarker().isPresent() && mortar.getRangingMarker().get().step() == radii.size()) {
                TowerEffectFrameBuilder builder = new TowerEffectFrameBuilder(context, t, 1.0, 0.0);
                mortar.accept(builder);
                RingDraw ring = (RingDraw) builder.build().getFirst();
                assertThat(ring.palette()).isEqualTo(Palette.TOWER_MORTAR_RANGING);
                assertThat(ring.centerX()).isEqualTo((float) target.getX());
                radii.add(ring.radius());
            }
        }

        assertThat(radii).hasSize(2);
        assertThat(radii.get(1)).isLessThan(radii.get(0));
    }

    @Test
    void aMortarThatHasNotLandedAShellDrawsNoRangingRing() {
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

    private static List<BeamDraw> beamsIn(List<TowerEffectDraw> draws, Palette palette) {
        return draws.stream()
                .filter(BeamDraw.class::isInstance)
                .map(BeamDraw.class::cast)
                .filter(beam -> beam.palette() == palette)
                .toList();
    }

    @Test
    void aNukeLandsInAWhiteFlashAndAnExpandingRingThatEndAfterTenTicks() {
        GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        MortarTower mortar = new MortarTower(context, 3, 3);
        UpgradePaths.buy(mortar, context, "Tactical Nuke");
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
}

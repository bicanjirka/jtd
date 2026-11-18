package td.projectile;

import org.junit.jupiter.api.Test;
import td.fixtures.BoardFixtures;
import td.fixtures.FakeEnemyMob;
import td.ui.ProjectileFrameBuilder;
import td.ui.render.CannonballDraw;
import td.ui.render.LoadedShell;
import td.ui.render.MissileDraw;
import td.ui.render.Palette;
import td.ui.render.ProjectileDraw;
import td.ui.render.SmokeDraw;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ProjectileFrameBuilderTest {

    @Test
    void aCannonballIsInterpolatedBetweenItsPreviousAndCurrentPosition() {
        CannonballProjectile shell = new CannonballProjectile(0, 0, 100, 0, 10f, (x, y, gameTime) -> {
        });
        shell.doTick(1); // prevX/Y = (0,0), x/y = (10,0)

        ProjectileFrameBuilder builder = new ProjectileFrameBuilder(0.5, BoardFixtures.SCALE);
        shell.accept(builder);
        List<ProjectileDraw> draws = builder.build();

        assertThat(draws).hasSize(1);
        CannonballDraw draw = (CannonballDraw) draws.getFirst();
        assertThat(draw.x()).isCloseTo(5f, within(0.01f));
        assertThat(draw.y()).isEqualTo(0f);
    }

    /** How high {@code shell} is drawn once it has flown {@code ticks} ticks. */
    private static float liftAfter(CannonballProjectile shell, int ticks) {
        for (int t = 1; t <= ticks; t++) {
            shell.doTick(t);
        }
        ProjectileFrameBuilder builder = new ProjectileFrameBuilder(1.0, BoardFixtures.SCALE);
        shell.accept(builder);
        return ((CannonballDraw) builder.build().getFirst()).lift();
    }

    private static CannonballProjectile lob(float distance, float speed) {
        return new CannonballProjectile(0, 0, distance, 0, speed, (x, y, gameTime) -> {
        });
    }

    @Test
    void aShellLeavesFromItsLaunchHeightAndArcsSymmetricallyOverItsDescentOntoTheGround() {
        float leaving = liftAfter(lob(200, 10f), 0);
        float quarter = liftAfter(lob(200, 10f), 5);
        float threeQuarters = liftAfter(lob(200, 10f), 15);
        float landed = liftAfter(lob(200, 10f), 20);

        assertThat(leaving).isPositive();
        assertThat(quarter).isGreaterThan(leaving);
        assertThat(quarter - leaving * 0.75f).isCloseTo(threeQuarters - leaving * 0.25f, within(0.01f));
        assertThat(landed).isZero();
    }

    @Test
    void aShellClimbsHigherTheFurtherItFliesOnTheGroundWhateverItsSpeed() {
        float fast = liftAfter(lob(2 * BoardFixtures.SCALE, 8f), BoardFixtures.SCALE / 8);
        float slow = liftAfter(lob(2 * BoardFixtures.SCALE, 4f), BoardFixtures.SCALE / 4);
        float further = liftAfter(lob(4 * BoardFixtures.SCALE, 8f), BoardFixtures.SCALE / 4);

        assertThat(slow).isCloseTo(fast, within(0.01f));
        assertThat(further).isGreaterThan(fast);
    }

    @Test
    void aSpecialShellIsDrawnInItsLookAtItsSize() {
        CannonballProjectile shell = new CannonballProjectile(0, 0, 100, 0, ProjectileStats.of(10f).withSize(2f),
                ShellLook.CRYO, (x, y, gameTime) -> {
                });

        ProjectileFrameBuilder builder = new ProjectileFrameBuilder(1.0, BoardFixtures.SCALE);
        shell.accept(builder);

        CannonballDraw draw = (CannonballDraw) builder.build().getFirst();
        assertThat(draw.palette()).isEqualTo(Palette.PROJECTILE_FROST);
        assertThat(draw.size()).isEqualTo(2f);
    }

    @Test
    void aShellIsDrawnAsItsKindAndOnlyAShellThatBurnsFreezesOrGlowsTrails() {
        CannonballProjectile napalm = new CannonballProjectile(0, 0, 100, 0, ProjectileStats.of(10f), ShellLook.NAPALM,
                (x, y, gameTime) -> {
                });
        CannonballProjectile heavy = new CannonballProjectile(0, 0, 100, 0, ProjectileStats.of(10f), ShellLook.HEAVY,
                (x, y, gameTime) -> {
                });
        napalm.doTick(1);
        heavy.doTick(1);

        ProjectileFrameBuilder builder = new ProjectileFrameBuilder(1.0, BoardFixtures.SCALE);
        napalm.accept(builder);
        heavy.accept(builder);

        CannonballDraw burning = (CannonballDraw) builder.build().getFirst();
        CannonballDraw plain = (CannonballDraw) builder.build().getLast();
        assertThat(burning.kind()).contains(LoadedShell.NAPALM);
        assertThat(plain.kind()).contains(LoadedShell.HEAVY);
        assertThat(burning.tailX()).isLessThan(burning.x());
        assertThat(plain.tailX()).isEqualTo(plain.x());
    }

    @Test
    void aMissilesFacingMatchesItsDirectionOfTravelThisTick() {
        FakeEnemyMob target = FakeEnemyMob.at(0, 100);
        MissileProjectile missile = new MissileProjectile(0, 0, Math.PI / 2, target, ProjectileStats.of(10f),
                new FakeMissileGuide());

        missile.doTick(1);

        ProjectileFrameBuilder builder = new ProjectileFrameBuilder(1.0, BoardFixtures.SCALE);
        missile.accept(builder);
        MissileDraw draw = (MissileDraw) builder.build().getLast();

        assertThat(draw.facingRadians()).isCloseTo(Math.PI / 2, within(0.01));
    }

    @Test
    void aMissileIsDrawnAtItsSizeBehindAFadingTrailOfSmoke() {
        FakeEnemyMob target = FakeEnemyMob.at(0, 1000);
        MissileProjectile missile = new MissileProjectile(0, 0, Math.PI / 2, target,
                ProjectileStats.of(10f).withSize(2f), new FakeMissileGuide());
        MissileProjectile standard = new MissileProjectile(0, 0, Math.PI / 2, target, ProjectileStats.of(10f),
                new FakeMissileGuide());
        for (int t = 1; t <= 4; t++) {
            missile.doTick(t);
        }

        ProjectileFrameBuilder builder = new ProjectileFrameBuilder(1.0, BoardFixtures.SCALE);
        standard.accept(builder);
        float standardLength = ((MissileDraw) builder.build().getLast()).length();
        missile.accept(builder);
        List<ProjectileDraw> draws = builder.build().subList(1, builder.build().size());

        assertThat(draws.getLast()).isInstanceOfSatisfying(MissileDraw.class,
                draw -> assertThat(draw.length()).isEqualTo(2 * standardLength));
        List<SmokeDraw> smoke = draws.stream().filter(SmokeDraw.class::isInstance).map(SmokeDraw.class::cast).toList();
        assertThat(smoke).hasSize(4);
        assertThat(smoke).extracting(SmokeDraw::alpha).isSorted();
        assertThat(smoke.getFirst().alpha()).isLessThan(smoke.getLast().alpha());
    }
}

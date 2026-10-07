package td.projectile;

import org.junit.jupiter.api.Test;
import td.fixtures.FakeEnemyMob;
import td.ui.ProjectileFrameBuilder;
import td.ui.render.CannonballDraw;
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
        CannonballProjectile shell = new CannonballProjectile(0, 0, 100, 0, 10f, (x, y) -> {
        });
        shell.doTick(1); // prevX/Y = (0,0), x/y = (10,0)

        ProjectileFrameBuilder builder = new ProjectileFrameBuilder(0.5);
        shell.accept(builder);
        List<ProjectileDraw> draws = builder.build();

        assertThat(draws).hasSize(1);
        CannonballDraw draw = (CannonballDraw) draws.getFirst();
        assertThat(draw.x()).isCloseTo(5f, within(0.01f));
        assertThat(draw.y()).isEqualTo(0f);
    }

    @Test
    void aSpecialShellIsDrawnInItsLookAtItsSize() {
        CannonballProjectile shell = new CannonballProjectile(0, 0, 100, 0, ProjectileStats.of(10f).withSize(2f),
                ShellLook.FROST, (x, y) -> {
                });

        ProjectileFrameBuilder builder = new ProjectileFrameBuilder(1.0);
        shell.accept(builder);

        CannonballDraw draw = (CannonballDraw) builder.build().getFirst();
        assertThat(draw.palette()).isEqualTo(Palette.PROJECTILE_FROST);
        assertThat(draw.size()).isEqualTo(2f);
    }

    @Test
    void aMissilesFacingMatchesItsDirectionOfTravelThisTick() {
        FakeEnemyMob target = FakeEnemyMob.at(0, 100);
        FakeEnemyRegistry registry = new FakeEnemyRegistry(target);
        MissileProjectile missile = new MissileProjectile(0, 0, target, registry, ProjectileStats.of(10f), t -> {
        });

        missile.doTick(1); // moves straight toward (0, 100), i.e. facing +Y

        ProjectileFrameBuilder builder = new ProjectileFrameBuilder(1.0);
        missile.accept(builder);
        MissileDraw draw = (MissileDraw) builder.build().getLast();

        assertThat(draw.facingRadians()).isCloseTo(Math.PI / 2, within(0.01));
    }

    @Test
    void aMissileIsDrawnAtItsSizeBehindAFadingTrailOfSmoke() {
        FakeEnemyMob target = FakeEnemyMob.at(0, 1000);
        FakeEnemyRegistry registry = new FakeEnemyRegistry(target);
        MissileProjectile missile = new MissileProjectile(0, 0, target, registry,
                ProjectileStats.of(10f).withSize(2f), t -> {
                });
        for (int t = 1; t <= 4; t++) {
            missile.doTick(t);
        }

        ProjectileFrameBuilder builder = new ProjectileFrameBuilder(1.0);
        missile.accept(builder);
        List<ProjectileDraw> draws = builder.build();

        assertThat(draws.getLast()).isInstanceOfSatisfying(MissileDraw.class, draw -> assertThat(draw.size()).isEqualTo(2f));
        List<SmokeDraw> smoke = draws.stream().filter(SmokeDraw.class::isInstance).map(SmokeDraw.class::cast).toList();
        assertThat(smoke).hasSize(4);
        assertThat(smoke).extracting(SmokeDraw::alpha).isSorted();
        assertThat(smoke.getFirst().alpha()).isLessThan(smoke.getLast().alpha());
    }
}

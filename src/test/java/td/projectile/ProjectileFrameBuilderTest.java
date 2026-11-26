package td.projectile;

import org.junit.jupiter.api.Test;
import td.fixtures.BoardFixtures;
import td.ui.ProjectileFrameBuilder;
import td.ui.render.CannonballDraw;
import td.ui.render.ProjectileDraw;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ProjectileFrameBuilderTest {

    @Test
    void aCannonballIsInterpolatedBetweenItsPreviousAndCurrentPosition() {
        CannonballProjectile shell = new CannonballProjectile(0, 0, 100, 0, 10f, (x, y, gameTime) -> {
        });
        shell.doTick(1); // prevX/Y = (0,0), x/y = (10,0)

        ProjectileFrameBuilder builder = new ProjectileFrameBuilder(0, 0.5, BoardFixtures.SCALE, Map.of());
        shell.accept(builder);
        List<ProjectileDraw> draws = builder.build();

        assertThat(draws).hasSize(1);
        CannonballDraw draw = (CannonballDraw) draws.getFirst();
        assertThat(draw.x()).isCloseTo(5f, within(0.01f));
        assertThat(draw.y()).isEqualTo(0f);
    }
}

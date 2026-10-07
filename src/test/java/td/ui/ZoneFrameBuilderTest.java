package td.ui;

import org.junit.jupiter.api.Test;
import td.fixtures.WorldFixtures;
import td.ui.render.Palette;
import td.ui.render.ZoneDraw;
import td.util.GameWorld;
import td.zone.Zone;
import td.zone.ZoneKind;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ZoneFrameBuilderTest {

    @Test
    void eachZoneIsDrawnInItsKindsPaletteAtItsPlaceAndRadiusWithItsLifeLeft() {
        GameWorld world = WorldFixtures.newWorld();
        world.zones().add(new Zone(ZoneKind.TAR, 40, 60, 30, 100, 100, (target, effect) -> {
        }));
        world.zones().add(new Zone(ZoneKind.FROST_GROUND, 80, 20, 10, 100, 100, (target, effect) -> {
        }));
        world.zones().doTick(1);

        List<ZoneDraw> draws = ZoneFrameBuilder.build(world.zones(), 2.0);

        assertThat(draws).extracting(ZoneDraw::palette).containsExactly(Palette.ZONE_TAR, Palette.ZONE_FROST);
        assertThat(draws.getFirst().centerX()).isEqualTo(40f);
        assertThat(draws.getFirst().centerY()).isEqualTo(60f);
        assertThat(draws.getFirst().radius()).isEqualTo(30f);
        assertThat(draws.getFirst().life()).isEqualTo(0.99f);
        assertThat(draws.getFirst().phase()).isEqualTo(2f);
    }
}

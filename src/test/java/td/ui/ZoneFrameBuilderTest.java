package td.ui;

import org.junit.jupiter.api.Test;
import td.fixtures.WorldFixtures;
import td.ui.render.Palette;
import td.ui.render.ZoneDraw;
import td.ui.render.ZonePatchDraw;
import td.util.GameWorld;
import td.zone.Zone;
import td.zone.ZoneKind;
import td.zone.ZoneOwner;
import td.zone.ZonePulse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ZoneFrameBuilderTest {

    private final GameWorld world = WorldFixtures.newWorld();

    private void addZone(ZoneKind kind, double x, double y, float radius, int lifetimeTicks) {
        this.world.zones().add(new Zone(kind, x, y, radius, lifetimeTicks, ZonePulse.of(100), ZoneOwner.nobody()));
    }

    @Test
    void aZoneIsADiscInItsKindsPatchAtItsPlaceAndRadius() {
        this.addZone(ZoneKind.TAR, 40, 60, 30, 100);
        this.world.zones().doTick(1);

        List<ZonePatchDraw> patches = ZoneFrameBuilder.build(this.world.zones(), List.of(), 2.0);

        assertThat(patches).extracting(ZonePatchDraw::palette).containsExactly(Palette.ZONE_TAR);
        ZoneDraw disc = patches.getFirst().discs().getFirst();
        assertThat(disc.centerX()).isEqualTo(40f);
        assertThat(disc.centerY()).isEqualTo(60f);
        assertThat(disc.radius()).isEqualTo(30f);
        assertThat(disc.fade()).isEqualTo(1f);
        assertThat(disc.phase()).isEqualTo(2f);
    }
}

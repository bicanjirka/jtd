package td.ui;

import org.junit.jupiter.api.Test;
import td.fixtures.WorldFixtures;
import td.ui.render.Palette;
import td.ui.render.ZoneDraw;
import td.ui.render.ZonePatchDraw;
import td.util.GameWorld;
import td.zone.Zone;
import td.zone.ZoneKind;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ZoneFrameBuilderTest {

    private final GameWorld world = WorldFixtures.newWorld();

    private void addZone(ZoneKind kind, double x, double y, float radius, int lifetimeTicks) {
        this.world.zones().add(new Zone(kind, x, y, radius, lifetimeTicks, 100, (target, effect) -> {
        }));
    }

    @Test
    void aZoneIsADiscInItsKindsPatchAtItsPlaceAndRadius() {
        this.addZone(ZoneKind.TAR, 40, 60, 30, 100);
        this.world.zones().doTick(1);

        List<ZonePatchDraw> patches = ZoneFrameBuilder.build(this.world.zones(), 2.0);

        assertThat(patches).extracting(ZonePatchDraw::palette).containsExactly(Palette.ZONE_TAR);
        ZoneDraw disc = patches.getFirst().discs().getFirst();
        assertThat(disc.centerX()).isEqualTo(40f);
        assertThat(disc.centerY()).isEqualTo(60f);
        assertThat(disc.radius()).isEqualTo(30f);
        assertThat(disc.fade()).isEqualTo(1f);
        assertThat(disc.phase()).isEqualTo(2f);
    }

    @Test
    void zonesOfOneKindShareOnePatchAndZonesOfAnotherKindGetTheirOwn() {
        this.addZone(ZoneKind.TAR, 40, 60, 30, 100);
        this.addZone(ZoneKind.FROST_GROUND, 80, 20, 10, 100);
        this.addZone(ZoneKind.TAR, 200, 60, 30, 100);

        List<ZonePatchDraw> patches = ZoneFrameBuilder.build(this.world.zones(), 0.0);

        assertThat(patches).hasSize(2);
        assertThat(patches).filteredOn(patch -> patch.palette() == Palette.ZONE_TAR).singleElement()
                .satisfies(tar -> assertThat(tar.discs()).hasSize(2));
    }

    @Test
    void aZoneNearItsEndIsDrawnShrunkInStepWithItsFade() {
        float radius = 30f;
        this.addZone(ZoneKind.TAR, 40, 60, radius, 100);
        for (int tick = 1; tick <= 90; tick++) {
            this.world.zones().doTick(tick);
        }

        ZoneDraw disc = ZoneFrameBuilder.build(this.world.zones(), 0.0).getFirst().discs().getFirst();

        assertThat(disc.fade()).isBetween(0f, 0.99f);
        assertThat(disc.radius()).isEqualTo(radius * disc.fade());
    }
}

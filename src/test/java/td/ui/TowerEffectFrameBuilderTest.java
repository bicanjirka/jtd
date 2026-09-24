package td.ui;

import org.junit.jupiter.api.Test;
import td.fixtures.WorldFixtures;
import td.tower.AuraTower;
import td.tower.SniperTower;
import td.ui.render.BeamDraw;
import td.ui.render.Palette;
import td.ui.render.TowerEffectDraw;
import td.util.GameWorld;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the Aura tower's buffed-tower link beams - everything else this builder draws is
 * exercised through {@code BoardRendererTest} instead.
 */
class TowerEffectFrameBuilderTest {

    @Test
    void anAuraTowerDrawsALinkBeamToEachTowerItBuffsAndNoOthers() {
        GameWorld context = WorldFixtures.newWorld();
        AuraTower aura = new AuraTower(context, 0, 0);
        context.towers().add(aura);
        SniperTower near = new SniperTower(context, 0, 0);
        context.towers().add(near);
        // far outside the aura's range
        SniperTower far = new SniperTower(context, 100, 100);
        context.towers().add(far);

        TowerEffectFrameBuilder builder = new TowerEffectFrameBuilder(0, 0.0, 0.0);
        aura.accept(builder);

        List<BeamDraw> linkBeams = linkBeamsIn(builder.build());
        assertThat(linkBeams).hasSize(1);
        assertThat(linkBeams.getFirst().toX()).isEqualTo((float) near.getX());
        assertThat(linkBeams.getFirst().toY()).isEqualTo((float) near.getY());
    }

    @Test
    void anAuraTowerWithNothingInRangeDrawsNoLinkBeams() {
        GameWorld context = WorldFixtures.newWorld();
        AuraTower aura = new AuraTower(context, 0, 0);
        context.towers().add(aura);

        TowerEffectFrameBuilder builder = new TowerEffectFrameBuilder(0, 0.0, 0.0);
        aura.accept(builder);

        assertThat(linkBeamsIn(builder.build())).isEmpty();
    }

    private static List<BeamDraw> linkBeamsIn(List<TowerEffectDraw> draws) {
        return draws.stream()
                .filter(BeamDraw.class::isInstance)
                .map(BeamDraw.class::cast)
                .filter(beam -> beam.palette() == Palette.TOWER_AURA_LINK)
                .toList();
    }
}

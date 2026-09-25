package td.ui;

import org.junit.jupiter.api.Test;
import td.fixtures.WorldFixtures;
import td.stat.DisruptionAura;
import td.tower.AuraTower;
import td.tower.SniperTower;
import td.ui.render.BeamDraw;
import td.ui.render.Palette;
import td.ui.render.TowerEffectDraw;
import td.ui.render.TowerStatusDraw;
import td.util.GameWorld;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Covers aura link beams and the disruption marker; the other effects are covered by {@code BoardRendererTest}. */
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

    @Test
    void aDisruptedTowerWearsAMarkerInTheDisruptionColourAndAnUndisruptedOneNone() {
        GameWorld context = WorldFixtures.newWorld();
        SniperTower jammed = new SniperTower(context, 0, 0);
        SniperTower clear = new SniperTower(context, 10, 10);
        context.disruptions().add(jammed.getX(), jammed.getY(), new DisruptionAura(20f, 0.3f, 0.2f));
        jammed.refreshDisruption();
        clear.refreshDisruption();

        TowerEffectFrameBuilder builder = new TowerEffectFrameBuilder(0, 0.0, 0.0);
        builder.addStatus(jammed, context.getBoard().scale());
        builder.addStatus(clear, context.getBoard().scale());

        assertThat(builder.build()).singleElement().isInstanceOfSatisfying(TowerStatusDraw.class,
                marker -> assertThat(marker.palette()).isEqualTo(Palette.DISRUPTION));
    }

    private static List<BeamDraw> linkBeamsIn(List<TowerEffectDraw> draws) {
        return draws.stream()
                .filter(BeamDraw.class::isInstance)
                .map(BeamDraw.class::cast)
                .filter(beam -> beam.palette() == Palette.TOWER_AURA_LINK)
                .toList();
    }
}

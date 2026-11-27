package td.ui;

import org.junit.jupiter.api.Test;
import td.fixtures.WorldFixtures;
import td.stat.DisruptionAura;
import td.tower.SniperTower;
import td.ui.render.Palette;
import td.ui.render.RefundMarkDraw;
import td.ui.render.TowerStatusDraw;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

/** Covers the tower effects {@code BoardRendererTest} leaves out. */
class TowerEffectFrameBuilderTest {

    @Test
    void aTowerNoWaveHasUsedWearsTheRefundMarkUntilOneHas() {
        GameWorld context = WorldFixtures.newWorld();
        SniperTower tower = new SniperTower(context, 0, 0);
        TowerEffectFrameBuilder unused = new TowerEffectFrameBuilder(context, 0, 0.0, 0.0);
        unused.addStatus(tower);

        WorldFixtures.passAWave(context);
        TowerEffectFrameBuilder used = new TowerEffectFrameBuilder(context, 0, 0.0, 0.0);
        used.addStatus(tower);

        assertThat(unused.build()).extracting(PlacedDraw::draw).singleElement().isInstanceOf(RefundMarkDraw.class);
        assertThat(used.build()).extracting(PlacedDraw::draw).isEmpty();
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

        assertThat(builder.build()).extracting(PlacedDraw::draw).singleElement().isInstanceOfSatisfying(TowerStatusDraw.class,
                marker -> assertThat(marker.palette()).isEqualTo(Palette.DISRUPTION));
    }
}

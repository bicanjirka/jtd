package td.ui;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyMob;
import td.fixtures.BoardFixtures;
import td.fixtures.FakeEnemyMob;
import td.fixtures.WorldFixtures;
import td.stat.DisruptionAura;
import td.tower.AuraTower;
import td.tower.MortarTower;
import td.tower.SniperTower;
import td.tower.SonarTower;
import td.tower.Tower;
import td.tower.upgrade.UpgradeNode;
import td.ui.render.BeamDraw;
import td.ui.render.Palette;
import td.ui.render.RingDraw;
import td.ui.render.TowerEffectDraw;
import td.ui.render.TowerStatusDraw;
import td.util.GameWorld;
import td.util.TickRate;

import java.util.ArrayList;
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

        TowerEffectFrameBuilder builder = new TowerEffectFrameBuilder(context.getBoard().scale(), 0, 0.0, 0.0);
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

        TowerEffectFrameBuilder builder = new TowerEffectFrameBuilder(context.getBoard().scale(), 0, 0.0, 0.0);
        aura.accept(builder);

        assertThat(linkBeamsIn(builder.build())).isEmpty();
    }

    @Test
    void aDisruptedTowerWearsAMarkerInTheDisruptionColourAndAnUndisruptedOneNone() {
        GameWorld context = WorldFixtures.newWorld();
        SniperTower jammed = new SniperTower(context, 0, 0);
        SniperTower clear = new SniperTower(context, 10, 10);
        context.disruptions().add(jammed.getX(), jammed.getY(), new DisruptionAura(20f, 0.3f, 0.2f));
        jammed.beginTick(0);
        clear.beginTick(0);

        TowerEffectFrameBuilder builder = new TowerEffectFrameBuilder(context.getBoard().scale(), 0, 0.0, 0.0);
        builder.addStatus(jammed);
        builder.addStatus(clear);

        assertThat(builder.build()).singleElement().isInstanceOfSatisfying(TowerStatusDraw.class,
                marker -> assertThat(marker.palette()).isEqualTo(Palette.DISRUPTION));
    }

    @Test
    void anAttunedSnipersAimLaserIsFaintAndBrightensWithItsSteadyAimStack() {
        GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        SniperTower attuned = new SniperTower(context, 3, 3);
        attuned.earnXp(1_000);
        context.economy().startEconomy(1_000, 5);
        attuned.buyUpgrade(nodeNamed(attuned, "Attune"));
        SniperTower plain = new SniperTower(context, 3, 3);
        context.enemies().setEnemies(new EnemyMob[]{FakeEnemyMob.at(attuned.getX() + 40, attuned.getY())});
        attuned.doTick(1);
        plain.doTick(1);

        TowerEffectFrameBuilder builder = new TowerEffectFrameBuilder(BoardFixtures.SCALE, 1, 0.0, 0.0);
        attuned.accept(builder);
        plain.accept(builder);

        assertThat(beamsIn(builder.build(), Palette.TOWER_SNIPER_LASER)).singleElement()
                .extracting(BeamDraw::alpha).isEqualTo(0.2f + 0.15f);
    }

    @Test
    void aSonarSendsARingEachRevolutionAndPutsACrosshairOnTheEnemyItPinged() {
        GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        SonarTower sonar = new SonarTower(context, 3, 3);
        sonar.earnXp(1_000);
        context.economy().startEconomy(1_000, 5);
        sonar.buyUpgrade(nodeNamed(sonar, "Attune"));
        FakeEnemyMob enemy = FakeEnemyMob.at(sonar.getX() + 40, sonar.getY() - 2);
        context.enemies().setEnemies(new EnemyMob[]{enemy});
        int revolution = Math.round(SonarTower.SECONDS_PER_REVOLUTION * TickRate.TICKS_PER_SECOND);
        for (int tick = 1; tick <= revolution; tick++) {
            sonar.doTick(tick);
        }

        TowerEffectFrameBuilder builder = new TowerEffectFrameBuilder(BoardFixtures.SCALE, revolution + 6, 0.0, 0.0);
        sonar.accept(builder);

        List<RingDraw> rings = builder.build().stream().filter(RingDraw.class::isInstance).map(RingDraw.class::cast)
                .toList();
        assertThat(rings).extracting(RingDraw::palette)
                .containsExactlyInAnyOrder(Palette.TOWER_SONAR_PING, Palette.TOWER_SONAR_CROSSHAIR);
        assertThat(rings).filteredOn(ring -> ring.palette() == Palette.TOWER_SONAR_CROSSHAIR).singleElement()
                .satisfies(ring -> assertThat(ring.centerX()).isEqualTo((float) enemy.getX()));
        assertThat(beamsIn(builder.build(), Palette.TOWER_SONAR_CROSSHAIR)).hasSize(2);
    }

    @Test
    void anAttunedMortarRingsItsLastLandingAndTheRingDrawsInWithEachStep() {
        GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, 20, 20);
        context.economy().startEconomy(1000, 5);
        MortarTower mortar = new MortarTower(context, 3, 3);
        mortar.earnXp(1000);
        mortar.buyUpgrade(nodeNamed(mortar, "Attune"));
        FakeEnemyMob target = FakeEnemyMob.at(mortar.getX() + 3 * BoardFixtures.SCALE, mortar.getY());
        context.enemies().setEnemies(new EnemyMob[]{target});
        List<Float> radii = new ArrayList<>();

        for (int t = 1; t < 400 && radii.size() < 2; t++) {
            mortar.doTick(t);
            context.projectiles().doTick(t);
            if (mortar.getRangingMarker().isPresent() && mortar.getRangingMarker().get().step() == radii.size()) {
                TowerEffectFrameBuilder builder = new TowerEffectFrameBuilder(BoardFixtures.SCALE, t, 1.0, 0.0);
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

        TowerEffectFrameBuilder builder = new TowerEffectFrameBuilder(BoardFixtures.SCALE, 0, 0.0, 0.0);
        mortar.accept(builder);

        assertThat(builder.build()).isEmpty();
    }

    private static UpgradeNode nodeNamed(Tower tower, String name) {
        return tower.upgradeTree().nodes().stream().filter(node -> node.displayName().equals(name)).findFirst()
                .orElseThrow();
    }

    private static List<BeamDraw> beamsIn(List<TowerEffectDraw> draws, Palette palette) {
        return draws.stream()
                .filter(BeamDraw.class::isInstance)
                .map(BeamDraw.class::cast)
                .filter(beam -> beam.palette() == palette)
                .toList();
    }

    private static List<BeamDraw> linkBeamsIn(List<TowerEffectDraw> draws) {
        return draws.stream()
                .filter(BeamDraw.class::isInstance)
                .map(BeamDraw.class::cast)
                .filter(beam -> beam.palette() == Palette.TOWER_AURA_LINK)
                .toList();
    }
}

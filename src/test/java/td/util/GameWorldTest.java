package td.util;

import org.junit.jupiter.api.Test;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.TowerListener;
import td.wave.Wave;
import td.wave.WaveScript;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GameWorld is the shared mutable world (the tower list, the enemy roster, the
 * wave-start hub) that GameEngineTest exercises only incidentally through
 * gameplay flows. These tests pin down its own wiring directly: that each
 * listener family actually fires. Economy-specific behavior is covered by
 * EconomyLedgerTest, which GameWorld's doPay/apply/etc. delegate to.
 */
class GameWorldTest {

    private final RecordingGameHost host = new RecordingGameHost();
    private final GameWorld context = new GameWorld(host);

    @Test
    void addingATowerNotifiesTowerListeners() {
        List<Tower> built = new ArrayList<>();
        context.addTowerListener(new TowerListener() {
            @Override
            public void towerRemoved(Tower t) {
            }

            @Override
            public void towerBuild(Tower t) {
                built.add(t);
            }
        });
        Tower tower = TowerFactory.createTower(TowerFactory.type.first, context, 0, 0);

        context.addTower(tower);

        assertThat(built).containsExactly(tower);
    }

    @Test
    void sellingATowerNotifiesTowerListenersAndClearsItsCell() {
        List<Tower> removed = new ArrayList<>();
        context.addTowerListener(new TowerListener() {
            @Override
            public void towerRemoved(Tower t) {
                removed.add(t);
            }

            @Override
            public void towerBuild(Tower t) {
            }
        });
        Tower tower = TowerFactory.createTower(TowerFactory.type.first, context, 2, 3);
        context.addTower(tower);

        context.sellTower(tower);

        assertThat(removed).containsExactly(tower);
        assertThat(context.getTowers()).doesNotContain(tower);
        assertThat(host.lastClearedCell).containsExactly(2, 3);
    }

    @Test
    void startingAWaveNotifiesWaveStartListeners() {
        AtomicInteger waveStartedCalls = new AtomicInteger();
        context.addWaveStartListener(waveStartedCalls::incrementAndGet);
        Wave wave = new Wave(context, 100, 5, 1, WaveScript.parse("c c"));

        context.startWave(wave);

        assertThat(waveStartedCalls).hasValue(1);
    }

    @Test
    void removedTowerListenerStopsReceivingEvents() {
        List<Tower> built = new ArrayList<>();
        TowerListener listener = new TowerListener() {
            @Override
            public void towerRemoved(Tower t) {
            }

            @Override
            public void towerBuild(Tower t) {
                built.add(t);
            }
        };
        context.addTowerListener(listener);
        context.removeTowerListener(listener);

        context.addTower(TowerFactory.createTower(TowerFactory.type.first, context, 0, 0));

        assertThat(built).isEmpty();
    }
}

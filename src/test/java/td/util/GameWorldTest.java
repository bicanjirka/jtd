package td.util;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyCatalog;
import td.enemy.Rank;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.TowerListener;
import td.wave.Wave;
import td.wave.WaveScript;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class GameWorldTest {

    private final RecordingGameHost host = new RecordingGameHost();
    private final GameWorld context = new GameWorld(host);

    @Test
    void addingATowerNotifiesTowerListeners() {
        List<Tower> built = new ArrayList<>();
        context.towers().addListener(new TowerListener() {
            @Override
            public void towerRemoved(Tower t) {
            }

            @Override
            public void towerBuild(Tower t) {
                built.add(t);
            }
        });
        Tower tower = TowerFactory.createTower(TowerFactory.Type.SNIPER, context, 0, 0);

        context.towers().add(tower);

        assertThat(built).containsExactly(tower);
    }

    @Test
    void sellingATowerNotifiesTowerListenersAndClearsItsCell() {
        List<Tower> removed = new ArrayList<>();
        context.towers().addListener(new TowerListener() {
            @Override
            public void towerRemoved(Tower t) {
                removed.add(t);
            }

            @Override
            public void towerBuild(Tower t) {
            }
        });
        Tower tower = TowerFactory.createTower(TowerFactory.Type.SNIPER, context, 2, 3);
        context.towers().add(tower);

        context.towers().sell(tower);

        assertThat(removed).containsExactly(tower);
        assertThat(context.towers().all()).doesNotContain(tower);
        assertThat(host.lastClearedCell).containsExactly(2, 3);
    }

    @Test
    void startingAWaveNotifiesWaveStartListeners() {
        AtomicInteger waveStartedCalls = new AtomicInteger();
        context.waves().addListener(waveStartedCalls::incrementAndGet);
        Wave wave = new Wave(context, WaveScript.parse("c c", Rank.GRUNT, EnemyCatalog.builtIn()), 1L);

        context.startWave(List.of(wave));

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
        context.towers().addListener(listener);
        context.towers().removeListener(listener);

        context.towers().add(TowerFactory.createTower(TowerFactory.Type.SNIPER, context, 0, 0));

        assertThat(built).isEmpty();
    }
}

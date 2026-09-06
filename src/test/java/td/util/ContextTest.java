package td.util;

import org.junit.jupiter.api.Test;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.wave.Wave;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Context is the shared mutable world (credits/score/lives, the tower list,
 * the listener hub) that GameEngineTest exercises only incidentally through
 * gameplay flows. These tests pin down its own contract directly: payment
 * gating and that each listener family actually fires.
 */
class ContextTest {

    private final RecordingGameHost host = new RecordingGameHost();
    private final Context context = new Context(host);

    @Test
    void doPayChargesCreditsWhenAffordable() {
        context.setCredits(100);

        boolean paid = context.doPay(40);

        assertThat(paid).isTrue();
        assertThat(context.getCredits()).isEqualTo(60);
    }

    @Test
    void doPaySucceedsWhenAmountExactlyMatchesAvailableCredits() {
        context.setCredits(40);

        assertThat(context.doPay(40)).isTrue();
        assertThat(context.getCredits()).isZero();
    }

    @Test
    void doPayFailsAndLeavesCreditsUnchangedWhenTooExpensive() {
        context.setCredits(10);

        boolean paid = context.doPay(40);

        assertThat(paid).isFalse();
        assertThat(context.getCredits()).isEqualTo(10);
    }

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
        assertThat(context.towers).doesNotContain(tower);
        assertThat(host.lastClearedCell).containsExactly(2, 3);
    }

    @Test
    void removingALifeNotifiesContextListeners() {
        AtomicInteger livesChangedCalls = new AtomicInteger();
        context.addContextListener(new ContextListener() {
            @Override
            public void moneyChanged() {
            }

            @Override
            public void livesChanged() {
                livesChangedCalls.incrementAndGet();
            }
        });
        int livesBefore = context.getLives();

        context.removeLife();

        assertThat(context.getLives()).isEqualTo(livesBefore - 1);
        assertThat(livesChangedCalls).hasValue(1);
    }

    @Test
    void startingAWaveNotifiesWaveStartListeners() {
        AtomicInteger waveStartedCalls = new AtomicInteger();
        context.addWaveStartListener(waveStartedCalls::incrementAndGet);
        Wave wave = new Wave(context, 100, 5, 1);
        wave.addEnemiesFromNames(new String[]{"c", "c"});

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

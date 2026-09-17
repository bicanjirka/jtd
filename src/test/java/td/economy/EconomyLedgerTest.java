package td.economy;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins down EconomyLedger's own contract directly: payment gating, and that a mutation
 * fires exactly one EconomyListener notification (GameEngineTest exercises the same rules
 * only incidentally through gameplay flows).
 */
class EconomyLedgerTest {

    private final EconomyLedger ledger = new EconomyLedger();

    @Test
    void stateReportsCreditsScoreAndLivesFromOneSnapshot() {
        EconomyLedger ledger = new EconomyLedger();
        ledger.startEconomy(120, 4);

        ledger.apply(EconomyDelta.kill(7));

        EconomyState state = ledger.state();
        assertThat(state.credits()).isEqualTo(ledger.getCredits());
        assertThat(state.score()).isEqualTo(ledger.getScore());
        assertThat(state.lives()).isEqualTo(ledger.getLives());
    }

    @Test
    void doPayChargesCreditsWhenAffordable() {
        ledger.startEconomy(100, 5);

        boolean paid = ledger.doPay(40);

        assertThat(paid).isTrue();
        assertThat(ledger.getCredits()).isEqualTo(60);
    }

    @Test
    void doPaySucceedsWhenAmountExactlyMatchesAvailableCredits() {
        ledger.startEconomy(40, 5);

        assertThat(ledger.doPay(40)).isTrue();
        assertThat(ledger.getCredits()).isZero();
    }

    @Test
    void doPayFailsAndLeavesCreditsUnchangedWhenTooExpensive() {
        ledger.startEconomy(10, 5);

        boolean paid = ledger.doPay(40);

        assertThat(paid).isFalse();
        assertThat(ledger.getCredits()).isEqualTo(10);
    }

    @Test
    void applyingALeakNotifiesEconomyListenersExactlyOnce() {
        AtomicInteger economyChangedCalls = new AtomicInteger();
        ledger.addEconomyListener(state -> economyChangedCalls.incrementAndGet());
        int livesBefore = ledger.getLives();

        ledger.apply(EconomyDelta.leak(10));

        assertThat(ledger.getLives()).isEqualTo(livesBefore - 1);
        assertThat(economyChangedCalls).hasValue(1);
    }

    @Test
    void aKillAppliesCreditsAndScoreInASingleNotification() {
        List<EconomyState> notifications = new ArrayList<>();
        ledger.addEconomyListener(notifications::add);

        ledger.apply(EconomyDelta.kill(7));

        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).credits()).isEqualTo(7);
        assertThat(notifications.get(0).score()).isEqualTo(7);
    }

    @Test
    void scoreChangeNotifiesListeners() {
        AtomicInteger economyChangedCalls = new AtomicInteger();
        ledger.addEconomyListener(state -> economyChangedCalls.incrementAndGet());

        ledger.apply(EconomyDelta.score(5));

        assertThat(ledger.getScore()).isEqualTo(5);
        assertThat(economyChangedCalls).hasValue(1);
    }

    @Test
    void concurrentEconomyEventsDoNotLoseUpdates() throws InterruptedException {
        // apply() is a read-modify-write reached from both the EDT (a purchase) and the
        // game-loop thread (a kill or a leak). Without the lock this loses updates, and the
        // final total comes out short.
        int threads = 4;
        int eventsPerThread = 2000;
        CountDownLatch go = new CountDownLatch(1);
        List<Thread> workers = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Thread worker = new Thread(() -> {
                try {
                    go.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int e = 0; e < eventsPerThread; e++) {
                    ledger.apply(EconomyDelta.credits(1));
                }
            });
            workers.add(worker);
            worker.start();
        }

        go.countDown();
        for (Thread worker : workers) {
            worker.join(10_000);
        }

        assertThat(ledger.getCredits()).isEqualTo(threads * eventsPerThread);
    }

    @Test
    void removedListenerStopsReceivingEvents() {
        AtomicInteger economyChangedCalls = new AtomicInteger();
        EconomyListener listener = state -> economyChangedCalls.incrementAndGet();
        ledger.addEconomyListener(listener);
        ledger.removeEconomyListener(listener);

        ledger.apply(EconomyDelta.score(5));

        assertThat(economyChangedCalls).hasValue(0);
    }
}

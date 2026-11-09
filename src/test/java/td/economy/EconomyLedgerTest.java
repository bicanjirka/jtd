package td.economy;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

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
    void aRefusedPaymentTellsTheListenersOnceAndChangesNothing() {
        List<String> heard = new ArrayList<>();
        ledger.addEconomyListener(new EconomyListener() {
            @Override
            public void economyChanged(EconomyState state) {
                heard.add("changed");
            }

            @Override
            public void paymentRefused() {
                heard.add("refused");
            }
        });
        int before = ledger.getCredits();

        ledger.doPay(before + 1);

        assertThat(heard).containsExactly("refused");
        assertThat(ledger.getCredits()).isEqualTo(before);
    }

    @Test
    void duringFreeBuildAPaymentBeyondTheCreditsGoesThroughUnchargedAndNotifiesLikeAnyPurchase() {
        List<String> heard = new ArrayList<>();
        ledger.startEconomy(10, 5);
        ledger.setFreeBuild(true);
        ledger.addEconomyListener(new EconomyListener() {
            @Override
            public void economyChanged(EconomyState state) {
                heard.add("changed");
            }

            @Override
            public void paymentRefused() {
                heard.add("refused");
            }
        });

        boolean payable = ledger.canPay(40);
        boolean paid = ledger.doPay(40);

        assertThat(payable).isTrue();
        assertThat(paid).isTrue();
        assertThat(ledger.getCredits()).isEqualTo(10);
        assertThat(heard).containsExactly("changed");
    }

    @Test
    void aPriceIsShortOfWhatTheCreditsLackAndOfNothingWhenCoveredOrDuringFreeBuild() {
        ledger.startEconomy(10, 5);

        int lacking = ledger.shortOf(40);
        int covered = ledger.shortOf(10);
        ledger.setFreeBuild(true);

        assertThat(lacking).isEqualTo(30);
        assertThat(covered).isZero();
        assertThat(ledger.shortOf(40)).isZero();
    }

    @Test
    void refundsAreOwedInFullExceptDuringFreeBuild() {
        Refund owed = new Refund(40, 30);
        Refund normally = ledger.refundOf(owed);

        ledger.setFreeBuild(true);

        assertThat(normally).isEqualTo(owed);
        assertThat(ledger.refundOf(owed)).isEqualTo(Refund.none());
    }

    @Test
    void turningFreeBuildOffChargesAgainAndTheCreditsAreAsTheyWere() {
        ledger.startEconomy(100, 5);
        ledger.setFreeBuild(true);
        ledger.doPay(40);
        ledger.apply(EconomyDelta.kill(7));

        ledger.setFreeBuild(false);
        boolean paid = ledger.doPay(40);

        assertThat(paid).isTrue();
        assertThat(ledger.getCredits()).isEqualTo(67);
    }

    @Test
    void aLevelStartingLeavesFreeBuildOn() {
        ledger.setFreeBuild(true);

        ledger.startEconomy(0, 5);

        assertThat(ledger.freeBuild()).isTrue();
    }

    @Test
    void switchingFreeBuildTellsTheListenersSoPricesShowAgain() {
        List<EconomyState> heard = new ArrayList<>();
        ledger.addEconomyListener(heard::add);

        ledger.setFreeBuild(true);

        assertThat(heard).containsExactly(ledger.state());
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
        assertThat(notifications.getFirst().credits()).isEqualTo(7);
        assertThat(notifications.getFirst().score()).isEqualTo(7);
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
}

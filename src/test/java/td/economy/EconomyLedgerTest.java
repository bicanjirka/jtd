package td.economy;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
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
    void removedListenerStopsReceivingEvents() {
        AtomicInteger economyChangedCalls = new AtomicInteger();
        EconomyListener listener = state -> economyChangedCalls.incrementAndGet();
        ledger.addEconomyListener(listener);
        ledger.removeEconomyListener(listener);

        ledger.apply(EconomyDelta.score(5));

        assertThat(economyChangedCalls).hasValue(0);
    }
}

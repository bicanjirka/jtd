package td.effect;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EffectTransitionsTest {

    @Test
    void aKindNeverObservedHasNoGainOrLossTick() {
        EffectTransitions transitions = new EffectTransitions();

        assertThat(transitions.ticksSinceGained(EffectKind.SLOW, 10)).isEqualTo(-1);
        assertThat(transitions.ticksSinceLost(EffectKind.SLOW, 10)).isEqualTo(-1);
    }

    @Test
    void aKindPresentOnTheFirstObservationCountsAsGainedThatTick() {
        EffectTransitions transitions = new EffectTransitions();

        transitions.observe(Set.of(EffectKind.SLOW), 5);

        assertThat(transitions.ticksSinceGained(EffectKind.SLOW, 5)).isZero();
        assertThat(transitions.ticksSinceGained(EffectKind.SLOW, 8)).isEqualTo(3);
    }

    @Test
    void aKindMissingFromASubsequentObservationCountsAsLostThatTick() {
        EffectTransitions transitions = new EffectTransitions();
        transitions.observe(Set.of(EffectKind.SHIELD), 1);

        transitions.observe(Set.of(), 4);

        assertThat(transitions.ticksSinceLost(EffectKind.SHIELD, 4)).isZero();
        assertThat(transitions.ticksSinceLost(EffectKind.SHIELD, 9)).isEqualTo(5);
    }

    @Test
    void aKindStillActiveAcrossObservationsRecordsNoNewGain() {
        EffectTransitions transitions = new EffectTransitions();
        transitions.observe(Set.of(EffectKind.INVISIBLE), 1);

        transitions.observe(Set.of(EffectKind.INVISIBLE), 2);
        transitions.observe(Set.of(EffectKind.INVISIBLE), 3);

        assertThat(transitions.ticksSinceGained(EffectKind.INVISIBLE, 3)).isEqualTo(2);
    }

    @Test
    void regainingAKindAfterLosingItRecordsANewGainTick() {
        EffectTransitions transitions = new EffectTransitions();
        transitions.observe(Set.of(EffectKind.BURN), 1);
        transitions.observe(Set.of(), 2);

        transitions.observe(Set.of(EffectKind.BURN), 5);

        assertThat(transitions.ticksSinceGained(EffectKind.BURN, 5)).isZero();
        assertThat(transitions.ticksSinceLost(EffectKind.BURN, 5)).isEqualTo(3);
    }

    @Test
    void independentKindsAreTrackedSeparately() {
        EffectTransitions transitions = new EffectTransitions();

        transitions.observe(EnumSet.of(EffectKind.SLOW, EffectKind.BURN), 1);
        transitions.observe(EnumSet.of(EffectKind.BURN), 2); // SLOW lost, BURN still active

        assertThat(transitions.ticksSinceLost(EffectKind.SLOW, 2)).isZero();
        assertThat(transitions.ticksSinceLost(EffectKind.BURN, 2)).isEqualTo(-1);
        assertThat(transitions.ticksSinceGained(EffectKind.BURN, 2)).isEqualTo(1);
    }
}

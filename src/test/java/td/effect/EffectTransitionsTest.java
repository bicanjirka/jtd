package td.effect;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EffectTransitionsTest {

    @Test
    void aKindNeverObservedHasNoGainOrLossTick() {
        EffectTransitions transitions = new EffectTransitions();

        assertThat(transitions.ticksSinceGained(EffectKind.CHILL, 10)).isEmpty();
        assertThat(transitions.ticksSinceLost(EffectKind.CHILL, 10)).isEmpty();
    }

    @Test
    void aKindPresentOnTheFirstObservationCountsAsGainedThatTick() {
        EffectTransitions transitions = new EffectTransitions();

        transitions.observe(Set.of(EffectKind.CHILL), 5);

        assertThat(transitions.ticksSinceGained(EffectKind.CHILL, 5)).hasValue(0);
        assertThat(transitions.ticksSinceGained(EffectKind.CHILL, 8)).hasValue(3);
    }

    @Test
    void aKindMissingFromASubsequentObservationCountsAsLostThatTick() {
        EffectTransitions transitions = new EffectTransitions();
        transitions.observe(Set.of(EffectKind.SHIELD), 1);

        transitions.observe(Set.of(), 4);

        assertThat(transitions.ticksSinceLost(EffectKind.SHIELD, 4)).hasValue(0);
        assertThat(transitions.ticksSinceLost(EffectKind.SHIELD, 9)).hasValue(5);
    }

    @Test
    void aKindStillActiveAcrossObservationsRecordsNoNewGain() {
        EffectTransitions transitions = new EffectTransitions();
        transitions.observe(Set.of(EffectKind.INVISIBLE), 1);

        transitions.observe(Set.of(EffectKind.INVISIBLE), 2);
        transitions.observe(Set.of(EffectKind.INVISIBLE), 3);

        assertThat(transitions.ticksSinceGained(EffectKind.INVISIBLE, 3)).hasValue(2);
    }

    @Test
    void regainingAKindAfterLosingItRecordsANewGainTick() {
        EffectTransitions transitions = new EffectTransitions();
        transitions.observe(Set.of(EffectKind.BURN), 1);
        transitions.observe(Set.of(), 2);

        transitions.observe(Set.of(EffectKind.BURN), 5);

        assertThat(transitions.ticksSinceGained(EffectKind.BURN, 5)).hasValue(0);
        assertThat(transitions.ticksSinceLost(EffectKind.BURN, 5)).hasValue(3);
    }

    @Test
    void independentKindsAreTrackedSeparately() {
        EffectTransitions transitions = new EffectTransitions();

        transitions.observe(EnumSet.of(EffectKind.CHILL, EffectKind.BURN), 1);
        transitions.observe(EnumSet.of(EffectKind.BURN), 2); // SLOW lost, BURN still active

        assertThat(transitions.ticksSinceLost(EffectKind.CHILL, 2)).hasValue(0);
        assertThat(transitions.ticksSinceLost(EffectKind.BURN, 2)).isEmpty();
        assertThat(transitions.ticksSinceGained(EffectKind.BURN, 2)).hasValue(1);
    }
}

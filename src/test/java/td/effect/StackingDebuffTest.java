package td.effect;

import org.junit.jupiter.api.Test;
import td.util.TickRate;

import static org.assertj.core.api.Assertions.assertThat;

class StackingDebuffTest {

    @Test
    void everyDebuffAddsTheAskedStacksOfItsOwnKind() {
        for (StackingDebuff debuff : StackingDebuff.values()) {
            Effect effect = debuff.effect(2, DamageSink.none());

            assertThat(effect.kind()).as(debuff.name()).isEqualTo(debuff.kind());
            assertThat(effect.stacks()).as(debuff.name()).isEqualTo(2);
        }
    }

    @Test
    void everyDebuffStacksOnItsKindsSharedClock() {
        for (StackingDebuff debuff : StackingDebuff.values()) {
            assertThat(debuff.kind().maxStacks()).as(debuff.name()).isGreaterThan(0);
            assertThat(debuff.effect(1, DamageSink.none()).remainingTicks()).as(debuff.name()).isGreaterThan(0);
        }
    }

    @Test
    void vulnerableAndResonanceLastFourSecondsAndSunderAndUnravelFive() {
        assertThat(secondsOf(StackingDebuff.VULNERABLE)).isEqualTo(4f);
        assertThat(secondsOf(StackingDebuff.RESONATING)).isEqualTo(4f);
        assertThat(secondsOf(StackingDebuff.SUNDERED)).isEqualTo(5f);
        assertThat(secondsOf(StackingDebuff.UNRAVELED)).isEqualTo(5f);
    }

    private static float secondsOf(StackingDebuff debuff) {
        return debuff.effect(1, DamageSink.none()).remainingTicks() / (float) TickRate.TICKS_PER_SECOND;
    }
}

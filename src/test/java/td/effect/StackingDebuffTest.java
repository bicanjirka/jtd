package td.effect;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StackingDebuffTest {

    @Test
    void everyDebuffStacksOnItsKindsSharedClock() {
        for (StackingDebuff debuff : StackingDebuff.values()) {
            assertThat(debuff.kind().maxStacks()).as(debuff.name()).isGreaterThan(0);
            assertThat(debuff.effect(1, DamageSink.none()).remainingTicks()).as(debuff.name()).isGreaterThan(0);
        }
    }
}

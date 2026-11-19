package td.effect;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StackingDebuffTest {

    @Test
    void everyDebuffIsASharedCountWithAHold() {
        for (StackingDebuff debuff : StackingDebuff.values()) {
            assertThat(debuff.kind().isCount()).as(debuff.name()).isTrue();
            assertThat(debuff.effect(1).remainingTicks()).as(debuff.name()).isEqualTo(Effect.HOLD_TICKS);
        }
    }
}

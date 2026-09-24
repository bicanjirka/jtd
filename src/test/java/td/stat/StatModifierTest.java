package td.stat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class StatModifierTest {

    @Test
    void noneIsTheIdentityOfPlus() {
        StatModifier modifier = new StatModifier(3f, 0.2f, 0.5f, 7f);

        assertThat(modifier.plus(StatModifier.none())).isEqualTo(modifier);
        assertThat(StatModifier.none().plus(modifier)).isEqualTo(modifier);
    }

    @Test
    void plusAddsFlatsAndPercentagesAndMultipliesMultipliers() {
        StatModifier combined = StatModifier.flat(10f).plus(StatModifier.flat(5f))
                .plus(StatModifier.percent(0.2f)).plus(StatModifier.percent(0.3f))
                .plus(StatModifier.times(0.5f)).plus(StatModifier.times(0.5f));

        assertThat(combined.applyTo(100f)).isCloseTo((100f + 15f) * 1.5f * 0.25f, within(0.001f));
    }

    @Test
    void theLowerSetValueWinsAndOverridesEverythingElse() {
        StatModifier combined = StatModifier.setTo(1f).plus(StatModifier.setTo(0f)).plus(StatModifier.flat(50f));

        assertThat(combined.hasSetValue()).isTrue();
        assertThat(combined.applyTo(100f)).isZero();
    }

    @Test
    void noneLeavesABaseUnchanged() {
        assertThat(StatModifier.none().hasSetValue()).isFalse();
        assertThat(StatModifier.none().applyTo(42f)).isEqualTo(42f);
    }

    @Test
    void bundlesCombineStatByStat() {
        StatModifiers a = StatModifiers.of(EnemyStat.ARMOR, StatModifier.flat(10f));
        StatModifiers b = StatModifiers.of(EnemyStat.ARMOR, StatModifier.flat(5f))
                .and(EnemyStat.SPIRIT, StatModifier.flat(20f));

        StatModifiers combined = a.plus(b);

        assertThat(combined.get(EnemyStat.ARMOR)).isEqualTo(StatModifier.flat(15f));
        assertThat(combined.get(EnemyStat.SPIRIT)).isEqualTo(StatModifier.flat(20f));
        assertThat(combined.get(EnemyStat.MOVE_SPEED)).isEqualTo(StatModifier.none());
        assertThat(combined.plus(StatModifiers.none())).isEqualTo(combined);
    }
}
